package roo.display.encode;

public interface PixelEncoder {
  int bitsPerPixel();
  int encodePixel(int argb);
  boolean isPixelVisible(int argb);
}
