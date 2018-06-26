package forex

import forex.services.oneforge.RatesError
import monix.eval.Task
import org.atnos.eff._
import org.zalando.grafter._

import forex.processes.rates.messages.AppError

package object main {

  type RatesErrorOrA[A] = RatesError Either A
  type ErrorOrA[A] = AppError Either A
  type RatesStack = Fx.fx2[Task, RatesErrorOrA]
  type RatesEffect[R] = Eff[RatesStack, R]
  type _eitherRatesErrorOr[R] = RatesErrorOrA |= R

  def toStartErrorString(results: List[StartResult]): String =
    s"Application startup failed. Modules: ${results
      .collect {
        case StartError(message, ex) ⇒ s"$message [${ex.getMessage}]"
        case StartFailure(message)   ⇒ message
      }
      .mkString(", ")}"

  def toStartSuccessString(results: List[StartResult]): String =
    s"Application startup successful. Modules: ${results
      .collect {
        case StartOk(message) ⇒ message
      }
      .mkString(", ")}"

}
