package roo.display.encode;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Logger;

public class BitWriter {
  private OutputStream os;
  int bitIndex;
  int buffer;

  BitWriter(OutputStream os) {
      this.os = os;
      bitIndex = 0;
      buffer = 0;
  }

  void write(boolean bit) throws IOException {
    buffer >>= 1;
    buffer |= (bit ? 0x80 : 0);
    bitIndex++;
    if (bitIndex == 8) {
      os.write(buffer);
      bitIndex = 0;
    }
  }

  void close() throws IOException {
    if (bitIndex > 0) {
      buffer >>= (8 - bitIndex);
      os.write(buffer);
    }
    os.close();
  }
}
