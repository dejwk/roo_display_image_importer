package roo.display.encode.grayscale8;

import roo.display.encode.*;
import java.io.OutputStream;

public class Grayscale8EncoderFactory implements EncoderFactory {
  public Grayscale8EncoderFactory() {
  }

  public Encoder create(boolean rle, OutputStream os) {
    return MultiByteEncoderFactory.create(rle, new PixelEncoder() {
      public int bitsPerPixel() {
        return 8;
      }
    
      public int encodePixel(int argb) {
        // Using fast approximate formula;
        // See https://stackoverflow.com/questions/596216
        return ((((argb >> 16) & 0xFF) * 3) + (((argb >> 8) & 0xFF) * 4) + (argb & 0xFF)) >> 3;
      }

      public boolean isPixelVisible(int argb) {
        return true;
      }
    }, os);
  }
}
