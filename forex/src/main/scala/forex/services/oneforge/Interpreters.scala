package forex.services.oneforge

import java.math.MathContext

import cats.data.EitherT
import forex.cache.Cache
import forex.client.Client
import forex.config.ApplicationConfig
import forex.domain._
import forex.main._eitherRatesErrorOr
import forex.services.oneforge.RatesError.toRatesError
import monix.eval.Task
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._
import org.atnos.eff.all._
import org.zalando.grafter.macros.{ defaultReader, readerOf }
import monix.cats._

import scala.util.Random
@defaultReader[LiveInter]
trait Interpreter {
  def implementation[R: _task: _eitherRatesErrorOr]: Algebra[Eff[R, ?]]
}

@readerOf[ApplicationConfig]
case class LiveInter(
    cache: Cache,
    client: Client
) extends Interpreter {
  def implementation[R: _task: _eitherRatesErrorOr]: Algebra[Eff[R, ?]] =
    new LiveInterpreter[R](cache, client)
}

private[oneforge] final class LiveInterpreter[R: _task: _eitherRatesErrorOr](
    val cache: Cache,
    val client: Client
) extends Algebra[Eff[R, ?]] {

  override def get(pair: Rate.Pair): Eff[R, Rate] = {
    val eitherRateOrErrorTask = EitherT(cache.get(pair)).leftMap(toRatesError).value
    for {
      eitherRateOrError ← fromTask(eitherRateOrErrorTask)
      rate ← fromEither(eitherRateOrError)
    } yield rate
  }

  override def updateCache(): Eff[R, Unit] =
    for {
      ratesOrError ← fromTask(EitherT(client.fetchRates).leftMap(toRatesError).value)
      rates ← fromEither(ratesOrError)
      updatedOrError ← fromTask(EitherT(cache.update(rates)).leftMap(toRatesError).value)
      updated ← fromEither(updatedOrError)
    } yield updated
}

private[oneforge] final class Dummy[R: _task: _eitherRatesErrorOr]() extends Algebra[Eff[R, ?]] {
  import cats.implicits._

  def randomPrice: Price = Price(BigDecimal.decimal(Random.nextDouble(), new MathContext(2)))

  override def get(pair: Rate.Pair): Eff[R, Rate] =
    for {
      eitherErrorOrRate ← fromTask(Task.now(Rate(pair, randomPrice, Timestamp.now).asRight[RatesError]))
      rates ← fromEither(eitherErrorOrRate)
    } yield rates

  override def updateCache(): Eff[R, Unit] =
    for {
      updatedOrError ← fromTask(Task.now(().asRight[RatesError]))
      updated ← fromEither(updatedOrError)
    } yield updated
}
