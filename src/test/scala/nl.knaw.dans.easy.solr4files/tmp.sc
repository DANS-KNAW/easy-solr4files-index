import scala.collection.mutable
import scala.xml._

val x = <div class="content"><a></a><p><q>hello</q></p><r><p>world</p></r><s></s></div>

val s = mutable.ListBuffer[String]()
def strings(n: Seq[Node]): Unit =
  n.foreach { x =>
    if (x.child.nonEmpty) strings(x.child)
    else {
      s += x.text
      strings(x.child)
    }
  }

strings(x)
s.mkString(" ")


def spacedText(n: Node): String = {
  val s = mutable.ListBuffer[String]()
  def strings(n: Seq[Node]): Unit =
    n.foreach { x =>
      if (x.child.nonEmpty) strings(x.child)
      else {
        s += x.text
        strings(x.child)
      }
    }
  strings(n)
  s.mkString(" ")
}
spacedText(x)