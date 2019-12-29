package roo.display.encode;

import java.io.OutputStream;

public interface EncoderFactory {
  Encoder create(boolean rle, OutputStream os);
}
