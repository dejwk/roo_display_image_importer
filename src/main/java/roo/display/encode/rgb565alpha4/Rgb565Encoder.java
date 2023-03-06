package roo.display.encode;

import java.io.IOException;
import java.io.OutputStream;

public class Rgb565Encoder extends Encoder {
  private OutputStream os;

  public Rgb565Encoder(OutputStream os) {
    this.os = os;
  }

  public void encodePixel(int pixel) throws IOException {
    int encoded = encode565(pixel);
    os.write(encoded >> 8);
    os.write(encoded & 0xFF);
  }

  public boolean isPixelVisible(int argb) {
    return true;
  }

  public void close() throws IOException {
    os.close();
  }

  private static int encode565(int pixel) {
    return ((pixel >> 8) & 0xF800) | ((pixel >> 5) & 0x07E0) | ((pixel >> 3) & 0x1F);
  }
}
