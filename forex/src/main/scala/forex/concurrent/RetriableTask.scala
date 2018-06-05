package forex.concurrent

import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task

import scala.concurrent.duration._

object RetriableTask extends LazyLogging {
  val fallbackFn: Throwable PartialFunction Boolean = { case _ ⇒ false }

  def ofFuture[X](
      maxRetries: Int,
      timeoutPerRetry: FiniteDuration,
      backoffTime: FiniteDuration,
      totalTimeout: FiniteDuration,
      shouldTriggerRetry: PartialFunction[Throwable, Boolean]
  )(
      computation: Task[X]
  ): Task[X] =
    retriableTaskFromFuture(
      maxRetries,
      maxRetries,
      timeoutPerRetry,
      backoffTime,
      shouldTriggerRetry.orElse(fallbackFn)
    )(computation)
      .timeout(totalTimeout)

  def retriableTaskFromFuture[X](
      maxRetries: Int,
      retriesLeft: Int,
      timeout: FiniteDuration,
      backoffTime: FiniteDuration,
      shouldTriggerRetry: Throwable PartialFunction Boolean
  )(
      computation: Task[X]
  ): Task[X] =
    computation.onErrorRecoverWith {
      case e if shouldTriggerRetry(e) && retriesLeft > 0 ⇒
        logger.info("Retrying triggered by={}, retriesLeft={}/{}", e, retriesLeft, maxRetries)
        retriableTaskFromFuture(maxRetries, retriesLeft - 1, timeout, backoffTime, shouldTriggerRetry)(computation)
          .delayExecution(backoffTime)
    }
}
