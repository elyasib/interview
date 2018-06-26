package forex.services.oneforge

import cats.Eval
import com.typesafe.scalalogging.LazyLogging
import forex.config._
import forex.main._
import forex.services.OneForge
import forex.cache.Cache
import forex.client.Client
import monix.eval.Task
import org.zalando.grafter.{Start, StartResult}
import org.zalando.grafter.macros.{defaultReader, readerOf}

@defaultReader[CacheRefresherLive]
trait CacheRefresher {
  def service: OneForge[RatesEffect]
}

@readerOf[ApplicationConfig]
case class CacheRefresherDummy(
    interpreter: Interpreter
) extends CacheRefresher {
  override val service: OneForge[RatesEffect] = interpreter.implementation[RatesStack]
}

@readerOf[ApplicationConfig]
case class CacheRefresherLive(
    interpreter: Interpreter,
    actorSystems: ActorSystems,
    executors: Executors,
    serviceConfig: RatesServiceConfig,
    runners: Runners
) extends CacheRefresher
    with Start
    with LazyLogging {
  import monix.execution.Scheduler
  import monix.execution.ExecutionModel

  implicit lazy val executor = executors.default
  implicit lazy val taskScheduler = Scheduler(executor, ExecutionModel.Default)
  override val service: OneForge[RatesEffect] = interpreter.implementation[RatesStack]
  val scheduler = actorSystems.system.scheduler
  val timeToRefreshCache = serviceConfig.timeToRefreshCache
  import scala.concurrent.duration._

  // The cache refresher task should be created/scheduled in Start#start to avoid task duplications
  override def start: Eval[StartResult] =
    StartResult.eval("Starting the 1Forge live service") {
      scheduler.schedule(0.seconds, timeToRefreshCache) { refreshCacheTaskWithLogging.runAsync }
    }

  val refreshCacheTask: Task[RatesError Either Unit] =
    runners.run(service.updateCache())

  val refreshCacheTaskWithLogging: Task[Unit] =
    refreshCacheTask.map {
      case Left(e) =>
        logger.error(s"Failed to refresh the cache. Error=${e.toString}")
      case _ =>
        logger.info("Cache refreshed successfully")
    }
}
