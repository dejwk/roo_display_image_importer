package roo.display.encode;

import java.io.IOException;
import java.io.OutputStream;

public class HalfByteWriter {
  private OutputStream os;
  boolean hasHalfByte;
  int buffer;

  public HalfByteWriter(OutputStream os) {
      this.os = os;
      hasHalfByte = false;
      buffer = 0;
  }

  public void write(int halfByte) throws IOException {
    if (halfByte < 0 || halfByte > 0xF) {
      throw new IllegalArgumentException();
    }
    if (hasHalfByte) {
      os.write(buffer | halfByte);
      hasHalfByte = false;
    } else {
      buffer = halfByte << 4;
      hasHalfByte = true;
    }
  }

  public void close() throws IOException {
    if (hasHalfByte) {
      os.write(buffer);
    }
    os.close();
  }
}
