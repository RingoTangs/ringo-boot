# Redis 验证码存储部署指南

本文说明如何使用 Docker 部署供 `RedisVerificationStore` 使用的 Redis 实例。示例面向 Linux
服务器，默认应用和 Redis 运行在同一台主机。

## 镜像版本

推荐使用 Docker Official Image 的 `redis:7.4.10-alpine`：

- 当前项目的 Redis 集成测试以 Redis 7.4 系列作为兼容基线。
- 部署示例固定到具体补丁版本，避免 `latest` 或浮动标签升级后产生不可预期的变化。
- 升级到新的 7.4 补丁版本前，应运行 `mvn -Predis-it verify` 验证真实 Redis 行为。
- Alpine 镜像体积较小，足以运行本项目使用的 Redis Hash 和 Lua 脚本。

可在 [Redis Docker Official Image](https://hub.docker.com/_/redis/) 查看仍受支持的标签。

## 推荐配置

验证码状态的生命周期很短，但它会记录校验尝试次数。签发限流使用独立的短期 Redis 键。生产环境建议使用以下配置：

| 配置 | 示例值 | 作用 |
| --- | --- | --- |
| `protected-mode` | `yes` | 保留 Redis 的保护模式，减少错误网络配置造成的风险。 |
| `requirepass` | 随机密码 | 要求客户端认证；它与验证码 HMAC secret 不是同一个密钥。 |
| `appendonly` | `yes` | 开启 AOF，Redis 重启后可恢复尚未过期的验证码状态。 |
| `appendfsync` | `everysec` | 在性能和最多约一秒的数据风险之间取得平衡。 |
| `maxmemory` | `256mb` | 限制实例最大内存；应根据验证码流量和 TTL 调整。 |
| `maxmemory-policy` | `noeviction` | 内存不足时让写入显式失败，避免静默淘汰验证码或限流状态。 |

同时需要：

- 将 `/data` 挂载到命名卷，否则删除容器后持久化数据会丢失。
- 配置容器自动重启和健康检查。
- 默认只发布 `127.0.0.1:6379`，不要将 Redis 直接暴露到公网。
- 如果应用运行在其他服务器，应通过私网或 VPN 访问，并通过防火墙只允许应用服务器连接；有跨越不可信网络的需求时还应配置 TLS。

本地临时测试可以不启用 AOF，但生产环境建议保留 AOF，以免 Redis 重启后重置校验尝试次数和签发限流状态。有关持久化策略的取舍，参见
[Redis persistence](https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/)。

## 使用 Docker 创建 Redis

### 1. 创建密码和 Redis 配置文件

密码文件只保存在服务器上，不应提交到源码仓库：

```shell
sudo install -d -m 700 -o "$(id -un)" -g "$(id -gn)" /opt/ringo-redis/config /opt/ringo-redis/secrets
REDIS_PASSWORD="$(openssl rand -base64 32 | tr -d '\n')"
printf '%s' "$REDIS_PASSWORD" > /opt/ringo-redis/secrets/redis-password.txt

printf '%s\n' \
  'protected-mode yes' \
  'port 6379' \
  'dir /data' \
  'appendonly yes' \
  'appendfsync everysec' \
  'maxmemory 256mb' \
  'maxmemory-policy noeviction' \
  "requirepass $REDIS_PASSWORD" \
  > /opt/ringo-redis/config/redis.conf

unset REDIS_PASSWORD
chmod 644 /opt/ringo-redis/config/redis.conf
chmod 600 /opt/ringo-redis/secrets/redis-password.txt
```

`redis.conf` 必须能被容器内的非 root `redis` 用户读取。它所在的宿主机目录权限为 `700`，防止其他
宿主机用户访问其中的密码。容器通过只读挂载读取该文件。

### 2. 创建数据卷并启动容器

```shell
docker volume create ringo-redis-data

docker run -d \
  --name ringo-redis \
  --restart unless-stopped \
  -p 127.0.0.1:6379:6379 \
  --mount type=volume,src=ringo-redis-data,dst=/data \
  --mount type=bind,src=/opt/ringo-redis/config/redis.conf,dst=/usr/local/etc/redis/redis.conf,readonly \
  --mount type=bind,src=/opt/ringo-redis/secrets/redis-password.txt,dst=/run/secrets/redis_password,readonly \
  --health-cmd='REDISCLI_AUTH="$(cat /run/secrets/redis_password)" redis-cli ping | grep -q PONG' \
  --health-interval=10s \
  --health-timeout=3s \
  --health-retries=5 \
  --health-start-period=10s \
  redis:7.4.10-alpine \
  redis-server /usr/local/etc/redis/redis.conf
```

### 3. 验证实例

```shell
docker ps --filter name=ringo-redis
docker logs ringo-redis

docker exec \
  -e REDISCLI_AUTH="$(cat /opt/ringo-redis/secrets/redis-password.txt)" \
  ringo-redis redis-cli ping
```

最后一条命令应返回：

```text
PONG
```

常用管理命令：

```shell
docker stop ringo-redis
docker start ringo-redis
docker restart ringo-redis
docker rm -f ringo-redis
```

`docker rm -f` 会删除容器，但不会删除 `ringo-redis-data` 命名卷。只有确认不再需要数据时才执行：

```shell
docker volume rm ringo-redis-data
```

## 使用 Docker Compose 创建 Redis

### 1. 准备目录、密码和 Redis 配置

```shell
sudo install -d -m 700 -o "$(id -un)" -g "$(id -gn)" /opt/ringo-redis/config /opt/ringo-redis/secrets
cd /opt/ringo-redis
REDIS_PASSWORD="$(openssl rand -base64 32 | tr -d '\n')"
printf '%s' "$REDIS_PASSWORD" > secrets/redis-password.txt

printf '%s\n' \
  'protected-mode yes' \
  'port 6379' \
  'dir /data' \
  'appendonly yes' \
  'appendfsync everysec' \
  'maxmemory 256mb' \
  'maxmemory-policy noeviction' \
  "requirepass $REDIS_PASSWORD" \
  > config/redis.conf

unset REDIS_PASSWORD
chmod 644 config/redis.conf
chmod 600 secrets/redis-password.txt
```

### 2. 创建 `compose.yaml`

```yaml
services:
  redis:
    image: redis:7.4.10-alpine
    container_name: ringo-redis
    restart: unless-stopped
    ports:
      - "127.0.0.1:6379:6379"
    volumes:
      - redis-data:/data
      - ./config/redis.conf:/usr/local/etc/redis/redis.conf:ro
    secrets:
      - redis_password
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    healthcheck:
      test: ["CMD-SHELL", 'REDISCLI_AUTH="$$(cat /run/secrets/redis_password)" redis-cli ping | grep -q PONG']
      interval: 10s
      timeout: 3s
      retries: 5
      start_period: 10s

volumes:
  redis-data:

secrets:
  redis_password:
    file: ./secrets/redis-password.txt
```

健康检查中的 `$$` 会向容器命令传递字面量 `$`，因此密码文件是在容器内部读取的，而不是由 Compose
提前插值。Redis 主进程直接读取只读挂载的配置文件，仍会使用官方镜像的非 root 用户启动。关于 Secret
的详细规则参见 [Docker Compose Secrets](https://docs.docker.com/reference/compose-file/secrets/)。

### 3. 启动并验证

```shell
docker compose config
docker compose up -d
docker compose ps
docker compose logs redis

docker compose exec \
  -e REDISCLI_AUTH="$(cat secrets/redis-password.txt)" \
  redis redis-cli ping
```

停止容器但保留数据卷：

```shell
docker compose down
```

删除容器及数据卷会丢失 Redis 数据，只有确认不再需要时才使用：

```shell
docker compose down --volumes
```

Docker Compose 健康检查的工作方式可参考
[Docker Compose Quickstart](https://docs.docker.com/compose/gettingstarted/)。

## 使用外部 Redis 运行集成测试

`redis-it` Profile 连接显式配置的外部 Redis，不依赖 Docker。测试会写入带随机 subject 的短期验证码
记录，并在正常执行过程中消费或失效这些记录；不会执行 `FLUSHDB` 或扫描删除共享数据。

PowerShell：

```powershell
$env:REDIS_IT_HOST="redis.example.com"
$env:REDIS_IT_PORT="6379"
$env:REDIS_IT_USERNAME="default"
$env:REDIS_IT_PASSWORD="Redis 密码"
$env:REDIS_IT_DATABASE="0"
$env:REDIS_IT_SSL="false"
mvn -Predis-it verify
```

Linux/macOS：

```shell
export REDIS_IT_HOST="redis.example.com"
export REDIS_IT_PORT="6379"
export REDIS_IT_USERNAME="default"
export REDIS_IT_PASSWORD="Redis 密码"
export REDIS_IT_DATABASE="0"
export REDIS_IT_SSL="false"
mvn -Predis-it verify
```

只有 `REDIS_IT_HOST` 是必填项。端口默认 `6379`，database 默认 `0`，用户名和密码可省略，TLS 默认关闭。
显式启用 `redis-it` 后，缺少主机、连接失败或认证失败都会使构建失败。测试启动时会先执行 `PING`，但不会
将认证信息输出到日志。建议使用专用测试实例或专用 database，不要对生产 Redis 运行集成测试。

## 配置 Spring Boot 应用

应用需要引入 `spring-boot-starter-data-redis`，并配置 Redis 连接：

```yaml
spring:
  application:
    name: identity-service
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ${REDIS_PASSWORD}

ringo:
  boot:
    verification:
      enabled: true
      store: redis
```

应用还必须提供唯一的 `VerificationHmacKey` Bean。下面示例从环境变量读取 Base64 密钥：

```java
@Bean
VerificationHmacKey verificationHmacKey(Environment environment) {
    return VerificationHmacKey.fromBase64(
            environment.getRequiredProperty("VERIFICATION_HMAC_KEY"));
}
```

验证码状态使用 Redis Hash，key 格式为
`{applicationName}:verification:v1:{namespace}:{purpose}:{keyDigest}`。应用名称读取 `spring.application.name`，
缺失时应用启动失败。
应用名称必须以字母或数字开头，只能包含字母、数字、点、下划线和连字符。应用名称也参与 key 摘要和验证码
摘要计算，因此不同应用即使业务维度、验证主体和验证码完全相同，也不会共享 Redis 状态。修改应用名称会使
旧名称下尚未过期的验证码不可访问。

签发限流使用 Redis ZSET，key 格式为
`{identity-service:verification:issue-limit}:v2:{ruleId}:{bucketDigest}`。
花括号中的应用及功能级 hash tag 使同一次签发涉及的所有规则 key 位于同一个 Redis Cluster slot，从而可以通过一个
Lua 脚本原子检查和消费额度。ZSET score 是签发时间戳，member 是每次请求生成的随机标识；规则生成的手机号、
邮箱、IP 等额度桶分段只参与 HMAC 摘要，不会以明文写入 key 或 value。

其中 `v1` 是由框架维护的 Redis 存储协议版本，用于隔离未来不兼容的 key、Hash 字段或摘要协议变更，
不是可由应用修改的运行时配置。摘要计算中的 `key:v1` 和 `code:v1` 分别隔离 key 摘要与验证码摘要用途，
同样由框架内部维护。

启动应用前，可以从服务器密码文件加载 Redis 密码：

```shell
export REDIS_PASSWORD="$(cat /opt/ringo-redis/secrets/redis-password.txt)"
```

验证码 HMAC 密钥需要单独生成。推荐使用 OpenSSL 创建 32 字节的密码学安全随机数据，并直接输出 Base64 字符串：

```shell
export VERIFICATION_HMAC_KEY="$(openssl rand -base64 32)"
```

没有 OpenSSL 时，也可以使用 Java 的 `SecureRandom` 生成：

```java
byte[] bytes = new byte[32];
new SecureRandom().nextBytes(bytes);
String encoded = Base64.getEncoder().encodeToString(bytes);
```

Base64 只是把二进制密钥转换成便于配置的文本，不会增加随机性或安全强度。不要使用密码、手机号、UUID 或普通
`Random` 的输出代替密码学安全随机数据。

两个密钥的职责不同：

- `REDIS_PASSWORD` 用于 Redis 客户端认证。
- `VERIFICATION_HMAC_KEY` 用于生成 Redis 验证键和验证码的 HMAC-SHA256 摘要。它必须是 Base64
  编码且解码后至少 32 字节；共享同一 Redis 数据的所有应用实例必须使用相同值，并且应用重启后不能改变。

生产环境应通过部署平台的 Secret Manager 注入应用密钥，不要将真实密码、HMAC secret 或
`secrets/redis-password.txt` 提交到 Git，也不要输出到日志。

更换 HMAC 密钥后，使用旧密钥生成的验证码状态将无法继续读取，签发限流计数也会从新桶重新开始。轮换密钥时，
应先停止签发并等待现有验证码 TTL 和最长限流窗口结束，再让所有应用实例统一切换到新密钥。

## 跨服务器连接

前面的示例仅监听宿主机 `127.0.0.1`。如果应用不在 Redis 所在服务器：

1. 将端口发布地址改为 Redis 服务器的私网地址，例如 `10.0.0.10:6379:6379`。
2. 使用安全组或主机防火墙，仅允许应用服务器的私网 IP 访问 TCP 6379。
3. 不要使用 `0.0.0.0:6379:6379` 将 Redis 暴露到公网。
4. 如果连接会经过不可信网络，使用 VPN 或配置 Redis TLS，密码认证不能替代传输加密。
