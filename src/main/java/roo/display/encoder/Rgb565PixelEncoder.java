package roo.display.encoder;

public class Rgb565PixelEncoder implements PixelEncoder {
  public int bitsPerPixel() { return 2; }

  public int encodePixel(int argb) {
    return ((argb >> 8) & 0xF800) | ((argb >> 5) & 0x07E0) | ((argb >> 3) & 0x1F);
  }
}
