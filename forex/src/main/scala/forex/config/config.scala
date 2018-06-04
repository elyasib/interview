package forex.config

import org.zalando.grafter.macros._

import scala.concurrent.duration.FiniteDuration
import org.zalando.grafter.syntax.rewriter._

@readers
case class ApplicationConfig(
    akka: AkkaConfig,
    api: ApiConfig,
    executors: ExecutorsConfig,
    ratesService: RatesServiceConfig
)

case class AkkaConfig(
    name: String,
    exitJvmTimeout: Option[FiniteDuration]
)

case class ApiConfig(
    interface: String,
    port: Int
)

case class ExecutorsConfig(
    default: String
)

case class RatesServiceConfig(
    serviceType: String,
    //client: Option[ClientConfig],
    //cacheConfig: Option[CacheConfig]
    client: ClientConfig,
    cache: CacheConfig
)

case class ClientConfig(
    url: String,
    apiKey: String
)

case class CacheConfig(
    maxAge: FiniteDuration,
    timeToRefresh: FiniteDuration
)
