package hexwriter;

import java.io.IOException;
import java.io.OutputStream;

public class BinaryWriter extends PayloadWriter {
  private final OutputStream os;

  public BinaryWriter(OutputStream os) { this.os = os; }

  protected void writeByte(int val) throws IOException { os.write(val); }

  protected void writeBytes(final byte[] buffer) throws IOException {
    os.write(buffer);
  }
}
