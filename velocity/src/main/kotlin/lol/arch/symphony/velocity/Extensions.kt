package lol.arch.symphony.velocity

import net.evilblock.cubed.serializers.Serializers
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * @author GrowlyX
 * @since 2/15/2025
 */
inline fun <reified T> String.into() = Serializers.gson.fromJson(this, T::class.java)

private val playerLocks = ConcurrentHashMap<UUID, ReentrantLock>()

fun <T> UUID.acquirePlayerLock(use: () -> T): T
{
    val lock = playerLocks.computeIfAbsent(this) { ReentrantLock() }
    lock.lock()
    try
    {
        return use()
    }
    finally
    {
        lock.unlock()
    }
}
