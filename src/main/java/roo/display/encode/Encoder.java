package roo.display.encode;

import java.io.IOException;
import java.util.Properties;

public abstract class Encoder {
  protected Properties properties;

  public Encoder() {
    this.properties = new Properties();
  }

  public String getProperty(String key) { return properties.getProperty(key); }

  public Properties getProperties() { return properties; }

  public abstract void encodePixel(int pixel) throws IOException;

  public abstract void close() throws IOException;
}
