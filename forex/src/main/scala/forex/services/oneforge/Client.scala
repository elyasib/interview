package forex.services.oneforge

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import forex.config.{ ApplicationConfig, RatesServiceConfig }
import forex.domain.{ Currency, Rate }
import com.typesafe.scalalogging.LazyLogging
import forex.main.{ ActorSystems, Executors }
import monix.eval.Task
import org.zalando.grafter.macros.{ defaultReader, readerOf }
import cats.implicits._
import forex.concurrent.RetriableTask
import forex.services.oneforge.ClientError.{ NotFound, ServerError, UnknownError }

import scala.concurrent.TimeoutException

@defaultReader[AkkaHttpClient]
trait Client {
  def fetchRates: Task[ClientError Either Seq[Rate]]
}

@readerOf[ApplicationConfig]
case class AkkaHttpClient(
    config: RatesServiceConfig,
    actorSystems: ActorSystems,
    executors: Executors
) extends Client
    with LazyLogging {
  import forex.services.oneforge.client.OneForgeResponseHandler._
  import scala.concurrent.duration._

  implicit lazy val executor = executors.default
  implicit lazy val system = actorSystems.system
  implicit lazy val materializer = actorSystems.materializer
  val apiKey = config.client.apiKey
  val url = buildUrl(config.client.url, apiKey)
  val request = HttpRequest(HttpMethods.GET, url)

  val shouldRetry: Throwable PartialFunction Boolean = {
    case e @ (_: TimeoutException | _: ServerError) ⇒
      true
  }

  override def fetchRates: Task[ClientError Either Seq[Rate]] =
    RetriableTask.ofFuture(
      5,
      5.seconds,
      2.seconds,
      3.minutes + 30.seconds,
      shouldRetry
    ) {
      requestQuotes
    }

  private def requestQuotes: Task[ClientError Either Rates] = Task.deferFuture {
    logger.info("Trying to fetch rates from 1forge")

    Http()
      .singleRequest(request)
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) ⇒
          handleOkResponse(entity)
        case HttpResponse(StatusCodes.NotFound, _, entity, _) ⇒
          handleNotFound(entity)
        case response @ HttpResponse(status, _, _, _) if status.intValue >= 400 && status.intValue < 500 ⇒
          handle4xxResponse(response)
        case response @ HttpResponse(status, _, _, _) if status.intValue >= 500 ⇒
          handle5xxResponse(response)
        case response ⇒
          handleOtherResponse(response)
      }
      .recover {
        case t ⇒ UnknownError(t.getMessage, t).asLeft[Rates]
      }
      .map {
        case Left(e: ServerError) ⇒ throw e
        case Left(e: NotFound)    ⇒ throw e
        case other                ⇒ other
      }
  }

  def buildUrl(baseUrl: String, apiKey: String): String =
    baseUrl
      .replace("{API_KEY}", apiKey)
      .replace("{PAIRS}", Currency.currencyPairsAsString.mkString(","))
}
