val deps = {((loadNode \\ "dep") filter { dep => (dep \ "@type").toString == "nn" }) \ "governor" \\ "@idx"} zip ((loadNode \\ "dep") filter { dep => (dep \ "@type").toString == "nn" }) \ "dependent" \\ "@idx"

val nns = {(((loadNode \\ "basic-dependencies") \\ "dep") filter { dep => (dep \ "@type").toString == "nn" }) \ "governor" \\ "@idx"} zip (((loadNode \\ "basic-dependencies") \\ "dep") filter { dep => (dep \ "@type").toString == "nn" }) \ "dependent" \\ "@idx"


val depsL = deps map { dep => (dep._1,List(dep._2)) }

def group(list: List[(Node,List[Node])]): List[(Node,List[Node])] = list match {
     case List() => List()
     case d1 :: d2 :: rest => if (d1._1 == d2._1)
        group((d1._1,d1._2 ::: d2._2) :: rest)
     else
        d1 :: group(d2 :: rest)
        case d1 :: rest => d1 :: group(rest)
     }

val depsT = group(depsL.toList)

val depsS = depsT map { dep => (dep._1,dep._2.toSet) }

val br = new BufferedReader(new FileReader("ww2sample.txt"))

val toks = PTBTokenizerFactory.newPTBTokenizerFactory(false).getTokenizer(br).tokenize().toList

depsS map { 
    dep => (dep._2 map 
        { ds => toks(ds.text.toInt-1).toString })
         + toks(dep._1.text.toInt-1).toString 
    }
    
def getID(node: NodeSeq, id: String): String = {
    (node filter { w => (w \\ "@id").text == id }) \ "word" text
}

val nns = {(((loadNode \\ "basic-dependencies") \\ "dep") filter { dep => (dep \ "@type").toString == "nn" }) \ "governor" \\ "@idx"} zip (((loadNode \\ "basic-dependencies") \\ "dep") filter { dep => (dep \ "@type").toString == "nn" }) \ "dependent" \\ "@idx"

val nnsL = nns map { dep => (dep._1,List(dep._2)) }

val nnsT = group(nnsL.toList)

val nnsS = nnsT map { dep => (dep._1,dep._2.toSet) }

nnsS map { dep => (dep._2 map { ds => getID(s,ds.text) }) + getID(s,dep._1.text) }
    
def getNoundsFromDependencies(node: NodeSeq): List[Set[String]] = {

val nns = {((node \\ "dep") filter { dep => (dep \ "@type").toString == "nn" }) \ "governor" \\ "@idx"} zip ((node \\ "dep") filter { dep => (dep \ "@type").toString == "nn" }) \ "dependent" \\ "@idx"

val nnsL = nns map { dep => (dep._1,List(dep._2)) }

val nnsT = group(nnsL.toList)

val nnsS = nnsT map { dep => (dep._1,dep._2.toSet) }

nnsS map { dep => (dep._2 map { ds => getID(s,ds.text) }) + getID(s,dep._1.text) }

}






// get ners
val tokList = ((loadNode \\ "token") toList) map { tok => List(tok) }
def groupNer(list: List[List[Node]]): List[List[Node]] = list match {
    case List() => List()
    case d1 :: d2 :: rest => 
        if ((d1.last \ "NER").text == (d2.last \ "NER").text)
            groupNer((d1 ::: d2) :: rest)
        else
            d1 :: groupNer(d2 :: rest)
    case d1 :: rest => d1 :: groupNer(rest)
}
val ners = groupNer(tokList) filter { l => (l.head \ "NER").text != "O" }



def parsetree(node: ParseTreeNode, elems: List[String]): ParseTreeNode = {    
    if (elems isEmpty) { return node }
    else if ('(' == ((elems head) head)) {
        if (node.parent != null)
            println("1\t" + elems.head + "-" + node.parent.word + "/" + node.parent.pos)
        node.pos = ((elems head) tail)
        val child = new ParseTreeNode(node)           
        node :+ child
        return parsetree(child,elems tail)
    }
    else if (elems.head == ")") {     
        println("2\t" + elems.head + "-" + node.parent.word + "/" + node.parent.pos)  
        return parsetree(node.parent,elems tail)     
    }
    else    {
        println("3\t" + elems.head + "-" + node.parent.word + "/" + node.parent.pos)
        node.parent.word = elems.head
        node.parent.childs = List[ParseTreeNode]()
        return parsetree(node,elems tail)
    }
}
// perhaps split on ( as well, use pattern matching
// parent not being used correctly?
// write tab-delemited tree visualization for debugging!