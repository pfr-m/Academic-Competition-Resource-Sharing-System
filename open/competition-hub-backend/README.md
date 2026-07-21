# Competition Hub Spring Boot Backend

Spring Boot 3 后端 API，和前端 `competition-hub` 是同级目录。

## 运行

Windows:

```bash
.\mvnw.cmd spring-boot:run
```

默认 API 地址：

```text
http://127.0.0.1:3002/api
```

健康检查：

```text
GET /api/health
```

## 演示账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 普通学生 | student | 123456 |
| 学长学姐 | mentor | 123456 |
| 管理员 | admin | Admin123 |
| 超级管理员 | PFR | PFR123456 |

登录：

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "PFR",
  "password": "Admin123"
}
```

其他接口带：

```http
Authorization: Bearer <accessToken>
```

## 已实现模块

- JWT 登录鉴权与 Spring Security RBAC。
- 用户隐私设置与联系方式脱敏。
- 竞赛资源检索、上传审核、下载计数、短时下载链接模拟。
- Markdown 博客发布、匿名、评论和二级回复。
- 沙龙发布、预约、防重复预约、凭证生成。
- 技术红娘组队大厅、申请入队、队长同意后解锁联系方式。
- 管理员审核队列和脱敏撮合池。

当前版本使用内存数据，适合演示。后续可替换为 MySQL 8.0 + MyBatis-Plus，并加入 Redis 和 OSS/COS。
