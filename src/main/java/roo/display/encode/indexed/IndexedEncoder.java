package roo.display.encode.indexed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.Map.Entry;
import roo.display.encode.*;

public class IndexedEncoder extends Encoder {

  final int bits;
  final boolean msb;
  final boolean rle;
  private final OutputStream os;
  List<Integer> buffer;
  Map<Integer, ColorProperties> freq;
  List<Integer> palette;

  public IndexedEncoder(OutputStream os, int bits, boolean msb, boolean rle) {
    this.os = os;
    this.bits = bits;
    this.msb = msb;
    this.rle = rle;
    this.buffer = new ArrayList<Integer>();
    this.freq = new HashMap<>();
    this.palette = new ArrayList<>();
  }

  public final int maxSize() {
    return 1 << bits;
  }

  public void encodePixel(int pixel) throws IOException {
    buffer.add(pixel);
    ColorProperties props = freq.get(pixel);
    if (props == null) {
      props = new ColorProperties();
      props.frequency = 1;
      freq.put(pixel, props);
    } else {
      ++props.frequency;
    }
  }

  public void close() throws IOException {
    List<Map.Entry<Integer, ColorProperties>> top = new ArrayList<>(
      freq.entrySet()
    );
    top.sort(
      new Comparator<Map.Entry<Integer, ColorProperties>>() {
        @Override
        public int compare(
          Entry<Integer, ColorProperties> o1,
          Entry<Integer, ColorProperties> o2
        ) {
          return o2.getValue().frequency - o1.getValue().frequency;
        }
      }
    );
    // Order colors in the palette by descending popularity. If there are too many
    // colors, they get
    // remapped to the most popular color.
    for (int i = 0; i < top.size(); ++i) {
      int palette_idx = i < maxSize() ? i : 0;
      top.get(i).getValue().paletteIndex = palette_idx;
      if (i < maxSize()) {
        palette.add(top.get(i).getKey());
      }
    }

    if (!rle) {
      writeTo(os);
      return;
    }
    // Handle RLE by encoding everything into a byte buffer, and then pretending
    // that this byte
    // buffer is a 1bpp pixel stream.
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    writeTo(bos);
    byte[] raw = bos.toByteArray();
    Encoder rleEncoder = MultiByteEncoderFactory.create(
      true,
      new PixelEncoder() {
        @Override
        public int bitsPerPixel() {
          return 8;
        }

        @Override
        public int encodePixel(int argb) {
          return argb;
        }

        @Override
        public boolean isPixelVisible(int argb) {
          return true;
        }
      },
      os
    );
    for (byte b : raw) {
      rleEncoder.encodePixel((b));
    }
    rleEncoder.close();
    os.close();
  }

  public boolean isPixelVisible(int argb) {
    return (argb >>> 24) != 0;
  }

  private void writeTo(OutputStream os) throws IOException {
    if (bits != 8) {
      SubByteWriter sos = new SubByteWriter(os, bits, msb);
      for (int pixel : buffer) {
        sos.write(freq.get(pixel).paletteIndex);
      }
      sos.close();
    } else {
      for (int pixel : buffer) {
        os.write(freq.get(pixel).paletteIndex);
      }
    }
  }

  @Override
  public List<Integer> getPalette() {
    return palette;
  }

  private static class ColorProperties {

    int frequency;
    int paletteIndex;
  }
}
