package forex.services.oneforge

import java.math.MathContext

import forex.config.{ApplicationConfig, RatesServiceConfig}
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.main.{ActorSystems, Executors}
import org.zalando.grafter.macros.{defaultReader, readerOf}

import scala.util.Random

@defaultReader[AkkaHttpClient]
trait Client {
  //def fetchRates: Seq[OneForgeQuote]
  def fetchRates: Seq[Rate]
}

@readerOf[ApplicationConfig]
case class AkkaHttpClient(
  ratesServiceConfig: RatesServiceConfig,
  actorSystems: ActorSystems,
  executors: Executors
) extends Client  {
  val apiKey = ratesServiceConfig.client.apiKey
  val url = ratesServiceConfig.client.url.replace("API_KEY", apiKey)

  def randomPrice: Price = Price(BigDecimal.decimal(Random.nextDouble(), new MathContext(2)))

  //def fetchRates: Seq[Rate] = for {
  //  pair ← Currency.currencyPairs
  //} yield OneForgeQuote(pair, randomPrice, randomPrice, randomPrice, Timestamp.now)

  override def fetchRates: Seq[Rate] = for {
    pair ← Currency.currencyPairs
  } yield Rate(pair, randomPrice, Timestamp.now)
}

case class OneForgeQuote(
  symbol: String,
  price: Price,
  bid: Price,
  ask: Price,
  timestamp: Timestamp
)
