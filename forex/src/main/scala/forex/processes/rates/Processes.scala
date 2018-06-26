package forex.processes.rates

import cats.Monad
import forex.domain.Rate.Pair
import forex.domain._
import forex.services._

object Processes {
  def apply[F[_]]: Processes[F] =
    new Processes[F] {}
}

trait Processes[F[_]] {
  import messages._

  def get(
      request: GetRequest
  )(
      implicit
      M: Monad[F],
      OneForge: OneForge[F]
  ): F[Rate] =
    OneForge.get(Pair(request.from, request.to))
}
