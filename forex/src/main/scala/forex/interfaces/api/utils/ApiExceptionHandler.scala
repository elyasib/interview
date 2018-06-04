package forex.interfaces.api.utils

import java.util.UUID

import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes
import com.typesafe.scalalogging.LazyLogging
import forex.processes._

// TODO: json Marshalling
object ApiExceptionHandler extends LazyLogging {

  def apply(): server.ExceptionHandler =
    server.ExceptionHandler {
      case e: RatesError.NotFound  ⇒
        ctx ⇒
          ctx.complete(StatusCodes.NotFound, e.reason)
      case RatesError.System(reason, _)  ⇒
        ctx ⇒
          ctx.complete(StatusCodes.InternalServerError, reason)
      case RatesError.Generic  ⇒
        ctx ⇒
          ctx.complete(StatusCodes.InternalServerError, "Error in the rates process")
      case t: Throwable ⇒
        ctx ⇒
          val id = UUID.randomUUID
          val reason = s"Unexpected error. ID=$id"
          logger.error(reason, t)
          ctx.complete(StatusCodes.InternalServerError, reason)
    }

}
