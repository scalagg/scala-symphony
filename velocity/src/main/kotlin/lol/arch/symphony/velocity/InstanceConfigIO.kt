package lol.arch.symphony.velocity

import net.evilblock.cubed.serializers.Serializers
import java.io.File

/**
 * @author GrowlyX
 * @since 2/15/2025
 */
object InstanceConfigIO
{
    fun load(file: File): InstanceConfig
    {
        if (!file.exists())
        {
            file.parentFile?.mkdirs()
            val default = InstanceConfig()
            file.writeText(Serializers.gson.toJson(default))
            return default
        }

        val loaded = runCatching {
            Serializers.gson.fromJson(file.readText(), InstanceConfig::class.java)
        }.getOrNull()

        if (loaded == null)
        {
            val default = InstanceConfig()
            file.writeText(Serializers.gson.toJson(default))
            return default
        }

        return loaded
    }

    fun save(file: File, config: InstanceConfig)
    {
        file.parentFile?.mkdirs()
        file.writeText(Serializers.gson.toJson(config))
    }
}
