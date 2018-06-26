package forex.client

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import com.typesafe.scalalogging.LazyLogging
import forex.client.ClientError._
import forex.client.OneForgeResponseHandler._
import forex.config.{ ApplicationConfig, ClientConfig }
import forex.domain.{ Currency, Rate }
import forex.main.{ ActorSystems, Executors }
import forex.task.RetriableTask
import monix.eval.Task
import org.zalando.grafter.macros.{ defaultReader, readerOf }

import scala.collection.immutable
import scala.concurrent.TimeoutException

@defaultReader[AkkaHttpClient]
trait Client {
  def fetchRates: Task[ClientError Either Seq[Rate]]
}

@readerOf[ApplicationConfig]
case class AkkaHttpClient(
    config: ClientConfig,
    actorSystems: ActorSystems,
    executors: Executors
) extends Client
    with LazyLogging {

  implicit lazy val executor = executors.default
  implicit lazy val system = actorSystems.system
  implicit lazy val materializer = actorSystems.materializer
  val url = buildUrl(config.url, config.apiKey)
  val request = HttpRequest(HttpMethods.GET, url)

  val shouldRetry: Throwable PartialFunction Boolean = {
    case e @ (_: TimeoutException | _: ServerError) ⇒ true
    case _                                          ⇒ false
  }

  override def fetchRates: Task[ClientError Either Seq[Rate]] =
    RetriableTask.retriableOf(
      maxRetries = config.maxRetries,
      timeoutPerRetry = config.timeoutPerRetry,
      backoffTime = config.backoffTime,
      totalTimeout = config.totalTimeout,
      shouldTriggerRetry = shouldRetry
    ) {
      requestQuotes
    }

  private def requestQuotes: Task[ClientError Either Rates] = Task.deferFuture {
    logger.info("Trying to fetch rates from 1forge")

    Http()
      .singleRequest(request)
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _)       ⇒ handleOkResponse(entity)
        case HttpResponse(StatusCodes.NotFound, _, entity, _) ⇒ handleNotFound(entity)
        case response @ Is4xx(_)                              ⇒ handle4xxResponse(response)
        case response @ Is5xx(_)                              ⇒ handle5xxResponse(response)
        case response                                         ⇒ handleOtherResponse(response)
      }
      .map {
        case Left(e) if shouldRetry.apply(e) ⇒ throw e
        case other                           ⇒ other
      }
  }

  def buildUrl(baseUrl: String, apiKey: String): String =
    baseUrl
      .replace("{API_KEY}", apiKey)
      .replace("{PAIRS}", Currency.currencyPairsAsString.mkString(","))
}

object Is4xx {
  def unapply(response: HttpResponse): Option[HttpResponse] =
    if (response.status.intValue >= 400 && response.status.intValue < 500) Some(response) else None
}

object Is5xx {
  def unapply(response: HttpResponse): Option[HttpResponse] =
    if (response.status.intValue >= 500) Some(response) else None
}
