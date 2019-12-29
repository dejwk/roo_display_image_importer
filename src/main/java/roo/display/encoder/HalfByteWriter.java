package roo.display.encoder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * Optimized for anti-aliased monochrome content with 4-bit alpha. Runs of
 * extreme values (0x0 and 0xF, i.e. fully opaque and fully transparent) are
 * encoded very efficiently (aiming at the cost of ~1 bit per value). Runs of
 * intermediate values are not encoded nearly as efficiently.
 *
 * Specifically:
 * <ul>
 * <li>runs of 1-7 'F's are encoded as half-byte = 0x8 | the count of 'F's (0x9
 * - 0xF)
 * <li>runs of 1-7 '0's are encoded as half-byte = the count of zeros (0x1 -
 * 0x7)
 * <li>a single value 'V' in the range of 0x1 to 0xE is encoded as 0x0 0xV
 * <li>list of values in the range ox 0x1 to 0xE is encoded as 0x8 <values> 0x0
 * <li>runs of 3-10 values 'V' in the range of 0x1 to 0xE are encoded as 0x8 0x0
 * 0x<count-3> 0xV
 * </ul>
 */
public class HalfByteWriter {
  private OutputStream os;
  boolean hasHalfByte;
  int buffer;

  HalfByteWriter(OutputStream os) {
      this.os = os;
      hasHalfByte = false;
      buffer = 0;
  }

  void write(int halfByte) throws IOException {
    if (halfByte < 0 || halfByte > 0xF) {
      throw new IllegalArgumentException();
    }
    if (hasHalfByte) {
      os.write(buffer | halfByte);
      hasHalfByte = false;
    } else {
      buffer = halfByte << 4;
      hasHalfByte = true;
    }
  }

  void close() throws IOException {
    if (hasHalfByte) {
      os.write(buffer);
    }
    os.close();
  }
}
