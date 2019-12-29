package roo.display.encoder;

public interface PixelEncoder {
  int bitsPerPixel();
  int encodePixel(int argb);
}
