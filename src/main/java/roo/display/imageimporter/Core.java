package roo.display.imageimporter;

import java.awt.image.BufferedImage;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.Writer;
import java.io.File;
import java.io.FileOutputStream;

import java.io.*;

import hexwriter.HexWriter;
import roo.display.encode.*;
import roo.display.imageimporter.ImportOptions.Compression;
import roo.display.imageimporter.ImportOptions.Storage;

import roo.display.encode.pixel.*;

import java.text.MessageFormat;

public class Core {

  static enum TypeScoping {
    UNQUALIFIED(""), QUALIFIED("::roo_display::");

    TypeScoping(String scope) {
      this.scope = scope;
    }

    final String scope;

    public String scope() {
      return scope;
    }
  }

  public Core() {
  }

  public void execute(BufferedImage image, ImportOptions options) throws IOException {
    File headerFile = new File(options.getOutputHeaderDirectory(), options.getName() + ".h");
    File cppFile = new File(options.getOutputHeaderDirectory(), options.getName() + ".cpp");
    File dataFile = new File(options.getOutputPayloadDirectory(), options.getName() + ".img");
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    OutputStream os = new BufferedOutputStream(bos);
    Encoder encoder = createEncoder(options, os);
    for (int i = 0; i < image.getHeight(); ++i) {
      for (int j = 0; j < image.getWidth(); ++j) {
        int rgb = image.getRGB(j, i);
        encoder.encodePixel(rgb);
      }
    }
    encoder.close();
    os.close();

    if (options.getStorage() == Storage.SPIFFS) {
      OutputStream payloadStream = new FileOutputStream(dataFile);
      bos.writeTo(payloadStream);
      payloadStream.close();
    }

    String variable = options.getResourceName();
    String qualified_typename = getCppImageTypeNameForEncoding(options, TypeScoping.QUALIFIED);
    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(headerFile)));
    writer.write("#include <Arduino.h>\n");
    writer.write("#include \"roo_display/image/image.h\"\n");
    if (options.getStorage() == Storage.SPIFFS) {
      writer.write("#include \"roo_display/io/file.h\"\n");
    }
    writer.write("\n");
    writer.write("const " + qualified_typename + "& " + variable + "_streamable();\n");
    writer.write("const ::roo_display::Drawable& " + variable + "();\n");
    writer.close();

    String unqualified_typename = getCppImageTypeNameForEncoding(options, TypeScoping.UNQUALIFIED);
    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cppFile)));
    writer.write("#include \"" + options.getName() + ".h\"\n");
    writer.write("#include \"SPIFFS.h\"\n");
    writer.write("\n");
    writer.write("using namespace roo_display;\n");
    writer.write("\n");
    boolean rle = (options.getCompression() == Compression.RLE);
    if (options.getStorage() == Storage.SPIFFS) {
      writer.write("const " + unqualified_typename + "& " + variable + "_streamable() {\n");
      writer.write("  static FileResource file(SPIFFS, \"/" + dataFile.getName() + "\");\n");
      writer.write("  static " + unqualified_typename + " value(\n      " + image.getWidth() + ", " + image.getHeight()
          + ", file, " + getCppEncodingConstructor(options, encoder.getProperty("transparentColor")) + ");\n");
      writer.write("  return value;\n");
      writer.write("}\n");
    } else {
      HexWriter hexWriter = new HexWriter(writer);
      hexWriter.writeDeclaration(variable + "_data");
      writer.write("\n\n");
      writer.write("const " + unqualified_typename + "& " + variable + "_streamable() {\n");
      writer.write("  static " + unqualified_typename + " value(\n      " + image.getWidth() + ", " + image.getHeight()
          + ", " + variable + "_data" + ", "
          + getCppEncodingConstructor(options, encoder.getProperty("transparentColor")) + ");\n");
      writer.write("  return value;\n");
      writer.write("}\n\n");
      hexWriter.printComment("Image file " + options.getName() + " " + image.getWidth() + "x" + image.getHeight() + ", "
          + options.getEncoding().description + ", " + (rle ? " RLE, " : "") + bos.size() + " bytes \n");
      hexWriter.begin(variable + "_data");
      hexWriter.printBuffer(bos.toByteArray());
      hexWriter.end();
    }
    writer.write("\n");
    writer.write("const Drawable& " + variable + "() {\n");
    writer.write("  static auto drawable = MakeDrawableStreamable(" + variable + "_streamable());\n");
    writer.write("  return drawable;\n");
    writer.write("}\n");
    writer.close();
  }

  private static String getCppImageTypeNameForEncoding(ImportOptions options, TypeScoping scoping) {
    String resource = scoping.scope() + (options.getStorage() == Storage.SPIFFS ? "FileResource" : "PrgMemResource");
    return MessageFormat.format(getCppImageTypeNameFormatForEncoding(options), resource, scoping.scope());
  }

  private static String getCppImageTypeNameFormatForEncoding(ImportOptions options) {
    boolean rle = (options.getCompression() == Compression.RLE);
    switch (options.getEncoding()) {
    case ARGB8888:
      return rle ? "{1}RleImage<{1}Argb8888, {0}>" : "{1}Raster<{0}, {1}Argb8888>";
    case ARGB6666:
      return rle ? "{1}RleImage<{1}Argb6666, {0}>" : "{1}Raster<{0}, {1}Argb6666>";
    case ARGB4444:
      return rle ? "{1}RleImage<{1}Argb4444, {0}>" : "{1}Raster<{0}, {1}Argb4444>";
    case RGB565:
      return rle ? "{1}RleImage<{1}Rgb565, {0}>" : "{1}Raster<{0}, {1}Rgb565>";
    // case RGB565_ALPHA4: return "{1}Rgb565Alpha4RleImage<{0}>";
    case GRAYSCALE8:
      return rle ? "{1}RleImage<{1}Grayscale8, {0}>" : "{1}Raster<{0}, {1}Grayscale8>";
    // return rle ? "{1}ImageRle4bppxPolarized<{1}Alpha4, {0}>" : "{1}Raster<{0}, {1}Alpha4>";
    case ALPHA8:
      return rle ? "{1}RleImage<{1}Alpha8, {0}>" : "{1}Raster<{0}, {1}Alpha8>";
    case GRAYSCALE4:
      return rle ? "{1}RleImage4bppxPolarized<{1}Grayscale4, {0}>" : "{1}Raster<{0}, {1}Grayscale4>";
    case ALPHA4:
      return rle ? "{1}RleImage4bppxPolarized<{1}Alpha4, {0}>" : "{1}Raster<{0}, {1}Alpha4>";
    case MONOCHROME:
      return "{1}Raster<{0}, {1}Monochrome>";
    default:
      return null;
    }
  }

  // private static String getCppEncodingConstructor(ImportOptions options, String
  // transparent565Color, TypeScoping scoping) {return
  // MessageFormat.format(getCppEncodingConstructorTemplate(options, transparent565Color),
  // scoping.scope());
  // }

  private static String getCppEncodingConstructor(ImportOptions options, String transparent565Color) {
    switch (options.getEncoding()) {
    case ARGB8888:
      return "Argb8888()";
    case ARGB6666:
      return "Argb6666()";
    case ARGB4444:
      return "Argb4444()";
    case RGB565:
      return "Rgb565(" + (transparent565Color == null ? "" : transparent565Color) + ")";
    // case RGB565_ALPHA4: return "Argb8888()";
    case GRAYSCALE8:
      return "Grayscale8()";
    case GRAYSCALE4:
      return "Grayscale4()";
    case ALPHA8:
      return "Alpha8(" + options.getFgColor() + ")";
    case ALPHA4:
      return "Alpha4(" + options.getFgColor() + ")";
    case MONOCHROME:
      return "Monochrome(" + options.getFgColor() + ", " + options.getBgColor() + ")";
    default:
      return null;
    }
  }

  private static Encoder createEncoder(ImportOptions options, OutputStream os) {
    boolean rle = options.getCompression() == Compression.RLE;
    switch (options.getEncoding()) {
    case ARGB8888:
      return rle ? new MultiByteRleEncoder(new Argb8888PixelEncoder(), os)
        : new MultiByteRasterEncoder(new Argb8888PixelEncoder(), os);
    case ARGB6666:
      return rle ? new MultiByteRleEncoder(new Argb6666PixelEncoder(), os)
        : new MultiByteRasterEncoder(new Argb6666PixelEncoder(), os);
    case ARGB4444:
      return rle ? new MultiByteRleEncoder(new Argb4444PixelEncoder(), os)
        : new MultiByteRasterEncoder(new Argb4444PixelEncoder(), os);
    case RGB565:
      return new Rgb565TransparencyCapturer(rle ? new MultiByteRleEncoder(new Rgb565PixelEncoder(), os)
        : new MultiByteRasterEncoder(new Rgb565PixelEncoder(), os));
    // case RGB565_ALPHA4: return new Rgb565EncoderAlpha4(os, rle);
    case GRAYSCALE8:
      return rle ? new MultiByteRleEncoder(new Grayscale8PixelEncoder(), os)
        : new MultiByteRasterEncoder(new Grayscale8PixelEncoder(), os);
    case ALPHA8:
      return rle ? new MultiByteRleEncoder(new Alpha8PixelEncoder(), os)
        : new MultiByteRasterEncoder(new Alpha8PixelEncoder(), os);
    case ALPHA4:
      return rle ? new Alpha4AntiAliasRleEncoder(os) : new PlainAlpha4Encoder(os);
    case MONOCHROME:
      return new MonochromeEncoder(os, 0xFF000000);
    default:
      return null;
    }
  }
}
