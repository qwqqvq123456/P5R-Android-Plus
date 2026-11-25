# P5R Android 简体中文版

<p align="center">
  <b>简体中文</b> &nbsp;·&nbsp; <a href="README.en.md">English / Español</a>
</p>

[Persona 5 Royal](https://github.com/synndras/P5R-Android-Plus) 的 Android 原生移植（**不包含任何游戏代码或数据**）的中文分支：
启动器界面全部简体中文，并且自动跟随上游更新、自动构建发布 APK。

- 上游仓库：<https://github.com/synndras/P5R-Android-Plus>
- 本仓库：<https://github.com/qwqqvq123456/P5R-Android-Plus>
- 最新构建下载：<https://github.com/qwqqvq123456/P5R-Android-Plus/releases/latest/download/p5r-cn.apk>

> 你需要**自己**拥有 Persona 5 Royal（Switch 版 1.0.0）的游戏文件 `.nsp` / `.xci` 和 `prod.keys`。
> 这个 APK 里没有任何游戏内容，它只提供运行环境和把游戏数据转换成手机能用的形式。

## 用的是哪套界面

上游把新启动器开发在 `Chakeyy-Launcher-Rework` 分支上，**0.5.31 的 Release 就是用这个分支打的标签**，
而那套界面还没有并回 `main`（`main` 上仍是旧的单页启动器）。所以本仓库也跟着这个分支走，
界面与官方 0.5.31 完全一致：横屏、顶部分页（开始游戏 / 显示 / 性能 / 控制 / 文件 / 鸣谢）、
游戏内暂停菜单、手柄按键重映射、壁纸与动效等。

一旦上游把这套界面并回 `main`，同步工作流会自动改成跟 `main`（判断逻辑见 `sync-upstream.yml`），不需要手动改。

## 这个分支改了什么

| 改动 | 位置 | 说明 |
| --- | --- | --- |
| 界面简体中文 | `games/p5r/java/io/github/p5rrecomp/LangZh.java` | 启动器全部文案的中文翻译（273 条，含上游多语言表里没有的、新启动器新增的） |
| 中文入口 | `Lang.java` | 简体中文下先查 `LangZh`，查不到再退回上游自带的多语言表，最后退回英文 |
| 按键震动 | `runtime/src/platform_android*.cpp`、`Launcher.java`、`PauseActivity.java` | 按下屏幕虚拟按键时轻微震动；与控制页「按键震动」对应 `NX_TOUCH_HAPTICS=0/35/60/100`（默认 60），与游戏内震动互不影响 |
| 震动权限 | `games/p5r/AndroidManifest.xml` | 补回上游 `main` 有的 `VIBRATE` 权限。缺它时所有 `vibrate()` 被系统拒绝，表现就是完全不震（任何 Android 版本） |
| Android 17 兼容 | `runtime/src/platform_android.cpp` | 内存对齐按系统页大小计算；16 KiB 页的设备（Android 15+，Android 17 镜像默认）上 `mprotect` 不再返回 `EINVAL` 导致一启动就闪退 |
| 自定义 GPU 驱动 | `games/p5r/tools/build_android.sh` | 打包 `libadrenotools` 的 4 个 hook 库。上游的 bash 构建脚本漏了它们（官方 APK 是用 `build_apk.sh` 打的，里面有），结果导入的 Turnip 驱动加载失败、被静默丢弃，只能用系统 Adreno 驱动，大场景就会 GPU 挂死闪退 |
| 自动同步上游 | `.github/workflows/sync-upstream.yml` | 每天 05:00（北京时间）合并上游发布分支，有更新就提交并触发构建；已并回 `main` 时自动改跟 `main` |
| 自动构建发布 | `.github/workflows/build-release.yml` | 构建 APK，发布成滚动 Release（标签 `cn-latest`），并写入版本号 |
| 中文覆盖率检查 | `tools/check_zh.py` | 列出还没翻译的界面文案；同步上游后会跑一次并写进 Actions 摘要 |

界面语言跟随系统：中文系统直接显示简体中文，不需要任何设置。

> 关于震动：`Chakeyy-Launcher-Rework` 分支的 `AndroidManifest.xml` 少了上游 `main` 里的 `VIBRATE` 权限，
> 而 `Vibrator.vibrate()` 没有这条权限会被系统直接拒绝（表现就是完全不震，任何 Android 版本都一样，
> 不只是 Android 17）。本仓库已补上，并由 `tools/ensure_manifest.py` 在同步和构建前检查，构建出的 APK
> 还会校验这条权限，避免再被上游覆盖掉。

## 下载与安装

1. 打开 [Releases](https://github.com/qwqqvq123456/P5R-Android-Plus/releases/latest)，下载 `p5r-cn.apk`（或直接用上面的固定链接）。
2. 允许“安装未知来源应用”，安装。
3. 首次启动会请求**所有文件访问权限**（需要它才能读你放在内部存储里的游戏数据）。
4. 在「文件 → 数据」里选择你自备的 `Persona 5 Royal 1.0.0` 的 `.nsp` / `.xci`，再选择 `prod.keys`。
5. 点「准备游戏」，保持应用在前台几分钟（只需一次，会提取并转换着色器）。
6. 回到「开始游戏」页，点 **开始游戏**。

装过上游原版的用户注意：签名不同，需要先卸载原版再装这个版本；之后本仓库的每次构建签名一致，可以直接覆盖升级。

### 进游戏闪退（GPU 挂死）怎么办

如果日志 `nx.log` 末尾是 `nvn/vk: GPU hang (vkQueueSubmit): stopping`，说明是 **Vulkan 驱动**报告了 device lost（GPU 挂死），
不是游戏逻辑崩了。系统自带的 Adreno 驱动（尤其 Adreno 8xx + Android 17）跑这个重编译器的大场景容易挂，换 Turnip 驱动基本能解决。

1. 启动器里选「GPU 驱动」，导入 Turnip 的 `libvulkan_freedreno.so`（`.adpkg` 也行）。
2. 装好后再看日志：正常会出现 `gpu driver: adrenotools custom driver opened successfully`；
   如果还是 `adrenotools returned null`，看它后面那句提示——**缺 hook 库**就是 APK 打包不全（本仓库已修，用最新版），
   **驱动文件被拒**就是换一个 Turnip 版本（要 aarch64 + Vulkan 1.3 的包）。
3. 仍然偶尔挂死，再按顺序降负载：分辨率从 `native` 改成 1080p → 关「超宽屏 HOR+」→「异步管线」设 OFF → 纹理质量降一档。

## 自动更新是怎么运作的

```
每天 05:00 ──► sync-upstream.yml ──► fetch 上游 main + Chakeyy-Launcher-Rework
                                      │   （发布分支已并回 main 就跟 main，否则跟发布分支）
                        有更新 ──► 合并 ──► 推送 main ──► 触发 build-release.yml ──► 构建 APK ──► 发布 cn-latest
                                      │
                        有冲突 ──► 放弃合并 + 开 issue（不会强推、不会破坏本仓库）
```

也可以手动跑：**Actions → 同步上游更新 → Run workflow**（勾选 `force_release` 可以不管有没有更新都重新构建一次）。

构建发布的工作流同样支持手动触发：**Actions → 构建 APK 并发布 Release**。

### 上游改了文案怎么办

中文文案以英文原文为键，放在 `LangZh.java`。上游加新文案后：

1. 同步工作流的摘要里会列出“待翻译”的条目（也可以本地跑 `python3 tools/check_zh.py`）；
2. 在 `LangZh.java` 的 `static { ... }` 里加一行 `put("English text", "中文");`
3. 提交后自动触发新的构建。

带 `%s` / `%d` 的文案要保持占位符数量不变，否则填充时会出错。

## 上游更新日志

上游每个版本的发布说明都会自动抓一份到 [`docs/upstream-release-notes.md`](docs/upstream-release-notes.md)，
每天随同步一起更新；每个 Release 的说明里也会直接贴出当版上游写了什么。

- 原文是作者写的西班牙语，**不做翻译**——翻译会失真，而且上游每次改文案都要重新对一遍。
- 版本号对应关系：本仓库 `0.5.31-cn.19` = 上游 `v0.5.31` + 第 19 次中文构建。
- 上游历史版本：<https://github.com/synndras/P5R-Android-Plus/releases>

只想看中文的，看每次 Release 说明里的「最近提交」，那里是本仓库自己的改动。

## 自己构建

和上游完全一样（需要 Docker，用于编译着色器转换器）：

```bash
# 依赖：Android SDK 35 + NDK 28.2.13676358 + build-tools 35.0.0、JDK 17、cmake、ninja
docker run --rm -v "$PWD:/src" -w /src mcr.microsoft.com/dotnet/sdk:10.0 sh games/p5r/tools/build_harness_android.sh
bash games/p5r/tools/build_android.sh
# 产物：private/p5r/apk_nce/p5r.apk
```

Windows 下用 `android/build_apk.ps1` 或 `games/p5r/tools/build_android.cmd`，细节见上游 `docs/BUILD.md`。

### 签名钥匙（可选）

仓库 Secrets 里如果配置了下面四项，构建会一直用同一把钥匙签名，装过的旧版本能直接覆盖升级：

| Secret | 说明 |
| --- | --- |
| `P5R_KEYSTORE_BASE64` | keystore（JKS）文件的 base64 |
| `P5R_KEYSTORE_PASSWORD` | keystore 密码 |
| `P5R_KEY_ALIAS` | 密钥别名 |
| `P5R_KEY_PASSWORD` | 密钥密码 |

没有配置时每次构建都会生成新的调试签名，升级前要先卸载。

## 说明文档的语言

- **简体中文**：本文件（`README.md`，仓库主页默认显示）
- **English / Español**：[`README.en.md`](README.en.md)，内容与上游 `README.md` 保持一致

两份文档互相在顶部有切换链接。英文版由同步工作流从上游自动刷新，中文版由本仓库单独维护。

## 致谢与许可

- **原始 Android 移植**：[Brandon04Gonzalez / p5r-android](https://github.com/Brandon04Gonzalez/p5r-android)
- **启动器重做**：[Chakeyy / p5r-android](https://github.com/Chakeyy/p5r-android)，即 `Chakeyy-Launcher-Rework` 分支中的新界面
- **当前 fork 与整合**：[synndras / P5R-Android-Plus](https://github.com/synndras/P5R-Android-Plus)
- **运行时**：[SwitchGamesRecomp / Super Mario Bros. Wonder Recomp](https://github.com/BelmanteGu/SuperMarioWonderRecomp)（BelmanteGu / Projeto Belmante），MIT
- **着色器翻译**：Ryujinx（Ryubing）的着色器翻译器，MIT，以原生库形式打进 APK

代码遵循上游的 MIT 许可，见 [`LICENSE`](LICENSE)；第三方组件请同时参考各自上游仓库的许可。

## 免责声明

- 本项目不提供游戏本体、密钥或任何游戏数据；请使用你自己合法拥有的游戏副本。
- 与 Atlus / SEGA / Nintendo 无关。仅供个人在自有设备上使用。
