package roo.display.imageimporter;

import picocli.CommandLine.ITypeConverter;
import roo.display.imageimporter.ImportOptions.CppPayloadFormat;

public class CppPayloadFormatConverter implements ITypeConverter<CppPayloadFormat> {
  @Override
  public CppPayloadFormat convert(String value) {
    return CppPayloadSupport.parseCppPayloadFormat(value);
  }
}
