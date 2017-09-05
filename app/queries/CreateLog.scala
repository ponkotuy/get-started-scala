package queries

import models.LogType

case class CreateLog(logType: Int, content: String)
