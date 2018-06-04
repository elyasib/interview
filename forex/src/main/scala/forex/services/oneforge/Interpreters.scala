package forex.services.oneforge

import java.math.MathContext

import cats.data.EitherT
import com.typesafe.scalalogging.LazyLogging
import forex.domain._
import monix.eval.Task
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._

import scala.util.Random

object Interpreters {
  def dummy[R](
      cacheService: Cache
  )(
      implicit
      m1: _task[R]
  ): Algebra[Eff[R, ?]] = new Dummy[R](cacheService)

  def live[R](
      cacheService: Cache
  )(
      implicit
      m1: _task[R]
  ): Algebra[Eff[R, ?]] = new LiveInterpreter[R](cacheService)
}

private[oneforge] final class LiveInterpreter[R](
    val cache: Cache
)(
    implicit val m1: _task[R]
) extends Algebra[Eff[R, ?]] with LazyLogging {
  import monix.cats._

  override def get(pair: Rate.Pair): Eff[R, OneForgeError Either Rate] = {
    val eitherRateOrError = EitherT(cache.get(pair)).leftMap(OneForgeError.toOneForgeError).value
    for {
      result ← fromTask(eitherRateOrError)
    } yield result
  }

  //override def updateCache(rate: Rate): Eff[R, Error Either Unit] = {
  //  val eitherUpdatedOrError = EitherT(cache.update(rate.pair, rate)).leftMap(Error.Cache).value
  //  for {
  //    cacheUpdateResult ← fromTask(eitherUpdatedOrError)
  //  } yield cacheUpdateResult
  //}

}

private[oneforge] final class Dummy[R](
    val cacheService: Cache
)(
    implicit val m1: _task[R]
) extends Algebra[Eff[R, ?]] {
  import cats.implicits._

  def randomPrice: Price = Price(BigDecimal.decimal(Random.nextDouble(), new MathContext(2)))

  override def get(pair: Rate.Pair): Eff[R, OneForgeError Either Rate] =
    for {
      result ← fromTask(Task.now(Rate(pair, randomPrice, Timestamp.now).asRight[OneForgeError]))
    } yield result

  //override def updateCache(rate: Rate): Eff[R, Either[Error, Unit]] =
  //  for {
  //    result ← fromTask(Task.now(().asRight[Error]))
  //  } yield result
}
