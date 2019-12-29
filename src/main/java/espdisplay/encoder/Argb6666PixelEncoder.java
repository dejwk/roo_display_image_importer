package espdisplay.encoder;

public class Argb6666PixelEncoder implements PixelEncoder {
  public int bitsPerPixel() { return 24; }

  public int encodePixel(int argb) {
    return ((argb >> 8) & 0xFC0000) | ((argb >> 6) & 0x03F000) |
           ((argb >> 4) & 0x000FC0) | ((argb >> 2) & 0x00003F);
  }
}
