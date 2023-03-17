package roo.display.encode.indexed;

import roo.display.encode.*;
import java.io.OutputStream;

public class IndexedEncoderFactory implements EncoderFactory {
  private int bits;

  public IndexedEncoderFactory(int bits) {
    this.bits = bits;
  }

  public Encoder create(boolean rle, OutputStream os) {
    return new IndexedEncoder(os, bits, true, rle);
  }
}
