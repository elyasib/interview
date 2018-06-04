package forex.domain

import cats.Show
import io.circe._
import cats.implicits._
import forex.domain.Rate.Pair

sealed trait Currency
object Currency {
  final case object AUD extends Currency
  final case object CAD extends Currency
  final case object CHF extends Currency
  final case object EUR extends Currency
  final case object GBP extends Currency
  final case object NZD extends Currency
  final case object JPY extends Currency
  final case object SGD extends Currency
  final case object USD extends Currency

  implicit val show: Show[Currency] = Show.show {
    case AUD ⇒ "AUD"
    case CAD ⇒ "CAD"
    case CHF ⇒ "CHF"
    case EUR ⇒ "EUR"
    case GBP ⇒ "GBP"
    case NZD ⇒ "NZD"
    case JPY ⇒ "JPY"
    case SGD ⇒ "SGD"
    case USD ⇒ "USD"
  }

  implicit val pairShow: Show[Pair] = Show.show(pair ⇒ pair.from.show + pair.to.show)

  val currencies = Set(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD)

  val currencyPairs: Set[Pair] = for {
    currency1 ← currencies
    currency2 ← currencies
    if currency1 != currency2
  } yield Pair(currency1, currency2)

  val currencyPairsAsString: Set[String] = currencyPairs.map(_.show)

  def fromString(s: String): Currency = s match {
    case "AUD" | "aud" ⇒ AUD
    case "CAD" | "cad" ⇒ CAD
    case "CHF" | "chf" ⇒ CHF
    case "EUR" | "eur" ⇒ EUR
    case "GBP" | "gbp" ⇒ GBP
    case "NZD" | "nzd" ⇒ NZD
    case "JPY" | "jpy" ⇒ JPY
    case "SGD" | "sgd" ⇒ SGD
    case "USD" | "usd" ⇒ USD
  }

  implicit val encoder: Encoder[Currency] =
    Encoder.instance[Currency] { show.show _ andThen Json.fromString }
}
