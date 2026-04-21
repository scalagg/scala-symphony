package lol.arch.symphony.velocity

import java.util.UUID

/**
 * @author GrowlyX
 * @since 2/15/2025
 */
data class InstanceConfig(
    var id: String = "proxy-1",
    var secret: String = UUID.randomUUID().toString(),
)
