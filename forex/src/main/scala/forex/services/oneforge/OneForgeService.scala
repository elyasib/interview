package forex.services.oneforge

import cats.Eval
import com.typesafe.scalalogging.LazyLogging
import forex.config._
import forex.main.{ActorSystems, AppEffect, AppStack, Executors}
import forex.services.OneForge
import monix.eval.Task
import org.zalando.grafter.{Start, StartResult}
import org.zalando.grafter.macros.{defaultReader, readerOf}

@defaultReader[OneForgeServiceLive]
trait OneForgeService {
  def service: OneForge[AppEffect]
}

@readerOf[ApplicationConfig]
case class OneForgeServiceDummy(
    cache: Cache
) extends OneForgeService {
  override val service: OneForge[AppEffect] = Interpreters.dummy[AppStack](cache)
}

@readerOf[ApplicationConfig]
case class OneForgeServiceLive(
    client: Client,
    cache: Cache,
    actorSystems: ActorSystems,
    executors: Executors,
    serviceConfig: RatesServiceConfig
) extends OneForgeService
    with Start
    with LazyLogging {
  import monix.execution.Scheduler
  import monix.execution.ExecutionModel

  implicit lazy val executor = executors.default
  implicit lazy val taskScheduler = Scheduler(executor, ExecutionModel.Default)
  override val service: OneForge[AppEffect] = Interpreters.live[AppStack](cache)
  val scheduler = actorSystems.system.scheduler
  val timeToRefreshCache = serviceConfig.timeToRefreshCache
  import scala.concurrent.duration._

  // The cache refresher task should be created/scheduled in Start#start to avoid task duplications
  override def start: Eval[StartResult] =
    StartResult.eval("Starting the 1Forge live service") {
      scheduler.schedule(0.seconds, timeToRefreshCache) { refreshCacheTask.runAsync }
    }

  val refreshCacheTask = client.fetchRates.flatMap {
      case Left(e) ⇒
        logger.error("Failed to refresh the cache. reason={}", e.reason)
        Task.raiseError(e)
      case Right(rates) ⇒
        cache.update(rates)
    }
}
