package hexwriter;

import java.io.IOException;

public abstract class PayloadWriter {
  int bytesWritten;

  public PayloadWriter() {
    this.bytesWritten = 0;
  }

  public int getBytesWritten() { return bytesWritten; }

  public void writeDeclaration(String var) throws IOException {}

  public void begin(String tableName) throws IOException {}

  public void printComment(String comment) throws IOException {}

  public void newLine() throws IOException {}

  public void end() throws IOException {}

  protected abstract void writeByte(int val) throws IOException;

  protected void writeBytes(byte[] buffer) throws IOException {
    for (int i = 0; i < buffer.length; ++i) writeByte(buffer[i]);
  }

  public void printHex8(int val) throws IOException {
    if (val >= (1 << 8) || val < 0) {
      throw new IllegalArgumentException("Out of bounds: " + val);
    }
    writeByte(val);
    bytesWritten++;
  }

  public void printSignedHex8(int val) throws IOException {
    if (val >= (1 << 7) || val < -(1 << 7)) {
      throw new IllegalArgumentException("Out of bounds: " + val);
    }
    writeByte(val);
    bytesWritten++;
  }

  public void printHex16(int val) throws IOException {
    if (val >= (1 << 16) || val < 0) {
      throw new IllegalArgumentException("Out of bounds: " + val);
    }
    writeByte(val >>> 8);
    writeByte(val & 0xFF);
  }

  public void printSignedHex16(int val) throws IOException {
    if (val > (1 << 15) || val <= (1 << 15)) {
      throw new IllegalArgumentException("Out of bounds: " + val);
    }
    writeByte(val >>> 8);
    writeByte(val & 0xFF);
  }

  public void printHex24(int val) throws IOException {
    if (val >= (1 << 24) || val < 0) {
      throw new IllegalArgumentException("Out of bounds: " + val);
    }
    writeByte(val >>> 16);
    writeByte((val >>> 8) & 0xFF);
    writeByte(val & 0xFF);
  }

  public void printSignedHex24(int val) throws IOException {
    if (val > (1 << 23) || val <= (1 << 23)) {
      throw new IllegalArgumentException("Out of bounds: " + val);
    }
    writeByte(val >>> 16);
    writeByte((val >>> 8) & 0xFF);
    writeByte(val & 0xFF);
  }

  public void printBuffer(final byte[] buffer) throws IOException {
    writeBytes(buffer);
    bytesWritten += buffer.length;
  }
}
