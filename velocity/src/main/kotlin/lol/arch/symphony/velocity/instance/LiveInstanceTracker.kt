package lol.arch.symphony.velocity.instance

import lol.arch.symphony.velocity.InstanceConfig
import lol.arch.symphony.velocity.VelocitySymphonyPlugin
import java.time.Duration
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * @author GrowlyX
 * @since 2/15/2025
 */
class LiveInstanceTracker : Runnable
{
    private lateinit var instanceConfig: InstanceConfig
    private lateinit var plugin: VelocitySymphonyPlugin

    private val lock = ReentrantReadWriteLock()
    private var cache = setOf<String>()

    fun startTracking(plugin: VelocitySymphonyPlugin, instanceConfig: InstanceConfig)
    {
        this.plugin = plugin
        this.instanceConfig = instanceConfig

        plugin.server.scheduler
            .buildTask(plugin, this)
            .repeat(Duration.ofMillis(500L))
            .schedule()
    }

    fun liveInstances() = lock.read { cache }
    fun sentinelInstance() = lock.read { cache.minOf { it } }

    fun globalPlayerCount() = cache
        .sumOf { serverID ->
            plugin.channels.redisWithReturn { redis ->
                redis.sync().hget("symphony:instances", serverID)
            }
                ?.toIntOrNull()
                ?: 0
        }

    fun playerCount(instance: String) = plugin.channels.redisWithReturn { redis ->
        redis.sync().hget("symphony:instances", instance)
    }
        ?.toIntOrNull()
        ?: 0

    fun deadInstances(): Set<String>
    {
        val instanceIds: Set<String> = plugin.channels.redisWithReturn { redis ->
            redis.sync().hkeys("symphony:instances").toSet()
        } ?: emptySet()

        return instanceIds.filter { instanceId ->
            val heartbeat = plugin.channels.redisWithReturn { redis ->
                redis.sync().hget("symphony:heartbeats", instanceId)
            }?.toLongOrNull() ?: 0L

            System.currentTimeMillis() - heartbeat >= Duration
                .ofSeconds(5L)
                .toMillis()
        }.toSet()
    }

    override fun run()
    {
        lock.write {
            val heartbeats = plugin.channels.redisWithReturn { redis ->
                val sync = redis.sync()
                sync.hset("symphony:instances", instanceConfig.id, plugin.server.playerCount.toString())
                sync.hset("symphony:heartbeats", instanceConfig.id, System.currentTimeMillis().toString())
                sync.hgetall("symphony:heartbeats")
            } ?: emptyMap()

            cache = heartbeats
                .filterNot { entry ->
                    val heartbeat = entry.value.toLongOrNull() ?: 0L
                    System.currentTimeMillis() - heartbeat > Duration.ofSeconds(5L).toMillis()
                }
                .keys
                .toSet()
        }
    }
}
