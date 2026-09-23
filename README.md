# MomentKeep 朝暮记

> 记录打卡、待办、专注与倒计时 —— 朝有目标，暮有记录。
> uni-app 三端（H5 / 微信小程序 / App）+ Spring Boot 3 后端。
>
> 本文档已按代码现状重写，并合并了原《前端开发文档》《后端开发文档》《数据库设计文档》《前端开发文档（设计要求优化版）》四份文档。
> 部署步骤仍以 **[阿里云ECS部署说明.md](./阿里云ECS部署说明.md)** 为准（该文档与代码一致，未改动）。

---

## 一、项目简介

MomentKeep（朝暮记）是一个个人时间管理应用，围绕「打卡 → 待办 → 专注 → 倒计时」四条主线，帮助用户记录日常并回看节奏。

- **打卡**：早起 / 睡眠 / 用餐 / 运动四类，带时间分布图与历史分组
- **待办**：今日待办增删改、标记完成、填写完成说明、关联专注
- **专注**：正计时 / 倒计时 / 番茄钟，须关联一件待办
- **倒计时**：目标时间 + 颜色标记，按剩余天数分层呈现
- **AI 助手**：内置于侧栏，可问打卡 / 待办 / 专注相关问题，带每日配额

---

## 二、技术栈

| 层 | 技术 |
|---|---|
| 前端框架 | uni-app 3（`@dcloudio/* 3.0.0-5020620260917001`）+ Vue 3.4.21 + Pinia 2.1 |
| 构建 | Vite 5.2.8 + `@vitejs/plugin-vue` 5 |
| 图表 | **`@qiun/ucharts` 2.5.0**（柱 + 折线双 Y 轴组合图） |
| 富文本 | `marked` 4.3 + `dompurify` 3.4（AI 回复渲染） |
| 后端 | Spring Boot **3.5.13** / **JDK 21** / Maven |
| ORM | MyBatis-Plus 3.5.15（`spring-boot3-starter` + `jsqlparser`，**无 XML 映射**） |
| 数据库 | TiDB Cloud（MySQL 兼容），库名 `momentkeep` |
| 鉴权 | Spring Security + JWT（jjwt 0.12.5，HMAC），密码 BCrypt |
| 存储 | 缤纷云 S3 兼容（AWS SDK S3 2.20.29） |
| AI | DeepSeek（OkHttp 4.12） |
| 接口文档 | springdoc 2.8.17（生产关闭）、Actuator（health/info） |

> **Redis 依赖已在 pom 中注释、未启用**，代码中亦无 `RedisConfig`。

---

## 三、目录结构

```
MomentKeep/
├─ frontend/MomentKeep/            # uni-app 前端
│  ├─ src/
│  │  ├─ pages/                    # 13 个页面（见 §4.1）
│  │  ├─ components/
│  │  │  ├─ Layout.vue             # 侧栏 + 主内容 + AI 聊天面板（响应式）
│  │  │  └─ CheckinTimeChart.vue   # 打卡时间分布图（uCharts，跨端指针事件归一）
│  │  ├─ store/                    # index.js(Pinia) / user.js(登录态) / app.js(布局状态)
│  │  ├─ utils/                    # request / cache / datetime / fontScale / theme
│  │  ├─ styles/                   # tokens.css / motion.css / icons.css
│  │  ├─ App.vue                   # 全局初始化 + 全局样式引入
│  │  ├─ pages.json                # 13 页注册（无 tabBar / 无分包）
│  │  └─ manifest.json             # 应用配置（mp-weixin.appid 目前为空）
│  └─ vite.config.js               # /api → http://localhost:8080 代理
├─ backend/MomentKeep/             # Spring Boot 后端
│  ├─ src/main/java/cn/edu/scnu/momentkeep/
│  │  ├─ controller/ service(+impl)/ mapper/ entity/ dto/request/ vo/
│  │  ├─ config/                   # Security / MybatisPlus / S3 / OkHttp
│  │  ├─ security/                 # JwtTokenProvider / JwtAuthenticationFilter / LoginUser
│  │  └─ common/                   # Result / ResultCode / PageResult / GlobalExceptionHandler
│  ├─ sql/schema.sql               # 全量 DDL
│  ├─ sql/upgrade_2026-09-18.sql   # 增量（token_version / ai_chat_quota / 索引）
│  ├─ config/application-local.yml # 本地密钥（gitignore，不进 jar）
│  └─ Dockerfile
├─ 阿里云ECS部署说明.md            # 权威部署文档
└─ README.md                       # 本文档
```

---

## 四、前端开发

### 4.1 页面（13 个，全部在 `pages.json` 注册）

| 目录 | 功能 |
|---|---|
| `login/` | 登录（含「记住我」） |
| `register/` | 注册 |
| `index/` | 首页：欢迎语、快捷入口、最近倒计时、今日打卡统计 |
| `profile/` | 个人中心：资料、头像/背景上传、改密、注销 |
| `daily-checkin/` | 每日打卡：4 类型 + 统计 + 时间分布图 |
| `todo/` | 今日待办：增删改、完成、完成说明 |
| `focus/` | 专注计时：正计时/倒计时/番茄钟 |
| `countdown/` | 未来倒计时 |
| `settings/` | 设置：主题、字号、背景、反馈 |
| `about/` | 关于我们 |
| `checkin-history/` | 历史打卡：分页 + 类型/时间筛选 + 按天分组 |
| `privacy/` / `agreement/` | 隐私政策 / 用户协议（小程序审核要求） |

`index/profile/daily-checkin/todo/focus/countdown/settings/about` 使用 `navigationStyle: custom`，其余用系统导航栏。

### 4.2 工具模块（`src/utils`）

| 模块 | 职责 |
|---|---|
| `request.js` | 封装 `uni.request`：多端 baseURL、自动注入 `Bearer token`、401/403 清态跳登录、`buildUrl` / `toCssUrl` |
| `cache.js` | 带过期缓存 + `fetchWithCache`（缓存优先后台更新）+ `clearAllCache` |
| `datetime.js` | 本地墙钟时间序列化，避免 `toISOString()` 的 UTC 偏移 |
| `fontScale.js` | 字号三档（1 / 1.15 / 0.85），改写 `--font-scale`，**仅 H5 生效** |
| `theme.js` | 三套主题变量表 + 运行时换肤，**仅 H5 生效** |

### 4.3 接口基址

配置在 `src/utils/request.js` 的 `getApiBaseUrl()`，取自环境变量：

| 平台 | 变量 | 开发 | 生产 |
|---|---|---|---|
| H5 | `VITE_API_BASE_URL` | `/api`（Vite 代理 → 8080） | `/api`（Nginx 反代） |
| 微信小程序 | `VITE_API_BASE_URL_MP` | `http://localhost:8080/api` | `https://momentkeep.xyz/api` |
| App | `VITE_API_BASE_URL_APP` | `http://localhost:8080/api` | `https://momentkeep.xyz/api` |

### 4.4 设计 token（`src/styles/tokens.css`）

**所有 UI 数值都应取自这里，不要在页面里重新硬编码。**

| 组 | 变量与取值 |
|---|---|
| 圆角 | `--radius-xs` 4 / `-sm` 8 / `-md` 12 / `-lg` 16 / `-pill` 999 / `-circle` 50% |
| 阴影 | `--shadow-sm` `0 1px 3px rgba(0,0,0,.05)`；`-md` `0 2px 8px .05`；`-lg` `0 4px 12px .1`；`-xl` `0 8px 24px .15` |
| 动效 | `--duration-fast` .15s；`--duration-base` .25s；`--ease-standard` `cubic-bezier(.4,0,.2,1)`；`--transition-interactive`（color/bg/border/shadow/transform） |
| 字号 | `--fs-xs` 12 / `-sm` 13 / `-body` 14 / `-md` 16 / `-lg` 18 / `-xl` 20 / `-2xl` 24 / `-3xl` 28 / `-4xl` 32 / `-display` 64（单位 px，**均乘 `var(--font-scale,1)`**） |
| 间距 | `--space-1` 4 / `-2` 8 / `-3` 12 / `-4` 16 / `-5` 20 / `-6` 24 / `-8` 32 / `-10` 40 / `-12` 48 / `-16` 64 |
| 行高/字距 | `--lh-tight` 1.25 / `--lh-body` 1.5 / `--lh-loose` 1.75 / `--ls-title` .02em |
| 字重 | `--fw-normal` 400（读的正文）/ `-medium` 500（标签·按钮·次要标题）/ `-bold` 600（区块标题·数值·品牌名） |
| 半透明描边 | `--border-subtle` rgba(0,0,0,.05) / `-soft` .1 / `-strong` .15；`--primary-soft`；`--secondary-soft` |

> 主题色变量（`--bg-color` / `--surface-color` / `--text-color` 等）**刻意不在 tokens.css 声明**：该选择器在 H5 会被编译为 `:root, uni-page-body`，而 `uni-page-body` 处于 `<html>` 与组件之间，一旦声明就会**遮蔽运行时写入 `documentElement` 的值**（换肤与字号失效的根因）。它们只在 `theme.js` 的主题表与各使用处的 `var(--x, 兜底值)` 出现。

### 4.5 配色

| 用途 | 取值 |
|---|---|
| 背景 | `#F8F6F2` |
| 表面（卡片） | `#F2EEE8` / 强化表面 `#FFFFFF` |
| 主色 | `#C2977F`（暖棕） |
| 辅色 | `#94A7C8` |
| 正文 / 次要 / 弱化 | `#333333` / `#666666` / `#999999` |
| 深色主题 | 暖棕灰 `#26211E`（页底）/ `#322C29`（卡片）/ `#D9B79B`（主色） |

### 4.6 图标与动效

- **图标**：`src/styles/icons.css` 为**内联 base64 PNG 自绘图标**，零外部依赖 —— 小程序 `background-image` 不支持 SVG data-URI，且第三方 CDN 在国内弱网下不稳定。
- **动效**：`src/styles/motion.css` 只提供三种入场效果 —— 列表错落淡入 `mk-rise`（延迟 30ms 递增、最多 8 项）、页面淡入 `mk-page-in`（**仅 opacity**，避免 `transform` 让 fixed 侧栏成为包含块而错位）、数值 `mk-pop`。

### 4.7 跨端约束（踩过的坑，写代码前务必看）

| 约束 | 说明 |
|---|---|
| **WXSS 不支持 `*` 通配选择器** | 一旦出现在 `app.wxss` 中，会以 `unexpected token '*'` **中断整份全局样式编译**，表现为小程序整页白屏。全局规则必须用 `/* #ifdef H5 */` 包住 |
| **小程序没有 `document` / `window`** | 运行时改 CSS 变量（字号、换肤）**仅 H5 可用**；所有 `window`/`document` 访问必须有 `typeof` 守卫 |
| **`<image>` 用 `mode` 而非 `object-fit`** | 小程序 `<image>` 默认 `scaleToFill` 会**拉伸**图片，须写 `mode="aspectFill"`；CSS 的 `object-fit` 对小程序内层无效 |
| **`<button>` 自带 `::after` 边框** | 会与自定义圆角叠成"双框"，需 `button::after { border: none }` |
| **固定高度 + 大内边距会压扁布局** | 全局 reset 是 `box-sizing: border-box`，内边距会挤占内容区；容器用 `min-height` |
| **`@keyup.enter` 在小程序无效** | 需配合 `@confirm` |
| 条件编译是**注释** | 静态检查（lint）会同时看到 H5 与小程序两侧代码，两侧同名 `const` 会报重复声明，变量名需区分（如 `mpClientX`） |
| 顶层可变绑定不稳 | 组件内状态一律用 `ref()`，不要用模块级 `let`（曾出现声明丢失导致 `ReferenceError`） |

### 4.8 运行与构建

```bash
cd frontend/MomentKeep
npm install --legacy-peer-deps      # 必须，否则依赖树 ERESOLVE
npm run dev:h5                      # H5 开发（默认 5173，/api 代理到 8080）
npm run yield:mp-weixin             # 见下方说明
```

| 命令 | 产物 |
|---|---|
| `npm run dev:h5` / `build:h5` | H5 |
| `npm run dev:mp-weixin` / `build:mp-weixin` | 微信小程序（产物 `dist/dev|build/mp-weixin`） |
| `npm run dev:app` / `build:app` | App |

> 小程序端 `manifest.json` 的 `mp-weixin.appid` 目前为**空串**（走测试号）。发布前必须填入真实 AppID —— 否则基础库调用 `webapi_getwxaasyncsecinfo` 会失败并可能连带整页不渲染。

---

## 五、后端开发

### 5.1 包结构（`cn.edu.scnu.momentkeep`）

`controller` / `service(+impl)` / `mapper` / `entity` / `dto/request` / `vo` / `config` / `security` / `common`，启动类 `MomentKeepApplication`（在 `cn.edu.scnu` 包下）。

### 5.2 接口清单

所有业务接口前缀 `/api`。**除注册/登录外全部需要 JWT**。

**UserController** `/api/user`

| 方法 | 路径 | 认证 | 功能 |
|---|---|---|---|
| POST | `/register` | 否 | 注册 |
| POST | `/login` | 否 | 登录，返回 JWT + 用户信息 |
| GET / PUT | `/profile` | 是 | 获取 / 更新资料 |
| POST | `/avatar` | 是 | 上传头像（multipart） |
| POST | `/background` | 是 | 上传背景图 |
| DELETE | `/background` | 是 | 清空背景图 |
| POST | `/logout` | 是 | 登出（自增 `token_version`） |
| POST | `/delete` | 是 | 注销账户 |
| POST | `/change-password` | 是 | 修改密码 |

**CheckinController** `/api/checkin`（全部需认证）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/` | 保存打卡（服务端置 `userId` / `checkinTime`） |
| GET | `/by-date?date=` | 按日期查 |
| GET | `/stats` | 统计 |
| GET | `/has-checked-in?type=&date=` | 是否已打卡 |
| DELETE | `/?type=&date=` | 取消打卡 |
| GET | `/time-distribution?type=&subType=` | 单类型时间分布 |
| GET | `/time-distribution/summary?startDate=&endDate=` | 图表聚合 |
| GET | `/history?limit=` | 历史（默认 30，上限 200） |
| GET | `/history/page?page=&size=&startDate=&endDate=&type=&subType=` | 分页（上限 100/页） |

**TodoController** `/api/todo`（全部需认证）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/` | 创建 |
| PUT | `/` | 更新（**整对象覆盖语义，未传字段可能被置空，见 §8.1**） |
| DELETE | `/{id}` | 删除 |
| GET | `/{id}` | 详情 |
| GET | `/` | 该用户全部待办 |
| GET | `/date/{date}` | 按日期 |
| GET | `/today` | **今日**（未完成 + 今日已完成）—— 前端「今日待办」页使用 |
| POST | `/{id}/complete?completionNote=` | 标记完成（`completionNote` 为**必填**查询参数） |

**FocusController** `/api/focus`（需认证）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/record` | 保存专注记录（时长 ≤24h，mode ∈ stopwatch/countdown/pomodoro） |
| GET | `/records` | 今日记录 + `todayTotal` / `weekTotal` / `total` |
| GET | `/record/todo/{todoId}` | 某待办的专注记录 |

**CountdownController** `/api/countdown`（需认证）：`POST /`、`PUT /`、`DELETE /{id}`、`GET /{id}`、`GET /`、`GET /active`

**FeedbackController** `/api/feedback`（需认证）：`POST /`、`GET /my?page=&size=`

**AiChatController** `/api/ai`（需认证）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/chat` | body `{message}`（≤500 字），返回 `reply` + `quota`；超限 **429** |
| GET | `/quota` | 剩余次数 |
| GET | `/history` | 历史 |
| DELETE | `/clear` | 清空 |

> 代码中**不存在** `AdminController` / `AdminService`。`SecurityConfig` 仅保留 `/api/admin/**` 需 `ROLE_ADMIN` 的规则（属预留）。

### 5.3 安全与全局组件

| 组件 | 职责 |
|---|---|
| `JwtAuthenticationFilter` + `JwtTokenProvider` | JWT 校验，claims 含 `tv`（token 版本号），登出/改密自增 `token_version` 使旧令牌失效 |
| `SecurityConfig` | STATELESS、CORS 白名单（`app.cors.allowed-origins`，默认 `http://localhost:5173`）、放行 register/login/actuator/swagger、`/api/admin/**` 需 ADMIN |
| `GlobalExceptionHandler` | 统一 `Result` 体；`AiQuotaExceededException` → 429；`BusinessException` → 400 |
| `MybatisPlusConfig` | 分页（`maxLimit` 200）+ 乐观锁 |
| `MyMetaObjectHandler` | `createTime` / `updateTime` 自动填充 |
| `AiQuotaService` | 单用户每日 3 次 + 全局每日 200 次，按**北京时间**切自然日，`UPDATE ... WHERE used_count < limit` 原子扣减，失败归还 |

### 5.4 配置（`application.yml`，仅列键名）

`spring.profiles.active`（默认 `local`）、`spring.datasource.*` + `hikari.*`、`spring.sql.init.mode=never`、`spring.servlet.multipart.*`（10MB）、`server.port`（8080）、`mybatis-plus.*`、`deepseek.api.key/url`、`jwt.secret.key/expiration.time/issuer`、`s3.access-key/secret-key/bucket-name/endpoint/region`、`app.cors.allowed-origins`、`ai.quota.daily-limit/global-daily-limit`、`ai.http.*-timeout-ms`、`management.endpoints`、`logging.level`。

**密钥全部经环境变量注入**：`DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET_KEY` / `DEEPSEEK_API_KEY` / `S3_*`。本地开发放 `config/application-local.yml`（已 gitignore）；生产放 `/etc/momentkeep/momentkeep.env`（600）。**任何文档与代码都不应出现明文密钥。**

`application-prod.yml`：关闭 springdoc、Tomcat 100/10 线程、日志写 `/opt/momentkeep/logs/app.log`。

### 5.5 运行与打包

```bash
cd backend/MomentKeep
mvn spring-boot:run                                  # 本地（需先提供 application-local.yml）
mvn clean package -DskipTests                        # 产物 target/MomentKeep-0.0.1-SNAPSHOT.jar
```

---

## 六、数据库设计

初始化 SQL：`backend/MomentKeep/sql/schema.sql`（全量）+ `sql/upgrade_2026-09-18.sql`（增量）。**无 `data.sql`**，`spring.sql.init.mode=never`，建表请手动执行。

| 表 | 主要字段 |
|---|---|
| `user` | id, username, nickname, email, phone, avatar, password(BCrypt), status, role, version, **token_version**, create_time, update_time |
| `checkin` | id, user_id, type(varchar20), checkin_time, meal_type, exercise_type, custom_type, note, create_time, update_time, version |
| `todo` | id, user_id, title, description, priority, completed, completed_time, completion_note, create_time, update_time, version |
| `countdown` | id, user_id, title, description, target_time, color, sort_order, create_time, update_time, version |
| `focus_record` | id, user_id, mode, duration(秒), todo_id, todo_title, start_time, end_time, create_time, update_time, version |
| `feedback` | id, user_id, content, contact, status, reply, create_time |
| `ai_chat` | id, user_id, messages(TEXT), create_time |
| `ai_chat_quota` | id, user_id, quota_date, used_count, create_time, update_time，**唯一键 `(user_id, quota_date)`** |
| `user_setting` | id, user_id(唯一), theme, background_image, ai_auto_fill, notifications, create_time, update_time |
| `admin` | id, username(唯一), password, name, role, permissions, status, version, create_time, update_time |

索引（见 `schema.sql`）：`idx_todo_user`、`idx_countdown_user`、`idx_focus_user_time`、`idx_ai_chat_user_time`、`uk_setting_user` 等。

---

## 七、部署

**完整步骤见 [阿里云ECS部署说明.md](./阿里云ECS部署说明.md)**（Nginx 配置、systemd unit、HTTPS、小程序与 App 上线、AI 限流、上线检查清单、常见问题）。此处只留要点：

- 前端 H5 产物 `frontend/MomentKeep/dist/build/h5`，由 Nginx 托管并反代 `/api` → 后端 8080
- 后端 `mvn clean package -DskipTests` 得 `target/MomentKeep-0.0.1-SNAPSHOT.jar`，Hermes 用 `SPRING_PROFILES_ACTIVE=prod` + `/etc/momentkeep/momentkeep.env` 启动
- 仓库内 `backend/MomentKeep/Dockerfile` 为多阶段构建（`maven:3.9-amazoncorretto-21-alpine` → `amazoncorretto:21-alpine`，非 root，`EXPOSE 8080`）
- 生产 API 域名：`https://momentkeep.xyz/api`

---

## 八、已知问题与技术债

### 8.1 待办更新是「整对象覆盖」语义（易踩）

`TodoServiceImpl.updateTodo` 对 `description` / `priority` **无条件赋值**，而 `title` 却有 null 保护 —— 风格不一致。当前 MyBatis-Plus 走默认 `NOT_NULL` 策略（未配置 `update-strategy`），**未传字段（null）会被跳过**；但当客户端传 `description: ""`、`priority: 0` 这类**非 null 空值**时会被原样写入，且一旦有人把全局策略改成 `ALWAYS` 就会变成真正的整对象覆盖。

- 现状：前端已在勾选/编辑处补齐字段（防御）
- 建议：统一为「仅对非 null 字段 set」的局部更新（`CountdownServiceImpl` 是正确范式），或引入 PATCH 语义

### 8.2 后端待处理项（按优先级）

| 位置 | 问题 |
|---|---|
| `TodoServiceImpl` | 更新 `completed` 时**未维护 `completed_time`** → `getTodayTodos` 会漏掉经 PUT 完成的待办；已完成改回未完成会残留旧时间 |
| `FocusServiceImpl` | 今日/本周查询**只有下界无上界** → **未来时间的记录会被计入** |
| `TodoController` | `complete` 接口的 `completionNote` 是**必填**查询参数 → 不传备注无法完成 |
| `TodoServiceImpl` / `FocusServiceImpl` / `CheckinServiceImpl` / `CountdownServiceImpl` | "今日/本周"用服务器默认时区 `now()`，而 AI 配额用 `Asia/Shanghai` → 服务器为 UTC 时跨天统计错位 |
| `FeedbackServiceImpl` | 分页回传未收敛的 `page/size`，实际查询用 `safePage/safeSize` |
| `MyMetaObjectHandler` | `strictUpdateFill` 导致 `update_time` **更新后不刷新** |
| `GlobalExceptionHandler` / `SecurityConfig` | 业务异常一律 400（"不存在"也 400）；401/403 响应体手写 JSON **缺 `timestamp`** |
| `CountdownServiceImpl` / `UserServiceImpl` | 用 `UpdateWrapper` 绕过实体 `@Version`，乐观锁失效 |

### 8.3 冗余与死代码

`Admin` 实体 + `AdminMapper`（无 service/controller）、`MomentKeepApplication` 中无人使用的 `RestTemplate` Bean、`LoginDTO.remember/agreement`、`ResultCode` 的 1001-1006、`Feedback.type`（实体有、**表中无此列**）、`UserSetting.theme/ai_auto_fill/notifications` 无读写出口；`CheckinServiceImpl.getTimeDistribution` 与 `time-distribution/summary` 功能重复。

**性能**：`FocusServiceImpl` 三次全量 `selectList` 再 stream 求和（可用 SQL `SUM`）；旧版 `getTimeDistribution` 把用户全部记录读进内存。

### 8.4 前端后续项

- 等权卡片分级（首页三类卡片圆角/阴影/留白完全一致，缺少主次）
- 设置页四个开关只改本地 `ref`，无持久化也无后端调用 → 重启即复位（应置灰并注明"暂未开放"）
- 登录/注册表单无即时校验（只在提交时 Toast）
- 打卡页"取消打卡"无二次确认（删除待办/倒计时已有）
- 列表加载失败只弹 Toast，页面停留空态且**无重试入口**
- 字号三档文案顺序为 `['标准','偏大','偏小']`，与 `SCALES` 映射绑定，**不可只改文案**

---

## 九、本次更新记录（截至 2026-09-23）

**设计体系**
- 新增设计 token 层（圆角 / 阴影 / 动效 / 字号 / 间距 / 行高 / 字重），全项目字号统一为 `calc(Npx * var(--font-scale,1))`（224 处）
- 间距收敛到 4 的倍数（78 条声明吸附，残留 0）；全局行高基线 1.5；字重收敛为三档（`bold`→600 归一）
- 新增 `motion.css`：列表错落淡入（15 处内容项，剔除 11 处控件）、页面淡入（仅 opacity）、数值入场
- 图标系统改为**内联 base64 自绘**，全项目移除 icons8 CDN 依赖
- 文案与可用性：38 条"AI 腔/口号式"文案改为陈述事实与下一步动作；失败提示补"怎么办"；触控热区补齐 44px；首页去掉孤立下划线；背景模糊仅对自定义背景生效

**功能修复**
- 字号设置：修复被 `uni-page-body` 遮蔽导致"设置无效"的问题；启动时恢复
- 换肤：确认真实生效（`theme.js` 三套主题）
- 登录「记住我」：修正语义（记住账号 + 由标记决定下次是否自动登录 + 已登录直接进首页）
- 待办：改用 `/todo/today`；修复缓存导致的"新增消失/删除复活"；提交前 trim 与长度校验；补 `@confirm`；插入位置对齐后端排序；勾选与编辑补齐字段；防连点
- 图表：修复双 Y 轴分组（uCharts 按 `series.index` 而非 `yAxisIndex`）、柱间距、小程序点击定位
- 小程序：修复 `*` 通配选择器导致 `app.wxss` 编译失败（白屏）；修复 AI 输入栏被固定高度压扁；贡献者头像改用 `<image mode="aspectFill">`；`window`/`document` 全部加 `typeof` 守卫
- AI 助手：悬浮按钮图标由蓝色位图改为纯 CSS 白色气泡

**文档**
- 将原《前端开发文档》《后端开发文档》《数据库设计文档》《前端开发文档（设计要求优化版）》合并进本 README，并按代码现状校正
- **移除数据库中曾出现的明文 TiDB 凭据**
- 《阿里云ECS部署说明》经核对与代码一致，保留原文

---

## 十、贡献与许可

- 提交前请确保三端可构建：`npm run build:h5` / `build:mp-weixin` / `build:app`
- 新增 UI 数值请使用 `tokens.css` 变量，不要硬编码
- 许可证：MIT

---

**联系**：项目仓库 Issue / 站内反馈（设置 → 意见反馈）
