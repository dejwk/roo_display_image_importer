package roo.display.imageimporter;

import hexwriter.HexWriter;
import hexwriter.PayloadWriter;
import hexwriter.StringLiteralPayloadWriter;
import java.io.IOException;
import java.io.Writer;
import roo.display.imageimporter.ImportOptions.CppPayloadFormat;

public final class CppPayloadSupport {
  private CppPayloadSupport() {}

  public static CppPayloadFormat parseCppPayloadFormat(String value) {
    for (CppPayloadFormat format : CppPayloadFormat.values()) {
      if (format.toString().equals(value) || format.name().equalsIgnoreCase(value)) {
        return format;
      }
    }
    throw new IllegalArgumentException("expected one of: byte-list, string-literal-wrapper");
  }

  public static PayloadWriter createPayloadWriter(
      Writer writer, CppPayloadFormat cppPayloadFormat) {
    if (usesStringLiteralPayloadWrapper(cppPayloadFormat)) {
      return new StringLiteralPayloadWriter(writer);
    }
    return new HexWriter(writer);
  }

  public static boolean usesStringLiteralPayloadWrapper(CppPayloadFormat cppPayloadFormat) {
    return cppPayloadFormat == CppPayloadFormat.STRING_LITERAL_WRAPPER;
  }

  public static String getPayloadPointerExpression(
      CppPayloadFormat cppPayloadFormat, String dataVar) {
    return usesStringLiteralPayloadWrapper(cppPayloadFormat) ? dataVar + ".bytes" : dataVar;
  }

  public static void writeStringLiteralWrapperIncludes(Writer writer) throws IOException {
    writer.write("#include <stddef.h>\n");
    writer.write("#include <stdint.h>\n");
  }

  public static void writeGeneratedPayloadHelper(Writer writer) throws IOException {
    writer.write("template <size_t N>\n");
    writer.write("struct GeneratedPayload {\n");
    writer.write("  uint8_t bytes[N];\n\n");
    writer.write("  constexpr GeneratedPayload(const char (&literal)[N + 1]) : bytes{} {\n");
    writer.write("    for (size_t i = 0; i < N; ++i) {\n");
    writer.write("      bytes[i] = static_cast<uint8_t>(literal[i]);\n");
    writer.write("    }\n");
    writer.write("  }\n");
    writer.write("};\n\n");
  }
}
