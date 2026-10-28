package cn.finder0519.tvplayer.server

/**
 * 手机端控制页面的 HTML。\n * 包含 URL 输入、代理设置（http / socks5）和播放按钮，并轮询播放状态。\n */
object ControlPage {

    const val MIME = "text/html; charset=utf-8"

    val html: String = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<title>TV 播放控制</title>
<style>
  :root { color-scheme: dark; }
  * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
  body {
    margin: 0; min-height: 100vh; padding: 24px 20px 48px;
    font-family: -apple-system, "PingFang SC", "Segoe UI", Roboto, sans-serif;
    background: #121212; color: #eee;
    display: flex; flex-direction: column; align-items: center;
  }
  h1 { font-size: 22px; font-weight: 600; margin: 8px 0 20px; letter-spacing: 1px; }
  .card {
    width: 100%; max-width: 480px; background: #1e1e1e;
    border: 1px solid #2c2c2c; border-radius: 16px; padding: 20px;
  }
  label { display: block; font-size: 13px; color: #aaa; margin: 14px 0 6px; }
  input[type="text"], input[type="number"] {
    width: 100%; padding: 12px 14px; font-size: 16px;
    background: #0f0f0f; color: #fff; border: 1px solid #333; border-radius: 10px; outline: none;
  }
  input:focus { border-color: #4a90e2; }
  .row { display: flex; align-items: center; gap: 10px; margin-top: 14px; cursor: pointer; }
  .row input { width: 18px; height: 18px; accent-color: #4a90e2; }
  .row span { font-size: 15px; color: #ddd; }
  .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
  select {
    width: 100%; padding: 12px 14px; font-size: 15px;
    background: #0f0f0f; color: #fff; border: 1px solid #333; border-radius: 10px; outline: none;
    -webkit-appearance: none; appearance: none;
  }
  button {
    width: 100%; margin-top: 22px; padding: 15px; font-size: 18px; font-weight: 600;
    border: none; border-radius: 12px; color: #fff; background: #4a90e2;
    transition: opacity .15s; letter-spacing: 2px;
  }
  button:active { opacity: .8; }
  button:disabled { background: #555; }
  #status {
    margin-top: 16px; text-align: center; font-size: 14px; min-height: 20px;
    color: #8bc34a; word-break: break-all;
  }
  #status.err { color: #ef5350; }
  .hint { font-size: 12px; color: #666; margin-top: 8px; }
  .proxyHidden { display: none; }
</style>
</head>
<body>
  <h1>📺 TV 播放控制</h1>
  <div class="card">
    <label for="url">播放 URL</label>
    <input type="text" id="url" placeholder="https://... 或 http://... 或 m3u8/mpd 地址" autocomplete="off">

    <div class="row">
      <input type="checkbox" id="useProxy">
      <span>通过代理播放</span>
    </div>

    <div id="proxySection" class="proxyHidden">
      <div class="grid" style="margin-top:14px">
        <div>
          <label for="proxyType">代理类型</label>
          <select id="proxyType">
            <option value="http">HTTP</option>
            <option value="socks5">SOCKS5</option>
          </select>
        </div>
        <div>
          <label for="proxyPort">端口</label>
          <input type="number" id="proxyPort" placeholder="18000" value="18000">
        </div>
      </div>
      <label for="proxyHost">代理地址</label>
      <input type="text" id="proxyHost" placeholder="192.168.31.2" value="192.168.31.2" autocomplete="off">
    </div>

    <button id="playBtn">▶ 播放</button>
    <div id="status"></div>
    <div class="hint">手机需与电视处于同一局域网</div>
  </div>

<script>
(function () {
  var useProxy = document.getElementById("useProxy");
  var proxySection = document.getElementById("proxySection");
  var inputUrl = document.getElementById("url");
  var btn = document.getElementById("playBtn");
  var status = document.getElementById("status");

  useProxy.addEventListener("change", function () {
    proxySection.classList.toggle("proxyHidden", !useProxy.checked);
  });

  function setStatus(text, isErr) {
    status.textContent = text;
    status.classList.toggle("err", !!isErr);
  }

  function postPlay() {
    var url = inputUrl.value.trim();
    if (!url) { setStatus("请输入播放 URL", true); return; }
    var params = new URLSearchParams();
    params.set("url", url);
    params.set("useProxy", useProxy.checked ? "1" : "0");
    if (useProxy.checked) {
      params.set("proxyType", document.getElementById("proxyType").value);
      params.set("proxyHost", document.getElementById("proxyHost").value.trim());
      params.set("proxyPort", document.getElementById("proxyPort").value.trim());
    }
    btn.disabled = true;
    setStatus("正在请求电视播放…", false);
    fetch("/play", { method: "POST", body: params })
      .then(function (r) { return r.text(); })
      .then(function (t) {
        setStatus(t);
        btn.disabled = false;
      })
      .catch(function () {
        setStatus("请求失败，请重试", true);
        btn.disabled = false;
      });
  }

  btn.addEventListener("click", postPlay);
  inputUrl.addEventListener("keydown", function (e) { if (e.key === "Enter") postPlay(); });

  // 轮询播放状态
  setInterval(function () {
    fetch("/state", { cache: "no-store" })
      .then(function (r) { return r.text(); })
      .then(function (t) {
        if (t.indexOf("playing") === 0 || t.indexOf("已") === 0) {
          setStatus(t);
        }
      })
      .catch(function () {});
  }, 2000);
})();
</script>
</body>
</html>
"""
}
