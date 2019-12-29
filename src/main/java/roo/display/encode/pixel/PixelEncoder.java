package roo.display.encode.pixel;

public interface PixelEncoder {
  int bitsPerPixel();
  int encodePixel(int argb);
}
