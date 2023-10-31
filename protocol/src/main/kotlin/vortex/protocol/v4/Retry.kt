package vortex.protocol.v4

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class RetryOptions(
    val times: Int = Int.MAX_VALUE,
    val initialDelay: Duration = 100.milliseconds,
    val maxDelay: Duration = 1.seconds,
    val factor: Double = 2.0
) {
    companion object {
        val Default = RetryOptions(
            times = 3,
            initialDelay = 100.milliseconds,
            maxDelay = 1.seconds,
            factor = 2.0
        )
    }
}

suspend fun <T> retry(
    options: RetryOptions = RetryOptions.Default,
    block: suspend () -> T): T
{
    var currentDelay = options.initialDelay.inWholeMilliseconds
    repeat(options.times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            LOG.println("Retrying operation after ${it + 1} attempts. Cause: ${e.message}")
        }
        delay(currentDelay)
        currentDelay = (currentDelay * options.factor).toLong()
            .coerceAtMost(options.maxDelay.inWholeMilliseconds)
    }
    return block() // last attempt
}
