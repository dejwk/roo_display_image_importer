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
import javax.imageio.ImageIO;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

final class ImageLoader {
  private ImageLoader() {}

  public static BufferedImage load(File input) throws IOException {
    if (isSvg(input)) {
      return loadSvg(input);
    }

    BufferedImage image = ImageIO.read(input);
    if (image == null) {
      throw new IOException("Unsupported or unreadable image file: " +
                            input.getAbsolutePath());
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

  private static BufferedImage loadSvg(File input) throws IOException {
    try (InputStream stream =
             new BufferedInputStream(new FileInputStream(input))) {
      BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
      transcoder.transcode(new TranscoderInput(stream), null);
      BufferedImage image = transcoder.getImage();
      if (image == null) {
        throw new IOException("Unsupported or unreadable SVG file: " +
                              input.getAbsolutePath());
      }
      return image;
    } catch (TranscoderException e) {
      throw new IOException(
          "Failed to render SVG file: " + input.getAbsolutePath(), e);
    }
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

    public BufferedImage getImage() { return image; }
  }
}