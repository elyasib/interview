package forex.interfaces.api.utils

import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, MalformedQueryParamRejection}
import Directives._
import forex.domain.Currency

object ApiRejectionHandler {

  def apply(): server.RejectionHandler =
    server.RejectionHandler.newBuilder()
      .handle{
        case MalformedQueryParamRejection(name, msg, _) if (name == "to" || name == "from") ⇒
          val message = s"$msg is not a supported currency. Currencies supported are: ${Currency.currencies.mkString(",")}"
          complete((StatusCodes.BadRequest, message))
      }
      .result()
      .withFallback(server.RejectionHandler.default)
}
