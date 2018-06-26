package forex.client

import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import forex.domain.oneforge.{OneForgeApiErrorResponse, OneForgeApiQuote}
import forex.domain.oneforge.OneForgeApiResponse._
import forex.domain.{Currency, Rate}
import Rate.Pair
import Currency.fromString
import ClientError._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

object OneForgeResponseHandler extends LazyLogging {
  type Rates = Seq[Rate]
  type Quotes = Seq[OneForgeApiQuote]

  def toRate(quote: OneForgeApiQuote): Rate =
    Rate(toPair(quote.symbol), quote.price, quote.timestamp)

  def toPair(symbol: String): Pair = Pair(
    fromString(symbol.take(3)),
    fromString(symbol.takeRight(3))
  )

  def handleOkResponse(entity: HttpEntity)(
      implicit m: Materializer,
      e: ExecutionContext
  ): Future[ClientError Either Rates] =
    Unmarshal(entity)
      .to[OneForgeApiErrorResponse Either Quotes]
      .map {
        case Left(e) ⇒
          UnexpectedResponse(e.message, 200).asLeft[Rates]
        case Right(quotes) ⇒
          quotes.map(toRate).asRight[ClientError]
      }
      .recover {
        case t ⇒
          UnmarshallingError(t.getMessage, 200, t).asLeft[Rates]
      }

  def handleNotFound(entity: HttpEntity)(
      implicit m: Materializer,
      e: ExecutionContext
  ): Future[ClientError Either Rates] =
    Future.successful(NotFound("Not found").asLeft[Rates])

  def handle4xxResponse(response: HttpResponse)(
      implicit m: Materializer,
      e: ExecutionContext
  ): Future[ClientError Either Rates] =
    handleResponse(response, "Error in request to 1forge", RequestError.apply)

  def handle5xxResponse(response: HttpResponse)(
      implicit m: Materializer,
      e: ExecutionContext
  ): Future[ClientError Either Rates] =
    handleResponse(response, "1forge server error", ServerError.apply)

  def handleOtherResponse(response: HttpResponse)(
      implicit m: Materializer,
      e: ExecutionContext
  ): Future[ClientError Either Rates] =
    handleResponse(response, "Unexpected response from 1forge", UnexpectedResponse.apply)

  private[this] def handleResponse[T <: ClientError](
      response: HttpResponse,
      defaultReason: String,
      toError: (String, Int) ⇒ T
  )(
      implicit m: Materializer,
      e: ExecutionContext
  ): Future[ClientError Either Rates] =
    Unmarshal(response.entity)
      .to[OneForgeApiErrorResponse]
      .map(apiError ⇒ toError(apiError.message, response.status.intValue).asLeft[Rates])
      .recover {
        case _ ⇒
          toError(defaultReason, response.status.intValue).asLeft[Rates]
      }
}
