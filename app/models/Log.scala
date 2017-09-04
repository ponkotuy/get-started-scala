package models

import scalikejdbc.{DBSession, TypeBinder, WrappedResultSet, autoConstruct}
import skinny.orm.SkinnyCRUDMapperWithId

case class Log(
    id: Long,
    logType: LogType,
    content: String
)

object Log extends SkinnyCRUDMapperWithId[Long, Log] {
  override def idToRawValue(id: Long) = id
  override def rawValueToId(value: Any) = value.toString.toLong

  override def defaultAlias = createAlias("l")
  override def extract(rs: WrappedResultSet, n: scalikejdbc.ResultName[Log]) = autoConstruct(rs, n)

  def create(log: Log)(implicit session: DBSession = autoSession): Long = {
    createWithAttributes(
      'logType -> log.logType.value,
      'content -> log.content
    )
  }
}

sealed abstract class LogType(val value: Int)

object LogType {
  case object NotFoundError extends LogType(1)
  case object Exception extends LogType(2)
  case object FormatError extends LogType(3)

  implicit val typeBinder: TypeBinder[LogType] = TypeBinder.int.map { i => find(i).get }

  val values = NotFoundError :: Exception :: FormatError :: Nil
  def find(value: Int): Option[LogType] = values.find(_.value == value)
}
