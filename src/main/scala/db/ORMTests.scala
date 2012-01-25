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
        
        transaction {
            ThesisSession.insertDocument(d)
            //ThesisSession.insertAlias("World War 2", false, "data/ww2sample.txt.xml")
            val alias = d.aliases.keys.head
            ThesisSession.insertAlias(d.resolve(alias), alias.representative, d.filePath, None)
        }

        transaction {
            val t = from(EntityGraph.entities)(s => select(s)).first
            println(t.value)
        }

        insertAllFromDocument(d)
    }

    def insertAllFromDocument(d: thesis.Document)  {

        transaction {
            ThesisSession.insertDocument(d)

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
            d.conMap foreach { case (rep, deps) =>

                println(rep._1)
                val eId = ThesisSession.insertAlias(rep._1, true, d.filePath, None)

                println("Inserted master alias")
                println(rep._1)
                println(deps._1)
                deps._1 match {
                    case Nil => {}
                    case d :: tail =>
                        ThesisSession.insertProperty(deps._1.tail.mkString(" "), eId);
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
                    ThesisSession.insertAlias(dep.mkString(" "), false, d.filePath, Some(eId))
//                    }
                    println("inserted dependent alias")
                }
            }
        }

    }

}
