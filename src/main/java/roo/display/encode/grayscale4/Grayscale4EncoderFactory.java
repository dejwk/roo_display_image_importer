package roo.display.encode.grayscale4;

import roo.display.encode.*;
import java.io.*;

public class Grayscale4EncoderFactory implements EncoderFactory {
  public Grayscale4EncoderFactory() {
  }

  public Encoder create(boolean rle, OutputStream os) {
    return rle ? new PlainGrayscale4Encoder(os) : new PlainGrayscale4Encoder(os);
  }

  private static class PlainGrayscale4Encoder extends Encoder {
    private SubByteWriter os;
  
    public PlainGrayscale4Encoder(OutputStream os) {
      this.os = new SubByteWriter(os, 4, true);
    }
  
    public void encodePixel(int argb) throws IOException {
      int intensity8 = ((((argb >> 16) & 0xFF) * 3) + (((argb >> 8) & 0xFF) * 4) + (argb & 0xFF)) >> 3;
      int intensity4 = (intensity8 - (intensity8 >> 5)) >> 4;
      os.write(intensity4);
    }
  
    public boolean isPixelVisible(int argb) {
      return true;
    }

    public void close() throws IOException {
      os.close();
    }
  }
  
}
