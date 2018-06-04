package forex.domain.oneforge

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import forex.domain.{ Price, Timestamp }
import io.circe.{ Decoder, HCursor }
import io.circe.generic.semiauto._

trait OneForgeApiResponse

case class OneForgeApiQuote(
    symbol: String,
    price: Price,
    bid: Price,
    ask: Price,
    timestamp: Timestamp
) extends OneForgeApiResponse

case class OneForgeApiErrorResponse(error: Boolean, message: String)

object OneForgeApiResponse extends FailFastCirceSupport {
  implicit val quoteDecoder: Decoder[OneForgeApiQuote] = deriveDecoder[OneForgeApiQuote]
  implicit val errorDecoder: Decoder[OneForgeApiErrorResponse] = deriveDecoder[OneForgeApiErrorResponse]
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
