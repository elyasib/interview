package forex.domain.oneforge

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import forex.domain.{ Price, Timestamp }
import io.circe.Decoder
import io.circe.generic.semiauto._

case class OneForgeQuote(
    symbol: String,
    price: Price,
    bid: Price,
    ask: Price,
    timestamp: Timestamp
)

object OneForgeQuote extends FailFastCirceSupport {
  implicit val decoder1: Decoder[OneForgeQuote] = deriveDecoder[OneForgeQuote]
}
