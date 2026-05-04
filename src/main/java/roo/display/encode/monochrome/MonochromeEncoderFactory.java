package roo.display.encode.monochrome;

import java.io.OutputStream;
import roo.display.encode.*;

public class MonochromeEncoderFactory implements EncoderFactory {
  public MonochromeEncoderFactory() {}

  public Encoder create(boolean rle, OutputStream os) {
    if (rle) {
      throw new IllegalArgumentException("Monochrome encoder doesn't support RLE");
    }
    return new MonochromeEncoder(os);
  }
}
