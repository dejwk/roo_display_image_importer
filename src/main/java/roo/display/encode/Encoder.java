package roo.display.encode;

import java.io.IOException;

public abstract class Encoder {
  public String getProperty(String key) { return null; }
  public abstract void encodePixel(int pixel) throws IOException;
  public abstract void close() throws IOException;
}
