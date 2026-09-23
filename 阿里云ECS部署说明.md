# MomentKeep 朝暮记 · 阿里云 ECS 上线部署说明（uni-app 三端）

> 适用配置：阿里云 ECS **2 vCPU / 2 GiB 内存 / Alibaba Cloud Linux 3.2104 LTS 64 位**
> 部署方案：**方案 A —— 数据库继续使用 TiDB Cloud**，对象存储继续使用缤纷云，ECS 只承载 Nginx + Spring Boot 应用。
> 覆盖平台：**H5（Web）/ 微信小程序 / App（Android·iOS）**
> 文档更新：**2026-09-19**（本次同步了生产 profile 内置、依赖变更、索引落地、真机运行修复等内容）

---

## 零、通过域名访问：从零到可访问（只想要结论就看这一节）

### 0.1 先明确「用哪个域名访问什么」

| 访问目标 | 地址形态 | 说明 |
|---|---|---|
| H5（浏览器） | `https://你的域名/` | Nginx 直接返回 `dist/build/h5` 下的静态文件 |
| H5 调接口 | `https://你的域名/api/...` | 与页面**同域**，Nginx 反代到 `127.0.0.1:8080` → **天然没有跨域** |
| 微信小程序 | `https://你的域名/api/...` | 需在微信后台配成 **request 合法域名**（必须 HTTPS + 已备案） |
| App | `https://你的域名/api/...` | 由 `.env.production` 的 `VITE_API_BASE_URL_APP` 注入 |

**强烈建议只用一个域名**同时承载页面与接口：一张证书、一处备案、H5 无跨域。
若确实要拆成 `www.`（页面）+ `api.`（接口），H5 就必须再配 `CORS_ALLOWED_ORIGINS`，否则浏览器会拦掉请求。

### 0.2 七步执行顺序

| 步骤 | 做什么 | 位置 | 详见 |
|---|---|---|---|
| 1 | 域名实名认证 + **ICP 备案**（大陆节点强制，通常 3–20 天，**先启动这一步**） | 阿里云控制台 → 备案 | §2.1 |
| 2 | 域名解析：A 记录指向 ECS 公网 IP | 云解析 DNS | §2.1 |
| 3 | 安全组只放行 22（限你的源 IP）/ 80 / 443，**不要开 8080** | ECS → 安全组 | §2.2 |
| 4 | ECS 初始化：时区、Swap、JDK、Nginx、目录、运维用户 | 服务器 | §2.3 / §2.4 |
| 5 | 申请证书，把 `pem` / `key` 放到 `/etc/nginx/ssl/` | 阿里云免费证书 或 acme.sh | §5.2 |
| 6 | 部署后端：上传 jar + 环境变量文件 + systemd（监听 `127.0.0.1:8080`） | 服务器 | 第四章 |
| 7 | 部署 H5：本地构建 → 上传到 `/opt/momentkeep/web` → 写 Nginx 配置 → `reload` | 本地 + 服务器 | §5.1 / 第六章 |

### 0.3 占位符替换清单（最容易漏，逐条打勾）

本文档与配置中的 `api.example.com` 全部要换成你的真实域名：

- [ ] `/etc/nginx/conf.d/momentkeep.conf` → **两处** `server_name`（80 与 443 各一处）
- [ ] `/etc/momentkeep/momentkeep.env` → `CORS_ALLOWED_ORIGINS`（同域部署可留空）
- [ ] `frontend/MomentKeep/.env.production` → `VITE_API_BASE_URL_MP`、`VITE_API_BASE_URL_APP`（H5 保持 `/api` 不变）
- [ ] `acme.sh --issue -d 你的域名`（若用方案二签证书）
- [ ] 微信公众平台 → 开发管理 → 开发设置 → **request 合法域名**：只填 `https://你的域名`（**不要带 `/api` 路径**）
- [ ] TiDB Cloud → 网络白名单：加入 **ECS 的公网 IP**

### 0.4 验收：3 条命令 + 2 个浏览器动作

```bash
# 1) 后端本机活着（应输出 {"status":"UP"}）
curl -fsS http://127.0.0.1:8080/actuator/health

# 2) HTTPS 与证书链正常（应返回 HTTP/2 200，且无证书告警）
curl -fsSI https://你的域名/ | head -1

# 3) 反代确实通到了后端：预期 HTTP 400 + {"code":400,"message":"用户名或密码错误"}
curl -s -o - -w '\nHTTP %{http_code}\n' https://你的域名/api/user/login \
     -X POST -H 'Content-Type: application/json' \
     -d '{"username":"notexist","password":"wrong"}'
```

浏览器里：

1. 打开 `https://你的域名/` → 出现登录页；F12 → Network 看 `/api/user/login` 返回 200/400（**不是 502 / 504 / CORS 报错**）。
2. 直接访问 `https://你的域名/pages/index/index` 后**刷新** → **不能 404**（`try_files $uri $uri/ /index.html` 生效，SPA history 路由）。

### 0.5 三个必踩的坑

1. **备案没通过前，大陆节点的 80/443 会被阿里云拦截**。想提前验证链路，可先把 `server_name` 临时改成 `_` 并用 `http://<ECS_IP>` 访问；但**小程序无法跳过备案**。
2. **HTTP 会 301 跳到 HTTPS**，所以小程序里的接口地址必须是 `https://`，写 `http://` 一定失败。
3. **上传体积三层必须对齐**：Nginx `client_max_body_size 12m` ≥ Spring `multipart.max-file-size 10MB` ≥ 业务层 5MB，任一层偏小都会在传头像/背景时得到 413。

---

## 一、架构与容量评估

### 1.1 部署形态

```
                    ┌──────────────────────────────────────────┐
   H5(浏览器) ──────▶│  Nginx :443  (HTTPS / 静态站点 / 反代)     │
  微信小程序 ───────▶│      ├── /            → dist/build/h5      │
   App(Android/iOS)─▶│      └── /api/        → 127.0.0.1:8080     │
                    └──────────────────┬───────────────────────┘
                                       │
                              ┌────────▼─────────┐
                              │ Spring Boot 3.5  │  JDK 21 + systemd
                              │  -Xmx640m        │
                              └───┬────────┬─────┘
                                  │        │
                     TiDB Cloud ◀─┘        └─▶ 缤纷云 S3 / DeepSeek API
                     （托管 MySQL）             （均为外部 HTTPS 服务）
```

### 1.2 为什么不做单机 MySQL

2 GiB 内存下的真实占用预估：

| 组件 | 峰值内存 | 说明 |
|---|---|---|
| 操作系统 + sshd + systemd | 250 ~ 350 MB | Alinux 3 最小化安装 |
| Nginx | 20 ~ 40 MB | 单 worker，静态站 + 反代 |
| Spring Boot（`-Xmx640m`） | 850 ~ 1000 MB | 堆 640M + Metaspace 192M + 线程栈/直接内存 |
| **方案 A 合计** | **≈ 1.2 ~ 1.4 GB** | 剩余约 600 MB，稳定 |
| 自建 MySQL 8（最低可用） | 400 ~ 600 MB | `innodb_buffer_pool` 压到 256M |
| **方案 B 合计** | **≈ 1.9 ~ 2.1 GB** | 无余量，构建/备份时必 OOM |

结论：**采用方案 A**。TiDB Cloud 已在使用，免运维、免备份、免单点；若将来要迁回自建，建议直接买阿里云 RDS MySQL，而不是装在本机。

### 1.3 三条硬性约束

1. **绝不在 ECS 上执行 `mvn package` 或 `npm run build:*`**。2 核 2 GiB 编译 Spring Boot + 前端会直接触发 OOM Killer（仓库里遗留的 `hs_err_pid*.log` 就是本地内存崩溃证据）。构建一律在本地或 CI 完成，服务器只接收产物。
2. **后端 8080 必须只监听 127.0.0.1**，外部流量统一走 Nginx，禁止在安全组放行 8080。
3. **必须配置 2 GB Swap**，作为内存尖峰的兜底（尤其是日志轮转、HeapDump 场景）。

---

## 二、环境准备

### 2.1 域名与备案（三端共用的前提）

| 项目 | 要求 | 说明 |
|---|---|---|
| 域名 | 例如 `api.example.com`（接口）/ `www.example.com`（H5） | 可共用一个域名 |
| ICP 备案 | **必须** | 微信小程序 `request` 合法域名要求已备案域名 |
| HTTPS 证书 | **必须** | 小程序与 App 均不接受 HTTP 与自签证书 |
| 微信小程序 appid | 需填入 `src/manifest.json` | 当前为空，不填无法上传代码 |

### 2.2 安全组

只放行以下入方向规则，其余全部拒绝：

| 端口 | 来源 | 用途 |
|---|---|---|
| 22 | 你的固定公网 IP/32 | SSH（不要对全网开放） |
| 80 | 0.0.0.0/0 | HTTP，用于跳转 HTTPS 与证书续期校验 |
| 443 | 0.0.0.0/0 | HTTPS |

> 3306 / 8080 / 4000 **一律不在安全组放行**。

### 2.3 系统初始化

```bash
# 1) 时区与时间同步（AI 配额按北京时间切分自然日，务必设置）
timedatectl set-timezone Asia/Shanghai
systemctl enable --now chronyd

# 2) 创建运维用户与目录规范
useradd -r -s /sbin/nologin momentkeep
mkdir -p /opt/momentkeep/{app,web,releases,logs} /etc/momentkeep /data/backup
chown -R momentkeep:momentkeep /opt/momentkeep
chmod 750 /etc/momentkeep

# 3) Swap（2 GiB 机器的必备兜底）
dd if=/dev/zero of=/swapfile bs=1M count=2048
chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# 4) 内核参数
cat > /etc/sysctl.d/99-momentkeep.conf <<'EOF'
vm.swappiness = 10
fs.file-max = 65535
net.core.somaxconn = 1024
net.ipv4.tcp_max_syn_backlog = 1024
net.ipv4.tcp_tw_reuse = 1
EOF
sysctl --system

# 5) 文件句柄数
cat >> /etc/security/limits.conf <<'EOF'
momentkeep soft nofile 65535
momentkeep hard nofile 65535
EOF
```

### 2.4 安装依赖

```bash
dnf update -y
dnf install -y nginx tar unzip curl policycoreutils-python-utils

# JDK 21：优先仓库安装；若仓库无 java-21 包，则使用 Temurin tarball
dnf install -y java-21-openjdk-headless || {
  curl -L -o /tmp/jdk21.tar.gz \
    "https://github.com/adoptium/temurin21-binaries/releases/latest/download/OpenJDK21U-jdk_x64_linux_hotspot_latest.tar.gz"
  mkdir -p /usr/lib/jvm && tar -xzf /tmp/jdk21.tar.gz -C /usr/lib/jvm
  ls -d /usr/lib/jvm/jdk-21* > /etc/momentkeep/java_home
}
java -version   # 必须输出 21.x

# SELinux 放行 Nginx 反代（Alinux 3 默认 enforcing）
setsebool -P httpd_can_network_connect 1
```

---

## 三、数据库准备（TiDB Cloud）

### 3.1 网络白名单

在 TiDB Cloud 控制台把 **ECS 的公网出口 IP** 加入 IP 访问白名单（`Connect` → `Allow Access from`）。ECS 上的公网 IP 可用：

```bash
curl -s ifconfig.me
```

> 若后续更换 ECS，需要同步更新白名单。

### 3.2 执行升级脚本

本次修复新增了「令牌吊销」与「AI 每日配额」两处表结构变更，**必须先执行**，否则服务能启动但相关功能会报错：

```bash
# 脚本位置：backend/MomentKeep/sql/upgrade_2026-09-18.sql
mysql -h gateway01.ap-southeast-1.prod.alicloud.tidbcloud.com -P 4000 \
      -u '<DB_USERNAME>' -p --ssl-mode=VERIFY_IDENTITY \
      momentkeep < sql/upgrade_2026-09-18.sql
```

脚本内容：
1. `ALTER TABLE user ADD COLUMN token_version INT NOT NULL DEFAULT 0`（登出/改密/注销后令牌立即失效）
2. `CREATE TABLE ai_chat_quota`（单用户每日 3 次限额 + 全局日上限统计）

> 脚本不可重复执行；若列/表已存在请跳过对应语句。

### 3.3 连接参数建议

- TiDB 连接串保持 `sslMode=VERIFY_IDENTITY`。
- 2 核机器上 HikariCP 连接池**不要超过 15**（默认已配置为 10）。

---

## 四、后端构建与部署

### 4.1 本地构建（在开发机执行）

```bash
cd backend/MomentKeep
mvn clean package -DskipTests
# 产物：target/MomentKeep-0.0.1-SNAPSHOT.jar
```

> 如果你用 **JDK 23 及以上**编译，`pom.xml` 已显式配置 `<proc>full</proc>`，无需额外参数。
> 构建成功后上传：

```bash
REL=$(date +%Y%m%d%H%M)
ssh root@<ECS_IP> "mkdir -p /opt/momentkeep/releases/$REL"
scp target/MomentKeep-0.0.1-SNAPSHOT.jar root@<ECS_IP>:/opt/momentkeep/releases/$REL/momentkeep.jar
```

### 4.2 环境变量文件

```bash
# /etc/momentkeep/momentkeep.env   —— 权限必须 600，属主 root:momentkeep
chmod 600 /etc/momentkeep/momentkeep.env
```

```ini
# ---- JVM 参数（2 GiB 机器的安全区间，不建议调大 -Xmx）----
JAVA_OPTS=-Xms256m -Xmx640m -XX:MaxMetaspaceSize=192m -XX:MaxDirectMemorySize=128m -Xss512k \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8 \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/momentkeep/logs

# ---- 数据库（TiDB Cloud）----
DB_USERNAME=<你的 TiDB 用户名>
DB_PASSWORD=<你的 TiDB 密码>
DB_POOL_SIZE=10

# ---- JWT（务必用新密钥，且与旧环境不同）----
# 生成命令：openssl rand -base64 48
JWT_SECRET_KEY=<48 位以上随机串>
JWT_EXPIRATION_TIME=86400000
JWT_ISSUER=momentkeep

# ---- DeepSeek ----
DEEPSEEK_API_KEY=<你的 Key>
DEEPSEEK_API_URL=https://api.deepseek.com

# ---- 缤纷云 S3 ----
S3_ACCESS_KEY=<...>
S3_SECRET_KEY=<...>
S3_BUCKET_NAME=momentkeep
S3_ENDPOINT=https://s3.bitiful.net
S3_REGION=cn-east-1

# ---- 跨域白名单（H5 与后端同域时可留空；小程序/App 不受 CORS 限制）----
CORS_ALLOWED_ORIGINS=https://www.example.com

# ---- AI 限额（防滥用核心配置）----
AI_DAILY_LIMIT=3
AI_GLOBAL_DAILY_LIMIT=200
```

### 4.3 systemd 托管

```bash
JAVA_BIN=$(command -v java)   # 例如 /usr/lib/jvm/java-21-openjdk-21.0.x/bin/java
cat > /etc/systemd/system/momentkeep.service <<EOF
[Unit]
Description=MomentKeep Backend
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=momentkeep
Group=momentkeep
WorkingDirectory=/opt/momentkeep/current
EnvironmentFile=/etc/momentkeep/momentkeep.env
ExecStart=${JAVA_BIN} \$JAVA_OPTS -jar /opt/momentkeep/current/app.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=always
RestartSec=10
LimitNOFILE=65535
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ProtectHome=true

[Install]
WantedBy=multi-user.target
EOF

# 建立软链接并启动
ln -sfn /opt/momentkeep/releases/<REL> /opt/momentkeep/current
ln -sfn /opt/momentkeep/releases/<REL>/momentkeep.jar /opt/momentkeep/current/app.jar
systemctl daemon-reload
systemctl enable --now momentkeep
systemctl status momentkeep
journalctl -u momentkeep -f
```

### 4.4 生产 profile（仓库已内置，无需手工创建）

`src/main/resources/application-prod.yml` **已随仓库提供，并且会打进 jar**，内容如下（不含任何密钥）：

```yaml
springdoc:
  # 生产关闭接口文档：否则 /v3/api-docs 与 /swagger-ui 会暴露全部接口与 DTO 结构
  api-docs:   { enabled: false }
  swagger-ui: { enabled: false }
server:
  # address: 127.0.0.1      # 配了 Nginx 再打开：杜绝绕过 Nginx 直连 8080
  tomcat:
    threads:      { max: 100, min-spare: 10 }
    accept-count: 100
logging:
  file: { name: /opt/momentkeep/logs/app.log }
  logback:
    rollingpolicy: { max-file-size: 20MB, max-history: 7, total-size-cap: 500MB }
```

启动时带 `--spring.profiles.active=prod` 即生效（systemd 与 Dockerfile 中已包含），**不需要**再在服务器上创建这个文件。
如需临时覆盖，放到 `/opt/momentkeep/current/application-prod.yml` 即可（外部配置优先级高于 jar 内）。

> 变更说明：早期版本这里让服务器手工创建该文件，且示例里带了 `spring.jpa.show-sql`。
> 本项目**未引入 JPA**，该段起不到任何作用，已移除；现在 Swagger 的开关由 jar 内的 profile 文件统一控制。
> `/actuator/health` 的暴露范围由 `application.yml` 的 `management.endpoints.web.exposure.include: health,info` 决定，只开这两个端点。

### 4.5 本地开发与生产配置的分工（重要）

后端配置拆成两条互不干扰的链路，**请勿把本地配置带到服务器上**：

| 环境 | 配置来源 | 密钥存放位置 | 是否进 Git | 是否进 jar |
|---|---|---|---|---|
| 本地开发（IDEA 直接 Run） | `application.yml` + `<工作目录>/config/application-local.yml` | `backend/MomentKeep/config/application-local.yml` | ❌ 已忽略 | ❌ 不在 `src` 下，不会打包 |
| 生产（ECS / 容器） | `application.yml` + 环境变量（`prod` profile） | `/etc/momentkeep/momentkeep.env`（600 权限） | ❌ | ❌ |

工作机制：

- `application.yml` 中 `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}`，本地默认激活 `local`，
  Spring Boot 会自动加载工作目录下 `config/application-local.yml`（外部配置优先级高于 jar 内的 `application.yml`），
  因此**IDEA 里直接 Run 不需要配置任何环境变量**。
- 该文件位于 `src/main/resources` 之外，`mvn package` 不会把它打进 jar；
  且已被根 `.gitignore` 的 `**/config/application-local.yml` 规则忽略。
- 生产启动**必须显式指定 prod**（命令行参数优先级最高，会覆盖默认值）：
  `java -jar app.jar --spring.profiles.active=prod`（systemd 与 Dockerfile 中已包含）。
  如果忘了带该参数，服务器上找不到 `config/application-local.yml`，启动会**直接报占位符解析失败**——
  属于"响亮失败"，不会静默使用错误的配置。

---

## 五、Nginx 与 HTTPS

### 5.1 站点配置

```nginx
# /etc/nginx/conf.d/momentkeep.conf

# 登录接口限流：每 IP 每分钟 6 次，防暴力破解
limit_req_zone $binary_remote_addr zone=mk_login:10m rate=6r/m;
# AI 对话限流：每 IP 每分钟 20 次，配合后端每日配额使用
limit_req_zone $binary_remote_addr zone=mk_ai:10m rate=20r/m;
# 注册限流：每 IP 每小时 5 次，防止被批量注册账号刷 AI 额度
limit_req_zone $binary_remote_addr zone=mk_register:10m rate=5r/h;

server {
    listen 80;
    server_name api.example.com;
    # 证书续期校验目录放行，其余全部跳转 HTTPS
    location /.well-known/acme-challenge/ { root /var/www/certbot; }
    location / { return 301 https://$host$request_uri; }
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate     /etc/nginx/ssl/momentkeep.pem;
    ssl_certificate_key /etc/nginx/ssl/momentkeep.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_session_cache   shared:SSL:10m;
    ssl_session_timeout 10m;
    add_header Strict-Transport-Security "max-age=31536000" always;

    # ---------- H5 静态站点 ----------
    root /opt/momentkeep/web;
    index index.html;
    client_max_body_size 12m;          # 与后端 10MB 上传限制对齐

    gzip on;
    gzip_min_length 1k;
    gzip_comp_level 5;
    gzip_types text/css application/javascript application/json image/svg+xml;

    location / {
        try_files $uri $uri/ /index.html;   # SPA history 路由
    }
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    location = /index.html {
        add_header Cache-Control "no-cache";
    }
    location /static/ {
        expires 30d;
    }

    # ---------- 后端接口 ----------
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 5s;
        proxy_send_timeout   120s;
        proxy_read_timeout   120s;       # DeepSeek 首字延迟较高，必须放大
    }

    # 敏感接口单独限流（注意 proxy_pass 必须重复声明）
    location = /api/user/login {
        limit_req zone=mk_login burst=5 nodelay;
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    location = /api/user/register {
        limit_req zone=mk_register burst=3 nodelay;
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    location = /api/ai/chat {
        limit_req zone=mk_ai burst=5 nodelay;
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 120s;
    }

    # ---------- 运维端点：仅本机可访问 ----------
    location /actuator/ {
        allow 127.0.0.1;
        deny all;
        proxy_pass http://127.0.0.1:8080;
    }

    # ---------- 生产环境屏蔽接口文档 ----------
    location ~ ^/(swagger-ui|v3/api-docs) {
        return 404;
    }

    access_log /var/log/nginx/momentkeep.access.log;
    error_log  /var/log/nginx/momentkeep.error.log;
}
```

```bash
nginx -t && systemctl enable --now nginx && systemctl reload nginx
```

### 5.2 HTTPS 证书

方式一（推荐）：阿里云控制台申请免费证书，下载 Nginx 版，放到 `/etc/nginx/ssl/`。

方式二：`acme.sh` 自动签发与续期

```bash
curl https://get.acme.sh | sh -s email=you@example.com
~/.acme.sh/acme.sh --issue -d api.example.com --webroot /var/www/certbot
~/.acme.sh/acme.sh --install-cert -d api.example.com \
  --key-file /etc/nginx/ssl/momentkeep.key \
  --fullchain-file /etc/nginx/ssl/momentkeep.pem \
  --reloadcmd "systemctl reload nginx"
```

---

## 六、H5 端上线

### 6.1 构建（本地执行）

**第 0 步：安装依赖**。项目存在 peer 依赖冲突（`vue` 被钉在 `3.4.21`，而 `@dcloudio/*` 版本要求更高的 Vue），
**不带参数会直接报 ERESOLVE 失败**：

```bash
cd frontend/MomentKeep
npm install --legacy-peer-deps      # 或 npm ci --legacy-peer-deps（干净拉取时）
```

**第 1 步：确认域名已写入 `.env.production`**，然后构建：

```bash
#    VITE_API_BASE_URL=/api                    ← H5 与后端同域，走 Nginx 反代，无跨域
#    VITE_API_BASE_URL_MP=https://你的域名/api   ← 必须 HTTPS 且已备案
#    VITE_API_BASE_URL_APP=https://你的域名/api
npm run build:h5
# 产物目录：dist/build/h5/   （index.html + assets/ + static/）
```

> ⚠️ **部署位置限制**：`vite.config.js` 未设置 `base`，因此 H5 **只能部署在域名根路径**（`https://你的域名/`）。
> 若想挂在子路径（如 `/app/`），需先在 `vite.config.js` 加 `base: '/app/'` 并重新构建，同时调整 Nginx 的 `root`/`location`。

> ⚠️ 必须使用 `uni build`（脚本已修正）。若直接 `vite build`，产物会错落到 `dist/` 根目录，导致 Nginx 指向的目录里没有 `index.html`。

> ⚠️ 必须使用 `uni build`（脚本已修正）。若直接 `vite build`，产物会错落到 `dist/` 根目录，导致 Nginx 指向的目录里没有 `index.html`。

### 6.2 发布

```bash
REL=$(date +%Y%m%d%H%M)
ssh root@<ECS_IP> "mkdir -p /opt/momentkeep/releases/$REL/web"
scp -r dist/build/h5/* root@<ECS_IP>:/opt/momentkeep/releases/$REL/web/
ssh root@<ECS_IP> "
  ln -sfn /opt/momentkeep/releases/$REL /opt/momentkeep/current
  # Nginx 的 root 指向 /opt/momentkeep/web，因此软链一份
  ln -sfn /opt/momentkeep/releases/$REL/web /opt/momentkeep/web_stage
"
```

简化做法：把 Nginx 的 `root` 直接改成 `/opt/momentkeep/current/web`，发布即生效、回滚即换软链。

### 6.3 验证

浏览器打开 `https://api.example.com`，检查：
- 登录接口返回 200（F12 Network 中 `/api/user/login`）
- 刷新任意子页面不出现 404（`try_files` 生效）
- 静态资源响应头含 `Cache-Control: public, immutable`

---

## 七、微信小程序端上线

### 7.1 上位前置修改（必须）

| 项 | 位置 | 要求 |
|---|---|---|
| 小程序 appid | `src/manifest.json` → `mp-weixin.appid` | 填真实 appid（当前为空，无法上传） |
| 接口域名 | `.env.production` → `VITE_API_BASE_URL_MP` | 必须 `https://` 且已备案 |
| 合法域名 | 微信公众平台 → 开发管理 → 服务器域名 | 把 `https://api.example.com` 加入 **request 合法域名** |
| 组件标签 | 各页面 `.vue` | 见 7.4，**这是小程序端最大的改造项** |

### 7.2 构建

```bash
cd frontend/MomentKeep
npm run build:mp-weixin
# 产物目录：dist/build/mp-weixin/
```

### 7.3 上传与发布

1. 用微信开发者工具打开 `dist/build/mp-weixin` 目录。
2. 工具内「上传」→ 填写版本号与备注。
3. 微信公众平台 → 版本管理 → 提交审核 → 审核通过后发布。
4. 审核期间可用「体验版」验证：添加体验成员，扫码使用。

### 7.4 小程序端必须完成的代码改造（当前尚未完成）

当前页面大量使用 HTML 标签（`div` / `span` / `h1` / `img` / `button` 等），**在微信小程序中不会被渲染**，页面会大面积空白。上线前必须完成标签替换：

| H5 标签 | uni-app 组件 | 说明 |
|---|---|---|
| `div` / `section` / `header` / `footer` / `p` / `h1~h6` | `<view>` | H5 下 `view` 渲染为 `uni-view`，`display:block`，原 class 样式仍生效 |
| `span` / `strong` / `em` | `<text>` | 纯文本容器，注意 `text` 为行内元素 |
| `img` | `<image>` | 必须指定宽高，建议配合 `mode="aspectFill"` |
| `button` | `<view @click>` 或 `<button>` | uni 的 `button` 自带 weui 默认样式，原自定义样式需覆盖 |
| `input` / `textarea` | `<input>` / `<textarea>` | 事件名不同：`@input` 取 `e.detail.value` |
| `ul` / `li` / `a` | `<view>` / `<navigator>` | `navigator` 不能跳转外部链接 |

建议改造顺序（按复杂度递增，每改完一个页面用真机预览验证）：

1. `pages/login/login.vue`（311 行）
2. `pages/register/register.vue`（328 行）
3. `pages/about/about.vue`（423 行）
4. `pages/index/index.vue`（557 行）
5. `pages/profile/profile.vue`（804 行）
6. `pages/countdown/countdown.vue`（863 行）
7. `pages/settings/settings.vue`（879 行）
8. `pages/todo/todo.vue`（922 行）
9. `pages/focus/focus.vue`（1051 行）
10. `components/Layout.vue`（1260 行）
11. `pages/daily-checkin/daily-checkin.vue`（2028 行，建议同时拆分组件）

其他必须同时处理的小程序端差异：

- **CSS 变量**：`App.vue` 已把 `--bg-color` 等变量同时声明到 `:root` 与 `page` 上；页面中 `var(--base-font-size)` 若还取不到值，请补兜底值 `var(--base-font-size, 16px)`（`todo.vue` 中有 18 处）。
- **H5 专有 API**：`document` / `window` / `CustomEvent` / `localStorage` 在小程序中不存在。`pages/settings/settings.vue` 的主题切换依赖这些 API，需改为 `uni.setStorageSync` + 全局状态方案，并用 `// #ifdef H5` 条件编译隔离。
- **`:hover` / `@media` / `backdrop-filter`**：仅 H5 生效，小程序端会被忽略，不影响功能但视觉有差异，建议用条件编译补足。
- **定时器**：`setInterval` 在小程序退到后台仍会执行，长任务需在 `onHide` 中暂停。

### 7.5 小程序端不需要处理的部分

- `CORS` 白名单：小程序不受浏览器同源策略限制，无需在后端放行。
- `HTTPS 证书链`：必须完整（缺失中间证书会导致小程序请求失败，浏览器却正常，是最容易踩的坑）。

---

## 八、App 端上线（Android / iOS）

### 8.1 前置条件：必须安装 App 平台运行时（否则只能停在基座欢迎页）

CLI 工程的 App 端依赖 **`@dcloudio/uni-app-plus`**（官方模板默认包含）。若缺失，`uni -p app` 会**静默退化成网页构建**：
产物目录里只有 `index.html` + `assets/*.js`，**没有 `app-service.js`** → 手机上的 HBuilderX 基座拿不到可运行资源，
就会一直停在「HBuilderX真机运行 / 本应用无法独立运行」那一页，**即使 HBuilderX 日志显示"编译成功、同步文件成功"**。

```bash
cd frontend/MomentKeep
# 版本号必须与 package.json 中其它 @dcloudio/* 包完全一致
npm i @dcloudio/uni-app-plus@<与 package.json 一致的版本号> --legacy-peer-deps
```

**自检**：`npm run build:app` 之后，`dist/build/app/` 下**必须**同时存在：

```
app-service.js        ← App 运行时代码（缺失即代表该依赖没装好）
app-config.js
app-config-service.js
app.css
manifest.json
uni-app-view.umd.js
```

### 8.2 打包方式选型

| 方式 | 适用 | 说明 |
|---|---|---|
| **HBuilderX 云打包** | 推荐（学生项目） | 无需配置 Android Studio / Xcode，按次免费额度足够 |
| HBuilderX 本地打包 | 需要自定义原生插件 | 需要 Android SDK / Xcode 环境 |
| `npm run build:app` + 本地工程 | 有原生开发能力 | 产物在 `dist/build/app/`，再用原生工程打包 |

```bash
cd frontend/MomentKeep
npm run build:app
# 产物目录：dist/build/app/
```

### 8.3 上线前必须完成的配置

`src/manifest.json`：

1. **appid**：`__UNI__1F1A9FD` 是示例值，需在 HBuilderX 中重新获取真实 appid。
2. **精简 Android 权限**：当前申请了 `READ_PHONE_STATE`、`GET_ACCOUNTS`、`MOUNT_UNMOUNT_FILESYSTEMS`、`READ_LOGS`、`WRITE_SETTINGS` 等与功能无关的敏感权限，应用商店审核会直接驳回。本项目实际只需要：
   - `INTERNET`（网络，uni-app 默认包含）
   - `ACCESS_NETWORK_STATE`（判断网络状态）
   - `CAMERA`（若支持拍照上传头像）
   - 相册/存储：使用 `READ_MEDIA_IMAGES`（Android 13+）或 `READ_EXTERNAL_STORAGE`（12-）
3. **图标与启动图**：`app-plus.distribute.icons` 与 `splashscreen` 为空的 `{}`，必须补齐。
4. **iOS 配置**：`ios: {}` 为空，需补 Bundle ID、隐私描述（相册/相机用途说明）、UILaunchStoryboardName 等。
5. **接口地址**：由 `.env.production` 的 `VITE_API_BASE_URL_APP` 注入，必须为 HTTPS 域名。

### 8.4 上传应用市场

- **Android**：华为/小米/OPPO/vivo/应用宝等，需提供软著或备案信息（各市场要求不同），多数需要《计算机软件著作权登记证书》。
- **iOS**：需 Apple Developer 账号（$99/年），在 App Store Connect 创建应用后用 Xcode/Transporter 上传 ipa；审核会检查隐私清单与权限说明。
- 学生项目若不想上架，可用 **HBuilderX 生成 APK 自行分发**（Android 直接安装），iOS 用 TestFlight 或开发者证书装机。

### 8.5 真机调试：一直停在基座欢迎页怎么办

按出现概率从高到低排查：

| # | 检查项 | 处理 |
|---|---|---|
| 1 | 有没有装 `@dcloudio/uni-app-plus`、`dist/build/app/app-service.js` 存不存在 | 见 §8.1。**这是本项目实际踩到的原因**：依赖缺失时 HBuilderX 日志照样显示"编译成功、同步文件成功"，但基座拿不到 App 资源 |
| 2 | **基座版本与编译器版本是否匹配** | `@dcloudio/*` 版本号里带 HBuilderX 版本与渠道（如 `3.0.0-5020620260917001` ≈ HBuilderX 5.2.6 alpha 渠道、2026-09-17）。HBuilderX 版本低于它时基座不认识产物 → 升级 HBuilderX，或把 `@dcloudio/*` 整体降到与你的 HBuilderX 版本一致的**正式版** |
| 3 | 手机端基座有没有**存储/文件权限**（Android 10+） | 缺权限时同步属于"假成功"。到系统设置里给 HBuilderX 基座开权限，再**彻底杀掉基座进程**重新运行 |
| 4 | 基座缓存了上一次的项目 | 手机上清除基座数据或卸载重装基座 |
| 5 | 手机与电脑不在同一网段 | 基座靠局域网连 HBuilderX，跨网段必然失败（日志会卡在"正在建立手机连接"） |

> **判定要点**：先看 `dist/build/app/` 里有没有 `app-service.js`。
> 有 → 问题在基座/版本/权限（第 2~5 条）；没有 → 问题在项目依赖，先按 §8.1 处理。

---

## 九、AI 限额与防滥用配置

### 9.1 三层防护

| 层级 | 机制 | 配置位置 | 默认值 |
|---|---|---|---|
| 业务层 | 单用户每日对话次数 | `AI_DAILY_LIMIT` | **3 次/天** |
| 业务层 | 全局每日调用总上限（所有账号合计） | `AI_GLOBAL_DAILY_LIMIT` | 200 次/天 |
| 网络层 | 单 IP 限流 | Nginx `mk_ai` / `mk_login` / `mk_register` | 20 次/分 / 6 次/分 / 5 次/时 |

实现要点（已在代码中落地）：

- 配额按 **(user_id, quota_date)** 唯一存储，自然日按 **北京时间** 切分，跨天自动重置。
- 扣减走 `UPDATE ... WHERE used_count < limit` 并判断影响行数，**并发下不会超额**。
- 只有外部 AI 调用真正失败时才归还配额（`refund`），避免用失败请求薅免费额度。
- 单条消息限制 **500 字符**（`@Size(max = 500)`），防止超长输入成倍放大 token 消耗。
- 会话上下文只上送最近 **10 条**、本地最多留存 **40 条**，控制 token 与存储膨胀。
- 配额超限返回 **HTTP 429**，前端只提示"今日次数已用完"，不会误清登录态。

### 9.2 为什么必须配全局上限

注册接口是公开的，恶意用户可批量注册账号绕过"每用户 3 次"的限制。因此在 2 核 2 GiB 的小站上，**全局日上限是钱包的最后一道防线**（200 次/天 ≈ 每天最多消耗 200 次 DeepSeek 调用）。若发现异常消耗，立即：

1. 把 `AI_GLOBAL_DAILY_LIMIT` 调到更低值并 `systemctl restart momentkeep`；
2. 在 Nginx 层临时把 `mk_ai` 的 `rate` 降到 `5r/m`；
3. 到 DeepSeek 控制台查看用量并把 API Key **轮换**。

### 9.3 前端展示

AI 面板标题栏会显示「今日剩余 N/3 次」，数据来自 `GET /api/ai/quota`；每次对话的响应体也会带上最新配额，无需额外请求。

---

## 十、运维与监控

### 10.1 健康检查与自愈

```bash
# 手动探活
curl -fsS http://127.0.0.1:8080/actuator/health

# 应用级探活（systemd 只能感知进程存活，探活可感知接口假死）
cat > /etc/cron.d/momentkeep-health <<'EOF'
*/2 * * * * root curl -fsS -m 5 http://127.0.0.1:8080/actuator/health >/dev/null || systemctl restart momentkeep
EOF
```

### 10.2 日志与轮转

```bash
cat > /etc/logrotate.d/momentkeep <<'EOF'
/opt/momentkeep/logs/*.log {
  daily
  rotate 7
  size 20M
  compress
  missingok
  notifempty
  copytruncate
}
EOF

cat > /etc/logrotate.d/nginx-momentkeep <<'EOF'
/var/log/nginx/momentkeep.*.log {
  daily
  rotate 14
  compress
  missingok
  notifempty
  sharedscripts
  postrotate
    /usr/bin/systemctl reload nginx >/dev/null 2>&1 || true
  endscript
}
EOF
```

后端日志级别与文件输出已在 `application.yml` 中收敛（`cn.edu.scnu.momentkeep: info`），避免 DEBUG 日志写满磁盘。

### 10.3 云监控告警

在阿里云云监控（CloudMonitor）为这台 ECS 配置以下告警规则——2 GiB 机器的瓶颈非常明确：

| 指标 | 阈值 | 说明 |
|---|---|---|
| 内存使用率 | > 85% 持续 5 分钟 | 最高优先级，接近上限时会触发 OOM Kill |
| CPU 使用率 | > 80% 持续 5 分钟 | 2 核容易被 AI 请求或慢查询打满 |
| 磁盘使用率 | > 80% | 日志 + HeapDump + 备份都在涨 |
| Swap 使用率 | > 50% | 说明物理内存已不够，需要降 -Xmx 或升配 |
| 公网带宽 | 接近上限 | 1 Mbps 带宽下 H5 首屏可能达到瓶颈 |

### 10.4 备份

- **数据库**：TiDB Cloud 自带备份与时间点恢复，ECS 无需额外操作（方案 A 的核心收益）。
- **用户上传文件**：存于缤纷云对象存储，不在 ECS 上，无需备份。
- **配置与版本**：`/etc/momentkeep/`、`/etc/nginx/conf.d/`、`/opt/momentkeep/releases/` 建议纳入 Git 或每周打包到 OSS。

### 10.5 发布与回滚

```bash
# 发布（后端）
REL=$(date +%Y%m%d%H%M)
# 1. 上传 jar 到 /opt/momentkeep/releases/$REL/
# 2. 切换软链
ln -sfn /opt/momentkeep/releases/$REL /opt/momentkeep/current
systemctl restart momentkeep
sleep 15
curl -fsS http://127.0.0.1:8080/actuator/health || echo "健康检查失败，准备回滚"

# 回滚：切回上一个版本
ln -sfn /opt/momentkeep/releases/<上一个版本> /opt/momentkeep/current
systemctl restart momentkeep

# 保留最近 3 个版本，自动清理更早的
ls -1dt /opt/momentkeep/releases/*/ | tail -n +4 | xargs -r rm -rf
```

---

## 十一、上线检查清单

### 后端

- [x] `sql/upgrade_2026-09-18.sql` 已在 TiDB Cloud 执行（`user.token_version`、`ai_chat_quota`）
- [x] 索引已落地：`todo(user_id,completed)`、`focus_record(user_id,start_time)`、`ai_chat(user_id,create_time)`、`user_setting` 唯一键
- [ ] 清理重复索引（可选，减少写入开销）：`checkin.idx_checkin_user_time` 与原有 `idx_user_time` 完全重复、`countdown.idx_countdown_user` 与原有 `idx_user_target` 完全重复
- [x] 仓库内明文密钥文件 `backend/MomentKeep/src/main/resources/.env` 已删除（它会被打进 jar，且 Spring 从不读取它）
- [ ] `/etc/momentkeep/momentkeep.env` 已按 §4.2 生成，权限 `600`、属主 `root:momentkeep`，且**取值与本地 `config/application-local.yml` 不同**
- [ ] `JWT_SECRET_KEY` 为 48 位以上随机串（`openssl rand -base64 48`），且与本地开发环境不同
- [ ] `CORS_ALLOWED_ORIGINS` 已改为正式域名（**H5 与接口同域时可留空**）
- [ ] `server.address=127.0.0.1`（`application-prod.yml` 中取消注释），安全组未放行 8080
- [ ] `AI_DAILY_LIMIT=3`、`AI_GLOBAL_DAILY_LIMIT` 已按预算设置
- [ ] `/actuator/health` 本机可达、公网不可达（Nginx 已 `allow 127.0.0.1; deny all;`）
- [ ] `/swagger-ui`、`/v3/api-docs` 在生产返回 404（由 `application-prod.yml` 的 `springdoc.*.enabled: false` 保证）
- [ ] 所有历史泄漏的凭据（TiDB 密码 / DeepSeek Key / S3 AK-SK / JWT Secret）**已在各自控制台轮换**

### 前端·H5

- [ ] 依赖已装：`npm install --legacy-peer-deps`（不带参数会因 peer 冲突报 ERESOLVE）
- [ ] `npm run build:h5` 产物在 `dist/build/h5/`（含 `index.html`）
- [ ] `.env.production` 的 `VITE_API_BASE_URL=/api`
- [ ] 站点部署在**域名根路径**（未设 `base`，不支持子路径）
- [ ] 刷新子路由不 404、登录/打卡/待办/倒计时/AI 全流程自测通过

### 前端·微信小程序

- [ ] `manifest.json` 的 `mp-weixin.appid` 已填真实 appid
- [ ] `VITE_API_BASE_URL_MP` 为已备案 HTTPS 域名，且已配置为 request 合法域名
- [ ] **HTML 标签已替换为 uni 组件**（否则页面大面积空白）
- [ ] 真机（非模拟器）验证网络请求与样式
- [ ] 已完成 `dist/build/mp-weixin` 上传 + 提审

### 前端·App

- [ ] **`@dcloudio/uni-app-plus` 已安装**，且 `npm run build:app` 产物含 `app-service.js`（见 §8.1）
- [ ] Android 权限已精简至最小集
- [ ] 图标 / 启动图 / Bundle ID / iOS 隐私描述已补齐
- [ ] `VITE_API_BASE_URL_APP` 为 HTTPS 域名
- [ ] 真机运行不再停在基座欢迎页（见 §8.5）
- [ ] 已完成云打包并在真机安装验证

---

## 十二、常见问题

| 现象 | 排查方向 |
|---|---|
| 服务起不来，日志报 `Could not resolve placeholder 'JWT_SECRET_KEY'` | **本地开发**：缺少 `backend/MomentKeep/config/application-local.yml`，或没激活 `local` profile（日志里应出现 `The following 1 profile is active: local`）。**生产**：`/etc/momentkeep/momentkeep.env` 缺少变量，或 `EnvironmentFile` 权限/属主不对，或启动时忘了加 `--spring.profiles.active=prod` |
| 启动成功但接口报 `Unknown column 'token_version'` | 未执行 `sql/upgrade_2026-09-18.sql`，请先补执行 |
| 日志报 `Access denied for user 'xxx.root'@'你的IP'` | ① TiDB Cloud 口令已变更（优先排查）；② 该公网 IP 未加入 TiDB Cloud 的 IP 访问白名单 |
| 启动即 OOM Kill（`dmesg` 可见） | 降低 `-Xmx`（建议 512m），确认 Swap 已启用 |
| 接口 502 | `systemctl status momentkeep`；再看 `/var/log/nginx/momentkeep.error.log` |
| AI 对话 504 | DeepSeek 响应慢；确认 Nginx `proxy_read_timeout 120s` 与后端 `ai.http.read-timeout-ms` |
| 登录后立刻又跳登录页 | 说明 401：多为 `JWT_SECRET_KEY` 变更导致旧令牌失效（属预期），或令牌版本已被吊销 |
| 小程序请求失败但浏览器正常 | 证书链不完整 / 域名未加入合法域名 / 未备案 |
| 上传头像失败 | 检查 `client_max_body_size`、Spring 的 10MB 限制、图片是否 ≤5MB 且为 jpg/png/webp/gif |
| 所有用户数据混在一起 | 说明运行的是修复前的版本，请确认已部署包含 `getCurrentUserId()` 的构建 |
| `Build failed: isInSSRComponentSetup is not exported` | `@dcloudio/*` 与 `vue` 版本不匹配。本项目已锁定精确版本，但仍需 `npm ci --legacy-peer-deps`（**不加 `--legacy-peer-deps` 会因 peer 冲突直接失败**） |
| `npm ERR! ERESOLVE could not resolve` / `peer ... from the root project` | 项目存在 peer 冲突（`vue` 被钉在 3.4.21，而 `@dcloudio/*` 期望更高版本）。安装/更新依赖一律加 `--legacy-peer-deps` |
| **真机运行日志显示成功，但手机停在「HBuilderX真机运行」欢迎页** | 见 §8.5：先确认 `dist/build/app/app-service.js` 是否存在（缺 `@dcloudio/uni-app-plus` 时最常见），再查基座版本/存储权限 |
| 域名打开是白屏，但接口用 curl 正常 | ① Nginx `root` 指向的目录里没有 `index.html`（构建产物错落到 `dist/` 根）；② 站点被部署到子路径但没设 `vite.config.js` 的 `base` |
| 浏览器控制台报跨域（CORS） | 说明你把页面和接口放在了**两个不同域名**。要么改成同域（推荐），要么把页面域名写入 `CORS_ALLOWED_ORIGINS` 并重启后端 |
| 域名访问返回阿里云的「该网站未备案」提示页 | 大陆节点的 80/443 强制备案，与 Nginx/后端无关，等备案通过即可 |
| 静态资源 404（`assets/xxx.js`） | 产物没传全：`scp -r dist/build/h5/*` 必须带 `-r`，且要包含 `assets/` 与 `static/` 子目录 |
| 首页能打开但登录后立刻退回登录页 | 401：多为 `JWT_SECRET_KEY` 与签发时不一致（换密钥后旧令牌全部失效，属预期），或该账号令牌版本已被登出/改密自增 |
