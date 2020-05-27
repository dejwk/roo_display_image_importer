package roo.display.encode.alpha4;

import java.io.IOException;
import java.io.OutputStream;
import roo.display.encode.*;

public class PlainAlpha4Encoder extends Encoder {
  private HalfByteWriter os;

  public PlainAlpha4Encoder(OutputStream os) {
    this.os = new HalfByteWriter(os);
  }

  public void encodePixel(int pixel) throws IOException {
    int trunc = (pixel >> 24) & 0xFF;
    int alpha = (trunc - (trunc >> 5)) >> 4;
    os.write(alpha);
  }

  public void close() throws IOException {
    os.close();
  }
}
