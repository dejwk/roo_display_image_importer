package hexwriter;

import java.io.IOException;
import java.io.Writer;

public class StringLiteralPayloadWriter extends PayloadWriter {
  private static final int BYTES_PER_LINE = 16;

  private final Writer writer;
  private boolean inline;
  private boolean lineHasComment;
  private boolean stringLiteralOpen;
  private boolean payloadEmitted;
  private int bytesOnLine;

  public StringLiteralPayloadWriter(Writer writer) {
    this.writer = writer;
  }

  public void writeDeclaration(String var) throws IOException {
    writer.write("static const uint8_t " + var + "[] PROGMEM;");
  }

  public void beginStatic(String tableName) throws IOException {
    throw new IllegalArgumentException(
        "StringLiteralPayloadWriter.beginStatic requires an explicit payload size");
  }

  public void beginStatic(String tableName, int payloadSize) throws IOException {
    beginStaticWithSize(tableName, String.valueOf(payloadSize));
  }

  public void beginStatic(String tableName, String sizeExpr) throws IOException {
    beginStaticWithSize(tableName, sizeExpr);
  }

  private void beginStaticWithSize(String tableName, String sizeExpr) throws IOException {
    payloadEmitted = false;
    writer.write("static constexpr GeneratedPayload<");
    writer.write(sizeExpr);
    writer.write("> ");
    writer.write(tableName);
    writer.write(" PROGMEM(");
    newLine();
  }

  public void beginExtern(String tableName) throws IOException {
    throw new IllegalArgumentException(
        "StringLiteralPayloadWriter.beginExtern requires an explicit payload size");
  }

  public void beginExtern(String tableName, int payloadSize) throws IOException {
    beginExternWithSize(tableName, String.valueOf(payloadSize));
  }

  public void beginExtern(String tableName, String sizeExpr) throws IOException {
    beginExternWithSize(tableName, sizeExpr);
  }

  private void beginExternWithSize(String tableName, String sizeExpr) throws IOException {
    writer.write("extern const GeneratedPayload<");
    writer.write(sizeExpr);
    writer.write("> ");
    writer.write(tableName);
    writer.write(" PROGMEM;");
  }

  public void printComment(String comment) throws IOException {
    closeStringLiteral();
    if (inline) {
      writer.write(" ");
    }
    writer.write("// ");
    writer.write(comment);
    if (comment.endsWith("\n")) {
      clearLineState();
    } else {
      inline = true;
      lineHasComment = true;
    }
  }

  public void newLine() throws IOException {
    closeStringLiteral();
    writer.write("\n  ");
    clearLineState();
  }

  public void end() throws IOException {
    if (!payloadEmitted) {
      if (inline && lineHasComment) {
        writer.write("\n  ");
        clearLineState();
      }
      writer.write("\"\"");
      inline = true;
    }
    closeStringLiteral();
    if (inline && lineHasComment) {
      writer.write("\n  ");
    }
    writer.write(");\n");
    clearLineState();
    payloadEmitted = false;
  }

  protected void writeByte(int val) throws IOException {
    if (lineHasComment || bytesOnLine == BYTES_PER_LINE) {
      newLine();
    }
    if (!stringLiteralOpen) {
      writer.write("\"");
      stringLiteralOpen = true;
      inline = true;
    }
    int byteValue = val & 0xFF;
    writer.write("\\x");
    printHexChar(writer, byteValue >>> 4);
    printHexChar(writer, byteValue & 0xF);
    bytesOnLine++;
    payloadEmitted = true;
  }

  protected void writeBytes(final byte[] buffer) throws IOException {
    for (int j = 0; j < buffer.length; ++j) writeByte(buffer[j] & 0xFF);
  }

  private void closeStringLiteral() throws IOException {
    if (!stringLiteralOpen) return;
    writer.write("\"");
    stringLiteralOpen = false;
  }

  private void clearLineState() {
    inline = false;
    lineHasComment = false;
    bytesOnLine = 0;
  }

  private static void printHexChar(Writer os, int val) throws IOException {
    os.write(val >= 10 ? val - 10 + 'A' : val + '0');
  }
}
