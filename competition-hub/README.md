# 学科竞赛资源共享与隐私组队系统前端

Vue 3 + Vite + Element Plus 前端工程，对接同级目录 `competition-hub-backend` 的 Spring Boot API。

## 项目目录结构

```
competition-hub/
├── public/
│   ├── favicon.svg         # 站点图标
│   ├── icons.svg           # 图标雪碧图
│   ├── system-map.png      # 系统架构图
│   └── legacy/             # 旧版前端编译产物（构建后产物，勿直接编辑）
├── src/
│   ├── App.vue             # 主应用（单文件，包含全部页面与组件）
│   ├── main.ts             # 应用入口
│   ├── env.d.ts            # 类型声明
│   ├── operationRefresh.ts # 数据刷新辅助
│   └── style.css           # 全局样式
├── index.html              # HTML 入口
├── package.json
├── tsconfig.json
└── vite.config.ts          # 开发代理：/api → http://127.0.0.1:8099
```

> 说明：主界面为单页应用，全部页面视图（仪表盘、资源库、博客、沙龙、组队、后台管理等）集中在 `App.vue` 中，按路由切换渲染。

## 功能概览

- 竞赛资源库：上传资料、筛选检索、安全下载、热门资源排行。
- 经验博客：Markdown/普通文本编辑，支持本地 `.md/.txt/.docx/.pdf` 导入和博客图片上传。
- 沙龙活动：用户提交沙龙，管理员审核后开放预约，管理员可直接发布。
- 技术组队：发布组队需求、申请入队、队长或管理员审核申请。
- 后台管理：资源、博客、沙龙、组队内容审核，用户管理，超级管理员转让。
- 登录态处理：JWT 鉴权，权限变更或会话失效时自动清理本地缓存。

## 模块与后端接口对应

| 前端视图 | 对应后端模块 | 主要 API |
| --- | --- | --- |
| 仪表盘 | 概览 | `GET /overview` |
| 资源库 | 资源管理 | `/resources/*` |
| 博客 / 创作中心 | 博客管理 | `/blogs/*`、`/blog/*` |
| 沙龙 | 沙龙活动 | `/salons/*`、`/salon/appointments/*` |
| 组队大厅 | 组队 | `/teams/*` |
| 个人中心 | 用户隐私 | `/users/me`、`/users/me/privacy` |
| 后台管理 | 审核 / 用户 / 预约 | `/admin/*` |
| 管理员权限管理 | 超级管理员 | `/admin/users/*`、`/admin/super/*` |

详细接口参数见后端 README（`competition-hub-backend/README.md`）。

## 技术栈

- Vue 3
- TypeScript
- Vite
- Element Plus
- MarkdownIt + highlight.js

## 本地运行

```bash
npm install
npm run dev
```

默认地址：

```text
http://127.0.0.1:5173/
```

本地开发时 Vite 会把 `/api` 代理到：

```text
http://127.0.0.1:8099
```

因此需要先启动后端服务。

## 构建

```bash
npm run build
```

构建产物输出到 `dist/`，线上由 Nginx 托管，并将 `/api` 反向代理到 Spring Boot 后端。