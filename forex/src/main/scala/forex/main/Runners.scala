package forex.main

import cats.~>
import forex.config._
import forex.processes.rates.converters
import forex.processes.rates.messages.AppError
import forex.services.oneforge.RatesError
import monix.eval.Task
import org.atnos.eff.syntax.addon.monix.task.toTaskOps
import org.atnos.eff.syntax.all._
import org.zalando.grafter.macros._

@readerOf[ApplicationConfig]
case class Runners() {
  def run[R](
      app: RatesEffect[R]
  ): Task[RatesError Either R] =
    app.runEither.runAsync

  def runWithProcessErrors[R](
      app: RatesEffect[R]
  ): Task[AppError Either R] =
    app.transform(fromRatesToProcess).runEither.runAsync

  val fromRatesToProcess = new (RatesErrorOrA ~> ErrorOrA) {
    override def apply[A](fa: RatesErrorOrA[A]): ErrorOrA[A] =
      fa.swap.map(converters.toProcessError).swap
  }
}
