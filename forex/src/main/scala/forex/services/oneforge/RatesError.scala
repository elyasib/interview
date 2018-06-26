package forex.services.oneforge

import forex.cache.CacheError.{ CacheException, ExpiredRate, NotSupported }
import forex.client.ClientError

import scala.util.control.NoStackTrace

trait WithReason {
  def reason: String
  override def toString = s"${this.getClass.getSimpleName}(reason=$reason)"
}

//sealed trait OneForgeError extends Throwable with WithReason with NoStackTrace {
sealed trait RatesError extends Throwable with NoStackTrace {
  def reason: String
  override def toString = s"${this.getClass.getSimpleName}(reason=$reason)"
}

object RatesError {
  final case class System(reason: String, underlying: Throwable) extends RatesError
  final case class NotFound(reason: String) extends RatesError
  final case object Generic extends RatesError {
    val reason = "Unknown"
  }

  def toRatesError[E <: Throwable](error: E): RatesError = error match {
    case e: ExpiredRate    ⇒ NotFound(e.reason)
    case e: NotSupported   ⇒ NotFound(e.reason)
    case e: CacheException ⇒ System(e.reason, e)
    case e: ClientError    ⇒ System(e.toString, e)
    case _                 ⇒ Generic
  }
}
