package forex.processes.rates

import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import forex.domain._
import io.circe.Json

import scala.util.control.NoStackTrace

package messages {

  sealed trait AppError extends Throwable with NoStackTrace

  object AppError extends ErrorAccumulatingCirceSupport {
    import io.circe.Encoder

    final case object Generic extends AppError
    final case class System(reason: String, underlying: Throwable) extends AppError
    final case class NotFound(reason: String) extends AppError
    final case class BadRequest(reason: String) extends AppError

    implicit def toJson[E <: AppError]: Encoder[E] = new Encoder[E] {
      override def apply(error: E): Json = error match {
        case Generic ⇒
          makeJsonError("Error in the rates process", true)
        case System(reason, _) ⇒
          makeJsonError(reason, true)
        case NotFound(reason) ⇒
          makeJsonError(reason, false)
        case BadRequest(reason) ⇒
          makeJsonError(reason, false)
      }
    }

    def makeJsonError(message: String, error: Boolean): Json =
      Json.obj(
        ("error", Json.fromBoolean(error)),
        ("message", Json.fromString(message))
      )
  }

  final case class GetRequest(
      from: Currency,
      to: Currency
  )
}
