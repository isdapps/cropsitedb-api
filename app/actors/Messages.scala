package cropsitedb.actors

object Messages {
	case class ProcessFile(dsid:String, filePath:String)
}
