package forex.cache

import cats.implicits._
import forex.domain.Currency
import forex.domain.Currency._
import forex.domain.Rate.Pair

import scala.util.control.NoStackTrace

trait CacheError extends Throwable with NoStackTrace {
  def reason: String
}

object CacheError {

  final case class ExpiredRate(pair: Pair) extends CacheError {
    override val reason =
      s"The currency pair ${pair.show} rate has not been updated yet"
  }

  final case class NotSupported(pair: Pair) extends CacheError {
    override val reason =
      s"The currency pair ${pair.show} is not supported. Supported pairs are ${Currency.currencyPairsAsString.mkString(",")}"
  }

  final case class CacheException(underlying: Throwable) extends CacheError {
    override val reason = underlying.getMessage
  }

}
