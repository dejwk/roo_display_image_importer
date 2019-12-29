package roo.display.encode.rgb565;

import roo.display.encode.*;
import java.io.OutputStream;

public class Rgb565EncoderFactory implements EncoderFactory {
  public Rgb565EncoderFactory() {
  }

  public Encoder create(boolean rle, OutputStream os) {
    return new Rgb565TransparencyCapturer(
      MultiByteEncoderFactory.create(rle, new PixelEncoder() {
        public int bitsPerPixel() { return 2; }

        public int encodePixel(int argb) {
          return ((argb >> 8) & 0xF800) | ((argb >> 5) & 0x07E0) | ((argb >> 3) & 0x1F);
        }
      }, os)
    );
  }
}
