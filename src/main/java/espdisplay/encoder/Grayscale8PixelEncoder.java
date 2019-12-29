package espdisplay.encoder;

public class Grayscale8PixelEncoder implements PixelEncoder {
  public int bitsPerPixel() {
    return 8;
  }

  public int encodePixel(int argb) {
    // Using fast approximate formula;
    // See https://stackoverflow.com/questions/596216
    return ((((argb >> 16) & 0xFF) * 3) + (((argb >> 8) & 0xFF) * 4) + (argb & 0xFF)) >> 3;
  }
}
