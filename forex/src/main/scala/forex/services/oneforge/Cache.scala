package forex.services.oneforge

import forex.config.{ApplicationConfig, RatesServiceConfig}
import forex.domain.{Currency, Rate, Timestamp}
import Rate.Pair
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import org.zalando.grafter.macros.{defaultReader, readerOf}
import CacheError._

import scala.collection.concurrent.{Map, TrieMap}

@defaultReader[SimpleCache]
trait Cache {
  def get(pair: Pair): Task[CacheError Either Rate]
  def update(rates: Seq[Rate]): Task[CacheError Either Unit]
}

@readerOf[ApplicationConfig]
case class SimpleCache(
    ratesServiceConfig: RatesServiceConfig
) extends Cache
    with LazyLogging {
  import cats.implicits._

  val maxAge = ratesServiceConfig.cache.maxAge
  val cache: Map[Pair, Rate] = TrieMap()

  override def get(pair: Pair): Task[CacheError Either Rate] = Task.now(getFromCache(pair))
    .onErrorRecover {
      case t: Throwable =>
        CacheException(t).asLeft[Rate]
    }

  def getFromCache(pair: Pair): CacheError Either Rate = cache.get(pair) match {
    case Some(rate) if !rate.isExpired(maxAge) ⇒
      Right(rate)
    case Some(rate) ⇒
      Left(ExpiredRate(pair))
    case None if Currency.currencyPairs.contains(pair) ⇒
      Left(ExpiredRate(pair))
    case None ⇒
      Left(NotSupported(pair))
  }

  override def update(rates: Seq[Rate]): Task[CacheError Either Unit] =
    Task.now(rates.foreach(rate ⇒ cache.update(rate.pair, rate)).asRight[CacheError])
      .onErrorRecover {
        case t: Throwable ⇒ CacheException(t).asLeft
      }
}
