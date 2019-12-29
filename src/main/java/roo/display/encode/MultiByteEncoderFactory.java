package roo.display.encode;

import java.io.OutputStream;

public class MultiByteEncoderFactory {
  private MultiByteEncoderFactory() {
  }

  public static Encoder create(boolean rle, PixelEncoder pe, OutputStream os) {
    return rle ? new MultiByteRleEncoder(pe, os) : new MultiByteRasterEncoder(pe, os);
  }
}
