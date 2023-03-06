package roo.display.encode.alpha8;

import roo.display.encode.*;
import java.io.OutputStream;

public class Alpha8EncoderFactory implements EncoderFactory {
  public Alpha8EncoderFactory() {
  }

  public Encoder create(boolean rle, OutputStream os) {
    return MultiByteEncoderFactory.create(rle, new PixelEncoder() {
      public int bitsPerPixel() {
        return 8;
      }
    
      public int encodePixel(int argb) {
        return argb >> 24;
      }

      public boolean isPixelVisible(int argb) {
        return (argb >>> 24) != 0;
      }
    }, os);
  }
}
