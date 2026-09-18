# 街舞工作室小程序与管理后台

Java 21 / Spring Boot / MySQL 8；Vue 管理后台；uni-app 微信小程序。产品只有一家门店。

## 用户与课时规则

- 用户通过微信登录注册；后台只能查看和更正资料、充值及带原因调整积分，不提供新增、删除或停用用户。
- 预约成功直接扣除课程所需课时；候补不扣课时，转正时扣除。
- 用户在免费取消时间窗外不能自行取消，由管理员处理；管理员代取消退回仍在有效期内的原课时。
- 扫微信扫一扫打开门店的课次签到码，小程序自动登录并提交签到。没有小程序内部扫码按钮。
- 重复预约、重复签到、重复退款和重复领取受数据库事务与条件更新保护。
- 积分商品只支持到店领取。用户出示兑换记录中的领取码，管理员核对后核销。
- 点赞只用于榜单，不再产生或扣减可消费积分。

课时举例：10 课时 → 预约成功后剩余 9 → 签到后仍为 9。正常取消则回到 10。过期只扣除批次剩余课时；已扣课时对应的预约仍可完成。如果取消时原课时已到期，不会把过期课时恢复为新课时。

## 本地开发

安装 Java 21、Maven 3.9、Node.js 22、MySQL 8。先创建一个 UTF-8 数据库，不要运行旧 `sql/schema.sql` 覆盖已有库。

后端默认生产模式。开发需显式指定 local：

```powershell
$env:SPRING_PROFILES_ACTIVE='local'
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/dance_studio?serverTimezone=Asia/Shanghai&characterEncoding=utf8'
$env:DB_USER='your_local_user'
$env:DB_PASSWORD='your_local_password'
$env:ADMIN_INITIAL_PASSWORD='choose-a-local-password-at-least-12-characters'
mvn -f back-end/pom.xml spring-boot:run
```

在 `back-end` 目录运行时，local 配置也可读取未入库的 `config/application-local.yml` 或现有 `src/main/resources/application-dev.yml`。本地配置不打包。local 的 JWT 密钥为空时每次启动随机生成，重启需要重新登录；生产必须提供独立随机密钥。

```powershell
cd front-end
npm ci
npm run dev
```

```powershell
cd mini_program
npm ci
npm run dev:mp-weixin
# H5 仅供本地预览：npm run dev:h5
```

H5 开发模拟身份按浏览器区分；正式微信登录必须关闭 WX_MOCK 并填写 WX_APPID、WX_SECRET。普通用户没有项目内密码。管理员初始密码从环境传入，不输出日志，首次使用后可在右上角修改密码。

## 数据库升级

Flyway 的 `V1` 只初始化空库；`V2` 添加课时批次分配、操作幂等、签到码、领取码等结构；`V5` 将旧预约转换为已扣课记录并删除冻结字段；后续迁移继续递增版本。

旧库升级步骤见 [部署与升级说明](docs/deployment.md)。不要运行包含 DROP/DELETE/造数的旧 SQL 作为生产升级。迁移先核对历史账户，发现不一致会提示用户 ID 并停止，不会静默覆盖余额。

## 自动验证

```powershell
node scripts/check_sources.mjs
mvn -f back-end/pom.xml clean verify
cd front-end
npm ci
npm run build
cd ../mini_program
npm ci
npm run build:mp-weixin
npm run build:h5
```

集成测试只接受名为 `dance_repair_test` 的独立数据库，默认连接本机 13316。可通过 `TEST_DB_URL`、`TEST_DB_USER`、`TEST_DB_PASSWORD` 指定 CI 测试实例。测试会清理这个专用测试库里的业务数据，不能指向日常开发数据库。Windows 的隔离测试实例可通过 `wsl -d Ubuntu -- python3 /mnt/d/wsheng/play_dance/scripts/start_test_mysql.py` 启动。

GitHub CI 校验页面源码、后台与小程序构建、真实 MySQL 回归以及 JAR 中是否夹带本地配置；Dependabot 每周检查依赖。小程序依赖告警作为 CI 附件保留，已知上游限制见 [修复状态](docs/implementation-status.md)。

## 发布微信小程序

`manifest.json` 的微信 AppID 与 `project.config.json` 应对应正式小程序。

```powershell
$env:VITE_API_BASE_URL='https://your-api-domain'
npm run build:release
```

上传 `mini_program/dist/build/mp-weixin`。正式包必须使用 HTTPS API 地址，并在微信公众平台配置请求、上传、下载和 web-view 业务域名。普通 `build:mp-weixin` 允许本地联调；正式发布使用带配置校验的 `build:release`。

签二维码流程、接口与需要的微信配置见 [微信扫一扫签到](docs/wechat-checkin.md)。生产代码默认不开 Swagger，管理后台只部署构建产物。
