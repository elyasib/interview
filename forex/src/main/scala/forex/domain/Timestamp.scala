package forex.domain

import io.circe._
import io.circe.generic.extras.wrapped._
import io.circe.java8.time._
import java.time.{Instant, OffsetDateTime, ZoneId, ZoneOffset}

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  val defaultTZ = ZoneId.of("UTC")

  def now: Timestamp =
    Timestamp(OffsetDateTime.now.withOffsetSameInstant(ZoneOffset.UTC))

  def ofEpoch(epoch: Long): Timestamp =
    Timestamp(OffsetDateTime.ofInstant(Instant.ofEpochSecond(epoch), defaultTZ))

  implicit val encoder: Encoder[Timestamp] =
    deriveUnwrappedEncoder[Timestamp]

  implicit val decoder: Decoder[Timestamp] =
    Decoder.decodeLong.map(ofEpoch)
}
