package forex.main

import forex.config._
import forex.services.oneforge.Interpreter
import forex.services.OneForge
import forex.{processes => p}
import org.zalando.grafter.macros._

@readerOf[ApplicationConfig]
case class Processes(
  interpreter: Interpreter
) {
  implicit final lazy val _oneForge: OneForge[AppEffect] =
  interpreter.implementation[AppStack]

  final val Rates = p.Rates[AppEffect]
}
