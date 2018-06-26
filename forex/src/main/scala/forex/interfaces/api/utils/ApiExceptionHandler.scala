package forex.interfaces.api.utils

import java.util.UUID

import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes
import com.typesafe.scalalogging.LazyLogging
import forex.processes.rates.messages.AppError
import AppError._

// TODO: json Marshalling
object ApiExceptionHandler extends LazyLogging {

  def apply(): server.ExceptionHandler =
    server.ExceptionHandler {
      case e: NotFound  ⇒
        ctx ⇒
          ctx.complete(StatusCodes.NotFound, e)
      case e: System  ⇒
        ctx ⇒
          ctx.complete(StatusCodes.InternalServerError, e)
      case Generic  ⇒
        ctx ⇒
          ctx.complete(StatusCodes.InternalServerError, Generic)
      case t: Throwable ⇒
        ctx ⇒
          val id = UUID.randomUUID
          val reason = s"Unexpected error. ID=$id"
          val error = System(reason, t)
          logger.error(reason, t)
          ctx.complete(StatusCodes.InternalServerError, error)
    }

}
