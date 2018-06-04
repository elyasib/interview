package forex.interfaces.api.utils

import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, MalformedQueryParamRejection}
import Directives._
import forex.domain.Currency
import forex.processes.rates.messages.Error
import Error._

object ApiRejectionHandler {

  def apply(): server.RejectionHandler =
    server.RejectionHandler.newBuilder()
      .handle{
        case MalformedQueryParamRejection(name, msg, _) if (name == "to" || name == "from") ⇒
          val currencies = Currency.currencies.mkString(",")
          val message = s"$msg is not a supported currency. Currencies supported are: $currencies"
          complete(StatusCodes.BadRequest, BadRequest(message))
      }
      .result()
      .withFallback(server.RejectionHandler.default)
}
