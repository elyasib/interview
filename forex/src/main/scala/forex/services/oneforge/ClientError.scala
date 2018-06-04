package forex.services.oneforge

import scala.util.control.NoStackTrace

sealed trait ClientError extends Throwable with NoStackTrace {
  def reason: String
  def status: Int
}

object ClientError {
  case class RequestError(reason: String, status: Int) extends ClientError
  case class ServerError(reason: String, status: Int) extends ClientError
  case class UnexpectedResponse(reason: String, status: Int) extends ClientError
  case class UnmarshallingError(reason: String, status: Int, underlying: Throwable) extends ClientError
  case class NotFound(reason: String) extends ClientError {
    val status = 404
  }
  case class UnknownError(reason: String, underlying: Throwable) extends ClientError {
    val status = -1
  }
}
