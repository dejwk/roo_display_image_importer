package roo.display.encode;

import java.io.IOException;
import java.io.OutputStream;

public class MultiByteRasterEncoder extends Encoder {
  private PixelEncoder pixelEncoder;
  private OutputStream os;
  int bytesPerPixel;

  public MultiByteRasterEncoder(PixelEncoder pixelEncoder, OutputStream os) {
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
    for (int i = bytesPerPixel - 1; i >= 0; i--) {
      os.write((encoded >> (i * 8)) & 0xFF);
    }
  }

  public void close() throws IOException {
    os.close();
  }
}
