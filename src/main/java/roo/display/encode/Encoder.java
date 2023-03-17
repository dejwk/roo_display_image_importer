package roo.display.encode;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public abstract class Encoder {
  protected Properties properties;

  public Encoder() {
    this.properties = new Properties();
  }

  public String getProperty(String key) { return properties.getProperty(key); }

  public Properties getProperties() { return properties; }

  public abstract void encodePixel(int pixel) throws IOException;

  public boolean isPixelVisible(int pixel) { return true; }

  public List<Integer> getPalette() { return List.of(); }

  public abstract void close() throws IOException;
}
