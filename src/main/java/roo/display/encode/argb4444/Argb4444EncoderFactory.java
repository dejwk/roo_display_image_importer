package roo.display.encode.argb4444;

import roo.display.encode.*;
import java.io.OutputStream;

public class Argb4444EncoderFactory implements EncoderFactory {
  public Argb4444EncoderFactory() {
  }

  public Encoder create(boolean rle, OutputStream os) {
    return MultiByteEncoderFactory.create(rle, new PixelEncoder() {
      public int bitsPerPixel() { return 16; }

      public int encodePixel(int argb) {
        return ((argb >> 16) & 0xF000) | ((argb >> 12) & 0x0F00) |
               ((argb >> 8) & 0x00F0) | ((argb >> 4) & 0x000F);
      }
    }, os);
  }
}
