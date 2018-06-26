package forex.task

import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task

import scala.concurrent.duration._

object RetriableTask extends LazyLogging {
  val fallbackFn: Throwable PartialFunction Boolean = { case _ ⇒ false }

  def retriableOf[X](
      maxRetries: Int,
      timeoutPerRetry: FiniteDuration,
      backoffTime: FiniteDuration,
      totalTimeout: FiniteDuration,
      shouldTriggerRetry: PartialFunction[Throwable, Boolean]
  )(
      computation: Task[X]
  ): Task[X] =
    retry(
      maxRetries,
      maxRetries,
      timeoutPerRetry,
      backoffTime,
      shouldTriggerRetry.orElse(fallbackFn)
    )(computation)
      .timeout(totalTimeout)

  def retry[X](
      maxRetries: Int,
      retriesLeft: Int,
      timeout: FiniteDuration,
      backoffTime: FiniteDuration,
      shouldTriggerRetry: Throwable PartialFunction Boolean
  )(
      computation: Task[X]
  ): Task[X] =
    computation.onErrorRecoverWith {
      case error if shouldTriggerRetry(error) && retriesLeft > 0 ⇒
        logger.info("Retrying triggered by={}, retriesLeft={}/{}", error, retriesLeft, maxRetries)
        retry(maxRetries, retriesLeft - 1, timeout, backoffTime, shouldTriggerRetry)(computation)
          .delayExecution(backoffTime)
      case error if !shouldTriggerRetry(error) ⇒
        logger.warn("Skipping retries. Error={}", error)
        throw error
      case error if retriesLeft <= 0 ⇒
        logger.warn("Retries exhausted. Error={}", error)
        throw error
    }
}
