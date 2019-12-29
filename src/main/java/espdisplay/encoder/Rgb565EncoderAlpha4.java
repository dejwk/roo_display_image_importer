package espdisplay.encoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Logger;

public class Rgb565EncoderAlpha4 extends Encoder {
  private final Encoder colorEncoder;
  private final Encoder alphaEncoder;
  private OutputStream os;
  private ByteArrayOutputStream colorOs;
  private ByteArrayOutputStream alphaOs;

  public Rgb565EncoderAlpha4(OutputStream os, boolean rle) {
    this.os = os;
    this.colorOs = new ByteArrayOutputStream();
    this.alphaOs = new ByteArrayOutputStream();
    if (rle) {
      this.colorEncoder = new Rgb565RleEncoder(colorOs);
      this.alphaEncoder = new Alpha4AntiAliasRleEncoder(alphaOs);
    } else {
      this.colorEncoder = new Rgb565Encoder(colorOs);
      this.alphaEncoder = new PlainAlpha4Encoder(alphaOs);
    }
  }

  public void encodePixel(int pixel) throws IOException {
    colorEncoder.encodePixel(pixel & 0x00FFFFFF);
    alphaEncoder.encodePixel(pixel & 0xFF000000);
  }

  public void close() throws IOException {
    colorEncoder.close();
    alphaEncoder.close();
    os.write((colorOs.size() >> 24) & 0xFF);
    os.write((colorOs.size() >> 16) & 0xFF);
    os.write((colorOs.size() >> 8) & 0xFF);
    os.write((colorOs.size() >> 0) & 0xFF);
    os.write(colorOs.toByteArray());
    os.write(alphaOs.toByteArray());
  }
}
