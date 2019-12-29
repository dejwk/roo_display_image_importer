package roo.display.encode;

import java.io.IOException;
import java.io.OutputStream;

class Rle4Encoder extends Encoder {
    private OutputStream os;

public Rle4Encoder(OutputStream os) {
    this.os = os;
}
  public void encodePixel(int pixel) throws IOException {
      os.write((pixel >> 24) & 0xFF);  // a
      os.write((pixel >> 16) & 0xFF);  // r
      os.write((pixel >> 8) & 0xFF);  // g
      os.write(pixel & 0xFF);  // b
  }

  public void close() {}
}
