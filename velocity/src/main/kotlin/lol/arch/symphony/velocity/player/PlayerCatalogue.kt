package lol.arch.symphony.velocity.player

import lol.arch.symphony.api.model.TrackedPlayer
import lol.arch.symphony.velocity.VelocitySymphonyPlugin
import lol.arch.symphony.velocity.acquirePlayerLock
import lol.arch.symphony.velocity.into
import java.time.Duration
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * @author GrowlyX
 * @since 2/15/2025
 */
class PlayerCatalogue : Runnable
{
    private val lock = ReentrantReadWriteLock()
    private var cache = setOf<TrackedPlayer>()

    private lateinit var plugin: VelocitySymphonyPlugin

    fun startTracking(plugin: VelocitySymphonyPlugin)
    {
        this.plugin = plugin

        plugin.server.scheduler
            .buildTask(plugin, this)
            .repeat(Duration.ofMillis(500L))
            .schedule()
    }

    private fun allPlayersRaw() = plugin.channels.redisWithReturn { redis ->
        redis.sync().hgetall("symphony:players").values
    } ?: emptyList()

    fun deadPlayers(): List<TrackedPlayer> = allPlayersRaw()
        .mapNotNull { it.into<TrackedPlayer>() }
        .filter { tracked ->
            System.currentTimeMillis() - tracked.lastHeartbeat >= Duration.ofSeconds(10L).toMillis()
        }

    fun players() = lock.read { cache }
    fun playerCount() = lock.read { cache.size }

    fun onProxyAll(proxyID: String) = allPlayersRaw()
        .mapNotNull { it.into<TrackedPlayer>() }
        .filter { player -> player.instance == proxyID }

    fun onProxy(proxyID: String) = cache.filter { it.instance == proxyID }
    fun onServer(server: String) = cache.filter { it.server == server }

    override fun run()
    {
        lock.write {
            val allPlayers = allPlayersRaw()
                .mapNotNull { it.into<TrackedPlayer>() }
                .filterNot { tracked ->
                    System.currentTimeMillis() - tracked.lastHeartbeat > Duration.ofSeconds(10L).toMillis()
                }

            cache = allPlayers.toSet()
        }

        onProxyAll(plugin.config.id)
            .filter {
                System.currentTimeMillis() - it.lastHeartbeat > Duration
                    .ofSeconds(5L)
                    .toMillis()
            }
            .filter {
                plugin.server.getPlayer(it.uniqueId).isPresent
            }
            .forEach {
                it.uniqueId.acquirePlayerLock {
                    plugin.playerTracker.update(it.uniqueId) {
                        lastHeartbeat = System.currentTimeMillis()
                    }
                }
            }
    }
}
