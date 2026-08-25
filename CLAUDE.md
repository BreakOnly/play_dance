# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

街舞工作室数字化管理系统，monorepo 含三端：

| 目录 | 内容 | 技术栈 |
|---|---|---|
| `back-end/` | 后端 API | Spring Boot 3.3.5 / Java 21 / **MyBatis 原生 XML（不是 MyBatis-Plus）** / PageHelper / MySQL 8 / jjwt |
| `front-end/` | 管理后台 | Vue 3 + Vite 5 + Element Plus + Pinia + vue-router |
| `mini_program/` | 学员端微信小程序 | uni-app (Vue 3) |
| `preview/`、根目录 `街舞工作室数字化管理系统_产品原型方案.md` | 产品原型 / 设计参考，只读 | — |

⚠️ 实际技术栈与 `.claude/CLAUDE.md` 里提到的不完全一致：**本项目没有 MyBatis-Plus、Redis、RocketMQ**，鉴权也不用 Spring Security（仅用 `spring-security-crypto` 的 BCrypt + 自写 JWT 拦截器）。写代码前先查 `back-end/pom.xml`，不要引用项目不存在的依赖。

## 常用命令

后端（在 `back-end/` 下，Windows 环境用 `mvn`，没有 mvnw）：

```bash
mvn -q -DskipTests compile        # 编译
mvn spring-boot:run               # 启动（默认 dev profile，端口 8080）
mvn clean package -DskipTests     # 打包
```

**没有单元测试**（无 `src/test`），`mvn test` 不会执行任何测试。验证改动靠：编译通过 + 启动后调接口（Swagger: `http://localhost:8080/swagger-ui.html`）。

数据库初始化（MySQL 8）：

```bash
mysql -uroot -p < back-end/sql/schema.sql       # 建库建表（含 dance_studio 库）
mysql -uroot -p dance_studio < back-end/sql/test_data.sql   # 测试数据（可选）
mysql -uroot -p dance_studio < back-end/sql/banner.sql      # 轮播图数据（可选）
```

连接配置在 `application-dev.yml`，可用环境变量覆盖：`DB_HOST` / `DB_PORT` / `DB_USER` / `DB_PASSWORD`。

前端管理后台（`front-end/`）：

```bash
npm install && npm run dev    # http://localhost:5173，/api 和 /files 已代理到 8080
npm run build                 # 产物在 dist/
```

小程序（`mini_program/`）：

```bash
npm install && npm run dev:mp-weixin   # 产物在 dist/dev/mp-weixin，用微信开发者工具打开
```

注意：小程序后端地址硬编码在 `mini_program/src/utils/config.js`（默认 `http://localhost:8080`），真机调试需改成局域网 IP；微信开发者工具需勾选「不校验合法域名」。

## 架构要点（需要跨多个文件才能理解的部分）

### 双端 API 与鉴权

- 接口分两个命名空间：`/api/admin/**`（后台）与 `/api/mp/**`（小程序学员端），controller 相应放在 `controller/admin/` 和 `controller/mp/`。
- `WebMvcConfig` 用**同一个 `AuthInterceptor` 类实例化两个 Bean**，分别要求 JWT 类型为 `admin` / `student`；登录接口（`/api/admin/auth/login`、`/api/mp/auth/login`）放行。
- 解析出的身份写入 `ContextHolder`（ThreadLocal，`afterCompletion` 清理），业务代码通过它取 adminId / studentId / storeId。
- token 存前端：后台在 `localStorage.admin_token`；小程序见 `mini_program/src/utils/auth.js`。

### 统一返回与错误码

- 返回体 `Result<T>{code,message,data}`，`code=0` 成功；分页 `PageResult<T>`。
- 业务异常抛 `BizException(ResultCode)`，由 `GlobalExceptionHandler` 兜底。错误码分段见 `ResultCode`：1000+ 通用、11xx 账户/课时、12xx 约课、13xx 签到、14xx 手办兑换、15xx 视频。**新增错误码沿用分段，不要另起炉灶。**
- 前端 axios 拦截器约定：`code===0` 才透传 data；`1002` 踢回登录页。

### 分层与 MyBatis 约定

- 分层：controller → service（**平铺，无 impl 子包**）→ mapper（接口 + XML）。
- **Mapper XML 放在 `src/main/java/com/dance/studio/mapper/xml/` 下**（不在 resources），靠 pom 的 resources 配置打包；`mapper-locations` 同时扫两个位置。新增 XML 必须放这里，否则找不到。
- 数据库状态字段（booking.status 等）是 **VARCHAR 字符串**（'booked'、'noshow'…），实体用 String，不用 Java 枚举映射。
- `map-underscore-to-camel-case: true`，实体字段驼峰、表列蛇形。
- `back-end/后端开发计划.md` 是早期方案稿（其中 `enums/`、`service/impl/` 等并未落地），**以现有代码为准**。

### 核心领域逻辑（改动风险最高）

- **课时账户**（`AccountService`）：充值建 `course_hour_batch` 批次 + `hour_log` 流水；`student.course_balance` 只是缓存值，**流水才是准**。扣减按批次到期 FIFO，用 `UPDATE ... WHERE remaining >= ?` 防超扣，流水与缓存必须同事务。
- **约课**（`BookingService`）：校验时间窗/余额/容量 → 冻结课时 → 满员进候补；容量用 `UPDATE ... SET booked_count=booked_count+1 WHERE booked_count<capacity` 行锁防超卖；取消有 6 小时规则（退/扣课时）。
- **签到**（`CheckinService`）：扫码/代签，唯一键防重复签到，正式消耗冻结课时并加积分。
- **爽约与课次流转**（`ScheduledTasks`，每 5 分钟）：过期课次 `open→finished`；`booked` 未签到 → `noshow` 并扣冻结课时。
- **兑换**（`RedeemService`）：娃娃机随机必得 + 指定兑换，库存 `stock>0` 条件扣减，驳回退积分回库存。
- **业务规则**（可约课时间窗、取消规则、积分规则等）全部存 `config` 表，经 `ConfigService` 读取，**不要硬编码规则**。
- 所有表带 `store_id`（多门店预留，当前固定 1），查询从登录上下文取。

### 文件存储与微信

- `StorageService` 抽象 + `@ConditionalOnProperty(app.storage.type)`：`local`（默认，存 `back-end/uploads/`，经 `/files/**` 访问）/ `oss`（上线用，视频走签名 URL）。切换只改环境变量，见 `back-end/存储切换说明.md`。
- 微信小程序登录：`app.wechat.mock=true`（默认）时直接用 code 当 openid，无需真实 appid/secret。
- 首次启动若 admin 密码仍是 schema 占位 hash，`AdminPasswordInitializer` 会重置为 **admin / admin123**。

## 编码约定（摘要）

详细风格要求见 `.claude/CLAUDE.md`（阿里巴巴规范、四空格、构造器注入、中文注释、复用现有工具类）。补充本项目特有的：

- 新接口按所属端放进对应 controller 包，路径前缀决定鉴权，无需额外注解。
- 参数校验用 `spring-boot-starter-validation`（`@Valid` + DTO 注解），与现有 DTO 一致。
- 事务加在 service 方法上；涉及余额/库存/流水的方法必须保证同事务 + 条件 UPDATE 防并发超扣。
