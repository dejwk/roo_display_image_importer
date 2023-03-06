package roo.display.imageimporter;

import java.io.File;

public class ImportOptions {

  public enum Encoding {
    ARGB8888("ARGB 8888"),
    ARGB6666("ARGB 6666"),
    ARGB4444("ARGB 4444"),
    RGB565("RGB 565"),
    // RGB565_ALPHA4("RGB 565 with extra 4-bit alpha channel"),
    ALPHA8("8-bit Alpha"),
    ALPHA4("4-bit Alpha"),
    MONOCHROME("1-bit monochrome"),
    GRAYSCALE8("8-bit grayscale"),
    GRAYSCALE4("4-bit grayscale");
    // TRANSPARENCY_BITMASK("1-bit transparency mask");

    public final String description;

    private Encoding(String description) {
      this.description = description;
    }
  }

  public enum Storage {
    PROGMEM("Data stored in PROGMEM"),
    SPIFFS("Data stored in a SPIFFS filesystem");

    public final String description;

    private Storage(String description) {
      this.description = description;
    }
  }

  public enum Compression {
    NONE("Non-compressed raster"),
    RLE("Run-length encoded");

    public final String description;

    private Compression(String description) {
      this.description = description;
    }
  }

  private Storage storage;
  private Encoding encoding;
  private Compression compression;
  // private String name;
  // private String resourceName;
  private File outputHeaderDirectory;
  private File outputPayloadDirectory;

  private String bgColor = "color::Transparent";
  private String fgColor = "color::Black";
  private boolean autoCrop = true;

  public ImportOptions() {
    storage = Storage.PROGMEM;
    encoding = Encoding.RGB565;
    compression = Compression.NONE;
  }

  public Storage getStorage() {
    return storage;
  }

  public Encoding getEncoding() {
    return encoding;
  }

  public Compression getCompression() {
    return compression;
  }

  public boolean getAutoCrop() {
    return autoCrop;
  }

  // public String getName() { return name; }
  // public String getResourceName() { return resourceName; }
  public File getOutputHeaderDirectory() {
    return outputHeaderDirectory;
  }

  public File getOutputPayloadDirectory() {
    return outputPayloadDirectory;
  }

  public String getBgColor() {
    return bgColor;
  }

  public String getFgColor() {
    return fgColor;
  }

  public ImportOptions initFromInput(File input) {
    // setName(getRecommendedNameFromInputFilename(input.getName()));
    setOutputDirectory(input.getParentFile());
    return this;
  }

  public ImportOptions setStorage(Storage storage) {
    this.storage = storage;
    return this;
  }

  public ImportOptions setEncoding(Encoding encoding) {
    this.encoding = encoding;
    return this;
  }

  public ImportOptions setCompression(Compression compression) {
    this.compression = compression;
    return this;
  }

  public ImportOptions setAutoCrop(boolean autoCrop) {
    this.autoCrop = autoCrop;
    return this;
  }

  // public ImportOptions setName(String name) {
  // this.name = name;
  // //if (this.resourceName == null) {
  // this.resourceName = getResourceNameFromName(name);
  // //}
  // return this;
  // }

  public ImportOptions setOutputHeaderDirectory(File outputHeaderDirectory) {
    this.outputHeaderDirectory = outputHeaderDirectory;
    return this;
  }

  public ImportOptions setOutputPayloadDirectory(File outputPayloadDirectory) {
    this.outputPayloadDirectory = outputPayloadDirectory;
    return this;
  }

  public ImportOptions setOutputDirectory(File outputDirectory) {
    this.outputHeaderDirectory = outputDirectory;
    this.outputPayloadDirectory = outputDirectory;
    return this;
  }

  public ImportOptions setBgColor(String color) {
    bgColor = color;
    return this;
  }

  public ImportOptions setFgColor(String color) {
    fgColor = color;
    return this;
  }

  public static String getRecommendedNameFromInputFilename(String filename) {
    String name = filename;
    int tmp;
    if ((tmp = name.lastIndexOf('.')) >= 0) {
      name = name.substring(0, tmp);
    }
    if ((tmp = name.lastIndexOf(File.pathSeparator)) >= 0) {
      name = name.substring(tmp + 1);
    }
    return name;
  }

  public static String getResourceNameFromName(String name) {
    return name.replace(".", "_").replace("-", "_").replace(" ", "_");
  }
}
