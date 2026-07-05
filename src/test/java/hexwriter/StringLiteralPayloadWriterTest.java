package hexwriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class StringLiteralPayloadWriterTest {
  @Test
  void hexWriterKeepsByteListOutputByDefault() throws Exception {
    StringWriter output = new StringWriter();
    PayloadWriter writer = new HexWriter(output);

    writer.beginStatic("image_data", 4);
    writer.printBuffer(new byte[] {0x00, 0x22, 0x5C, (byte)0xFF});
    writer.end();

    assertEquals("static const uint8_t image_data[] PROGMEM = {\n"
            + "  0x00, 0x22, 0x5C, 0xFF,\n"
            + "};\n",
        output.toString());
  }

  @Test
  void stringLiteralWrapperEscapesAndPreservesAllByteValues() throws Exception {
    byte[] payload = new byte[260];
    for (int i = 0; i < 256; ++i) {
      payload[i] = (byte)i;
    }
    payload[256] = 0x22;
    payload[257] = 0x5C;
    payload[258] = 0x0A;
    payload[259] = 0x00;

    StringWriter output = new StringWriter();
    PayloadWriter writer = new StringLiteralPayloadWriter(output);
    writer.beginStatic("image_data", payload.length);
    writer.printBuffer(payload);
    writer.end();
    String generated = output.toString();

    assertTrue(generated.contains("static constexpr GeneratedPayload<260> image_data PROGMEM("));
    assertTrue(generated.contains("\\x00\\x01\\x02"));
    assertTrue(generated.contains("\\x22"));
    assertTrue(generated.contains("\\x5C"));
    assertTrue(generated.contains("\\x0A\\x00"));
    assertPayloadCompilesAndMatches(generated, payload);
  }

  @Test
  void stringLiteralWrapperSupportsEmptyPayloads() throws Exception {
    StringWriter output = new StringWriter();
    PayloadWriter writer = new StringLiteralPayloadWriter(output);

    writer.beginStatic("empty_data", 0);
    writer.end();

    assertEquals("static constexpr GeneratedPayload<0> empty_data PROGMEM(\n"
            + "  \"\");\n",
        output.toString());
  }

  private static void assertPayloadCompilesAndMatches(String generated, byte[] payload)
      throws Exception {
    assumeTrue(commandSucceeds("g++", "--version"), "g++ is not available");

    File dir = Files.createTempDirectory("payload-writer-test").toFile();
    File source = new File(dir, "payload_test.cpp");
    File binary = new File(dir, "payload_test");
    Files.writeString(source.toPath(), buildCppFixture(generated, payload), StandardCharsets.UTF_8);

    run(dir, "g++", "-std=c++14", source.getAbsolutePath(), "-o", binary.getAbsolutePath());
    run(dir, binary.getAbsolutePath());
  }

  private static String buildCppFixture(String generated, byte[] payload) {
    StringBuilder source = new StringBuilder();
    source.append("#include <stddef.h>\n");
    source.append("#include <stdint.h>\n");
    source.append("#define PROGMEM\n");
    source.append("template <size_t N>\n");
    source.append("struct GeneratedPayload {\n");
    source.append("  uint8_t bytes[N];\n");
    source.append("  constexpr GeneratedPayload(const char (&literal)[N + 1]) : bytes{} {\n");
    source.append("    for (size_t i = 0; i < N; ++i) bytes[i] = static_cast<uint8_t>(literal[i]);\n");
    source.append("  }\n");
    source.append("};\n");
    source.append(generated);
    source.append("static_assert(sizeof(image_data.bytes) == ");
    source.append(payload.length);
    source.append(", \"payload size mismatch\");\n");
    source.append("int main() {\n");
    for (int i = 0; i < payload.length; ++i) {
      source.append("  if (image_data.bytes[");
      source.append(i);
      source.append("] != ");
      source.append(payload[i] & 0xFF);
      source.append(") return ");
      source.append((i % 250) + 1);
      source.append(";\n");
    }
    source.append("  return 0;\n");
    source.append("}\n");
    return source.toString();
  }

  private static boolean commandSucceeds(String... command)
      throws IOException, InterruptedException {
    return new ProcessBuilder(command).redirectErrorStream(true).start().waitFor() == 0;
  }

  private static void run(File directory, String... command)
      throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command)
        .directory(directory)
        .inheritIO()
        .start();
    assertEquals(0, process.waitFor());
  }
}
