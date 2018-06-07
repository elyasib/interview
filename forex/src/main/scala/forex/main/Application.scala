package forex.main

import forex.config._
import forex.services.oneforge.OneForgeServiceLive
import org.zalando.grafter.macros._
import org.zalando.grafter.syntax.rewriter._

@readerOf[ApplicationConfig]
case class Application(
    api: Api
) {
  def configure(): Application = this.modifyWith[Any] {
    // Use a separate executor service for the async cache refresh task
    case c: OneForgeServiceLive ⇒
      c.replace[ExecutorsConfig](ExecutorsConfig("executors.scheduler"))
  }.singletonsBy(singletonByConfig)

  val singletonByConfig: PartialFunction[Any, Any] = {
    case c: Executors ⇒ c.config
    case c: ExecutorsConfig ⇒ c
  }
}
