package forex.processes.rates

import forex.domain._
import scala.util.control.NoStackTrace
import forex.services.oneforge.OneForgeError

package messages {
  sealed trait Error extends Throwable with NoStackTrace

  object Error {
    final case object Generic extends Error
    final case class System(reason: String, underlying: Throwable) extends Error
    final case class NotFound(reason: String) extends Error
  }

  final case class GetRequest(
      from: Currency,
      to: Currency
  )
}
