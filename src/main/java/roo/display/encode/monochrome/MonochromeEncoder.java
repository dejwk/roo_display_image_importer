package roo.display.encode.monochrome;

import java.io.IOException;
import java.io.OutputStream;
import roo.display.encode.*;
import java.util.*;
import java.util.Map.Entry;

public class MonochromeEncoder extends Encoder {
  private BitWriter os;
  List<Integer> buffer;
  Map<Integer, Integer> freq;

  public MonochromeEncoder(OutputStream os) {
    this.os = new BitWriter(os);
    this.buffer = new ArrayList<Integer>();
    this.freq = new HashMap<>();
  }

  public void encodePixel(int pixel) throws IOException {
    if (isFullyTransparent(pixel)) {
      pixel = 0;
    }
    buffer.add(pixel);
    Integer f = freq.get(pixel);
    if (f == null) {
      f = 0;
    }
    freq.put(pixel, f + 1);
  }

  public void close() throws IOException {
    int bg = 0;
    int fg = 0;
    if (freq.size() == 0) {
      // Nothing to write.
    } else if (freq.size() == 1) {
      // Single color.
      bg = fg = freq.keySet().iterator().next();
    } else {
      List<Map.Entry<Integer, Integer>> top = new ArrayList<>(freq.entrySet());
      top.sort(new Comparator<Map.Entry<Integer, Integer>>() {
        @Override
        public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
          return o2.getValue() - o1.getValue();
        }
      });
      bg = top.get(0).getKey();
      fg = top.get(1).getKey();
      // A heuristic that uses the transparent color as a background.
      if (isFullyTransparent(fg) && !isFullyTransparent(bg)) {
        int tmp = fg;
        fg = bg;
        bg = tmp;
      }
    }
    properties.put("bgColor", color(bg));
    properties.put("fgColor", color(fg));

    for (int pixel : buffer) {
      os.write(pixel != bg);
    }
    os.close();
  }

  public boolean isPixelVisible(int argb) {
    return (argb >>> 24) != 0;
  }

  private static String color(int c) {
    return String.format("Color(0x%H)", (long)c & 0xFFFFFFFFL);
  }

  private static boolean isFullyTransparent(int pixel) {
    return (pixel & 0xFF000000) == 0;
  }
}
