package roo.display.encode.alpha4;

import java.io.OutputStream;
import roo.display.encode.*;

public class Alpha4EncoderFactory implements EncoderFactory {
  public Alpha4EncoderFactory() {}

  public Encoder create(boolean rle, OutputStream os) {
    return rle ? new Alpha4AntiAliasRleEncoder(os) : new PlainAlpha4Encoder(os);
  }
}
