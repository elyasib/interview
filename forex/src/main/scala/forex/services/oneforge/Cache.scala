package forex.services.oneforge

import forex.config.{ApplicationConfig, RatesServiceConfig}
import forex.domain.{Price, Rate, Timestamp}
import Rate.Pair
import org.zalando.grafter.macros.{defaultReader, readerOf}

import scala.collection.concurrent.{Map, TrieMap}

@defaultReader[SimpleCache]
trait Cache {
  def get(pair: Pair): Rate
  def update(pair: Pair, rate: Rate): Unit
}

@readerOf[ApplicationConfig]
case class SimpleCache(
  ratesServiceConfig: RatesServiceConfig
) extends Cache {

  val cache: Map[Pair, Rate] = TrieMap()

  override def get(pair: Pair): Rate =
    cache.getOrElse(pair, Rate(pair, Price(BigDecimal.valueOf(0)), Timestamp.now))

  override def update(pair: Pair, rate: Rate): Unit = cache.update(pair, rate)
}
