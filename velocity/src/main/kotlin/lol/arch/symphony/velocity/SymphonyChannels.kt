package lol.arch.symphony.velocity

import gg.scala.aware.Aware
import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.store.ScalaDataStoreShared
import io.lettuce.core.api.StatefulRedisConnection
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 2/15/2025
 */
class SymphonyChannels(
    private val logger: Logger
)
{
    val aware: Aware<AwareMessage> = AwareBuilder
        .of<AwareMessage>("symphony")
        .logger(logger)
        .codec(AwareMessageCodec)
        .build()

    fun connect(): CompletableFuture<Void> = aware
        .connect()
        .toCompletableFuture()

    private val redis = ScalaDataStoreShared.INSTANCE
        .getNewRedisConnection()

    fun <T> redisWithReturn(use: (StatefulRedisConnection<String, String>) -> T): T? =
        redis.useResourceWithReturn(use)

    fun redis(use: (StatefulRedisConnection<String, String>) -> Unit) =
        redis.useResource(use)

    fun publishNetworkEvent(
        packet: String,
        uniqueId: UUID,
        vararg pairs: Pair<String, Any>
    )
    {
        AwareMessage
            .of(packet, aware, "player" to uniqueId, *pairs)
            .publish(AwareThreadContext.ASYNC, "symphony:networkEvents")
    }

    fun publishRPC(
        packet: String,
        vararg pairs: Pair<String, Any>
    )
    {
        AwareMessage
            .of(packet, aware, *pairs)
            .publish(AwareThreadContext.ASYNC, "symphony:rpc")
    }
}
