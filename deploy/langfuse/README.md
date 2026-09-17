# 本地 Langfuse

给 `pmc-agent` 用的自托管 Langfuse（Docker Compose）。Agent 进程仍在宿主机 `8080` 运行，通过 `http://localhost:3000` 上报 trace。

## 启动

```bash
cd deploy/langfuse
cp .env.example .env
docker compose up -d
```

打开 [http://localhost:3000](http://localhost:3000)：

- 邮箱：`dev@example.com`
- 密码：`password1234`

项目密钥（与 `application.yml` 默认值一致）：

- public：`pk-lf-1234567890`
- secret：`sk-lf-1234567890`

## 关掉 tracing

启动 Java 时设 `LANGFUSE_ENABLED=false`。

## 与 MySQL Connector/J 的注意点

引入 `opentelemetry-sdk` 后，MySQL Connector/J 默认会在建连时调用 `GlobalOpenTelemetry.get()`，抢先占住全局实例，导致应用侧 `buildAndRegisterGlobal` 失败（[Quarkus #51456](https://github.com/quarkusio/quarkus/issues/51456) 同根因）。本仓库已在 JDBC URL / Hikari 属性里设置 `openTelemetry=DISABLED`，由应用统一向 Langfuse 导出 Agent 链路。

## 停止

```bash
docker compose down
```

数据在 Docker volume 里。需要清空时：`docker compose down -v`。
