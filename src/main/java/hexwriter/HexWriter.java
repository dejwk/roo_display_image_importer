package hexwriter;

import java.io.Writer;

import java.io.IOException;

public class HexWriter extends PayloadWriter {
  private final Writer writer;
  private boolean inline;

  public HexWriter(Writer writer) {
    this.writer = writer;
  }

  public void writeDeclaration(String var) throws IOException {
    writer.write("static const uint8_t " + var + "[] PROGMEM;");
  }

  public void beginStatic(String tableName) throws IOException {
    writer.write("static const uint8_t ");
    writer.write(tableName);
    writer.write("[] PROGMEM = {");
    newLine();
  }

  public void beginExtern(String tableName) throws IOException {
    writer.write("extern const uint8_t ");
    writer.write(tableName);
    writer.write("[] PROGMEM = {");
    newLine();
  }

  public void printComment(String comment) throws IOException {
    if (inline)
      writer.write(" ");
    writer.write("// ");
    writer.write(comment);
  }

  public void newLine() throws IOException {
    writer.write("\n  ");
    this.inline = false;
  }

  public void end() throws IOException {
    writer.write("\n};\n");
  }

  protected void writeByte(int val) throws IOException {
    if (inline)
      writer.write(" ");
    writer.write("0x");
    printHexChar(writer, val >>> 4);
    printHexChar(writer, val & 0xF);
    writer.write(",");
    inline = true;
  }

  protected void writeBytes(final byte[] buffer) throws IOException {
    for (int j = 0; j < buffer.length; ++j) {
      if (j > 0 && j % 16 == 0) {
        newLine();
        inline = false;
      }
      writeByte(buffer[j] & 0xFF);
    }
  }

  private static void printHexChar(Writer os, int val) throws IOException {
    os.write(val >= 10 ? val - 10 + 'A' : val + '0');
  }
}
