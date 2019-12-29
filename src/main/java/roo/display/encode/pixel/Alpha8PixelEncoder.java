package roo.display.encode.pixel;

public class Alpha8PixelEncoder implements PixelEncoder {
  public int bitsPerPixel() {
    return 8;
  }

  public int encodePixel(int argb) {
    return argb >> 24;
  }
}
