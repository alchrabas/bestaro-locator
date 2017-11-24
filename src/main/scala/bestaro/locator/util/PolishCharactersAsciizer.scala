package bestaro.locator.util

class PolishCharactersAsciizer {
  def convertToAscii(input: String): String = {
    input
      .replaceAll("ą", "a").replaceAll("Ą", "A")
      .replaceAll("ć", "c").replaceAll("Ć", "C")
      .replaceAll("ę", "e").replaceAll("Ę", "E")
      .replaceAll("ł", "l").replaceAll("Ł", "L")
      .replaceAll("ó", "o").replaceAll("Ó", "O")
      .replaceAll("ń", "n").replaceAll("Ń", "N")
      .replaceAll("ś", "s").replaceAll("Ś", "S")
      .replaceAll("ż", "z").replaceAll("Ż", "Z")
      .replaceAll("ź", "z").replaceAll("Ź", "Z")
  }
}
