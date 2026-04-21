package lol.arch.symphony.velocity.command

import com.velocitypowered.api.proxy.Player
import gg.scala.commons.acf.BaseCommand
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import lol.arch.symphony.velocity.VelocitySymphonyPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.UUID

/**
 * @author GrowlyX
 * @since 2/16/2025
 */
class TrackedPlayerCommand(private val plugin: VelocitySymphonyPlugin) : BaseCommand()
{
    private fun formatAbbrevSeconds(seconds: Int): String
    {
        val s = seconds.coerceAtLeast(0)
        val mins = s / 60
        val hrs = mins / 60
        val days = hrs / 24

        return when
        {
            days > 0 -> "${days}d"
            hrs > 0 -> "${hrs}h"
            mins > 0 -> "${mins}m"
            else -> "${s}s"
        }
    }

    @CommandCompletion("@players")
    @CommandAlias("trackedplayer")
    @CommandPermission("symphony.command.trackedplayer")
    fun onTrackedPlayer(player: Player, target: UUID)
    {
        val trackedPlayer = plugin.playerTracker.find(target)
            ?: throw ConditionFailedException("That player is not logged onto the network!")

        player.sendMessage(Component
            .text("Tracked player information:")
            .color(NamedTextColor.AQUA))

        player.sendMessage(Component
            .text("Instance: ")
            .color(NamedTextColor.GRAY)
            .append(
                Component
                    .text(trackedPlayer.instance)
                    .color(NamedTextColor.WHITE)
            ))

        player.sendMessage(Component
            .text("Logged in: ")
            .color(NamedTextColor.GRAY)
            .append(
                    Component
                        .text(formatAbbrevSeconds(((System.currentTimeMillis() - trackedPlayer.loginTime) / 1000L).toInt()))
                        .color(NamedTextColor.GREEN)
                ))

        player.sendMessage(Component
            .text("Server: ")
            .color(NamedTextColor.GRAY)
            .append(
                Component
                    .text(if (trackedPlayer.server == null) "None" else trackedPlayer.server!!)
                    .color(if (trackedPlayer.server == null) NamedTextColor.RED else NamedTextColor.WHITE)
            ))

        player.sendMessage(Component
            .text("Last Switch: ")
            .color(NamedTextColor.GRAY)
            .append(
                    Component
                        .text(
                            if (trackedPlayer.lastServerSwitchTime == null)
                                "None" else formatAbbrevSeconds(((System.currentTimeMillis() - trackedPlayer.lastServerSwitchTime!!) / 1000L).toInt())
                        )
                        .color(if (trackedPlayer.lastServerSwitchTime == null) NamedTextColor.RED else NamedTextColor.WHITE)
                ))

        player.sendMessage(Component
            .text("Previous Server: ")
            .color(NamedTextColor.GRAY)
            .append(
                Component
                    .text(if (trackedPlayer.lastServer == null) "None" else trackedPlayer.lastServer!!)
                    .color(if (trackedPlayer.lastServer == null) NamedTextColor.RED else NamedTextColor.WHITE)
            ))

        player.sendMessage(Component
            .text("Last Reconcile: ")
            .color(NamedTextColor.GRAY)
            .append(
                    Component
                        .text(
                            if (trackedPlayer.lastAttemptedReconcile == null)
                                "None" else formatAbbrevSeconds(((System.currentTimeMillis() - trackedPlayer.lastAttemptedReconcile!!) / 1000L).toInt())
                        )
                        .color(if (trackedPlayer.lastAttemptedReconcile == null) NamedTextColor.RED else NamedTextColor.WHITE)
                ))

        player.sendMessage(Component
            .text("Last Heartbeat: ")
            .color(NamedTextColor.GRAY)
            .append(
                    Component
                        .text(
                            formatAbbrevSeconds(((System.currentTimeMillis() - trackedPlayer.lastHeartbeat) / 1000L).toInt())
                        )
                        .color(NamedTextColor.GREEN)
                ))
    }
}
