package lol.arch.symphony.velocity.rpc

import lol.arch.symphony.velocity.VelocitySymphonyPlugin
import lol.arch.symphony.velocity.instance.actor.SymphonyCommandSource
import lol.arch.symphony.velocity.instance.requests.RunCommandRequest
import lol.arch.symphony.velocity.into
import lol.arch.symphony.velocity.player.requests.PlayerReconcileRequest

/**
 * @author GrowlyX
 * @since 2/15/2025
 */
object AwareRpcHandlers
{
    fun register(plugin: VelocitySymphonyPlugin)
    {
        val channels = plugin.channels

        channels.aware.listen("runCommand") {
            val request = retrieve<String>("request")
                .into<RunCommandRequest>()
            if (!(request.instance == "all" || request.instance == plugin.config.id))
            {
                return@listen
            }

            plugin.server.commandManager
                .executeImmediatelyAsync(SymphonyCommandSource, request.command)
        }

        channels.aware.listen("playerReconcile") {
            val request = retrieve<String>("request")
                .into<PlayerReconcileRequest>()
            if (plugin.config.id != request.instance)
            {
                return@listen
            }

            if (plugin.server.getPlayer(request.player).isPresent)
            {
                return@listen
            }

            plugin.playerTracker.delete(request.player)
        }
    }
}
