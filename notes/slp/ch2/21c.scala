val RepeatWords = """^.*\(\b[a-zA-Z]+\b\)\1.*""".r 

val tests = List(
  "This is a test without a match",
  "This is a test test with with a a match match",
  "True True",
  "And this is another false",
  "But this is another true true")

println(tests)

for (item <- tests) {
  item match    {
    case RepeatWords(repeated) =>
      println("String with Repeat \"" + repeated + "\"")
    case entry => println("Wrong! \"" + entry + "\"")
  }
}

