package forex.services.oneforge

import forex.services.oneforge.CacheError.{CacheException, ExpiredRate, NotSupported}

import scala.util.control.NoStackTrace

sealed trait OneForgeError extends Throwable with NoStackTrace

object OneForgeError {
  final case object Generic extends OneForgeError
  final case class System(reason: String, underlying: Throwable) extends OneForgeError
  final case class NotFound(reason: String) extends OneForgeError

  def toOneForgeError[E <: Throwable](cacheError: E): OneForgeError = cacheError match {
    case e: ExpiredRate ⇒ NotFound(e.reason)
    case e: NotSupported ⇒ NotFound(e.reason)
    case e: CacheException ⇒ System(e.reason, e)
    case e: ClientError ⇒ System(e.reason, e)
  }
}
