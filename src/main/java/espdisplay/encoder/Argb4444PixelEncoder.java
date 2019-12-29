package espdisplay.encoder;

public class Argb4444PixelEncoder implements PixelEncoder {
  public int bitsPerPixel() { return 16; }

  public int encodePixel(int argb) {
    return ((argb >> 16) & 0xF000) | ((argb >> 12) & 0x0F00) |
           ((argb >> 8) & 0x00F0) | ((argb >> 4) & 0x000F);
  }
}
