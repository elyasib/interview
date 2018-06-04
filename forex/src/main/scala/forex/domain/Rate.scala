package forex.domain

import java.time.OffsetDateTime

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto._
import Currency.fromString

import scala.concurrent.duration.FiniteDuration

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
) {
  def isExpired(maxAge: FiniteDuration): Boolean =
    OffsetDateTime.now.isAfter(timestamp.value.plusNanos(maxAge.toNanos))
}

object Rate extends FailFastCirceSupport {
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

  implicit val decoder: Decoder[Rate] = new Decoder[Rate] {
    override def apply(c: HCursor): Result[Rate] =
      for {
        from ← c.downField("from").as[String]
        to ← c.downField("to").as[String]
        price ← c.downField("price").as[Double]
        timestamp ← c.downField("timestamp").as[String]
      } yield
        Rate(
          Pair(fromString(from), fromString(to)),
          Price(BigDecimal.decimal(price)),
          Timestamp(OffsetDateTime.parse(timestamp))
        )
  }

}
