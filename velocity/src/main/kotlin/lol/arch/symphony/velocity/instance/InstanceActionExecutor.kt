package lol.arch.symphony.velocity.instance

import lol.arch.symphony.velocity.VelocitySymphonyPlugin
import lol.arch.symphony.velocity.instance.requests.RunCommandRequest
import lol.arch.symphony.velocity.rpc.AwareRpcHandlers
import net.evilblock.cubed.serializers.Serializers

/**
 * @author GrowlyX
 * @since 2/15/2025
 */
class InstanceActionExecutor
{
    private lateinit var plugin: VelocitySymphonyPlugin

    fun runCommand(request: RunCommandRequest)
    {
        plugin.channels.publishRPC(
            "runCommand",
            "request" to Serializers.gson.toJson(request)
        )
    }

    fun startActionTracking(plugin: VelocitySymphonyPlugin)
    {
        this.plugin = plugin
        AwareRpcHandlers.register(plugin)
    }
}
