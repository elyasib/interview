package forex.domain

import java.time.OffsetDateTime

import io.circe._
import io.circe.generic.semiauto._

import scala.concurrent.duration.FiniteDuration

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
) {
  def isExpired(maxAge: FiniteDuration): Boolean = {
    OffsetDateTime.now.isAfter(timestamp.value.plusNanos(maxAge.toNanos))
  }
}

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  object Pair {
    implicit val encoder: Encoder[Pair] =
      deriveEncoder[Pair]
  }

  implicit val encoder: Encoder[Rate] =
    deriveEncoder[Rate]
}
