package roo.display.encode.argb8888;

import roo.display.encode.*;
import java.io.OutputStream;

public class Argb8888EncoderFactory implements EncoderFactory {
  public Argb8888EncoderFactory() {
  }

  public Encoder create(boolean rle, OutputStream os) {
    return MultiByteEncoderFactory.create(rle, new PixelEncoder() {
      public int bitsPerPixel() {
        return 32;
      }

      public int encodePixel(int argb) {
        return argb;
      }

      public boolean isPixelVisible(int argb) {
        return (argb >>> 24) != 0;
      }
    }, os);
  }
}
