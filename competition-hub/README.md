# 学科竞赛资源共享与隐私组队系统前端

Vue 3 + Vite + Element Plus 前端工程，对接同级目录 `competition-hub-backend` 的 Spring Boot API。

## 功能概览

- 竞赛资源库：上传资料、筛选检索、安全下载、热门资源排行。
- 经验博客：Markdown/普通文本编辑，支持本地 `.md/.txt/.docx/.pdf` 导入和博客图片上传。
- 沙龙活动：用户提交沙龙，管理员审核后开放预约，管理员可直接发布。
- 技术组队：发布组队需求、申请入队、队长或管理员审核申请。
- 后台管理：资源、博客、沙龙、组队内容审核，用户管理，超级管理员转让。
- 登录态处理：JWT 鉴权，权限变更或会话失效时自动清理本地缓存。

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
