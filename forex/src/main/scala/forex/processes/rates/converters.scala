package forex.processes.rates

import forex.services.oneforge.OneForgeError

package object converters {
  import messages._

  def toProcessError[T <: Throwable](t: T): Error = t match {
    case OneForgeError.Generic ⇒
      Error.Generic
    case OneForgeError.System(reason, err) ⇒
      Error.System(reason, err)
    case OneForgeError.NotFound(reason) ⇒
      Error.NotFound(reason)
    case e: Error ⇒
      e
    case e ⇒
      Error.System(e.getMessage, e)
  }

}
