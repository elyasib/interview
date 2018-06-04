package forex.domain.oneforge

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import forex.domain.{ Price, Timestamp }
import io.circe.{ Decoder, HCursor }
import io.circe.generic.semiauto._

case class OneForgeQuote(
    symbol: String,
    price: Price,
    bid: Price,
    ask: Price,
    timestamp: Timestamp
)

case class OneForgeApiError(error: Boolean, message: String)

object OneForgeResponse extends FailFastCirceSupport {
  implicit val quoteDecoder: Decoder[OneForgeQuote] = deriveDecoder[OneForgeQuote]
  implicit val errorDecoder: Decoder[OneForgeApiError] = deriveDecoder[OneForgeApiError]
  implicit def decodeEither[A, B](
      implicit
      decoderA: Decoder[A],
      decoderB: Decoder[B]
  ): Decoder[Either[A, B]] = { c: HCursor ⇒
    c.as[A] match {
      case Right(a) ⇒ Right(Left(a))
      case _        ⇒ c.as[B].map(Right(_))
    }
  }
}
