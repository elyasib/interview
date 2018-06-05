package forex.services.oneforge

import forex.domain._

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[OneForgeError Either Rate]
  def updateCache(): F[OneForgeError  Either Unit]
}
