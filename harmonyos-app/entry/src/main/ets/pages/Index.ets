import { webview } from '@kit.ArkWeb';
import { common } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';

@Entry
@Component
struct Index {
  controller: webview.WebviewController = new webview.WebviewController();
  private context = getContext(this) as common.UIAbilityContext;

  aboutToAppear(): void {
    // 开启调试模式（可选，便于调试）
    webview.WebviewController.setWebDebuggingAccess(true);
  }

  onBackPress(): boolean {
    // 拦截系统返回键，优先处理网页后退
    if (this.controller.accessBackward()) {
      this.controller.backward();
      return true; // 拦截系统返回
    }
    return false; // 使用系统默认行为（退出应用）
  }

  build() {
    Column() {
      Web({ src: $rawfile('index.html'), controller: this.controller })
        .width('100%')
        .height('100%')
        .domStorageAccess(true)           // 启用 localStorage
        .javaScriptAccess(true)           // 启用 JavaScript
        .fileAccess(true)                 // 启用文件访问
        .imageAccess(true)                // 启用图片加载
        .onlineImageAccess(true)          // 启用网络图片加载
        .mixedMode(MixedMode.All)         // 允许 HTTP 和 HTTPS 混合加载
        .cacheMode(CacheMode.Default)     // 使用默认缓存策略
        .textZoomAtio(100)                // 文字缩放比例
        .zoomAccess(false)                // 禁用双指缩放
        .verticalScrollBarAccess(false)   // 隐藏垂直滚动条
        .horizontalScrollBarAccess(false) // 隐藏水平滚动条
        .onPageBegin((event) => {
          hilog.info(0x0000, 'YinwuChat', 'Page loading: %{public}s', event?.url ?? '');
        })
        .onPageEnd((event) => {
          hilog.info(0x0000, 'YinwuChat', 'Page loaded: %{public}s', event?.url ?? '');
        })
        .onErrorReceive((event) => {
          if (event?.error) {
            hilog.error(0x0000, 'YinwuChat', 'Web error: %{public}s', event.error.getErrorInfo());
          }
        })
        .onHttpErrorReceive((event) => {
          if (event?.response) {
            hilog.error(0x0000, 'YinwuChat', 'HTTP error: %{public}d', event.response.getResponseCode());
          }
        })
        .onConsole((event) => {
          // 将网页的 console.log 输出到鸿蒙日志
          if (event?.message) {
            hilog.info(0x0000, 'YinwuChat-Web', '%{public}s', event.message.getMessage());
          }
          return false;
        })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#1a1a2e')
  }
}
