package models;

object Utilities    {

    class FString(val string: String) { override def toString = stringFilter(string) }

    private def stringFilter(s: String) = s.replace("-LRB- ","[").replace(" -RRB-","]")

    implicit def stringToFiltered(st:String) = new FString(st)

}

// vim: set ts=4 sw=4 et:
