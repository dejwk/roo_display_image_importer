package roo.display.encode.alpha4;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import roo.display.encode.*;

import roo.display.encode.alpha4.RleAcummulator.Entry;

/**
 * Optimized for anti-aliased monochrome content with 4-bit alpha. Runs of
 * extreme values (0x0 and 0xF, i.e. fully opaque and fully transparent) are
 * encoded very efficiently (aiming at the cost of ~1 bit per value). Runs of
 * intermediate values are not encoded nearly as efficiently.
 *
 * Specifically:
 * <ul>
 * <li>A in {0x1 - 0x7}: a run of '0's, count A
 * <li>A in {0x9 - 0xF}: a run of 'F's, count A - 8
 * <li>A[] = {0x0 <V>}, V in {0x1 - 0xE}: a single V
 * <li>A[] = {0x0 0x0 <V>}, V in {0x1 - 0xE}: a run of 2 Vs
 * <li>A[] = {0x0 0xF <V>}, V in {0x1 - 0xE}: a run of 3 Vs
 * <li>A[] = {0x8 0x0 <X = varint...> <V>}: run of X+4 Vs
 * <li>A[] = {0x8 <X = varint...> <values>... }, list of X+2 arbitrary values
 * </ul>
 */
public class Alpha4AntiAliasRleEncoder extends Encoder {
  private HalfByteWriter os;
  ArrayDeque<Entry> deque;
  RleAcummulator acummulator;
  int absEncodingHighMark;

  public Alpha4AntiAliasRleEncoder(OutputStream os) {
    this.os = new HalfByteWriter(os);
    this.deque = new ArrayDeque<>();
    this.acummulator = new RleAcummulator();
    this.absEncodingHighMark = 0;
  }

  public void encodePixel(int pixel) throws IOException {
    int alpha = (pixel >> 28) & 0xF;
    Entry entry = acummulator.add(alpha);
    if (entry != null) {
      // Deque has a new run.
      processNewRun(entry);
    }
  }

  public void close() throws IOException {
    Entry entry = acummulator.close();
    if (entry != null) {
      // Deque has a new run.
      processNewRun(entry);
    }
    emitDeque();
    os.close();
  }

  private void processNewRun(Entry entry) throws IOException {
    if (deque.size() == 0) {
      // No prior acummulated state to consider for the 'absolute' mode. In this
      // state, it pays off to emit runs of any size (including 1) for 0x0 and
      // 0xF, and runs of size >= 3 for remaining pixel values.
      if (entry.value == 0x0 || entry.value == 0xF || entry.count >= 3) {
        emitRun(entry.value, entry.count);
      } else {
        // Otherwise, we will push the new entry in the deque, to decide on
        // later.
        deque.addLast(entry);
      }
    } else {
      // There is some prior state to perhaps push as an absolute sequence.
      // Let's see if we have a signal to terminate that sequence. That
      // definitely happens if we see a run of at least two 0x0 or 0xF, or a run
      // of 6+ other values. These counts follow from the math on the
      // cost/benefit of the various encodings above; e.g. terminating and
      // restarting a list costs two half-bytes, and encoding a run costs at
      // least 4 half-bytes, so at the length of 6 we begin to get even. For 0x0
      // and 0xF, the math is different because they only cost 1 half-byte.
      //
      // A single run of 5, in the middle of short runs, is a loss if we
      // RLE-encode. But, consecutive runs of 5s are a win. Finding the absolute
      // optimum is computationally expensive. We go by heuristics, assuming
      // that runs of 5 (representing things like anti-aliased horizontal lines)
      // are often neighboring other runs of 5, so it is likely a net win to
      // RLE-encode any run of 5.
      if (entry.count >= 5 || ((entry.value == 0x0 || entry.value == 0xF) && entry.count >= 2)) {
        // We need to emit the acummulated stuff.
        emitDeque();
        emitRun(entry.value, entry.count);
      } else {
        // Continue the sequence, and if the run was shorter than 3, mark
        // the whole thing as subject to absolute encoding.
        deque.addLast(entry);
        if (entry.value != 0x0 && entry.value != 0xF && entry.count < 3) {
          absEncodingHighMark = deque.size();
        }
      }
    }
  }

  private void emitVarInt(int value) throws IOException {
    if (value < 0) {
      throw new IllegalArgumentException("Varint can be negative; got: " + value);
    }
    emitVarIntRecursive(value, false);
  }

  private void emitVarIntRecursive(int value, boolean carryOver) throws IOException {
    if (value >= 8) {
      emitVarIntRecursive(value / 8, true);
      value %= 8;
    }
    if (carryOver)
      value |= 8;
    os.write(value);
  }

  /**
   * <ul>
   * <li>A[] = {0x8 0x0 <X = varint...> <V>}: run of X+4 Vs
   * </ul>
   */
  private void emitGenericRun(int value, int count) throws IOException {
    if (count < 4) {
      throw new IllegalArgumentException("Too low count: " + count + "; expecting at least 4");
    }
    os.write(0x8);
    os.write(0x0);
    emitVarInt(count - 4);
    os.write(value);
  }

  private void emitRun(int value, int count) throws IOException {
    if (count > 5 * 7) {
      // At this length, even extreme values are cheaper to encode using the
      // generic encoding, which will take 5+ bytes.
      emitGenericRun(value, count);
      return;
    }
    if (value == 0x0) {
      // A in {0x1 - 0x7}: a run of '0's, count A
      while (count >= 7) {
        os.write(0x7);
        count -= 7;
      }
      if (count > 0) os.write(count);
      return;
    }
    if (value == 0xF) {
      // A in {0x9 - 0xF}: a run of 'F's, count A - 8
      while (count >= 7) {
        os.write(0xF);
        count -= 7;
      }
      if (count > 0) os.write(count | 0x8);
      return;
    }
    // We have an in-between value. Check special cases.
    if (count == 1) {
      // A[] = {0x0 <V>}, V in {0x1 - 0xE}: a single V
      os.write(0x0);
      os.write(value);
    } else if (count == 2) {
      // A[] = {0x0 0x0 <V>}, V in {0x1 - 0xE}: a run of 2 Vs
      os.write(0x0);
      os.write(0x0);
      os.write(value);
      return;
    } else if (count == 3) {
      // A[] = {0x0 0xF <V>}, V in {0x1 - 0xE}: a run of 3 Vs
      os.write(0x0);
      os.write(0xF);
      os.write(value);
      return;
    } else {
      emitGenericRun(value, count);
    }
  }

  private void emitAbsolute(int entryCount) throws IOException {
    if (entryCount > deque.size()) {
      throw new IllegalArgumentException("Out of bounds: " + entryCount + ", vs " + deque.size());
    }
    Iterator<Entry> itr = deque.iterator();
    int sumLengths = 0;
    for (int i = 0; i < entryCount; ++i) {
      sumLengths += itr.next().count;
    }

    // First, check the corner case of tiny lists, which need to be RLE-encoded
    // (This can take up to 4 half-bytes, which is still not worse than absolute
    // encoding, and can be better, in case of a single run of 2)
    if (sumLengths < 3) {
      for (int i = 0; i < entryCount; ++i) {
        Entry entry = deque.removeFirst();
        emitRun(entry.value, entry.count);
      }
    } else {
      // Proper list
      // A[] = {0x8 <X = varint...> <values>... }, list of X+2 arbitrary values
      os.write(0x8);
      emitVarInt(sumLengths - 2);
      for (int i = 0; i < entryCount; ++i) {
        Entry entry = deque.removeFirst();
        for (int j = 0; j < entry.count; ++j) {
          os.write(entry.value);
        }
      }
    }
  }

  private void emitDeque() throws IOException {
    emitAbsolute(absEncodingHighMark);
    absEncodingHighMark = 0;
    while (!deque.isEmpty()) {
      Entry entry = deque.removeFirst();
      emitRun(entry.value, entry.count);
    }
  }
}
