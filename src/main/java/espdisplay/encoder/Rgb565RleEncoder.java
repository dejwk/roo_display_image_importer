package espdisplay.encoder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Logger;

public class Rgb565RleEncoder extends Encoder {
  private OutputStream os;
  private Deque<Integer> deque = new ArrayDeque<>();
  private int runLength = 0;

  private static final int kMaxRunLength = 64 * 256;
  private static final int kMaxAbsLength = 64 * 256;

  public Rgb565RleEncoder(OutputStream os) {
    this.os = os;
  }

  public void encodePixel(int pixel) throws IOException {
    int encoded = ((pixel >> 8) & 0xF800) | ((pixel >> 5) & 0x07E0) | ((pixel >> 3) & 0x1F);
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
          if (runLength >= kMaxRunLength) {
            emitRun(runLength);
            runLength = 0;
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
        if (deque.size() >= kMaxAbsLength) {
          emitAbsolute(deque.size());
        }
      }
    }
  }

  private void emitAbsolute(int count) throws IOException {
    if (count <= 64) {
      os.write(0x00 | (count - 1));
    } else {
      os.write(0x40 | ((count - 1) >> 8));
      os.write((count - 1) & 0xFF);
    }
    for (int i = 0; i < count; ++i) {
      int value = deque.removeFirst();
      os.write((value >> 8) & 0xFF);
      os.write(value & 0xFF);
    }
  }

  private void emitRun(int count) throws IOException {
    if (count <= 64) {
      os.write(0x80 | (count - 1));
    } else {
      os.write(0xC0 | ((count - 1) >> 8));
      os.write((count - 1) & 0xFF);
    }
    int checkValue = 0;
    for (int i = 0; i < count; ++i) {
      int value = deque.removeFirst();
      if (i == 0) {
        checkValue = value;
        os.write((value >> 8) & 0xFF);
        os.write(value & 0xFF);
      } else if (value != checkValue) {
        throw new RuntimeException(
            "After emitting " + i + " out of " + count + " values: saw " + value + " while expected " + checkValue);
      }
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
