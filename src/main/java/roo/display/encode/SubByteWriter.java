package roo.display.encode;

import java.io.IOException;
import java.io.OutputStream;

public class SubByteWriter {
  private final int bits;
  private final boolean msb;

  private OutputStream os;
  int pos;
  int buffer;


  public SubByteWriter(OutputStream os, int bits, boolean msb) {
      if (bits != 1 && bits != 2 && bits != 4) {
        throw new IllegalArgumentException("Bits not adds up to a byte");
      }
      this.os = os;
      this.bits = bits;
      this.msb = msb;
      pos = 0;
      buffer = 0;
  }

  public void write(int data) throws IOException {
    if (data < 0 || data >= (1 << bits)) {
        throw new IllegalArgumentException();
    }
    if (msb) {
      buffer <<= bits;
      buffer |= data;
    } else {
      buffer >>= bits;
      buffer |= data << (8 - bits);
    }
    pos += bits;
    if (pos == 8) {
      os.write(buffer);
      pos = 0;
    }
  }

  public void close() throws IOException {
    while (pos != 0) { write(0); }
    os.close();
  }
}
