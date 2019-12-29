package roo.display.encode.monochrome;

import roo.display.encode.*;
import java.io.OutputStream;

public class MonochromeEncoderFactory implements EncoderFactory {
  public MonochromeEncoderFactory() {
  }

  public Encoder create(boolean rle, OutputStream os) {
    if (rle) {
      throw new IllegalArgumentException("Monochrome encoder doesn't support RLE");
    }
    return new MonochromeEncoder(os, 0xFF000000);
  }
}
