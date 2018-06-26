package forex.processes.rates

import forex.services.oneforge.RatesError

package object converters {
  import messages._

  def toProcessError[T <: Throwable](t: T): AppError = t match {
    case RatesError.Generic ⇒
      AppError.Generic
    case RatesError.System(reason, err) ⇒
      AppError.System(reason, err)
    case RatesError.NotFound(reason) ⇒
      AppError.NotFound(reason)
    case e: AppError ⇒
      e
    case e ⇒
      AppError.System(e.getMessage, e)
  }

}
