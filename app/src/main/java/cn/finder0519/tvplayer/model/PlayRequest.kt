package cn.finder0519.tvplayer.model

/**
 * 代理类型。
 */
enum class ProxyType {
    NONE,
    HTTP,
    SOCKS5;

    companion object {
        fun from(value: String?): ProxyType {
            return when (value?.lowercase()) {
                "http", "http_proxy" -> HTTP
                "socks5", "socks" -> SOCKS5
                else -> NONE
            }
        }
    }
}

/**
 * 代理配置。
 * [host]/[port] 仅在 [type] 不为 NONE 时有效。
 */
data class ProxyConfig(
    val type: ProxyType,
    val host: String,
    val port: Int,
) {
    companion object {
        val NONE = ProxyConfig(ProxyType.NONE, "", 0)

        fun from(
            useProxy: Boolean,
            typeStr: String?,
            host: String?,
            portStr: String?,
        ): ProxyConfig {
            if (!useProxy) return NONE
            val type = ProxyType.from(typeStr)
            if (type == ProxyType.NONE) return NONE
            val port = portStr?.trim()?.toIntOrNull() ?: return NONE
            val h = host?.trim().orEmpty()
            if (h.isEmpty()) return NONE
            return ProxyConfig(type, h, port)
        }
    }
}

/**
 * 手机端提交的播放请求。
 */
data class PlayRequest(
    val url: String,
    val proxy: ProxyConfig,
)
