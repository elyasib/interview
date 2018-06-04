package forex.services.oneforge

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.unmarshalling.Unmarshal
import forex.config.{ ApplicationConfig, RatesServiceConfig }
import forex.domain.{ Currency, Rate }
import Rate.Pair
import Currency.fromString
import com.typesafe.scalalogging.LazyLogging
import forex.main.{ ActorSystems, Executors }
import monix.eval.{ Task, TaskCircuitBreaker }
import org.zalando.grafter.macros.{ defaultReader, readerOf }

import scala.concurrent.Future

@defaultReader[AkkaHttpClient]
trait Client {
  def fetchRates: Task[ClientError Either Seq[Rate]]
}

@readerOf[ApplicationConfig]
case class AkkaHttpClient(
    ratesServiceConfig: RatesServiceConfig,
    actorSystems: ActorSystems,
    executors: Executors
) extends Client
    with LazyLogging {
  import forex.domain.oneforge.OneForgeQuote
  import OneForgeQuote._

  implicit val executor = executors.default
  implicit val system = actorSystems.system
  implicit val materializer = actorSystems.materializer
  val apiKey = ratesServiceConfig.client.apiKey
  val url = ratesServiceConfig.client.url
    .replace("{API_KEY}", apiKey)
    .replace("{PAIRS}", Currency.currencyPairsAsString.mkString(","))
  val request = HttpRequest(HttpMethods.GET, url)

  override def fetchRates: Task[ClientError Either Seq[Rate]] =
    Task.deferFuture {
      logger.info("Trying to fetch rates from 1forge")
      requestQuotes
    }

  private def requestQuotes: Future[ClientError Either Seq[Rate]] =
    Http()
      .singleRequest(request)
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) ⇒
          Unmarshal(entity)
            .to[List[OneForgeQuote]]
            .map(quotes ⇒ quotes.map(toRate))
            .map(Right(_))
        case HttpResponse(_, _, entity, _) ⇒
          Unmarshal(entity).to[String].map(errorReason ⇒ Left(ErrorResponse(errorReason)))
      }

  private def toRate(quote: OneForgeQuote): Rate =
    Rate(toPair(quote.symbol), quote.price, quote.timestamp)

  private def toPair(symbol: String): Pair = Pair(
    fromString(symbol.take(3)),
    fromString(symbol.takeRight(3))
  )
}
