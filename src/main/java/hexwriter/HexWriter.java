package hexwriter;

import java.io.InputStream;
import java.io.Writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class HexWriter extends PayloadWriter {
  private final Writer writer;

  public HexWriter(Writer writer) {
    this.writer = writer;
  }

  public void writeDeclaration(String var) throws IOException {
    writer.write("extern const uint8_t " + var + "[] PROGMEM;");
  }

  public void begin(String tableName) throws IOException {
    writer.write("extern const uint8_t ");
    writer.write(tableName);
    writer.write("[] PROGMEM = {");
    newLine();
  }

  public void printComment(String comment) throws IOException {
    writer.write("// ");
    writer.write(comment);
  }

  public void newLine() throws IOException { writer.write("\n  "); }

  public void end() throws IOException { writer.write("\n};\n"); }

  protected void writeByte(int val) throws IOException {
    writer.write("0x");
    printHexChar(writer, val >>> 4);
    printHexChar(writer, val & 0xF);
    writer.write(", ");
  }

  protected void writeBytes(final byte[] buffer) throws IOException {
    for (int j = 0; j < buffer.length; ++j) {
      if (j > 0 && j % 16 == 0) {
        newLine();
      }
      writeByte(buffer[j] & 0xFF);
    }
  }

  private static void printHexChar(Writer os, int val) throws IOException {
    os.write(val >= 10 ? val - 10 + 'A' : val + '0');
  }
}
