package roo.display.imageimporter;

import java.awt.image.BufferedImage;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.Writer;
import java.io.File;
import java.io.FileOutputStream;

import java.io.*;
import java.util.Properties;

import hexwriter.HexWriter;
import roo.display.encode.*;
import roo.display.encode.alpha4.Alpha4EncoderFactory;
import roo.display.encode.alpha8.Alpha8EncoderFactory;
import roo.display.encode.argb4444.Argb4444EncoderFactory;
import roo.display.encode.argb6666.Argb6666EncoderFactory;
import roo.display.encode.argb8888.Argb8888EncoderFactory;
import roo.display.encode.grayscale4.Grayscale4EncoderFactory;
import roo.display.encode.grayscale8.Grayscale8EncoderFactory;
import roo.display.encode.monochrome.MonochromeEncoderFactory;
import roo.display.imageimporter.ImportOptions.Compression;
import roo.display.imageimporter.ImportOptions.Storage;

import roo.display.encode.rgb565.Rgb565EncoderFactory;

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

  private void writeHeaderPreamble(OutputStream headerStream, ImportOptions options) throws IOException {
    Writer writer = new BufferedWriter(new OutputStreamWriter(headerStream));
    writer.write("#include <Arduino.h>\n");
    writer.write("#include \"roo_display/image/image.h\"\n");
    if (options.getStorage() == Storage.SPIFFS) {
      writer.write("#include \"roo_display/io/file.h\"\n");
    }
    writer.write("\n");
    writer.flush();
  }

  private void writeHeaderDeclaration(OutputStream headerStream, ImportOptions options) throws IOException {
    Writer writer = new BufferedWriter(new OutputStreamWriter(headerStream));
    String variable = options.getResourceName();
    String qualified_typename = getCppImageTypeNameForEncoding(options, TypeScoping.QUALIFIED);

    writer.write("const " + qualified_typename + "& " + variable + "_streamable();\n");
    writer.write("const ::roo_display::Drawable& " + variable + "();\n");
    writer.flush();
  }

  private void writeCppPreamble(OutputStream cppStream, ImportOptions options) throws IOException {
    Writer writer = new BufferedWriter(new OutputStreamWriter(cppStream));
    writer.write("#include \"" + options.getName() + ".h\"\n");
    if (options.getStorage() == Storage.SPIFFS) {
      writer.write("#include \"SPIFFS.h\"\n");
    }
    writer.write(
      "\n" +
      "using namespace roo_display;\n" +
      "\n");
    writer.flush();
  }

  private void writeCppDefinition(OutputStream cppStream, ImportOptions options, String dataFileName, 
                                  BufferedImage image, Properties encoderProperties,
                                  byte[] encoded) throws IOException {
    Writer writer = new BufferedWriter(new OutputStreamWriter(cppStream));
    String variable = options.getResourceName();
    String unqualified_typename = getCppImageTypeNameForEncoding(options, TypeScoping.UNQUALIFIED);
    boolean rle = (options.getCompression() == Compression.RLE);
    if (options.getStorage() == Storage.SPIFFS) {
      String template =
        "const {TYPE}& {VAR}_streamable() {\n" +
        "  static FileResource file(SPIFFS, \"/{DATAFILE}\");\n" +
        "  static {TYPE} value(\n" +
        "      {WIDTH}, {HEIGHT}, file, {CONSTRUCTOR});\n" +
        "  return value;\n" +
        "}\n";

      writer.write(template
        .replace("{TYPE}", unqualified_typename)
        .replace("{VAR}", variable)
        .replace("{DATAFILE}", dataFileName)
        .replace("{WIDTH}", String.valueOf(image.getWidth()))
        .replace("{HEIGHT}", String.valueOf(image.getHeight()))
        .replace("{CONSTRUCTOR}", getCppEncodingConstructor(options, encoderProperties)));
    } else {
      HexWriter hexWriter = new HexWriter(writer);
      hexWriter.printComment("Image file " + options.getName() + " " + image.getWidth() + "x" + image.getHeight() + ", "
          + options.getEncoding().description + ", " + (rle ? " RLE, " : "") + encoded.length + " bytes \n");
      hexWriter.beginStatic(variable + "_data");
      hexWriter.printBuffer(encoded);
      hexWriter.end();

      String template =
        "\n" +
        "const {TYPE}& {VAR}_streamable() {\n" +
        "  static {TYPE} value(\n" +
        "      {WIDTH}, {HEIGHT}, {VAR}_data, {CONSTRUCTOR});\n" +
        "  return value;\n" +
        "}\n";

      writer.write(template
          .replace("{TYPE}", unqualified_typename)
          .replace("{VAR}", variable)
          .replace("{WIDTH}", String.valueOf(image.getWidth()))
          .replace("{HEIGHT}", String.valueOf(image.getHeight()))
          .replace("{CONSTRUCTOR}", getCppEncodingConstructor(options, encoderProperties)));
    }

    String template =
      "\n" +
      "const Drawable& {VAR}() {\n" +
      "  static auto drawable = MakeDrawableStreamable({VAR}_streamable());\n" +
      "  return drawable;\n" +
      "}\n";

    writer.write(template.replace("{VAR}", variable));
    writer.flush();
  }

  public void execute(BufferedImage image, ImportOptions options) throws IOException {
    File headerFile = new File(options.getOutputHeaderDirectory(), options.getName() + ".h");
    File cppFile = new File(options.getOutputHeaderDirectory(), options.getName() + ".cpp");
    File dataFile = new File(options.getOutputPayloadDirectory(), options.getName() + ".img");

    OutputStream headerStream = new FileOutputStream(headerFile);
    OutputStream cppStream = new FileOutputStream(cppFile);

    writeHeaderPreamble(headerStream, options);
    writeCppPreamble(cppStream, options);

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

    writeHeaderDeclaration(headerStream, options);
    headerStream.close();

    writeCppDefinition(cppStream, options, dataFile.getName(), image, encoder.getProperties(), bos.toByteArray());
    cppStream.close();
  }

  private static String getCppImageTypeNameForEncoding(ImportOptions options, TypeScoping scoping) {
    String resource = scoping.scope() + (options.getStorage() == Storage.SPIFFS ? "FileResource" : "PrgMemResource");
    return MessageFormat.format(getCppImageTypeNameFormatForEncoding(options), resource, scoping.scope());
  }

  private static String getCppImageTypeNameFormatForEncoding(ImportOptions options) {
    boolean rle = (options.getCompression() == Compression.RLE);
    boolean prgmem = (options.getStorage() == Storage.PROGMEM);
    switch (options.getEncoding()) {
    case ARGB8888:
      return rle ? "{1}RleImage<{1}Argb8888, {0}>" : prgmem ? "{1}Raster<const uint8_t PROGMEM*, {1}Argb8888>" : "{1}SimpleImage<{0}, {1}Argb8888>";
    case ARGB6666:
      return rle ? "{1}RleImage<{1}Argb6666, {0}>" : prgmem ? "{1}Raster<const uint8_t PROGMEM*, {1}Argb6666>" : "{1}SimpleImage<{0}, {1}Argb6666>";
    case ARGB4444:
      return rle ? "{1}RleImage<{1}Argb4444, {0}>" : prgmem ? "{1}Raster<const uint8_t PROGMEM*, {1}Argb4444>" : "{1}SimpleImage<{0}, {1}Argb4444>";
    case RGB565:
      return rle ? "{1}RleImage<{1}Rgb565, {0}>" : prgmem ? "{1}Raster<const uint8_t PROGMEM*, {1}Rgb565>" : "{1}SimpleImage<{0}, {1}Rgb565>";
    // case RGB565_ALPHA4: return "{1}Rgb565Alpha4RleImage<{0}>";
    case GRAYSCALE8:
      return rle ? "{1}RleImage<{1}Grayscale8, {0}>" : prgmem ? "{1}Raster<const uint8_t PROGMEM*, {1}Grayscale8>" : "{1}SimpleImage<{0}, {1}Grayscale8>";
    // return rle ? "{1}ImageRle4bppxPolarized<{1}Alpha4, {0}>" : "{1}Raster<{0}, {1}Alpha4>";
    case ALPHA8:
      return rle ? "{1}RleImage<{1}Alpha8, {0}>" : prgmem ? "{1}Raster<{0}, {1}Alpha8>" : "{1}SimpleImage<{0}, {1}Alpha8>";
    case GRAYSCALE4:
      return rle ? "{1}RleImage4bppxPolarized<{1}Grayscale4, {0}>" : prgmem ? "{1}Raster<const uint8_t PROGMEM*, {1}Grayscale4>" : "{1}SimpleImage<{0}, {1}Grayscale4>";
    case ALPHA4:
      return rle ? "{1}RleImage4bppxPolarized<{1}Alpha4, {0}>" : prgmem ? "{1}Raster<const uint8_t PROGMEM*, {1}Alpha4>" : "{1}SimpleImage<{0}, {1}Alpha4>";
    case MONOCHROME:
      return prgmem ? "{1}Raster<const uint8_t PROGMEM*, {1}Monochrome>" : "{1}SimpleImage<{0}, {1}Monochrome>";
    default:
      return null;
    }
  }

  // private static String getCppEncodingConstructor(ImportOptions options, String
  // transparent565Color, TypeScoping scoping) {return
  // MessageFormat.format(getCppEncodingConstructorTemplate(options, transparent565Color),
  // scoping.scope());
  // }

  private static String getCppEncodingConstructor(ImportOptions options, Properties encoderProperties) {
    switch (options.getEncoding()) {
    case ARGB8888:
      return "Argb8888()";
    case ARGB6666:
      return "Argb6666()";
    case ARGB4444:
      return "Argb4444()";
    case RGB565:
      String t = encoderProperties.getProperty("transparentColor");
      return "Rgb565(" + (t == null ? "" : t) + ")";
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
      String bg = encoderProperties.getProperty("bgColor");
      String fg = encoderProperties.getProperty("fgColor");
      return "Monochrome(" + fg + ", " + bg + ")";
    default:
      return null;
    }
  }

  private static Encoder createEncoder(ImportOptions options, OutputStream os) {
    boolean rle = options.getCompression() == Compression.RLE;
    EncoderFactory factory;
    switch (options.getEncoding()) {
    case ARGB8888:
      factory = new Argb8888EncoderFactory();
      break;
    case ARGB6666:
      factory = new Argb6666EncoderFactory();
      break;
    case ARGB4444:
      factory = new Argb4444EncoderFactory();
      break;
    case RGB565:
      factory = new Rgb565EncoderFactory();
      break;
    case GRAYSCALE4:
      factory = new Grayscale4EncoderFactory();
      break;
    case GRAYSCALE8:
      factory = new Grayscale8EncoderFactory();
      break;
    case ALPHA8:
      factory = new Alpha8EncoderFactory();
      break;
    case ALPHA4:
      factory = new Alpha4EncoderFactory();
      break;
    case MONOCHROME:
      factory = new MonochromeEncoderFactory();
      break;
     default:
      throw new IllegalArgumentException("Unsupported encoding: " + options.getEncoding());
    }
    return factory.create(rle, os);
  }
}
