package thesis.db

import org.squeryl.Session
import org.squeryl.SessionFactory
import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query

import thesis._

object ORMTests     {

    def main(args: Array[String])   {
        // Get some sample data

        val d = Document.fromFile()

        ThesisSession.startDbSession()
        insertAllFromDocument(d)
    }

    def insertAllFromDocument(d: thesis.Document)  {
        var dId:Long = 0
        try {
        transaction {
            // Insert aliases
/*
            d.aliases foreach { case (rep, deps) =>
                val eId = ThesisSession.insertAlias(d.resolve(rep), rep.representative, d.filePath, None)

                // Get master alias ID for dependent insertion

                deps foreach { dep =>
                    ThesisSession.insertAlias(d.resolve(dep), 
                                               dep.representative, 
                                               d.filePath,
                                               Some(eId))
                }

            }
*/
            dId = ThesisSession.insertDocument(d)
        }
      transaction {
            d.sentences foreach { s =>
                val sId = ThesisSession.insertSentence(s, dId)

                  s.locations foreach { l =>
                    ThesisSession.insertLocation(l, sId)
                  }
            
            }
            println("Inserted sentences and locations")
            d.conMap foreach { case (rep, deps) =>

                println(rep._1)
                val eId = ThesisSession.insertAlias(rep._1, true, dId, None, rep._3)

                println("Inserted master alias")
                println(rep._1)
                println(deps._1)
                deps._1 match {
                    case Nil => {}
                    case d :: tail =>
                        deps._1 map { tokenList =>
                          ThesisSession.insertProperty(tokenList.tail.mkString(" "), eId);
                        }
                        println("Inserted property")
                }
                println(deps._2)
                println(deps._2.getClass)
                deps._2.asInstanceOf[List[String]] foreach { dep =>
//                    dep match {
//                        case s:String => 
//                            ThesisSession.insertAlias(s, false, d.filePath, Some(eId))
//                        case l:List[String]   =>
//                            ThesisSession.insertAlias(l.mkString(" "), false, d.filePath, Some(eId))
//                        case ll:List[List[String]] =>
                    ThesisSession.insertAlias(dep, false, dId, Some(eId), rep._3)
//                    }
                    println("inserted dependent alias")
                }
            }
        }

        } catch {
            case e:Exception => println("Error on document: " + e.getMessage())
        }
    }

}
