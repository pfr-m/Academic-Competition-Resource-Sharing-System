# 学科竞赛资源共享与隐私组队系统后端

Spring Boot 3 后端 API，和前端 `competition-hub` 是同级目录。当前版本使用 MySQL + MyBatis-Plus 持久化业务数据，并使用 Redis 承载缓存、热门榜和预约相关状态。

## 技术栈

- Spring Boot 3
- Spring Security + JWT + RBAC
- MyBatis-Plus
- MySQL 8
- Redis
- Maven
- Apache POI / PDFBox，用于博客本地文件导入

## 本地运行

准备 MySQL 数据库：

```sql
CREATE DATABASE competition_hub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

按需执行 `src/main/resources/schema/` 下的建表和升级脚本。

设置本地环境变量：

```bash
set DB_USERNAME=root
set DB_PASSWORD=your_mysql_password
set JWT_SECRET=please-change-to-a-long-random-secret-at-least-32-bytes
set MAIL_USERNAME=your_qq_mail@qq.com
set MAIL_PASSWORD=your_smtp_authorization_code
```

启动服务：

```bash
.\mvnw.cmd spring-boot:run
```

默认 API 地址：

```text
http://127.0.0.1:8099/api
```

健康检查：

```text
GET /api/health
```

## 配置说明

敏感信息不要写入 `application.properties` 或提交到 Git。当前配置通过环境变量读取：

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

如需本地私有配置，可创建不提交的 `application-local.properties`，并通过 Spring profile 启用。

## 主要模块

- 认证鉴权：JWT 登录、Spring Security 权限拦截、管理员/超级管理员角色控制。
- 用户管理：用户资料、隐私设置、管理员新增、超级管理员转让。
- 资源管理：资源上传、替换文件、安全下载、下载计数、热门资源缓存。
- 博客管理：发布、编辑、删除、评论、二级回复、本地文件导入、图片上传与清理。
- 沙龙活动：发布、审核、删除、预约、取消预约、预约数据导出。
- 组队大厅：发布组队需求、申请入队、队长或管理员审核申请。
- 审核后台：资源、博客、沙龙、组队需求统一审核。

## 测试

```bash
.\mvnw.cmd test
```

当前测试覆盖了内容提交流程和管理员新增逻辑，后续建议继续补充 JWT 解析、权限拦截、预约防重复、评论层级限制等用例。
