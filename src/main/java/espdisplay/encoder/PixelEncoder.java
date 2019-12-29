package espdisplay.encoder;

public interface PixelEncoder {
  int bitsPerPixel();
  int encodePixel(int argb);
}
