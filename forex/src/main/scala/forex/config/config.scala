package forex.config

import org.zalando.grafter.macros._

import scala.concurrent.duration.FiniteDuration

@readers
case class ApplicationConfig(
    akka: AkkaConfig,
    api: ApiConfig,
    executors: ExecutorsConfig,
    ratesService: RatesServiceConfig,
    cache: CacheConfig,
    client: ClientConfig
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
    timeToRefreshCache: FiniteDuration
)

case class ClientConfig(
    url: String,
    apiKey: String,
    maxRetries: Int,
    timeoutPerRetry: FiniteDuration,
    totalTimeout: FiniteDuration,
    backoffTime: FiniteDuration
)

case class CacheConfig(
    maxAge: FiniteDuration
)
