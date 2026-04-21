package lol.arch.symphony.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import gg.scala.commons.acf.ConditionFailedException
import lol.arch.symphony.velocity.command.*
import lol.arch.symphony.velocity.instance.InstanceActionExecutor
import lol.arch.symphony.velocity.instance.LiveInstanceTracker
import lol.arch.symphony.velocity.player.PlayerReconciler
import lol.arch.symphony.velocity.player.PlayerTracker
import lol.arch.symphony.velocity.player.PlayerCatalogue
import lol.arch.symphony.velocity.system.SystemSentinel
import java.io.File
import java.nio.file.Path
import java.util.UUID
import java.util.logging.Logger
import kotlin.properties.Delegates

/**
 * @author GrowlyX
 * @since 2/15/2025
 */
@Plugin(
    id = "symphony",
    name = "Symphony",
    version = "1.0.0",
    url = "https://arch.lol/",
    authors = ["GrowlyX"],
    dependencies = [
        Dependency(id = "scala-commons"),
        Dependency(id = "store-velocity"),
        Dependency(id = "combinator-proxy", optional = true),
    ]
)
class VelocitySymphonyPlugin
@Inject
constructor(
    val server: ProxyServer,
    val logger: Logger,
    @DataDirectory
    private val directory: Path
)
{
    companion object
    {
        @JvmStatic
        var instance by Delegates.notNull<VelocitySymphonyPlugin>()
    }

    val playerCatalogue by lazy { PlayerCatalogue() }
    val playerTracker by lazy { PlayerTracker() }
    val playerReconciler by lazy { PlayerReconciler() }

    val instanceTracker by lazy { LiveInstanceTracker() }
    val instanceActionExecutor by lazy { InstanceActionExecutor() }

    lateinit var channels: SymphonyChannels

    lateinit var config: InstanceConfig

    init
    {
        instance = this
    }

    @Subscribe(order = PostOrder.LATE)
    fun ProxyInitializeEvent.on()
    {
        if (!directory.toFile().exists())
        {
            directory.toFile().mkdirs()
        }

        val configFile = File(directory.toFile(), "instance.json")
        config = InstanceConfigIO.load(configFile)

        System.getenv("GAME_SERVER_ID")
            ?.takeIf { it.isNotBlank() }
            ?.let {
                config.id = it
                InstanceConfigIO.save(configFile, config)
                logger.info("Detected environment GAME_SERVER_ID, using ${config.id} as id.")
            }

        channels = SymphonyChannels(logger)
        channels.connect().join()

        playerTracker.plugin = this@VelocitySymphonyPlugin
        server.eventManager.register(this@VelocitySymphonyPlugin, playerTracker)

        instanceActionExecutor.startActionTracking(this@VelocitySymphonyPlugin)
        instanceTracker.startTracking(
            this@VelocitySymphonyPlugin,
            config
        )
        playerCatalogue.startTracking(this@VelocitySymphonyPlugin)
        playerReconciler.startReconciliation(this@VelocitySymphonyPlugin)

        val commandManager = gg.scala.commons.acf.VelocityCommandManager(server, this@VelocitySymphonyPlugin)
        commandManager.commandContexts.registerContext(UUID::class.java) {
            val firstArg = it.popFirstArg()

            return@registerContext try
            {
                UUID.fromString(firstArg)
            } catch (_: IllegalArgumentException)
            {
                throw ConditionFailedException("$firstArg is not a valid uuid.")
            }
        }

        commandManager.commandCompletions.registerCompletion("instances") {
            instanceTracker.liveInstances().toList()
        }
        commandManager.commandCompletions.registerCompletion("players") {
            playerCatalogue.players().map { it.uniqueId.toString() }
        }

        commandManager.enableUnstableAPI("help")

        commandManager.registerCommand(TrackedPlayerCommand(this@VelocitySymphonyPlugin))
        commandManager.registerCommand(GlobalListCommand(this@VelocitySymphonyPlugin))
        commandManager.registerCommand(RunCommandCommand(this@VelocitySymphonyPlugin))
        commandManager.registerCommand(ServerIDCommand(this@VelocitySymphonyPlugin))
        commandManager.registerCommand(ServerIDListCommand(this@VelocitySymphonyPlugin))
        commandManager.registerCommand(PlayerProxyCommand(this@VelocitySymphonyPlugin))
        commandManager.registerCommand(DeadServerIDListCommand(this@VelocitySymphonyPlugin))

        SystemSentinel.startSentinelWatcher(this@VelocitySymphonyPlugin)
    }
}
