package cn.ollyice.library.butterknife.compiler;

import android.text.*;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.sun.tools.javac.code.Symbol;

/**
 * Represents an ID of an Android resource.
 */
final class Id {
  private static final ClassName ANDROID_R = ClassName.get("android", "R");
  private static final String R = "R";

  final String value;
  final CodeBlock code;
  final boolean qualifed;

  Id(String value) {
    this(value, null);
  }

  Id(String value, Symbol rSymbol) {
    this.value = value;
    if (rSymbol != null) {
      ClassName className = ClassName.get(rSymbol.packge().getQualifiedName().toString(), R,
          rSymbol.enclClass().name.toString());
      String resourceName = rSymbol.name.toString();

      this.code = className.topLevelClassName().equals(ANDROID_R)
        ? CodeBlock.of("$L.$N", className, resourceName)
        : CodeBlock.of("$T.$N", className, resourceName);
      this.qualifed = true;
    } else {
      this.code = CodeBlock.of("$L", value);
      this.qualifed = false;
    }
  }

  @Override public boolean equals(Object o) {
    return o instanceof Id && value == ((Id) o).value;
  }

  @Override public int hashCode() {
    if (value == null){
      return "".hashCode();
    }
    return value.hashCode();
  }

  @Override public String toString() {
    throw new UnsupportedOperationException("Please use value or code explicitly");
  }
}
