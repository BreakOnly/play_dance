# 微信扫一扫签到

用户用微信自己的“扫一扫”，不是在小程序里调用 scanCode。

1. 管理员打开“签到管理 → 展示签到码”，选择课次，生成短时有效的小程序码。
2. 微信识别小程序码，打开 `pages/checkin/result`，并通过 `scene` 参数传入 32 位随机串。
3. 承接页取得当前用户微信登录会话，携带 Bearer token 调用签到接口。
4. 后端验证随机串的哈希、有效期、课次、当前登录用户的预约和签到时段，在同一事务里核销课时并记积分。
5. 显示结果；重复扫码返回同一次签到，不重复扣减。

## 接口

`POST /api/admin/checkin-codes`，管理员鉴权，body 为 `{"scheduleId":123}`。

返回小程序码图片 data URL、过期时间和有效秒数。二维码最多有效 5 分钟，且不得晚于课程结束。数据库只保存随机串 SHA-256，不保存可直接使用的原串。

`POST /api/mp/checkins/scan`，用户鉴权，body 为 `{"scene":"32位随机串"}`。

此接口处理二维码承接参数，不接收二维码图片，也不允许由请求体指定另一个 userId。默认签到窗口是开课前 30 分钟到课程结束；提前分钟数可在系统设置调整。没有预约、仅候补、已取消、错误码或过期码都不能签到。

`POST /api/admin/checkins` 保留管理员代签；用户新增/停用/删除接口不开放。

## 微信平台配置

- WX_APPID、WX_SECRET 必须对应实际微信小程序，WX_MOCK=false。
- 正式发布使用 WX_CODE_ENV=release，体验版可用 trial；生产服务器不能沿用 H5 模拟登录。
- 正式版需要先发布包含 `pages/checkin/result` 的小程序，再生成正式签到码。
- 调用微信的 `getwxacodeunlimit` 生成原生小程序码，无需额外配置“普通链接二维码跳转规则”。
- 真机最终验收包括：新用户扫码自动注册、老用户扫码、登录过期后扫码、重复扫码、未预约扫码、码过期和课次取消后扫码。

本次本地测试验证后端码校验和记账，不使用假 AppID/Secret 伪造微信平台通过结果。实际出码和微信扫一扫打开小程序仍须在已配置的微信环境验收。

官方接口参考：[获取不限制的小程序码](https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/qrcode-link/qr-code/getUnlimitedQRCode.html)。
