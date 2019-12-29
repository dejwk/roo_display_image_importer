package roo.display.encoder;

public class Argb8888PixelEncoder implements PixelEncoder {
  public Argb8888PixelEncoder() {}
  public int bitsPerPixel() { return 32; }
  public int encodePixel(int argb) {
    return argb;
  }
}
