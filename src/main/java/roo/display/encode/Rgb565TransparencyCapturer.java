package roo.display.encode;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class Rgb565TransparencyCapturer extends Encoder {
  List<Integer> buffer;
  int[] freq;
  boolean hasTransparency;
  Map<String, String> properties;
  Encoder delegate;

  public Rgb565TransparencyCapturer(Encoder delegate) { 
    this.delegate = delegate;
    this.buffer = new ArrayList<Integer>();
    this.freq = new int[1 << 16];
    this.properties = new HashMap<String, String>();
  }

  public String getProperty(String key) {
      return properties.get(key);
  }

  public void encodePixel(int pixel) throws IOException {
    buffer.add(pixel);
    if ((pixel >>> 24) <= 0x7F) {
      hasTransparency = true;
    } else {
      freq[encode565(pixel)]++;
    }
  }

  public void close() throws IOException {
    // Find the least frequently used color.
    int minFreqColor = 0;
    int minFreq = Integer.MAX_VALUE;
    int remapColor;
    for (int i = 0; i < (1 << 16); ++i) {
      if (freq[i] < minFreq) {
        minFreqColor = i;
        minFreq = freq[i];
        if (minFreq == 0) break;  // Found a winner!
      }
    }
    if (minFreq == 0) {
      remapColor = decode565(minFreqColor);
    } else {
      // Remap all the pixels of the least frequently used color to
      // a nearby color, by modifying G (which has the greatest resolution
      // of 6 pixels).
      int green6bit = (minFreqColor >> 5) & 0x3F;
      if (green6bit < 0x3F) {
        green6bit++;
      } else {
        green6bit--;
      }
      remapColor = decode565((minFreqColor & 0xF81F) | (green6bit << 5));
    }

    if (hasTransparency) {
        // Use the least frequent color (hopefully, unused color) as
      // transparent color.
      properties.put("transparentColor", 
                     String.valueOf(encode565(minFreqColor)));
    }
    for (int pixel : buffer) {
      if ((pixel >>> 24) <= 0x7F) {
        pixel = minFreqColor;
      } else if (encode565(pixel) == minFreqColor) {
        pixel = remapColor;
      }
      delegate.encodePixel(pixel);
    }
    delegate.close();
  }

  private static int encode565(int pixel) {
    return ((pixel >> 8) & 0xF800) | ((pixel >> 5) & 0x07E0) |
           ((pixel >> 3) & 0x1F);
  }

  private static int decode565(int encoded) {
    int r = ((encoded >> 8) & 0xF8) | (encoded >> 13);
    int g = ((encoded >> 3) & 0xFC) | ((encoded >> 9) & 0x03);
    int b = ((encoded << 3) & 0xF8) | ((encoded >> 2) & 0x07);
    return 0xFF000000 | r << 16 | g << 8 | b;
  }

}
