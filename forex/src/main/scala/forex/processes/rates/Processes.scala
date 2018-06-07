package forex.processes.rates

import cats.Monad
import cats.data.EitherT
import forex.domain._
import forex.services._
import Rate.Pair

object Processes {
  def apply[F[_]]: Processes[F] =
    new Processes[F] {}
}

trait Processes[F[_]] {
  import messages._
  import converters._

  def get(
      request: GetRequest
  )(
      implicit
      M: Monad[F],
      OneForge: OneForge[F]
  ): F[Error Either Rate] =
    EitherT(OneForge.get(Pair(request.from, request.to))).leftMap(converters.toProcessError).value

}
