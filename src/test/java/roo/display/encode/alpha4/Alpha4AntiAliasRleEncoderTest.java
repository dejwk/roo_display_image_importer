package roo.display.encode.alpha4;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import roo.display.encode.Encoder;

class Alpha4AntiAliasRleEncoderTest {

  @Test
  void testSingleValues() {
    assertEquals("90", encode("F"));
    assertEquals("10", encode("0"));
    assertEquals("0D", encode("D"));
  }

  @Test
  void testRunsOfTwo() {
    assertEquals("A0", encode("FF"));
    assertEquals("20", encode("00"));
    assertEquals("00D0", encode("DD"));
  }

  @Test
  void testRunsOfThree() {
    assertEquals("B0", encode("FFF"));
    assertEquals("30", encode("000"));
    assertEquals("0FD0", encode("DDD"));
  }

  @Test
  void testRunsOfFour() {
    assertEquals("C0", encode("FFFF"));
    assertEquals("40", encode("0000"));
    assertEquals("800C", encode("CCCC"));
  }

  @Test
  void testRunsOfFive() {
    assertEquals("D0", encode("FFFFF"));
    assertEquals("50", encode("00000"));
    assertEquals("801C", encode("CCCCC"));
  }

  @Test
  void testRunsOfSeven() {
    assertEquals("F0", encode("FFFFFFF"));
    assertEquals("70", encode("0000000"));
    assertEquals("803C", encode("CCCCCCC"));
  }

  @Test
  void testRunsOfEight() {
    assertEquals("F9", encode("FFFFFFFF"));
    assertEquals("71", encode("00000000"));
    assertEquals("804C", encode("CCCCCCCC"));
  }

  @Test
  void testRunsOfEleven() {
    assertEquals("FC", encode("FFFFFFFFFFF"));
    assertEquals("74", encode("00000000000"));
    assertEquals("807C", encode("CCCCCCCCCCC"));
  }

  @Test
  void testRunsOfTwelve() {
    assertEquals("FD", encode("FFFFFFFFFFFF"));
    assertEquals("75", encode("000000000000"));
    assertEquals("8090C0", encode("CCCCCCCCCCCC"));
  }

  @Test
  void testConsecutiveRuns() {
    assertEquals("B2", encode("FFF00"));
    assertEquals("39", encode("000F"));
    assertEquals("A0FD", encode("FFDDD"));
    assertEquals("A0FD0F320020", encode("FFDDD3330022"));
  }

  @Test
  void testTwoDistincts() {
    assertEquals("0D03", encode("D3"));
    assertEquals("9050", encode("F5"));
    assertEquals("1090", encode("09"));
  }

  @Test
  void testThreeDistincts() {
    assertEquals("1910", encode("0F0"));
    assertEquals("9190", encode("F0F"));
    assertEquals("1901", encode("0F1"));
    assertEquals("0119", encode("10F"));
    assertEquals("812F10", encode("2F1"));
    assertEquals("811040", encode("104"));
    assertEquals("812C10", encode("2C1"));
    assertEquals("811B40", encode("1B4"));
  }

  @Test
  void testLongChains() {
    assertEquals("863D3F03C5", encode("3D3F03C5"));
    assertEquals("863D3F03C5A863D3F03C50", encode("3D3F03C5FF3D3F03C5"));
    assertEquals("853D3F03C801A853D3F03C", encode("3D3F03CAAAAA3D3F03C"));
    assertEquals("853D3F03C0F5801A", encode("3D3F03C555AAAAA"));
    assertEquals("853D3F03C10F59801A", encode("3D3F03C0555FAAAAA"));
  }

  private static int parseHexDigit(char digit) {
    if (digit >= 'A' && digit <= 'F') return digit - 'A' + 10;
    if (digit >= 'a' && digit <= 'f') return digit - 'q' + 10;
    if (digit >= '0' && digit <= '9') return digit - '0';
    throw new AssertionError(digit);
  }

  private static char toHexDigit(int digit) {
    return digit >= 10 ? (char)(digit - 10 + 'A') : (char)(digit + '0');
  }

  private static char[] byteToString(byte value) {
    return new char[] { toHexDigit(value >> 4 & 0xF), toHexDigit(value & 0xF) };
  }

  private static String encode(String input) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      Encoder encoder = new Alpha4AntiAliasRleEncoder(bos);
      for (char c : input.toCharArray()) {
        int pixel = (parseHexDigit(c) * 0x11) << 24;
        encoder.encodePixel(pixel);
      }
      encoder.close();
      byte[] result = bos.toByteArray();
      StringBuilder builder = new StringBuilder();
      for (byte b : result) { builder.append(byteToString(b)); }
      return builder.toString();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
}
