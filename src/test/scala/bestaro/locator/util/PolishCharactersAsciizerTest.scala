package bestaro.locator.util

import org.scalatest.funspec.AnyFunSpec

class PolishCharactersAsciizerTest extends AnyFunSpec {

  describe("Characters asciizer") {
    val asciizer = new PolishCharactersAsciizer()
    it("should convert polish characters to ascii") {
      assert(asciizer.convertToAscii("żółć") == "zolc")
      assert(asciizer.convertToAscii("Żółć") == "Zolc")
      assert(asciizer.convertToAscii("Łódź") == "Lodz")
    }

    it("should not convert ascii-only characters") {
      val originalText = "Gallia est omnis divisa in partes tres, quarum unam incolunt Belgae, " +
        "aliam Aquitani, tertiam qui ipsorum lingua Celtae, nostra Galli appellantur"
      assert(asciizer.convertToAscii(originalText) == originalText)
    }
  }
}
