val AlphaStringsRE = """(\b[a-zA-Z]+\b)""".r

val tests = List.fromString("These are all words but now we sk21p and wonder what 31239 happened .lsdjfm, and here is the end!", ' ')

println(tests)

for (item <- tests) {
  item match {
    case AlphaStringsRE(alphastring) =>
      println("Alphastring \"" + alphastring)
    case _ => println("unrecognized entry")
    }
}
