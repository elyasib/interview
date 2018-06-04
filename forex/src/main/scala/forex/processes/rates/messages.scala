package forex.processes.rates

import forex.domain._
import scala.util.control.NoStackTrace
import forex.services.oneforge.OneForgeError

package messages {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
  import io.circe.Json

  sealed trait Error extends Throwable with NoStackTrace

  object Error extends FailFastCirceSupport {
    import io.circe.Encoder

    final case object Generic extends Error
    final case class System(reason: String, underlying: Throwable) extends Error
    final case class NotFound(reason: String) extends Error
    final case class BadRequest(reason: String) extends Error

    implicit def toJson[E <: Error]: Encoder[E] = new Encoder[E] {
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
