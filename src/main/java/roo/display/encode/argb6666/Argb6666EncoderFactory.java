package roo.display.encode.argb6666;

import roo.display.encode.*;
import java.io.OutputStream;

public class Argb6666EncoderFactory implements EncoderFactory {
  public Argb6666EncoderFactory() {
  }

  public Encoder create(boolean rle, OutputStream os) {
    return MultiByteEncoderFactory.create(rle, new PixelEncoder() {
      public int bitsPerPixel() { return 24; }

      public int encodePixel(int argb) {
        return ((argb >> 8) & 0xFC0000) | ((argb >> 6) & 0x03F000) |
               ((argb >> 4) & 0x000FC0) | ((argb >> 2) & 0x00003F);
      }

      public boolean isPixelVisible(int argb) {
        return (argb >>> 26) != 0;
      }
    }, os);
  }
}
