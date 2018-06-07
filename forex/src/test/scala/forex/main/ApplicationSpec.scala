package forex.main

import java.math.MathContext
import java.time.OffsetDateTime

import akka.http.scaladsl.model.StatusCodes
import forex.config.ApplicationConfig
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import forex.config._
import org.zalando.grafter.syntax.rewriter._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import forex.cache.CacheError
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.cache.cache.{Cache, SimpleCache}
import forex.services.oneforge.{CacheError, SimpleCache}
import monix.eval.Task

import scala.collection.concurrent
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._

class ApplicationSpec extends WordSpec with Matchers with BeforeAndAfter with ScalatestRouteTest {
  val precision = new MathContext(5)
  val testConfiguration = pureconfig.loadConfig[ApplicationConfig]("app")
    .right
    .get

  val cacheWithAllCurrencies = new Cache {
    import cats.implicits._
    override def get(pair: Rate.Pair): Task[Either[CacheError, Rate]] =
      Task.now(Rate(pair, Price(BigDecimal(0)), Timestamp.now).asRight[CacheError])

    override def update(rates: Seq[Rate]): Task[Either[CacheError, Unit]] =
      Task.now(().asRight[CacheError])
  }

  val cacheWithExpiredData = new SimpleCache(
    CacheConfig(5.minutes)
  ) {
    val oldRate = Rate(Rate.Pair(Currency.AUD, Currency.JPY), Price(BigDecimal(0)), Timestamp(OffsetDateTime.now.minusDays(1)))
    override val cache: concurrent.Map[Rate.Pair, Rate] = TrieMap(oldRate.pair -> oldRate)
  }

  val appWithAllCurrencies = configure[Application](testConfiguration)
    .replace[Cache](cacheWithAllCurrencies)
    .configure()

  val appWithExpiredData = configure[Application](testConfiguration)
    .replace[Cache](cacheWithExpiredData)
    .configure()

  before {
    appWithAllCurrencies.startAll
    appWithExpiredData.startAll
  }

  after {
    appWithAllCurrencies.stopAll
    appWithExpiredData.stopAll
  }

  import forex.domain.Rate._

  "The forex service" should {
    "return a rate for a valid currency pair" in {
      Get("?from=USD&to=EUR") ~> appWithAllCurrencies.api.routes.route ~> check {
        response.status shouldEqual StatusCodes.OK
        responseAs[Rate].price.value.round(precision) shouldEqual BigDecimal.decimal(0, precision)
      }
    }
  }

  "The forex service" should {
    "return return a BadRequest for an invalid currency pair" in {
      Get("?from=USD&to=MXN") ~> appWithAllCurrencies.api.routes.route ~> check {
        response.status shouldEqual StatusCodes.BadRequest
      }
    }
  }


  "The forex service" should {
    "return return 404 when a valid currency pair has not been updated in the cache" in {
      Get("?from=AUD&to=JPY") ~> appWithExpiredData.api.routes.route ~> check {
        response.status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
