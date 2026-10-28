package cn.finder0519.tvplayer.server

import cn.finder0519.tvplayer.model.PlayRequest
import cn.finder0519.tvplayer.model.ProxyConfig
import fi.iki.elonen.NanoHTTPD
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 局域网 HTTP 控制服务。
 *
 * - GET  /      返回控制页面
 * - POST /play  接收播放请求
 * - GET  /state 返回当前播放状态文本
 */
class ControlServer(
    port: Int,
    private val onPlayRequest: ((PlayRequest) -> Unit)? = null,
    private val stateTextProvider: () -> String = { "idle" },
) : NanoHTTPD(port) {

    private val started = AtomicBoolean(false)

    /** 服务端口（构造传入，用于拼装二维码 URL）。 */
    val port: Int = port

    fun startServer() {
        if (started.compareAndSet(false, true)) {
            start(SOCKET_READ_TIMEOUT, false)
        }
    }

    fun stopServer() {
        if (started.compareAndSet(true, false)) {
            stop()
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        return try {
            when (uri.trimEnd('/')) {
                "", "/" -> servePage()
                "/play" -> handlePlay(session)
                "/state" -> newFixedLengthResponse(stateTextProvider())
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain; charset=utf-8", "not found")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain; charset=utf-8", "server error")
        }
    }

    private fun servePage(): Response {
        val resp = newFixedLengthResponse(Response.Status.OK, ControlPage.MIME, ControlPage.html)
        resp.addHeader("Cache-Control", "no-store")
        return resp
    }

    private fun handlePlay(session: IHTTPSession): Response {
        val form = HashMap<String, String>()
        try {
            session.parseBody(form)
        } catch (_: Exception) {
        }
        val url = session.parms["url"]?.trim().orEmpty()
        if (url.isEmpty()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain; charset=utf-8", "缺少 URL")
        }
        val useProxy = session.parms["useProxy"] == "1"
        val proxy = ProxyConfig.from(
            useProxy = useProxy,
            typeStr = session.parms["proxyType"],
            host = session.parms["proxyHost"],
            portStr = session.parms["proxyPort"],
        )
        val request = PlayRequest(url = url, proxy = proxy)
        onPlayRequest?.invoke(request) ?: stateTextProvider()
        return newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", "已提交，开始播放…")
    }

    companion object {
        private const val SOCKET_READ_TIMEOUT = 10000
    }
}
