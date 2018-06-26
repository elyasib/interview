package forex.services.oneforge

import forex.domain._

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[Rate]
  def updateCache(): F[Unit]
}
