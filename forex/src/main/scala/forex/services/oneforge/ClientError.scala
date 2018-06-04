package forex.services.oneforge

sealed trait ClientError {
  def reason: String
}
case class ErrorResponse(reason: String) extends ClientError
