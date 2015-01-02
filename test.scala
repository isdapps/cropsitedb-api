import java.io.FileReader
import au.com.bytecode.opencsv.CSVReader

object Stealer {
val r = new FileReader("/home/frostbytten/Development/Projects/agmip/apis/cropsitedb-api/freezer/337d1e7e-ec5f-43a3-8309-de48cad21ba5/acmo.csv")
val c = new CSVReader(r)
var l = c.readNext
var h: List[String] = List()

while(Option(l).isDefined) {
val line = l.toList
line.head match {
case "#" => h = line.tail.map(_.toLowerCase)
case "*" => {
val d = h.zip(line.tail)
println(d)
}
case _ => {}
}
l = c.readNext
}
}
