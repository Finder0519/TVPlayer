package cn.finder0519.tvplayer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import cn.finder0519.tvplayer.model.PlayRequest
import cn.finder0519.tvplayer.player.PlayerManager
import cn.finder0519.tvplayer.server.ControlServer
import cn.finder0519.tvplayer.util.NetworkUtil
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * 统筹控制服务与播放器，并向 Compose UI 暴露状态。
 */
class TvPlayerController(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var server: ControlServer? = null

    val playerManager = PlayerManager(context)

    /** 供二维码使用的控制页地址，例如 http://192.168.1.5:30567/ */
    val qrUrl = mutableStateOf<String?>(null)

    /** 是否正在播放 */
    val isPlaying = mutableStateOf(false)

    /** 展示给电视界面的状态文本 */
    val statusText = mutableStateOf("")

    /**
     * 进入前台时调用：启动一个新端口上的控制服务，生成新二维码。\n     * 端口每次变化，故旧的手机控制页随之失效。\n     */
    fun start() {
        stop()
        val ip = NetworkUtil.getLanIpAddress()
        if (ip == null) {
            qrUrl.value = null
            statusText.value = "无法获取局域网 IP，请检查网络连接"
            return
        }
        val port = Random.nextInt(10000, 60000)
        val newServer = ControlServer(
            port = port,
            onPlayRequest = ::onPlayRequest,
            stateTextProvider = ::serverStateText,
        )
        newServer.startServer()
        server = newServer
        qrUrl.value = "http://$ip:$port/"
        statusText.value = ""
    }

    /**
     * 离开前台 / 初始化前调用：停止服务与播放。\n     */
    fun stop() {
        server?.stopServer()
        server = null
        playerManager.stop()
        isPlaying.value = false
    }

    /**
     * 回到二维码主页。播放器停止，但控制服务保留（仍在 app 内）。\n     */
    fun backToHome() {
        playerManager.stop()
        isPlaying.value = false
        statusText.value = "播放已停止"
    }

    private fun onPlayRequest(request: PlayRequest) {
        mainHandler.post {
            Toast.makeText(context.applicationContext, "即将开始播放URL：${request.url}", Toast.LENGTH_LONG).show()
            playerManager.onPlayerError = {
                mainHandler.post {
                    Toast.makeText(context.applicationContext, "播放失败", Toast.LENGTH_LONG).show()
                    playerManager.stop()
                    isPlaying.value = false
                    statusText.value = "播放失败：${request.url}"
                }
            }
            playerManager.play(request)
            isPlaying.value = true
            statusText.value = ""
        }
    }

    private fun serverStateText(): String {
        return if (isPlaying.value) "playing" else "idle"
    }
}
