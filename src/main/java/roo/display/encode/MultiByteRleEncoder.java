package roo.display.encode;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class MultiByteRleEncoder extends Encoder {
  private PixelEncoder pixelEncoder;
  private OutputStream os;
  int bytesPerPixel;
  private Deque<Integer> deque = new ArrayDeque<>();
  private int runLength = 0;

  public MultiByteRleEncoder(PixelEncoder pixelEncoder, OutputStream os) {
    this.pixelEncoder = pixelEncoder;
    this.os = os;
    int bitsPerPixel = pixelEncoder.bitsPerPixel();
    if ((bitsPerPixel % 8) != 0) {
      throw new IllegalArgumentException("Unsupported: " + bitsPerPixel);
    }
    this.bytesPerPixel = bitsPerPixel / 8;
  }

  public void encodePixel(int pixel) throws IOException {
    int encoded = pixelEncoder.encodePixel(pixel);
    if (deque.isEmpty()) {
      deque.addLast(encoded);
      runLength = 1;
    } else {
      if (deque.getLast() == encoded) {
        // Continued run.
        deque.addLast(encoded);
        ++runLength;
        if (runLength >= 2) {
          // Will make sense to emit this run. For now, flush any preceding stuff.
          if (deque.size() > runLength) {
            // Has non-run-length head; emit it now.
            emitAbsolute(deque.size() - runLength);
          }
        }
      } else {
        // Broken run.
        if (runLength >= 2) {
          // Emit the preceding run.
          if (deque.size() != runLength) {
            throw new RuntimeException();
          }
          emitRun(runLength);
        }
        deque.addLast(encoded);
        runLength = 1;
      }
    }
  }

  public boolean isPixelVisible(int argb) {
    return pixelEncoder.isPixelVisible(argb);
  }

  private void emitAbsolute(int count) throws IOException {
    emitVarIntRecursive(false, count - 1, false);
    for (int i = 0; i < count; ++i) {
      int value = deque.removeFirst();
      emitPixel(value);
    }
  }

  private void emitRun(int count) throws IOException {
    emitVarIntRecursive(true, count - 1, false);
    int checkValue = 0;
    for (int i = 0; i < count; ++i) {
      int value = deque.removeFirst();
      if (i == 0) {
        checkValue = value;
        emitPixel(value);
      } else if (value != checkValue) {
        throw new RuntimeException(
            "After emitting " + i + " out of " + count + " values: saw " + value + " while expected " + checkValue);
      }
    }
  }

  private void emitVarIntRecursive(boolean run, int value, boolean carryOver) throws IOException {
    if (value < 0x40) {
      os.write((run ? 0x80 : 0x00) | (carryOver ? 0x40 : 0x00) | value);
    } else {
      emitVarIntRecursive(run, value / 0x80, true);
      os.write((carryOver ? 0x80 : 0x00) | (value % 0x80));
    }
  }

  private void emitPixel(int encoded) throws IOException {
    for (int i = bytesPerPixel - 1; i >= 0; i--) {
      os.write((encoded >> (i * 8)) & 0xFF);
    }
  }

  public void close() throws IOException {
    if (runLength >= 2) {
      emitRun(runLength);
    } else {
      emitAbsolute(deque.size());
    }
    if (!deque.isEmpty()) {
      throw new AssertionError();
    }
  }
}
