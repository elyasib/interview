package forex.services.oneforge

import java.time.OffsetDateTime

import forex.domain._
import monix.eval.Task
import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.addon.monix.task._

object Interpreters {
  def dummy[R](
      cacheService: Cache
  )(
      implicit
      m1: _task[R]
  ): Algebra[Eff[R, ?]] = new Dummy[R](cacheService)
}

private[oneforge] final class Dummy[R](
    val cacheService: Cache
)(
    implicit val m1: _task[R]
) extends Algebra[Eff[R, ?]] {
  override def get(pair: Rate.Pair): Eff[R, Error Either Rate] =
    for {
      result ← fromTask(Task.now(cacheService.get(pair)))
    } yield Right(result)
}
