package roo.display.imageimporter;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

final class ImageLoader {
  private ImageLoader() {}

  public static BufferedImage load(File input) throws IOException {
    return load(input, 1.0);
  }

  public static BufferedImage load(File input, double scale) throws IOException {
    if (scale <= 0) {
      throw new IOException("Scale must be greater than zero: " + scale);
    }

    if (isSvg(input)) {
      return loadSvg(input, scale);
    }

    if (scale != 1.0) {
      Logger.getGlobal().warning("Ignoring --scale for raster input: " + input.getAbsolutePath());
    }

    BufferedImage image = ImageIO.read(input);
    if (image == null) {
      throw new IOException("Unsupported or unreadable image file: " + input.getAbsolutePath());
    }
    return image;
  }

  public static String[] getSupportedFileSuffixes() {
    Set<String> suffixes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    suffixes.addAll(Arrays.asList(ImageIO.getReaderFileSuffixes()));
    suffixes.add("svg");
    return suffixes.toArray(new String[0]);
  }

  private static boolean isSvg(File input) {
    return input.getName().toLowerCase(Locale.ROOT).endsWith(".svg");
  }

  private static BufferedImage loadSvg(File input, double scale) throws IOException {
    try {
      BufferedImage image = renderSvgIntrinsicScale(input);
      if (image != null && scale != 1.0) {
        image = renderSvg(input, new float[] {image.getWidth(), image.getHeight()}, scale);
      }
      if (image == null) {
        throw new IOException("Unsupported or unreadable SVG file: " + input.getAbsolutePath());
      }
      return image;
    } catch (TranscoderException e) {
      throw new IOException("Failed to render SVG file: " + input.getAbsolutePath(), e);
    }
  }

  private static BufferedImage renderSvgIntrinsicScale(File input)
      throws IOException, TranscoderException {
    return renderSvg(input, null, 1.0);
  }

  private static BufferedImage renderSvg(File input, float[] baseSize, double scale)
      throws IOException, TranscoderException {
    try (InputStream stream = new BufferedInputStream(new FileInputStream(input))) {
      BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
      if (baseSize != null) {
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, baseSize[0] * (float) scale);
        transcoder.addTranscodingHint(
            SVGAbstractTranscoder.KEY_HEIGHT, baseSize[1] * (float) scale);
      }
      transcoder.transcode(new TranscoderInput(stream), null);
      return transcoder.getImage();
    }
  }

  private static BufferedImage renderSvg(File input, float[] baseSize)
      throws IOException, TranscoderException {
    return renderSvg(input, baseSize, 1.0);
  }

  private static final class BufferedImageTranscoder extends ImageTranscoder {
    private BufferedImage image;

    @Override
    public BufferedImage createImage(int width, int height) {
      return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void writeImage(BufferedImage image, TranscoderOutput output) {
      this.image = image;
    }

    public BufferedImage getImage() {
      return image;
    }
  }
}