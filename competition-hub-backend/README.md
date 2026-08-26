# 学科竞赛资源共享与隐私组队系统后端

Spring Boot 3 后端 API，和前端 `competition-hub` 是同级目录。当前版本使用 MySQL + MyBatis-Plus 持久化业务数据，并使用 Redis 承载缓存、热门榜和预约相关状态。

## 技术栈

- Spring Boot 3.5（Java 21）
- Spring Security + JWT + RBAC
- MyBatis-Plus
- MySQL 8
- Redis
- Maven
- Apache POI / PDFBox，用于博客本地文件导入

## 项目目录结构

```
competition-hub-backend/
├── src/
│   ├── main/
│   │   ├── java/com/competition/hub/
│   │   │   ├── auth/          # JWT 认证：JwtService（签发/验签）、JwtAuthenticationFilter（请求拦截）、AuthPrincipal（认证主体）
│   │   │   ├── config/        # 配置：SecurityConfig（安全规则）、WebConfig（CORS）、RedisConfig、MybatisPlusConfig、UserDetailsConfig
│   │   │   ├── controller/    # REST API 层（15 个控制器，见下方接口清单）
│   │   │   ├── entity/        # 数据实体：User、Blog、Resource、Salon、Team、Review 等 12 个
│   │   │   ├── mapper/        # MyBatis-Plus Mapper 接口
│   │   │   ├── service/       # 业务层：接口 + impl 实现
│   │   │   ├── store/         # 领域模型：DomainModels（Role、ReviewStatus 等枚举）
│   │   │   └── task/          # 定时任务：BlogRedisFlushTask（博客浏览量缓存刷新）
│   │   └── resources/
│   │       ├── application.properties   # 应用配置
│   │       ├── schema/                  # 建表与升级 SQL 脚本
│   │       ├── static/                  # 静态资源（含上传文件）
│   │       └── templates/               # 模板（当前未使用）
│   └── test/                 # 单元/集成测试
└── uploads/                 # 用户上传文件存储目录
```

## 本地运行

准备 MySQL 数据库：

```sql
CREATE DATABASE competition_hub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

按需执行 `src/main/resources/schema/` 下的建表和升级脚本（`init.sql` 为基础建表，其余为增量升级）。

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

前端（`competition-hub`）开发时通过 Vite 将 `/api` 代理到本地址，先启动本后端再启动前端。

## 配置说明

敏感信息不要写入 `application.properties` 或提交到 Git。当前配置通过环境变量读取：

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

如需本地私有配置，可创建不提交的 `application-local.properties`，并通过 Spring profile 启用。

## 演示账号

数据库初始化（`schema/init.sql`）内置以下账号：

| 角色 | 用户名 | 密码 | 说明 |
| --- | --- | --- | --- |
| 普通学生 | `student` | `123456` | 浏览资源、写博客、组队 |
| 学长学姐 | `mentor` | `123456` | 额外可发布沙龙 |
| 管理员 | `admin` | `Admin123` | 内容审核、用户管理 |
| 超级管理员 | `PFR` | `PFR123456` | 管理员授权、超级管理员转让 |

> 密码以 BCrypt 哈希存储在 `init.sql` 中，请在生产环境修改。

## API 接口清单

所有接口前缀为 `/api`。除标注公开的外，均需携带登录凭证（`Authorization: Bearer <token>` 或 httpOnly Cookie `access_token`）。

### 认证 `/auth`

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/auth/login` | 用户名密码登录，签发 JWT 并下发 httpOnly Cookie | 公开 |
| POST | `/auth/login/phone` | 手机号 + 验证码登录（演示验证码固定 `123456`） | 公开 |
| POST | `/auth/register` | 邮箱验证码注册并自动登录 | 公开 |
| POST | `/auth/email/code` | 发送邮箱验证码 | 公开 |
| PUT | `/auth/reset-password` | 邮箱验证码重置密码 | 公开 |
| POST | `/auth/logout` | 退出登录，清除 Cookie | 登录 |
| POST | `/auth/first-login-acknowledged` | 首次登录提示确认 | 登录 |

### 用户 `/users`、`/user`

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/users/me` | 当前登录用户信息 | 登录 |
| PATCH | `/users/me/privacy` | 修改隐私可见性设置 | 登录 |
| GET | `/users/{id}/profile` | 查看他人公开主页 | 登录 |
| GET | `/users/page` | 用户分页（管理用） | ADMIN / SUPER_ADMIN |
| DELETE | `/users/{id}` | 删除用户 | ADMIN / SUPER_ADMIN |
| DELETE | `/users/batch` | 批量删除用户 | ADMIN / SUPER_ADMIN |
| PUT | `/user/password` | 修改登录密码 | 登录 |

### 概览 `/overview`

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/overview` | 首页统计（资源/博客/沙龙/队伍/用户数量） | 登录 |

### 资源 `/resources`

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/resources` | 上传竞赛资源（提交审核） | 登录 |
| PUT | `/resources` | 上传新资源文件 | 登录 |
| PUT | `/resources/{id}` | 编辑资源 / 替换文件 | 上传者或管理员 |
| GET | `/resources/hot-ranking` | 热门资源排行（Redis 缓存） | 登录 |
| POST | `/resources/{id}/download` | 获取下载链接（计数 + 短时有效） | 登录 |
| GET | `/resources/{id}/file` | 下载文件内容 | 登录 |
| DELETE | `/resources/{id}` | 删除资源 | 上传者或管理员 |
| POST | `/resources/batch-delete` | 批量删除 | 管理员 |
| DELETE | `/resources/batch` | 批量删除 | 管理员 |

### 博客 `/blogs`、`/blog`

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/blogs` | 博客列表（支持关键词/竞赛标签筛选） | 登录 |
| POST | `/blogs` | 发布博客（支持匿名、Markdown） | 登录 |
| GET | `/blogs/{id}` | 博客详情（Redis 缓存 + 浏览量计数） | 登录 |
| PUT | `/blogs/{id}` | 编辑博客 | 作者或管理员 |
| DELETE | `/blogs/{id}` | 删除博客 | 作者或管理员 |
| POST | `/blogs/upload-md` | 上传 Markdown/Word/PDF 文件解析为博客草稿 | 登录 |
| GET | `/blogs/creator-center` | 创作中心统计 | 登录 |
| POST | `/blogs/{id}/actions` | 点赞 / 收藏 / 取消 | 登录 |
| POST | `/blogs/{id}/follow-author` | 关注作者 | 登录 |
| POST | `/blogs/{id}/comments` | 发表评论 / 二级回复 | 登录 |
| DELETE | `/blogs/{blogId}/comments/{commentId}` | 删除评论 | 作者或管理员 |
| GET | `/blogs/images/{fileName}` | 博客图片访问 | 公开 |
| POST | `/blog/upload-md` | Markdown 文件导入（兼容路径） | 登录 |

### 沙龙 `/salons`、`/salon/appointments`

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/salons` | 发布沙龙（管理员直接发布，学生提交审核） | 登录 |
| GET | `/salons` | 沙龙列表 | 登录 |
| PUT | `/salons/{id}` | 编辑沙龙 | 作者或管理员 |
| DELETE | `/salons/{id}` | 删除沙龙 | 作者或管理员 |
| POST | `/salons/{id}/book` | 预约沙龙（防重复 + 生成凭证） | 登录 |
| GET | `/salons/me/bookings` | 我的预约列表 | 登录 |
| GET | `/salon/appointments/export` | 导出预约数据（Excel） | ADMIN / SUPER_ADMIN |

### 组队 `/teams`

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/teams` | 组队大厅列表（筛选/搜索） | 登录 |
| POST | `/teams` | 发布组队需求 | 登录 |
| PUT | `/teams/{id}` | 编辑组队需求 | 队长或管理员 |
| DELETE | `/teams/{id}` | 删除组队需求 | 队长或管理员 |
| POST | `/teams/{id}/apply` | 申请入队 | 登录 |
| POST | `/teams/{teamId}/applications/{applicationId}/approve` | 队长审批通过（解锁联系方式） | 队长 |
| POST | `/teams/applications/batch-approve` | 批量通过 | 队长/管理员 |
| POST | `/teams/applications/batch-reject` | 批量拒绝 | 队长/管理员 |
| DELETE | `/teams/applications/batch` | 批量删除申请 | 队长/管理员 |
| GET | `/teams/applications/manageable` | 我可处理的申请 | 队长/管理员 |
| GET | `/teams/members/manageable` | 我可管理的成员 | 队长/管理员 |
| DELETE | `/teams/{teamId}/members/{userId}` | 移出队伍 | 队长 |
| GET | `/teams/matchmaking/candidates` | 红娘撮合候选人（脱敏） | 登录 |

### 管理后台 `/admin`

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/admin/reviews` | 审核队列（分页） | ADMIN / SUPER_ADMIN |
| PATCH | `/admin/reviews/{id}` | 审核通过/驳回（联动内容状态） | ADMIN / SUPER_ADMIN |
| PATCH | `/admin/reviews/batch` | 批量审核 | ADMIN / SUPER_ADMIN |
| POST | `/admin/reviews/batch-delete` | 批量删除审核记录 | ADMIN / SUPER_ADMIN |
| GET | `/admin/matchmaking` | 脱敏撮合池 | ADMIN / SUPER_ADMIN |
| GET | `/admin/salon-appointments` | 全部预约记录 | ADMIN / SUPER_ADMIN |
| DELETE | `/admin/salon-appointments/{id}` | 删除预约 | ADMIN / SUPER_ADMIN |
| PATCH | `/admin/salon-appointments/{id}/cancel` | 取消预约 | ADMIN / SUPER_ADMIN |
| GET | `/admin/users` | 管理员列表 | SUPER_ADMIN |
| POST | `/admin/users` | 新增管理员 | SUPER_ADMIN |
| PUT | `/admin/users/{id}` | 编辑管理员信息 | SUPER_ADMIN |
| DELETE | `/admin/users/{id}` | 取消管理员权限 | SUPER_ADMIN |
| GET | `/admin/super/users` | 全部用户（超级管理） | SUPER_ADMIN |
| POST | `/admin/super/users/{userId}/promote-admin` | 提升为管理员 | SUPER_ADMIN |
| POST | `/admin/super/users/{userId}/demote-admin` | 降级为普通用户 | SUPER_ADMIN |
| POST | `/admin/super/users/{userId}/transfer` | 转让超级管理员 | SUPER_ADMIN |

### 健康检查 `/health`

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/health` | 服务健康状态 | 公开 |

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