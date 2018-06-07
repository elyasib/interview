package forex.services.oneforge

import java.math.MathContext

import cats.data.EitherT
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._
import forex.client.Client
import forex.domain._
import forex.services.oneforge.OneForgeError.toOneForgeError
import forex.cache.Cache

import scala.util.Random

object Interpreters {
  def dummy[R](
      cache: Cache,
      client: Client
  )(
      implicit
      m1: _task[R]
  ): Algebra[Eff[R, ?]] = new Dummy[R](cache, client)

  def live[R](
      cache: Cache,
      client: Client
  )(
      implicit
      m1: _task[R]
  ): Algebra[Eff[R, ?]] = new LiveInterpreter[R](cache, client)
}

private[oneforge] final class LiveInterpreter[R](
    val cache: Cache,
    val client: Client
)(
    implicit val m1: _task[R]
) extends Algebra[Eff[R, ?]]
    with LazyLogging {
  import cats.implicits._
  import monix.cats._

  override def get(pair: Rate.Pair): Eff[R, OneForgeError Either Rate] = {
    val eitherRateOrError = EitherT(cache.get(pair)).leftMap(toOneForgeError).value
    for {
      result ← fromTask(eitherRateOrError)
    } yield result
  }

  override def updateCache(): Eff[R, OneForgeError Either Unit] =
    for {
      ratesOrError ← fromTask(EitherT(client.fetchRates).leftMap(toOneForgeError).value)
      updateCacheTask = ratesOrError match {
        case Right(rates) ⇒
          EitherT(cache.update(rates)).leftMap(toOneForgeError).value
        case Left(e) ⇒ Task.now(e.asLeft[Unit])
      }
      updatedOrError ← fromTask(updateCacheTask)
    } yield updatedOrError
}

private[oneforge] final class Dummy[R](
    val cacheService: Cache,
    val client: Client
)(
    implicit val m1: _task[R]
) extends Algebra[Eff[R, ?]] {
  import cats.implicits._

  def randomPrice: Price = Price(BigDecimal.decimal(Random.nextDouble(), new MathContext(2)))

  override def get(pair: Rate.Pair): Eff[R, OneForgeError Either Rate] =
    for {
      result ← fromTask(Task.now(Rate(pair, randomPrice, Timestamp.now).asRight[OneForgeError]))
    } yield result

  override def updateCache(): Eff[R, Either[OneForgeError, Unit]] =
    for {
      result ← fromTask(Task.now(().asRight[OneForgeError]))
    } yield result
}
