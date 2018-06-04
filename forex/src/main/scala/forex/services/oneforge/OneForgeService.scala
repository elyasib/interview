package forex.services.oneforge

import java.util.concurrent.TimeUnit

import cats.Eval
import forex.config._
import forex.main.{ ActorSystems, AppEffect, AppStack, Executors }
import forex.services.OneForge
import org.zalando.grafter.{ Start, StartResult }
import org.zalando.grafter.macros.{ defaultReader, readerOf }

import scala.concurrent.duration.FiniteDuration

@defaultReader[OneForgeServiceLive]
trait OneForgeService {
  def service: OneForge[AppEffect]
}

@readerOf[ApplicationConfig]
case class OneForgeServiceLive(
    client: Client,
    cache: Cache,
    actorSystems: ActorSystems,
    executors: Executors,
    serviceConfig: RatesServiceConfig
) extends OneForgeService
    with Start {

  implicit val executor = executors.default

  override val service: OneForge[AppEffect] = Interpreters.dummy[AppStack](cache)

  val scheduler = actorSystems.system.scheduler

  override def start: Eval[StartResult] =
    StartResult.eval("Starting the 1Forge live service") {
      scheduler.schedule(FiniteDuration(0, TimeUnit.SECONDS), serviceConfig.cache.timeToRefresh) {
        for {
          rate ← client.fetchRates
        } cache.update(rate.pair, rate)
      }
    }

}

@readerOf[ApplicationConfig]
case class OneForgeServiceDummy(
    cache: Cache
) extends OneForgeService {
  override val service: OneForge[AppEffect] = Interpreters.dummy[AppStack](cache)
}
