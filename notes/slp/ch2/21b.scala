val LowerCaseWords = """(\b[a-z]+b[\.,\s])""".r

val tests = List.fromString("These are some test words such as Bob, bob, bub, slub, slob and cat.", ' ')

println(tests)

for (item <- tests) {
  item match    {
    case LowerCaseWords(lowercase) =>
      println("Lower Case with B \"" + lowercase + "\"")
    case entry => println("Wrong! \"" + entry + "\"")
  }
}

