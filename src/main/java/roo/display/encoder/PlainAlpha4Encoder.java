package roo.display.encoder;

import java.io.IOException;
import java.io.OutputStream;

public class PlainAlpha4Encoder extends Encoder {
  private HalfByteWriter os;

  public PlainAlpha4Encoder(OutputStream os) {
    this.os = new HalfByteWriter(os);
  }

  public void encodePixel(int pixel) throws IOException {
    int alpha = (pixel >> 28) & 0xF;
    os.write(alpha);
  }

  public void close() throws IOException {
    os.close();
  }
}
