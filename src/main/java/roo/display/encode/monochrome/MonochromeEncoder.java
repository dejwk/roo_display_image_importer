package roo.display.encode.monochrome;

import java.io.IOException;
import java.io.OutputStream;
import roo.display.encode.*;

public class MonochromeEncoder extends Encoder {
  private BitWriter os;
  private int fg;

  public MonochromeEncoder(OutputStream os, int fg) {
    this.os = new BitWriter(os);
    this.fg = fg;
  }

  public void encodePixel(int pixel) throws IOException {
    os.write(pixel == fg);
  }

  public void close() throws IOException {
    os.close();
  }
}
