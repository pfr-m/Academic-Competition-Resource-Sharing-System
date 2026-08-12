<template>
  <section v-if="!isLoggedIn" class="login-screen">
    <div class="login-panel">
      <div class="brand-mark login-mark">
        <el-icon><Trophy /></el-icon>
      </div>
      <h1>学科竞赛隐私组队系统</h1>
      <el-tabs v-model="authTab" class="auth-tabs">
        <el-tab-pane label="账号登录" name="password">
          <el-form label-position="top" @submit.prevent>
            <el-form-item label="账号">
              <el-input v-model="loginForm.username" placeholder="请输入学号/工号" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入登录密码" @keyup.enter="loginAndLoad" />
              <div class="form-tip">密码为 6-18 位字母与数字组合</div>
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="loginAndLoad">登录系统</el-button>
            <div class="auth-links">
              <el-button text type="primary" @click="resetPasswordVisible = true">忘记密码？</el-button>
            </div>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="邮箱注册" name="register">
          <el-form label-position="top" @submit.prevent>
            <el-form-item label="学号 / 登录账号">
              <el-input v-model="registerForm.studentNo" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="registerForm.nickname" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="registerForm.phone" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="registerForm.email">
                <template #append>
                  <el-button :loading="codeSending" @click="sendEmailCode">发送验证码</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="邮箱验证码">
              <el-input v-model="registerForm.code" />
              <div class="form-tip code-tip">验证码已发送至您的邮箱，有效时间为 5 分钟，请勿泄露。</div>
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="registerForm.password" type="password" show-password @keyup.enter="registerByEmail" />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="registerByEmail">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <el-dialog v-model="firstLoginDialogVisible" title="欢迎，超级管理员" width="520px" :close-on-click-modal="false" :close-on-press-escape="false">
        <el-alert title="您是平台的超级管理员" type="warning" :closable="false" show-icon />
        <p>您拥有以下额外权限：</p>
        <ul style="line-height: 2;">
          <li><strong>设置管理员</strong> — 将任意用户提升为平台管理员</li>
          <li><strong>取消管理员</strong> — 移除任意管理员身份</li>
          <li><strong>转让超级管理员</strong> — 将超级管理员权限转让给其他用户，转让后您变为普通管理员</li>
        </ul>
        <p>首次登录提示关闭后可在后台再次查看。</p>
        <template #footer>
          <el-button type="primary" @click="acknowledgeFirstLogin">我知道了</el-button>
        </template>
      </el-dialog>
      <el-dialog v-model="resetPasswordVisible" title="找回密码" width="460px">
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="注册邮箱">
            <el-input v-model="resetPasswordForm.email" placeholder="请输入注册时绑定的邮箱">
              <template #append>
                <el-button :loading="resetCodeSending" @click="sendResetPasswordCode">获取验证码</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="邮箱验证码">
            <el-input v-model="resetPasswordForm.code" maxlength="6" placeholder="请输入 6 位验证码" />
            <div class="form-tip code-tip">验证码已发送至您的邮箱，有效时间为 5 分钟，请勿泄露。</div>
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="resetPasswordForm.newPassword" type="password" show-password placeholder="6-18 位字母与数字组合" @keyup.enter="resetPassword" />
            <div class="form-tip">密码为 6-18 位字母与数字组合。</div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="resetPasswordVisible = false">取消</el-button>
          <el-button type="primary" :loading="resettingPassword" @click="resetPassword">确认修改</el-button>
        </template>
      </el-dialog>
    </div>
  </section>

  <el-container v-else class="app-shell">
    <el-aside width="248px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">
          <el-icon><Trophy /></el-icon>
        </div>
        <div>
          <strong>竞赛隐私组队系统</strong>
          <span>资源 · 经验 · 沙龙 · 红娘</span>
        </div>
      </div>

      <el-menu :default-active="activeView" class="nav-menu" @select="navigateTo">
        <el-menu-item index="dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>系统概览</span>
        </el-menu-item>
        <el-menu-item index="resources">
          <el-icon><FolderOpened /></el-icon>
          <span>竞赛资源库</span>
        </el-menu-item>
        <el-menu-item index="blog">
          <el-icon><EditPen /></el-icon>
          <span>经验博客</span>
        </el-menu-item>
        <el-menu-item index="salon">
          <el-icon><Calendar /></el-icon>
          <span>沙龙预约</span>
        </el-menu-item>
        <el-menu-item index="teams">
          <el-icon><Connection /></el-icon>
          <span>技术红娘组队</span>
        </el-menu-item>
        <el-menu-item index="profile">
          <el-icon><Lock /></el-icon>
          <span>个人隐私中心</span>
        </el-menu-item>
        <el-menu-item v-if="isContentAdmin" index="admin">
          <el-icon><Setting /></el-icon>
          <span>审核与管理后台</span>
        </el-menu-item>
        <el-menu-item v-if="isContentAdmin" index="userManagement">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item v-if="isSuperAdmin" index="superAdmin">
          <el-icon><UserFilled /></el-icon>
          <span>管理员权限管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>学科竞赛服务平台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ viewTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
          <div class="title-row">
            <el-button class="back-button" :icon="ArrowLeft" text @click="goBack">返回上一页</el-button>
            <h1>{{ viewTitle }}</h1>
          </div>
        </div>
        <el-dropdown trigger="click" popper-class="user-dropdown-popper" @command="handleUserCommand">
          <button class="user-chip" type="button" aria-label="打开用户菜单">
            <el-avatar :size="42">{{ currentUser.avatar }}</el-avatar>
            <div>
              <strong>{{ currentUser.name }}</strong>
              <span>{{ currentUser.roleLabel }}</span>
            </div>
            <el-icon class="user-chip-arrow"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人隐私中心</el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main>
        <section v-if="activeView === 'dashboard'" class="view">
          <div class="hero-band">
            <div class="hero-copy">
              <el-tag effect="dark" type="success">RBAC + 隐私优先</el-tag>
              <h2>已经将资源分享、经验博客、沙龙预约、隐私组队放到同一个系统中</h2>
              <p>普通学生可以浏览资料、写博客和组队；学长学姐可以发布沙龙经验；管理员审核内容、权限管理和人工撮合。</p>
              <div class="hero-actions">
                <el-button type="primary" :icon="Download" @click="navigateTo('resources')">进入资源库</el-button>
                <el-button :icon="ChatLineRound" @click="navigateTo('teams')">发布组队需求</el-button>
              </div>
            </div>
            <div class="hero-metrics">
              <div v-for="metric in metrics" :key="metric.label" class="metric">
                <strong>{{ metric.value }}</strong>
                <span>{{ metric.label }}</span>
              </div>
            </div>
          </div>

          <div class="section-grid">
            <article v-for="item in modules" :key="item.title" class="module-card clickable" @click="navigateTo(item.target)">
              <el-icon :class="item.color"><component :is="item.icon" /></el-icon>
              <h3>{{ item.title }}</h3>
              <p>{{ item.desc }}</p>
              <el-tag size="small">{{ item.badge }}</el-tag>
            </article>
          </div>

          <div class="two-column">
            <section class="panel">
              <div class="panel-head">
                <h3>近期活动日程</h3>
                <el-button text type="primary" @click="navigateTo('salon')">查看全部</el-button>
              </div>
              <el-timeline>
                <el-timeline-item v-for="event in salons" :key="event.id" :timestamp="`${event.date} · ${event.place}`" placement="top">
                  <strong>{{ event.title }}</strong>
                  <p>{{ event.speaker }} · 剩余 {{ event.capacity - event.booked }} / {{ event.capacity }} 名额</p>
                </el-timeline-item>
              </el-timeline>
            </section>
            <section class="advantage-panel">
              <div class="advantage-copy">
                <el-tag type="success">系统核心优势</el-tag>
                <h3>竞赛资源、经验分享、隐私组队、沙龙活动统一管理</h3>
                <p>围绕真实竞赛场景设计，融合资源上传、经验传授、线下活动和隐私匹配统一到一个服务平台。</p>
              </div>
              <div class="advantage-grid">
                <div v-for="item in advantages" :key="item.title" class="advantage-item">
                  <el-icon><component :is="item.icon" /></el-icon>
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.desc }}</span>
                </div>
              </div>
            </section>
          </div>
        </section>

        <section v-if="activeView === 'resources'" class="view">
          <div class="toolbar">
            <el-input v-model="resourceKeyword" placeholder="搜索赛事名称、资料类型、关键词..." clearable>
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="resourceContest" placeholder="竞赛名称" clearable>
              <el-option v-for="name in contests" :key="name" :label="name" :value="name" />
            </el-select>
            <el-select v-model="resourceType" placeholder="资料类型" clearable>
              <el-option v-for="type in resourceTypes" :key="type" :label="type" :value="type" />
            </el-select>
            <el-button type="primary" :icon="Upload" @click="openResourceDialog">{{ isContentAdmin ? '发布资源' : '上传资源' }}</el-button>
            <el-button v-if="isContentAdmin" type="danger" plain :disabled="selectedResourceIds.length === 0" @click="deleteSelectedResources">批量删除</el-button>
          </div>

          <section class="hot-ranking" v-if="hotResources.length > 0">
            <div class="panel-head">
              <h3>热门资料推荐榜</h3>
              <el-tag type="danger">Redis ZSet Top 5</el-tag>
            </div>
            <div class="hot-list">
              <button v-for="(item, index) in hotResources" :key="item.id" class="hot-item" @click="resourceKeyword = item.title">
                <strong>{{ index + 1 }}</strong>
                <span>{{ item.title }}</span>
                <em>{{ item.downloads }} 次</em>
              </button>
            </div>
          </section>

          <div class="resource-grid">
            <article v-for="resource in filteredResources" :key="resource.id" class="resource-card">
              <el-checkbox v-if="isContentAdmin" v-model="selectedResourceIds" class="card-checkbox" :label="resource.id" />
              <div class="card-topline">
                <el-tag>{{ resource.contest }}</el-tag>
                <el-tag type="info">{{ resource.type }}</el-tag>
              </div>
              <h3>{{ resource.title }}</h3>
              <p>{{ resource.summary }}</p>
              <div class="meta-row">
                <span><el-icon><View /></el-icon>{{ resource.downloads }} 次下载</span>
                <span><el-icon><User /></el-icon>{{ resource.uploader }}</span>
              </div>
              <div class="card-actions">
                <el-button type="primary" plain :icon="Download" :loading="isActionPending(actionKey('resource-download', resource.id))" :disabled="isActionPending(actionKey('resource-download', resource.id))" @click="downloadResource(resource)">
                  {{ isActionPending(actionKey('resource-download', resource.id)) ? '下载中' : '安全下载' }}
                </el-button>
                <template v-if="canManageResource(resource)">
                  <el-button plain :icon="EditPen" :disabled="isActionPending(actionKey('resource-delete', resource.id))" @click="openResourceEditor(resource)">编辑资源</el-button>
                  <el-button type="danger" plain :loading="isActionPending(actionKey('resource-delete', resource.id))" :disabled="isActionPending(actionKey('resource-delete', resource.id))" @click="deleteResource(resource)">删除资源</el-button>
                </template>
              </div>
            </article>
          </div>
          <el-pagination class="pager" background layout="prev, pager, next, sizes, total" :current-page="resourcePage.current" :page-size="resourcePage.size" :page-sizes="[4, 8, 12, 20]" :total="resourcePage.total" @current-change="changeResourcePage" @size-change="changeResourceSize" />

          <el-dialog v-model="resourceEditorVisible" title="编辑资源" width="520px">
            <el-form label-position="top">
              <el-form-item label="资料名称">
                <el-input v-model="resourceEditor.title" />
              </el-form-item>
              <el-form-item label="竞赛名称">
                <el-input v-model="resourceEditor.contest" />
              </el-form-item>
              <el-form-item label="资料类型">
                <el-input v-model="resourceEditor.type" />
              </el-form-item>
              <el-form-item label="资料简介">
                <el-input v-model="resourceEditor.summary" type="textarea" :rows="4" />
              </el-form-item>
              <el-form-item label="替换文件">
                <input class="native-file-input" type="file" @change="handleResourceEditFileChange" />
                <div class="form-tip">{{ resourceEditFile ? resourceEditFile.name : '不选择则保留原文件' }}</div>
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="resourceEditorVisible = false">取消</el-button>
              <el-button type="primary" :loading="isActionPending('resource-edit')" :disabled="isActionPending('resource-edit')" @click="saveResourceEdit">保存</el-button>
            </template>
          </el-dialog>
        </section>

        <section v-if="activeView === 'blog'" class="view">
          <div class="creator-stats">
            <div class="creator-card">
              <span>作品总获赞</span>
              <strong>{{ creatorStats.totalLikes }}</strong>
            </div>
            <div class="creator-card">
              <span>作品总收藏</span>
              <strong>{{ creatorStats.totalCollects }}</strong>
            </div>
            <div class="creator-card">
              <span>关注粉丝</span>
              <strong>{{ creatorStats.followers }}</strong>
            </div>
            <div class="creator-card">
              <span>发布作品</span>
              <strong>{{ creatorStats.works }}</strong>
            </div>
          </div>
          <div class="toolbar">
            <el-input v-model="blogKeyword" :prefix-icon="Search" placeholder="搜索标题、正文或标签..." clearable />
            <el-select v-model="blogContest" placeholder="竞赛标签" clearable>
              <el-option v-for="contest in contests" :key="contest" :label="contest" :value="contest" />
              <el-option label="经验分享" value="经验分享" />
            </el-select>
            <el-select v-model="blogVisibility" placeholder="可见性" clearable>
              <el-option label="公开广场" value="PUBLIC" />
              <el-option label="仅自己可见" value="PRIVATE" />
            </el-select>
            <el-button type="primary" :icon="EditPen" @click="openBlogEditor()">发布博客</el-button>
          </div>

          <div class="blog-list">
            <article v-for="post in blogs" :key="post.id" class="blog-card clickable-blog-card" tabindex="0" role="button" @click="goToBlogDetail(post)" @keydown.enter.prevent="goToBlogDetail(post)">
              <div class="blog-head">
                <div>
                  <h3>{{ post.title }}</h3>
                  <div class="author-line">
                    <span>{{ post.anonymous ? post.alias : post.author }}</span>
                    <el-tag type="success" size="small">{{ post.contestTag }}</el-tag>
                    <el-tag v-if="post.cert" type="warning" size="small">{{ post.cert }}</el-tag>
                    <el-tag v-if="post.visibility === 'private'" type="info" size="small">个人笔记</el-tag>
                    <span class="muted">{{ post.createdAt }}</span>
                  </div>
                </div>
                <div class="card-actions">
                  <el-button @click.stop="goToBlogDetail(post)">查看详情</el-button>
                  <el-button v-if="canEditBlog(post)" type="primary" plain @click.stop="openBlogEditor(post)">编辑</el-button>
                  <el-button v-if="canEditBlog(post)" type="danger" plain @click.stop="deleteBlog(post)">删除</el-button>
                </div>
              </div>
              <p class="blog-summary">{{ blogSummary(post.content) }}</p>
              <div v-if="hasBlogSocialStats(post)" class="blog-card-stats">
                <span v-if="post.social.likes > 0">点赞 {{ post.social.likes }}</span>
                <span v-if="post.social.collects > 0">收藏 {{ post.social.collects }}</span>
              </div>
            </article>
          </div>
          <el-pagination class="pager" layout="total, sizes, prev, pager, next" :page-sizes="[4, 6, 8, 12]" :current-page="blogPage.current" :page-size="blogPage.size" :total="blogPage.total" @current-change="changeBlogPage" @size-change="changeBlogSize" />
        </section>

        <section v-if="activeView === 'blogEditor'" class="view">
          <el-tabs v-model="blogEditorTab" class="blog-editor-tabs">
            <el-tab-pane label="在线撰写" name="write">
              <div class="toolbar">
                <el-radio-group v-model="blogEditorMode">
                  <el-radio-button label="markdown">Markdown</el-radio-button>
                  <el-radio-button label="text">普通文本（Word/PDF）</el-radio-button>
                </el-radio-group>
                <el-button @click="goBack">取消</el-button>
                <el-button type="primary" :icon="EditPen" :loading="isActionPending('blog-publish')" :disabled="isActionPending('blog-publish')" @click="publishBlog">{{ editingBlogId ? '保存修改' : '发布博客' }}</el-button>
              </div>
              <div class="split-layout blog-editor-layout">
                <section class="panel editor-panel">
                  <div class="panel-head">
                    <h3>{{ editingBlogId ? '编辑经验' : '发布经验 / 个人笔记' }}</h3>
                    <el-tag type="success">{{ blogEditorMode === 'markdown' ? 'Markdown' : '普通文本 / Word / PDF' }}</el-tag>
                  </div>
                  <el-form label-position="top">
                    <el-form-item label="标题">
                      <el-input v-model="draftBlog.title" />
                    </el-form-item>
                    <el-form-item label="竞赛标签">
                      <el-select v-model="draftBlog.contestTag" filterable allow-create default-first-option>
                        <el-option v-for="contest in contests" :key="contest" :label="contest" :value="contest" />
                        <el-option label="经验分享" value="经验分享" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="内容">
                      <el-input v-model="draftBlog.content" type="textarea" :rows="20" />
                    </el-form-item>
                    <el-form-item v-if="blogEditorMode === 'markdown'" label="博客插图">
                    <el-input v-model="draftBlog.imageUrl" placeholder="输入图片 URL，也可不填只显示文本">
                      <template #append>
                        <el-button @click="insertBlogImage">插入图片</el-button>
                      </template>
                    </el-input>
                    <input ref="blogImageInputRef" class="hidden-file-input" type="file" accept="image/*" @change="handleBlogImageUpload" />
                    <el-button class="mt-2" plain :loading="blogImageUploading" @click="triggerBlogImageInput">上传本地图片</el-button>
                  </el-form-item>
                    <div class="form-inline">
                      <el-radio-group v-model="draftBlog.visibility">
                        <el-radio-button label="public">公开广场</el-radio-button>
                        <el-radio-button label="private">仅自己可见</el-radio-button>
                      </el-radio-group>
                      <el-checkbox v-model="draftBlog.anonymous">使用匿名/笔名</el-checkbox>
                      <el-checkbox v-model="draftBlog.commentsOpen">开启评论</el-checkbox>
                    </div>
                  </el-form>
                </section>
                <section class="panel">
                  <div class="panel-head">
                    <h3>实时预览</h3>
                    <span>{{ blogEditorMode === 'markdown' ? '发布前请先预览格式' : '普通文本自动换行展示' }}</span>
                  </div>
                  <div class="markdown-preview editor-preview" v-html="renderedDraft"></div>
                </section>
              </div>
            </el-tab-pane>
            <el-tab-pane label="导入本地文件" name="import">
              <div class="md-upload-container">
                <div class="md-upload-card" @click="triggerMdFileInput" @dragover.prevent @drop.prevent="handleMdDrop">
                  <el-icon class="md-upload-icon"><Upload /></el-icon>
                  <h3>点击或拖拽博客文件至此</h3>
                  <p>支持 .md、.txt、.docx、.pdf；Markdown 关联图片请和文章文件一起选择</p>
                  <el-button type="primary" plain @click.stop="triggerMdFileInput">选择文件</el-button>
                  <input ref="mdFileInputRef" type="file" accept=".md,.txt,.docx,.pdf,image/*" multiple style="display:none" @change="handleMdFileChange" />
                </div>
                <div v-if="mdUploading" class="md-uploading">
                  <el-icon class="is-loading"><Plus /></el-icon>
                  <span>正在解析文件...</span>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </section>

        <section v-if="activeView === 'blogDetail'" class="view">
          <article v-if="activeBlog" class="panel blog-detail">
            <div class="blog-head">
              <div>
                <h2>{{ activeBlog.title }}</h2>
                <div class="author-line">
                  <span>{{ activeBlog.anonymous ? activeBlog.alias : activeBlog.author }}</span>
                  <el-tag type="success" size="small">{{ activeBlog.contestTag }}</el-tag>
                  <el-tag v-if="activeBlog.visibility === 'private'" type="info" size="small">个人笔记</el-tag>
                </div>
              </div>
              <div class="card-actions">
                <el-button v-if="canEditBlog(activeBlog)" type="primary" plain @click="openBlogEditor(activeBlog)">编辑</el-button>
                <el-button v-if="canEditBlog(activeBlog)" type="danger" plain @click="deleteBlog(activeBlog)">删除</el-button>
              </div>
            </div>
            <div class="markdown-preview detail-preview" v-html="renderMarkdown(activeBlog.content)"></div>
            <div class="social-actions detail-social-actions">
              <el-button class="social-button like" :class="{ active: activeBlog.social.liked }" :loading="socialPending.like" @click="toggleBlogAction('LIKE')">
                点赞 {{ activeBlog.social.likes }}
              </el-button>
              <el-button class="social-button collect" :class="{ active: activeBlog.social.collected }" :loading="socialPending.collect" @click="toggleBlogAction('COLLECT')">
                收藏 {{ activeBlog.social.collects }}
              </el-button>
              <el-button class="social-button follow" :class="{ active: activeBlog.social.followed }" :disabled="activeBlog.authorId === currentUser.id" :loading="socialPending.follow" @click="toggleFollowAuthor">
                {{ activeBlog.social.followed ? '已关注对方' : '关注作者' }}
              </el-button>
            </div>
          </article>
          <section v-if="activeBlog" class="panel">
            <div class="panel-head">
              <h3>评论</h3>
              <el-tag>{{ activeBlog.comments.length }} 条</el-tag>
            </div>
            <div v-if="activeBlog.commentsOpen" class="comment-box">
              <div v-for="comment in activeBlog.comments" :key="comment.id" class="comment">
                <strong>{{ comment.author }}</strong>
                <span>{{ comment.text }}</span>
                <div v-for="reply in comment.replies" :key="reply.id" class="reply">
                  <strong>{{ reply.author }}</strong>
                  <span>{{ reply.text }}</span>
                </div>
              </div>
              <el-input v-model="commentDrafts[activeBlog.id]" placeholder="写一条评论">
                <template #append>
                  <el-button @click="addComment(activeBlog)">评论</el-button>
                </template>
              </el-input>
            </div>
            <el-alert v-else title="作者已关闭评论区" type="info" :closable="false" />
          </section>

        </section>

        <section v-if="activeView === 'salon'" class="view">
          <div class="toolbar">
            <el-alert title="沙龙面向全校师生，所有用户均可提交沙龙，管理员审核通过后开放预约。" type="success" :closable="false" />
            <el-button type="primary" :icon="Plus" @click="openSalonDialog">发布沙龙</el-button>
          </div>
          <div class="salon-grid">
            <article v-for="salon in salons" :key="salon.id" class="salon-card">
              <div class="date-box">
                <strong>{{ salon.day }}</strong>
                <span>{{ salon.month }}</span>
              </div>
              <div class="salon-main">
                <h3>{{ salon.title }}</h3>
                <p>{{ salon.description }}</p>
                <div class="meta-row">
                  <span class="time-highlight"><el-icon><Calendar /></el-icon>{{ salon.date }}</span>
                  <span><el-icon><Location /></el-icon>{{ salon.place }}</span>
                  <span><el-icon><UserFilled /></el-icon>{{ salon.speaker }}</span>
                </div>
                <el-progress :percentage="salonPercent(salon)" :stroke-width="10" />
                <div class="card-actions">
                  <span>剩余 {{ salon.capacity - salon.booked }} / {{ salon.capacity }} 名额</span>
                  <el-button type="primary" :loading="isActionPending(actionKey('salon-book', salon.id))" :disabled="salon.booked >= salon.capacity || bookedSalonIds.includes(salon.id) || isActionPending(actionKey('salon-book', salon.id))" @click="bookSalon(salon)">
                    {{ bookedSalonIds.includes(salon.id) ? '已预约' : '立即预约' }}
                  </el-button>
                  <el-button v-if="canManageSalon(salon)" type="danger" plain @click="deleteSalon(salon)">删除沙龙</el-button>
                </div>
              </div>
            </article>
          </div>
          <el-pagination class="pager" background layout="prev, pager, next, sizes, total" :current-page="salonPage.current" :page-size="salonPage.size" :page-sizes="[3, 6, 9, 12]" :total="salonPage.total" @current-change="changeSalonPage" @size-change="changeSalonSize" />
        </section>

        <section v-if="activeView === 'teams'" class="view">
          <el-tabs v-model="teamTab" class="team-tabs">
            <el-tab-pane label="组队大厅" name="hall">
              <div class="split-layout">
                <section class="panel">
                  <div class="panel-head">
                    <h3>发布组队需求</h3>
                    <el-tag type="danger">默认隐藏联系方式</el-tag>
                  </div>
                  <el-form label-position="top">
                    <el-form-item label="队伍名称">
                      <el-input v-model="draftTeam.title" />
                    </el-form-item>
                    <el-form-item label="需求描述">
                      <el-input v-model="draftTeam.need" type="textarea" :rows="4" />
                    </el-form-item>
                    <el-form-item label="技能标签">
                      <el-select v-model="draftTeam.skills" multiple placeholder="选择技能或方向">
                        <el-option v-for="skill in allSkills" :key="skill" :label="skill" :value="skill" />
                      </el-select>
                    </el-form-item>
                    <el-button type="primary" :icon="Plus" :loading="isActionPending('team-publish')" :disabled="isActionPending('team-publish')" @click="publishTeam">发布组队卡片</el-button>
                  </el-form>
                </section>
                <section class="panel">
                  <div class="panel-head">
                    <h3>双向确认流程</h3>
                    <el-tag>申请 · 同意 · 解锁</el-tag>
                  </div>
                  <el-steps :active="teamStep" finish-status="success" align-center>
                    <el-step title="申请加入" />
                    <el-step title="队长审核" />
                    <el-step title="解锁联系方式" />
                  </el-steps>
                  <p class="flow-note">系统在对应层校验：JWT 验证资源权限，防止通过修改 URL ID 越权查看他人微信、手机号或真实姓名。</p>
                </section>
              </div>
              <section class="toolbar">
                <el-input v-model="teamKeyword" placeholder="搜索队伍、需求、竞赛..." clearable>
                  <template #prefix><el-icon><Search /></el-icon></template>
                </el-input>
                <el-select v-model="teamContest" placeholder="竞赛分类" clearable>
                  <el-option v-for="name in contests" :key="name" :label="name" :value="name" />
                  <el-option label="自定义竞赛" value="自定义竞赛" />
                </el-select>
              </section>
              <div class="team-grid">
                <article v-for="team in teams" :key="team.id" class="team-card">
                  <div class="card-topline">
                    <el-tag type="success">{{ team.contest }}</el-tag>
                    <el-tag type="info">{{ team.status }}</el-tag>
                  </div>
                  <h3>{{ team.title }}</h3>
                  <p>{{ team.need }}</p>
                  <div class="skill-row">
                    <el-tag v-for="skill in team.skills" :key="skill" size="small">{{ skill }}</el-tag>
                  </div>
                  <div class="privacy-contact">
                    <strong>联系方式</strong>
                    <span>{{ team.unlocked ? team.contact : '双向确认前隐藏' }}</span>
                  </div>
                  <div class="card-actions">
                    <el-button :icon="Position" :loading="isActionPending(actionKey('team-apply', team.id))" :disabled="currentUser.role === 'admin' || team.unlocked || isActionPending(actionKey('team-apply', team.id))" @click="applyTeam(team)">申请加入</el-button>
                    <el-button type="primary" plain :loading="isActionPending(actionKey('team-approve', team.applicationId || team.id))" :disabled="!team.applied || team.unlocked || currentUser.role === 'student' || isActionPending(actionKey('team-approve', team.applicationId || team.id))" @click="approveTeam(team)">
                      {{ currentUser.role === 'admin' ? '管理员同意' : '队长同意' }}
                    </el-button>
                    <el-button size="small" type="primary" plain :disabled="!canManageTeam(team)" @click="openTeamEditor(team)">编辑</el-button>
                    <el-button size="small" type="danger" plain :disabled="!canManageTeam(team)" @click="deleteTeam(team)">删除</el-button>
                  </div>
                </article>
              </div>
              <el-pagination class="pager" background layout="prev, pager, next, sizes, total" :current-page="teamPage.current" :page-size="teamPage.size" :page-sizes="[3, 6, 9, 12]" :total="teamPage.total" @current-change="changeTeamPage" @size-change="changeTeamSize" />
            </el-tab-pane>
            <el-tab-pane label="我的队伍申请管理" name="applications">
              <section class="panel">
                <div class="panel-head">
                  <h3>我的队伍申请</h3>
                  <div class="head-actions">
                    <el-button type="primary" plain size="small" :disabled="selectedManageableApplicationIds.length === 0" @click="approveSelectedManageableApplications">批量同意</el-button>
                    <el-button type="info" plain size="small" :disabled="selectedManageableApplicationIds.length === 0" @click="rejectSelectedManageableApplications">批量拒绝</el-button>
                    <el-button type="danger" plain size="small" :disabled="selectedManageableApplicationDeleteIds.length === 0" @click="deleteSelectedManageableApplications">批量删除</el-button>
                    <el-tag type="success">{{ teamApplicationPage.total }} 条</el-tag>
                  </div>
                </div>
                <el-table :data="manageableTeamApplications" stripe @selection-change="selectManageableApplications">
                  <el-table-column type="selection" width="55" />
                  <el-table-column prop="teamTitle" label="队伍" />
                  <el-table-column prop="applicant" label="申请人" width="180" />
                  <el-table-column prop="skillsText" label="技能标签" />
                  <el-table-column prop="status" label="状态" width="110" />
                  <el-table-column label="操作" width="210">
                    <template #default="{ row }">
                      <el-button size="small" type="primary" :loading="isActionPending(actionKey('team-application-approve', row.id))" :disabled="row.rawStatus !== 'PENDING' || isActionPending(actionKey('team-application-approve', row.id))" @click="approveTeamApplication(row)">同意</el-button>
                      <el-button size="small" type="info" plain :disabled="row.rawStatus !== 'PENDING'" @click="rejectTeamApplication(row)">拒绝</el-button>
                      <el-button size="small" type="danger" plain @click="deleteTeamApplication(row)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-pagination class="pager" background layout="prev, pager, next, sizes, total" :current-page="teamApplicationPage.current" :page-size="teamApplicationPage.size" :page-sizes="[5, 8, 12, 20]" :total="teamApplicationPage.total" @current-change="changeTeamApplicationPage" @size-change="changeTeamApplicationSize" />
              </section>
            </el-tab-pane>
            <el-tab-pane label="我的队伍成员管理" name="members">
              <section class="panel">
                <div class="panel-head">
                  <h3>我的队伍成员</h3>
                  <el-tag type="success">{{ teamMemberPage.total }} 人</el-tag>
                </div>
                <el-table :data="manageableTeamMembers" stripe>
                  <el-table-column prop="teamTitle" label="队伍" />
                  <el-table-column prop="member" label="成员" width="180" />
                  <el-table-column prop="role" label="角色" width="110" />
                  <el-table-column prop="skillsText" label="技能标签" />
                  <el-table-column label="操作" width="150">
                    <template #default="{ row }">
                      <el-button size="small" type="danger" plain :disabled="row.role === '队长'" @click="removeTeamMember(row)">移除队员</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-pagination class="pager" background layout="prev, pager, next, sizes, total" :current-page="teamMemberPage.current" :page-size="teamMemberPage.size" :page-sizes="[5, 8, 12, 20]" :total="teamMemberPage.total" @current-change="changeTeamMemberPage" @size-change="changeTeamMemberSize" />
              </section>
            </el-tab-pane>
          </el-tabs>
          <el-dialog v-model="teamEditorVisible" title="编辑队伍活动" width="560px">
            <el-form label-position="top">
              <el-form-item label="队伍方向">
                <el-input v-model="teamEditor.title" />
              </el-form-item>
              <el-form-item label="竞赛项目">
                <el-select v-model="teamEditor.contest">
                  <el-option v-for="name in contests" :key="name" :label="name" :value="name" />
                  <el-option label="自定义竞赛" value="自定义竞赛" />
                </el-select>
              </el-form-item>
              <el-form-item label="需求描述">
                <el-input v-model="teamEditor.need" type="textarea" :rows="4" />
              </el-form-item>
              <el-form-item label="技能标签">
                <el-select v-model="teamEditor.skills" multiple>
                  <el-option v-for="skill in allSkills" :key="skill" :label="skill" :value="skill" />
                </el-select>
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="teamEditorVisible = false">取消</el-button>
              <el-button type="primary" @click="saveTeamEdit">保存</el-button>
            </template>
          </el-dialog>
        </section>

        <section v-if="activeView === 'profile'" class="view">
          <div class="profile-layout">
            <section class="panel">
              <div class="panel-head">
                <h3>隐私可见度</h3>
                <div class="head-actions">
                  <el-tag type="success">数据自主权</el-tag>
                  <el-button type="primary" plain @click="openPasswordDialog">修改登录密码</el-button>
                </div>
              </div>
              <el-form label-position="top">
                <el-form-item label="真实姓名">
                  <el-segmented v-model="privacy.realName" :options="privacyOptions" />
                </el-form-item>
                <el-form-item label="微信">
                  <el-segmented v-model="privacy.wechat" :options="privacyOptions" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-segmented v-model="privacy.phone" :options="privacyOptions" />
                </el-form-item>
              </el-form>
            </section>
            <section class="panel">
              <div class="panel-head">
                <h3>我的预约凭证</h3>
                <span>{{ bookedSalonIds.length }} 个预约</span>
              </div>
              <el-empty v-if="bookedSalonIds.length === 0" description="暂无预约" />
              <div v-else class="ticket-list">
                <div v-for="ticket in bookedSalons" :key="ticket.id" class="ticket">
                  <strong>{{ ticket.title }}</strong>
                  <span>{{ ticket.date }} · {{ ticket.place }}</span>
                  <el-tag type="success">凭证码{{ ticket.certificateCode }}</el-tag>
                </div>
          </div>

        </section>
          </div>
          <el-dialog v-model="passwordDialogVisible" title="修改登录密码" width="460px">
            <el-form label-position="top" @submit.prevent>
              <el-form-item label="旧密码">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前登录密码" />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6-18 位字母与数字组合" />
                <div class="form-tip">密码为 6-18 位字母与数字组合。</div>
              </el-form-item>
              <el-form-item label="确认新密码">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" @keyup.enter="changePassword" />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="passwordDialogVisible = false">取消</el-button>
              <el-button type="primary" :loading="passwordChanging" @click="changePassword">确认修改</el-button>
            </template>
          </el-dialog>
        </section>

        <section v-if="activeView === 'admin'" class="view">
          <el-alert title="后台展示：管理员审核资源、博客、沙龙倒计时信息，查看脱敏技能画像和人工撮合，管理组队申请与沙龙预约。" type="warning" :closable="false" />
          <div class="admin-grid">
            <section class="panel">
              <div class="panel-head">
                <h3>审核队列</h3>
                <div class="head-actions">
                  <el-button type="success" plain size="small" :disabled="selectedReviewIds.length === 0" @click="reviewSelected('APPROVED')">批量通过审核</el-button>
                  <el-button type="info" plain size="small" :disabled="selectedReviewIds.length === 0" @click="reviewSelected('REJECTED')">批量驳回申请</el-button>
                  <el-button type="danger" plain size="small" :disabled="selectedReviewIds.length === 0" @click="deleteSelectedReviews">批量删除</el-button>
                  <el-tag type="warning">{{ reviewPage.total }} 条</el-tag>
                </div>
              </div>
              <el-table :data="reviewQueue" height="360" @selection-change="selectReviews">
                <el-table-column type="selection" width="44" />
                <el-table-column prop="type" label="类型" width="110" />
                <el-table-column prop="title" label="标题" />
                <el-table-column prop="owner" label="提交人" width="120" />
                <el-table-column label="操作" width="170">
                  <template #default="{ row }">
                    <el-button size="small" type="success" :loading="isActionPending(actionKey('review-approve', row.id))" :disabled="isActionPending(actionKey('review-approve', row.id)) || isActionPending(actionKey('review-reject', row.id))" @click="reviewItem(row, 'APPROVED')">通过</el-button>
                    <el-button size="small" type="danger" plain :loading="isActionPending(actionKey('review-reject', row.id))" :disabled="isActionPending(actionKey('review-approve', row.id)) || isActionPending(actionKey('review-reject', row.id))" @click="reviewItem(row, 'REJECTED')">驳回</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination class="pager compact-pager" background layout="prev, pager, next, total" :current-page="reviewPage.current" :page-size="reviewPage.size" :total="reviewPage.total" @current-change="changeReviewPage" />
            </section>
            <section class="panel">
              <div class="panel-head">
                <h3>红娘后台</h3>
                <el-tag>脱敏画像</el-tag>
              </div>
              <div v-for="candidate in candidates" :key="candidate.id" class="candidate">
                <div>
                  <strong>{{ candidate.maskedName }}</strong>
                  <span>{{ candidate.grade }} · {{ candidate.intent }}</span>
                </div>
                <div class="skill-row">
                  <el-tag v-for="skill in candidate.skills" :key="skill" size="small">{{ skill }}</el-tag>
                </div>
                <el-button type="primary" plain size="small">主动联系牵线</el-button>
              </div>
            </section>
          </div>
          <section class="panel">
            <div class="panel-head">
              <h3>组队申请</h3>
              <div class="head-actions">
                <el-button type="primary" plain size="small" :disabled="selectedAdminApplicationIds.length === 0" @click="approveSelectedAdminApplications">批量同意</el-button>
                <el-button type="danger" plain size="small" :disabled="selectedAdminApplicationDeleteIds.length === 0" @click="deleteSelectedAdminApplications">批量删除</el-button>
                <el-tag type="success">{{ teamApplications.length }} 条</el-tag>
              </div>
            </div>
            <el-table :data="teamApplications" stripe @selection-change="selectAdminApplications">
              <el-table-column type="selection" width="55" />
              <el-table-column prop="teamTitle" label="队伍" />
              <el-table-column prop="applicant" label="申请人" width="180" />
              <el-table-column prop="skillsText" label="技能标签" />
              <el-table-column prop="status" label="状态" width="110" />
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button size="small" type="primary" :loading="isActionPending(actionKey('team-application-approve', row.id))" :disabled="row.rawStatus !== 'PENDING' || isActionPending(actionKey('team-application-approve', row.id))" @click="approveTeamApplication(row)">同意组队</el-button>
                  <el-button size="small" type="danger" plain @click="deleteTeamApplication(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>
          <section class="panel">
            <div class="panel-head">
              <h3>沙龙预约管理</h3>
              <div class="head-actions">
                <el-button type="primary" plain @click="exportSalonAppointments">导出预约数据 (Excel)</el-button>
                <el-tag type="success">{{ salonAppointments.length }} 条</el-tag>
              </div>
            </div>
            <el-table :data="salonAppointments" stripe>
              <el-table-column prop="salonTitle" label="沙龙" />
              <el-table-column prop="userNickname" label="预约人" width="140" />
              <el-table-column prop="certificateCode" label="凭证码" />
              <el-table-column prop="status" label="状态" width="110" />
              <el-table-column label="操作" width="210">
                <template #default="{ row }">
                  <el-button v-if="row.rawStatus === 'BOOKED'" size="small" type="warning" plain @click="cancelSalonAppointment(row)">取消预约</el-button>
                  <el-button v-else size="small" type="danger" plain @click="deleteSalonAppointment(row)">删除记录</el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </section>



        <section v-if="activeView === 'userManagement'" class="view">
          <div class="toolbar user-management-toolbar">
            <el-input v-model="managedUserKeyword" placeholder="搜索学号、账号、昵称、邮箱或手机号" clearable @keyup.enter="loadManagedUsers">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="managedUserRole" placeholder="用户角色" clearable>
              <el-option label="普通学生" value="STUDENT" />
              <el-option label="特邀专家" value="MENTOR" />
              <el-option label="管理员" value="ADMIN" />
              <el-option label="超级管理员" value="SUPER_ADMIN" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="loadManagedUsers">查询</el-button>
            <el-button type="danger" plain :disabled="managedUserSelection.length === 0" @click="deleteSelectedManagedUsers">
              批量删除 ({{ managedUserSelection.length }})
            </el-button>
          </div>
          <section class="panel">
            <div class="panel-head">
              <h3>用户列表</h3>
              <el-tag type="success">{{ managedUserPage.total }} 位用户</el-tag>
            </div>
            <el-table :data="managedUsers" v-loading="managedUsersLoading" stripe style="width: 100%" @selection-change="handleManagedUserSelectionChange">
              <el-table-column type="selection" width="55" :selectable="canDeleteManagedUser" />
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="studentNo" label="学号/工号" width="150" />
              <el-table-column prop="username" label="账号" width="140" />
              <el-table-column prop="nickname" label="昵称" min-width="150" />
              <el-table-column prop="email" label="邮箱" min-width="190" />
              <el-table-column prop="phone" label="手机号" min-width="140" />
              <el-table-column prop="createdAt" label="注册时间" width="180" />
              <el-table-column label="角色" width="130">
                <template #default="{ row }">
                  <el-tag :type="managedUserRoleTag(row.role)">{{ managedUserRoleLabel(row.role) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" type="danger" plain :disabled="!canDeleteManagedUser(row)" @click.stop="deleteManagedUser(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination class="pager" background layout="total, sizes, prev, pager, next" :current-page="managedUserPage.current" :page-size="managedUserPage.size" :page-sizes="[8, 10, 20, 50]" :total="managedUserPage.total" @current-change="changeManagedUserPage" @size-change="changeManagedUserSize" />
          </section>
        </section>

        <section v-if="activeView === 'superAdmin'" class="view">
          <el-alert title="管理员权限管理：可以查看所有管理员账号，添加新管理员，或取消管理员权限。注意：超级管理员不能删除自己。" type="warning" :closable="false" />
          <div class="toolbar">
            <h3>管理员列表</h3>
            <el-button type="primary" :icon="Plus" @click="openNewAdminDialog">新增管理员</el-button>
            <el-button :loading="adminUsersLoading" @click="loadAdminUsers">刷新</el-button>
          </div>
          <el-table :data="adminUsers" stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="studentNo" label="学号/工号" width="150" />
            <el-table-column prop="username" label="账号" width="140" />
            <el-table-column prop="nickname" label="昵称" min-width="150" />
            <el-table-column prop="email" label="邮箱" min-width="190" />
            <el-table-column prop="phone" label="手机号" min-width="140" />
            <el-table-column prop="createdAt" label="注册时间" width="180" />
            <el-table-column label="角色" width="130">
              <template #default="{ row }">
                <el-tag :type="row.role === 'SUPER_ADMIN' ? 'danger' : 'warning'">
                  {{ row.role === 'SUPER_ADMIN' ? '超级管理员' : '管理员' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="danger" plain :disabled="row.role === 'SUPER_ADMIN'" @click="removeAdminUser(row)">取消管理员</el-button>
                <el-button size="small" type="danger" :disabled="row.id === currentUser.id" @click="transferSuperAdmin(row.id)">转让超级管理员</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-dialog v-model="newAdminDialogVisible" title="新增管理员" width="520px">
            <el-form label-position="top">
              <el-form-item label="学号/工号">
                <el-input v-model="newAdminForm.studentNo" placeholder="请输入学号或工号" />
              </el-form-item>
              <el-form-item label="用户名">
                <el-input v-model="newAdminForm.username" placeholder="请输入数据库中的用户名" />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="newAdminDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="createAdmin">确定新增</el-button>
            </template>
          </el-dialog>
        </section>

        <el-dialog v-model="resourceDialogVisible" title="上传资源" width="560px">
          <el-form label-position="top">
            <el-form-item label="资源标题">
              <el-input v-model="resourceForm.title" placeholder="请输入资源标题" />
            </el-form-item>
            <el-form-item label="竞赛名称">
              <el-select v-model="resourceForm.contest" filterable allow-create default-first-option placeholder="请选择或输入竞赛名称" style="width: 100%">
                <el-option v-for="name in contests" :key="name" :label="name" :value="name" />
              </el-select>
            </el-form-item>
            <el-form-item label="资料类型">
              <el-select v-model="resourceForm.type" filterable allow-create default-first-option placeholder="请选择或输入资料类型" style="width: 100%">
                <el-option v-for="type in resourceTypes" :key="type" :label="type" :value="type" />
              </el-select>
            </el-form-item>
            <el-form-item label="资料说明">
              <el-input v-model="resourceForm.summary" type="textarea" :rows="4" placeholder="请输入资料简介、适用人群或使用说明" />
            </el-form-item>
            <el-form-item label="选择文件">
              <input class="native-file-input" type="file" @change="handleResourceFileChange" />
              <div class="form-tip">{{ resourceFile ? resourceFile.name : '请选择要上传的资料文件' }}</div>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="resourceDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="isActionPending('resource-submit')" :disabled="isActionPending('resource-submit')" @click="submitResource">{{ isContentAdmin ? '直接发布' : '提交审核' }}</el-button>
          </template>
        </el-dialog>

        <el-dialog v-model="salonDialogVisible" title="发布沙龙" width="560px">
          <el-form label-position="top">
            <el-form-item label="沙龙标题">
              <el-input v-model="salonForm.title" placeholder="请输入沙龙标题" />
            </el-form-item>
            <el-form-item label="沙龙介绍">
              <el-input v-model="salonForm.description" type="textarea" :rows="4" placeholder="请输入分享内容、适合人群或活动亮点" />
            </el-form-item>
            <el-form-item label="开始时间">
              <el-date-picker v-model="salonForm.startsAt" type="datetime" value-format="YYYY-MM-DDTHH:mm" format="YYYY-MM-DD HH:mm" placeholder="选择开始时间" style="width: 100%" />
            </el-form-item>
            <el-form-item label="地点">
              <el-input v-model="salonForm.place" placeholder="请输入地点" />
            </el-form-item>
            <el-form-item label="容量">
              <el-input-number v-model="salonForm.capacity" :min="1" :max="500" style="width: 100%" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="salonDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="isActionPending('salon-submit')" :disabled="isActionPending('salon-submit')" @click="submitSalon">{{ isContentAdmin ? '直接发布' : '提交审核' }}</el-button>
          </template>
        </el-dialog>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowDown,
  Calendar,
  ChatLineRound,
  Connection,
  DataBoard,
  Download,
  EditPen,
  FolderOpened,
  Location,
  Lock,
  Plus,
  Position,
  Search,
  Setting,
  SwitchButton,
  Trophy,
  Upload,
  User,
  UserFilled,
  View,
} from '@element-plus/icons-vue'

type Role = 'student' | 'mentor' | 'admin' | 'superAdmin'
type CommentReply = { id: number; author: string; text: string }
type BlogComment = CommentReply & { replies: CommentReply[] }
type BlogSocialView = { likes: number; collects: number; liked: boolean; collected: boolean; followed: boolean }
type ResourceView = { id: number; uploaderId: number; contest: string; type: string; title: string; summary: string; downloads: number; uploader: string }
type HotResourceView = { id: number; title: string; contest: string; type: string; downloads: number }
type BlogView = { id: number; authorId: number; title: string; author: string; alias: string; cert: string; contestTag: string; anonymous: boolean; visibility: string; commentsOpen: boolean; content: string; createdAt: string; social: BlogSocialView; comments: BlogComment[] }
type SalonView = { id: number; speakerId: number; title: string; speaker: string; date: string; month: string; day: string; place: string; capacity: number; booked: number; description: string }
type TicketView = SalonView & { certificateCode: string }
type TeamView = { id: number; leaderId: number; contest: string; title: string; need: string; skills: string[]; status: string; contact: string; applied: boolean; unlocked: boolean; applicationId?: number; applications: TeamApplicationView[]; members: TeamMemberView[] }
type TeamApplicationView = { id: number; teamId: number; leaderId: number; teamTitle: string; applicant: string; applicantId: number; skills: string[]; skillsText: string; status: string; rawStatus: string }
type TeamMemberView = { teamId: number; leaderId: number; teamTitle: string; userId: number; member: string; role: string; rawRole: string; skills: string[]; skillsText: string }
type ReviewView = { id: number; type: string; targetType?: string; targetId?: number; title: string; owner: string; status: string }
type CandidateView = { id: number; maskedName: string; grade: string; intent: string; skills: string[] }
type SalonAppointmentView = { id: number; salonTitle: string; userNickname: string; certificateCode: string; status: string; rawStatus: string }
type ManagedUserView = { id: number; studentNo: string; username: string; nickname: string; email: string; phone: string; role: string; createdAt: string }

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  typographer: true,
  highlight(code, language) {
    const lang = language && hljs.getLanguage(language) ? language : 'plaintext'
    const highlighted = hljs.highlight(code, { language: lang, ignoreIllegals: true }).value
    return `<pre class="hljs markdown-code-block"><code class="language-${lang}">${highlighted}</code></pre>`
  },
})
md.normalizeLink = (url: string) => url
md.normalizeLinkText = (url: string) => url
const API_BASE = '/api'
const TOKEN_KEY = 'competition-hub-token'
const ROLE_KEY = 'competition-hub-role'
let sessionExpiredNotified = false

const activeView = ref('dashboard')
const selectedRole = ref<Role>(normalizeRole(localStorage.getItem(ROLE_KEY) || 'admin'))
const authTab = ref<'password' | 'register'>('password')
const bookedSalonIds = ref<number[]>([])
const bookedSalons = ref<TicketView[]>([])
const teamStep = ref(0)
const teamTab = ref('hall')
const teamEditorVisible = ref(false)
const teamKeyword = ref('')
const teamContest = ref('')
const resourceKeyword = ref('')
const resourceContest = ref('')
const resourceType = ref('')
const resourceEditorVisible = ref(false)
const selectedResourceIds = ref<number[]>([])
const selectedReviewIds = ref<number[]>([])
const selectedManageableApplicationIds = ref<number[]>([])
const selectedManageableApplicationDeleteIds = ref<number[]>([])
const selectedAdminApplicationIds = ref<number[]>([])
const selectedAdminApplicationDeleteIds = ref<number[]>([])
const token = ref(localStorage.getItem(TOKEN_KEY) || '')
const loggedInUser = ref<any>(null)
const loading = ref(false)
const codeSending = ref(false)
const resetCodeSending = ref(false)
const resettingPassword = ref(false)
const resetPasswordVisible = ref(false)
const firstLoginDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const passwordChanging = ref(false)
const isApplyingAuth = ref(false)
const pendingActions = reactive<Record<string, boolean>>({})
const commentDrafts = reactive<Record<number, string>>({})
const blogEditorTab = ref('write')
const blogEditorMode = ref<'markdown' | 'text'>('markdown')
const editingBlogId = ref<number | null>(null)
const mdFileInputRef = ref<HTMLInputElement | null>(null)
const blogImageInputRef = ref<HTMLInputElement | null>(null)
const mdUploading = ref(false)
const blogImageUploading = ref(false)
const resourceDialogVisible = ref(false)
const resourceForm = reactive({ title: '', contest: '', type: '', summary: '' })
const resourceFile = ref<File | null>(null)
const resourceEditFile = ref<File | null>(null)
const adminUsers = ref<any[]>([])
const adminUsersLoading = ref(false)
const newAdminDialogVisible = ref(false)
const newAdminForm = reactive({ studentNo: '', username: '' })
const salonDialogVisible = ref(false)
const salonForm = reactive({ title: '', description: '', startsAt: '', place: '', capacity: 36 })
const managedUsers = ref<ManagedUserView[]>([])
const managedUsersLoading = ref(false)
const managedUserKeyword = ref('')
const managedUserRole = ref('')
const managedUserSelection = ref<ManagedUserView[]>([])

const users = {
  student: { id: 1, studentNo: '20230118', username: 'student', name: '陈同学', avatar: '陈', role: 'student', roleLabel: '普通学生' },
  mentor: { id: 2, studentNo: 'M2026', username: 'mentor', name: '林学姐', avatar: '林', role: 'mentor', roleLabel: '特邀专家 · 国奖得主' },
  admin: { id: 3, studentNo: 'ADMIN', username: 'admin', name: '管理员', avatar: '管', role: 'admin', roleLabel: '平台管理员' },
  superAdmin: { id: 3, studentNo: 'ADMIN', username: 'admin', name: '超级管理员', avatar: '超', role: 'superAdmin', roleLabel: '平台超级管理员' },
} as const

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ studentNo: '', nickname: '', phone: '', email: '', code: '', password: '' })
const resetPasswordForm = reactive({ email: '', code: '', newPassword: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const isLoggedIn = computed(() => Boolean(token.value))
const isSuperAdmin = computed(() => currentUser.value.role === 'superAdmin')
const isContentAdmin = computed(() => currentUser.value.role === 'admin' || currentUser.value.role === 'superAdmin')
const currentUser = computed(() => {
  const fallback = users[selectedRole.value]
  if (!loggedInUser.value) return fallback
  const role = normalizeRole(loggedInUser.value.role || fallback.role)
  return {
    ...fallback,
    id: loggedInUser.value.id || fallback.id,
    username: loggedInUser.value.username || fallback.username,
    name: loggedInUser.value.nickname || fallback.name,
    avatar: (loggedInUser.value.nickname || fallback.avatar).slice(0, 1),
    role,
    roleLabel: role === 'superAdmin' ? '平台超级管理员' : role === 'admin' ? '平台管理员' : role === 'mentor' ? '特邀专家 · 国奖得主' : '普通学生',
  }
})

function normalizeRole(role: unknown): Role {
  const normalized = String(role || '').trim().replace(/^ROLE_/i, '').replace(/[_-]/g, '').toLowerCase()
  if (normalized === 'superadmin') return 'superAdmin'
  if (normalized === 'admin') return 'admin'
  if (normalized === 'mentor') return 'mentor'
  return 'student'
}

const resourcePage = reactive({ current: 1, size: 8, total: 0 })
const blogPage = reactive({ current: 1, size: 6, total: 0 })
const salonPage = reactive({ current: 1, size: 6, total: 0 })
const reviewPage = reactive({ current: 1, size: 8, total: 0 })
const managedUserPage = reactive({ current: 1, size: 10, total: 0 })
const teamPage = reactive({ current: 1, size: 6, total: 0 })
const teamApplicationPage = reactive({ current: 1, size: 8, total: 0 })
const teamMemberPage = reactive({ current: 1, size: 8, total: 0 })
const blogKeyword = ref('')
const blogContest = ref('')
const blogVisibility = ref('')
const activeBlog = ref<BlogView | null>(null)
const socialPending = reactive({ like: false, collect: false, follow: false })
const creatorStats = reactive({ totalLikes: 0, totalCollects: 0, followers: 0, works: 0 })
const resourceEditor = reactive({ id: null as number | null, title: '', contest: '', type: '', summary: '' })

const viewTitle = computed(() => ({
  dashboard: '系统概览',
  resources: '竞赛资源库',
  blog: '学长学姐经验',
  blogEditor: editingBlogId.value ? '编辑博客' : '发布博客',
  blogDetail: '博客详情',
  salon: '沙龙日程',
  teams: '技术红娘组队',
  profile: '个人隐私中心',
  admin: '审核后台',
  userManagement: '用户管理',
  superAdmin: '管理员权限管理',
}[activeView.value]))

watch([resourceKeyword, resourceContest, resourceType], () => { resourcePage.current = 1; void loadResources() })
watch([blogKeyword, blogContest, blogVisibility], () => { blogPage.current = 1; void loadBlogs() })
watch([teamKeyword, teamContest], () => { teamPage.current = 1; void loadTeams() })
watch(teamTab, (tab) => { if (tab === 'applications') void loadTeamApplications(); if (tab === 'members') void loadTeamMembers() })

const metrics = reactive([
  { key: 'resources', label: '竞赛资源', value: '0' },
  { key: 'blogs', label: '经验博客', value: '0' },
  { key: 'salons', label: '沙龙活动', value: '0' },
  { key: 'teams', label: '组队需求', value: '0' },
])

const modules = [
  { title: '隐私权限控制', desc: '普通学生可以浏览资料、写博客和组队，学长学姐可以发布沙龙经验，管理员审核内容、权限管理。', badge: 'RBAC', icon: Lock, color: 'blue', target: 'profile' },
  { title: '竞赛资源库', desc: '按竞赛名称、资料类型筛选，模拟安全下载和下载日志计数，结合 OSS/COS 存储权限。', badge: 'OSS/COS', icon: FolderOpened, color: 'green', target: 'resources' },
  { title: '经验博客', desc: 'Markdown 经验分享、公开/私密、匿名发布、评论开关和二级评论展示。', badge: 'Community', icon: EditPen, color: 'orange', target: 'blog' },
  { title: '隐私组队', desc: '双向确认机制隐藏联系方式，基于脱敏画像人工撮合，队长同意后解锁。', badge: 'Privacy Match', icon: Connection, color: 'red', target: 'teams' },
]

const advantages = [
  { title: '竞赛服务', desc: '资源分享、经验博客、隐私组队管理，一站式竞赛服务平台。', icon: FolderOpened },
  { title: '隐私组队', desc: '申请审批双向确认机制，队长同意后解锁联系方式，防止隐私泄露。', icon: Lock },
  { title: '沙龙活动', desc: '沙龙发布预约限额，凭证码自动生成，管理后台导出台账。', icon: Calendar },
  { title: '内容审核', desc: '博客/资源/沙龙/组队提交后进入审核队列，管理员通过/驳回。', icon: DataBoard },
]

const contests = ['蓝桥杯', '数学建模', '程序设计', 'ACM-ICPC', '创新创业']
const resourceTypes = ['赛题解析', '获奖作品', '学习笔记', '工具教程', '算法模板']

const resources = reactive<ResourceView[]>([])
const hotResources = reactive<HotResourceView[]>([])
const filteredResources = computed(() => resources)

const draftBlog = reactive({
  title: '我的算法学习笔记',
  contestTag: '蓝桥杯',
  content: '## 刷题心得\n\n- 先理解题意再编码\n- 注意边界条件处理\n- 多练习常见题型\n\n> 坚持每天刷题，保持手感很重要。',
  imageUrl: '',
  visibility: 'public',
  anonymous: false,
  commentsOpen: true,
})

const blogs = reactive<BlogView[]>([])
const renderedDraft = computed(() => blogEditorMode.value === 'markdown' ? renderMarkdown(draftBlog.content) : renderPlainText(draftBlog.content))
const salons = reactive<SalonView[]>([])
const allSkills = ['Python', 'Java', 'C++', '机器学习', '数据分析', '算法设计', '前端开发', '后端开发', '数据库']
const draftTeam = reactive({ title: '程序设计竞赛组队', need: '寻找有ACM经验的队友一起备战蓝桥杯，要求熟悉 Python 或 Java 语言、数据结构与算法。', skills: ['Python', '前端开发'] })
const teamEditor = reactive<{ id: number | null; title: string; contest: string; need: string; skills: string[] }>({ id: null, title: '', contest: '自定义竞赛', need: '', skills: [] })
const teams = reactive<TeamView[]>([])
const teamApplications = reactive<TeamApplicationView[]>([])
const manageableTeamApplications = computed(() => teamApplications)
const manageableTeamMembers = reactive<TeamMemberView[]>([])
const privacyOptions = ['私密隐藏', '双向确认', '完全公开']
const privacy = reactive({ realName: '双向确认', wechat: '双向确认', phone: '私密隐藏' })
const reviewQueue = reactive<ReviewView[]>([])
const salonAppointments = reactive<SalonAppointmentView[]>([])
const candidates = reactive<CandidateView[]>([])
function renderMarkdown(source: string) { return md.render(normalizeImportedMarkdown(source)) }
function renderPlainText(source: string) {
  const escaped = source.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
  return `<p>${escaped.replaceAll('\n', '<br>')}</p>`
}
function blogSummary(source: string, maxLength = 150) {
  const text = markdownToPlainText(source)
  if (text.length <= maxLength) return text || '暂无摘要'
  return `${text.slice(0, maxLength).trimEnd()}...`
}

function markdownToPlainText(source = '') {
  return source
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/~~~[\s\S]*?~~~/g, ' ')
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<table[\s\S]*?<\/table>/gi, ' ')
    .replace(/<div\b[^>]*>\s*<img\b[\s\S]*?<\/div>/gi, ' ')
    .replace(/<img\b[^>]*>/gi, ' ')
    .replace(/!\[[^\]]*]\([^)]+\)/g, ' ')
    .replace(/^\s*\|.*\|\s*$/gm, ' ')
    .replace(/^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/gm, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\[([^\]]+)]\([^)]+\)/g, '$1')
    .replace(/[#>*_`~\-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function normalizeImportedMarkdown(source: string) {
  return normalizeMarkdownStructure(normalizeMarkdownImages(source))
}

function normalizeMarkdownStructure(source: string) {
  let inFence = false
  return source.replace(/\r\n?/g, '\n').split('\n').map((line) => {
    if (/^\s*(```|~~~)/.test(line)) {
      inFence = !inFence
      return line.replace(/^[\u3000]+/, '')
    }
    if (inFence) return line
    if (/^[\s\u3000]{1,}#{1,6}\s+/.test(line)) return line.replace(/^[\s\u3000]+/, '')
    if (/^[\s\u3000]{1,}([-*+]\s+|\d+[、.]\s*)/.test(line)) return line.replace(/^[\s\u3000]+/, '')
    if (/^[\s\u3000]{1,}>\s?/.test(line)) return line.replace(/^[\s\u3000]+/, '')
    if (/^[\s\u3000]{1,}\|/.test(line)) return line.replace(/^[\s\u3000]+/, '')
    return line
  }).join('\n')
}

function normalizeMarkdownImages(source: string) {
  return source
    .replace(/<div\b([^>]*)position\s*=\s*["']?center["']?([^>]*)>/gi, '<div$1$2 class="md-image-center">')
    .replace(/<div\b([^>]*)align\s*=\s*["']?center["']?([^>]*)>/gi, '<div$1$2 class="md-image-center">')
    .replace(/<div\b([^>]*)style\s*=\s*["'][^"']*text-align\s*:\s*center;?[^"']*["']([^>]*)>/gi, '<div$1$2 class="md-image-center">')
    .replace(/<img\b([^>]*)\/?>/gi, (_match, attributes: string) => {
      const src = normalizeImageUrl(readHtmlAttribute(attributes, 'src'))
      if (!src || !isSafeImageUrl(src)) return ''
      const alt = readHtmlAttribute(attributes, 'alt') || readHtmlAttribute(attributes, 'title') || '博客图片'
      const safeSrc = escapeHtmlAttribute(src)
      const safeAlt = escapeHtmlAttribute(alt)
      return `<img src="${safeSrc}" alt="${safeAlt}" loading="lazy">`
    })
    .replace(/!\[([^\]]*)\]\((https?:\/\/[^)\s]+)\)/gi, (_match, alt: string, src: string) => {
      return `![${escapeMarkdownLabel(alt || '博客图片')}](${normalizeImageUrl(src)})`
    })
}

function readHtmlAttribute(attributes: string, name: string) {
  const quoted = attributes.match(new RegExp(`\\b${name}\\s*=\\s*(['"])(.*?)\\1`, 'i'))
  if (quoted?.[2]) return quoted[2].trim()
  const unquoted = attributes.match(new RegExp(`\\b${name}\\s*=\\s*([^\\s>]+)`, 'i'))
  return unquoted?.[1]?.trim() || ''
}

function normalizeImageUrl(url: string) {
  return url.trim().replaceAll('&amp;', '&')
}

function isSafeImageUrl(url: string) {
  return /^https?:\/\//i.test(url) || url.startsWith('/')
}

function escapeMarkdownLabel(label: string) {
  return label.replaceAll('[', '\\[').replaceAll(']', '\\]')
}

function escapeHtmlAttribute(value: string) {
  return value.replaceAll('&', '&amp;').replaceAll('"', '&quot;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
}

function actionKey(prefix: string, id: number | string) {
  return `${prefix}:${id}`
}

function isActionPending(key: string) {
  return Boolean(pendingActions[key])
}

async function runExclusive<T>(key: string, task: () => Promise<T>): Promise<T | undefined> {
  if (pendingActions[key]) return undefined
  pendingActions[key] = true
  try {
    return await task()
  } finally {
    pendingActions[key] = false
  }
}

function absoluteApiUrl(path: string) {
  if (/^https?:\/\//i.test(path)) return path
  return path.startsWith(API_BASE) ? path : `${API_BASE}${path.startsWith('/') ? path : `/${path}`}`
}

async function fetchWithTimeout(input: RequestInfo | URL, init: RequestInit = {}, timeoutMs = 120000) {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), timeoutMs)
  try {
    return await fetch(input, { ...init, signal: controller.signal })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new Error('文件较大或网络较慢，下载响应超时，请稍后重试')
    }
    throw error
  } finally {
    window.clearTimeout(timeout)
  }
}

function formatFileSize(size?: number) {
  if (!size || size <= 0) return ''
  const units = ['B', 'KB', 'MB', 'GB']
  let value = size
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit += 1
  }
  return `${value.toFixed(value >= 10 || unit === 0 ? 0 : 1)} ${units[unit]}`
}

async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(token.value ? { Authorization: `Bearer ${token.value}` } : {}), ...(options.headers || {}) },
  })
  if (!response.ok) {
    if (response.status === 401) { handleSessionExpired(); throw new Error('登录已过期，请重新登录') }
    const text = await response.text()
    if (response.status === 403 && !text.trim()) {
      handleSessionExpired()
      throw new Error('登录已过期或权限已变更，请重新登录')
    }
    throw new Error(text || `${response.status} ${response.statusText}`)
  }
  return response.json() as Promise<T>
}

function clearAuthStorage() {
  token.value = ''; loggedInUser.value = null
  localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(ROLE_KEY)
  sessionStorage.removeItem(TOKEN_KEY); sessionStorage.removeItem(ROLE_KEY)
}
function handleSessionExpired() {
  if (sessionExpiredNotified) return; sessionExpiredNotified = true; clearAuthStorage()
  ElMessage.warning('登录已过期，请重新登录')
  window.setTimeout(() => { window.location.href = '/login' }, 500)
}
async function confirmAction(message: string) {
  await ElMessageBox.confirm(message, '操作确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
}

async function loginAndLoad() {
  if (!loginForm.username.trim()) { ElMessage.warning('请输入学号/工号'); return }
  if (!validatePassword(loginForm.password)) { ElMessage.warning('密码需为 6-18 位字母与数字组合'); return }
  loading.value = true
  try {
    const login = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: loginForm.username, password: loginForm.password }),
    })
    if (!login.ok) throw new Error(await resolveLoginError(login))
    await applyLoginResult(await login.json())
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败，请检查用户名和密码')
  } finally { loading.value = false }
}

async function resolveLoginError(response: Response) {
  const fallback = response.status === 401 || response.status === 403 ? '用户名或密码错误' : '登录服务异常，请稍后重试'
  const raw = await response.text()
  if (!raw) return fallback
  try {
    const parsed = JSON.parse(raw)
    const message = parsed.message || parsed.error || parsed.detail
    if (typeof message === 'string' && message.trim()) {
      if (/password|Bad credentials|Unauthorized|Forbidden/i.test(message)) return '用户名或密码错误'
      if (/lock|locked/i.test(message)) return '账号已被锁定，请联系管理员'
      return `登录失败：${message}`
    }
  } catch {
    if (/Unauthorized|Forbidden|401|403/i.test(raw)) return '用户名或密码错误'
  }
  return fallback
}

async function sendEmailCode() {
  if (!registerForm.email) { ElMessage.warning('请填写邮箱'); return }
  codeSending.value = true
  try {
    const response = await fetch(`${API_BASE}/auth/email/code`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: registerForm.email }) })
    if (!response.ok) throw new Error(await response.text() || '验证码发送失败')
    ElMessage.success('验证码已发送至您的邮箱，有效时间 5 分钟')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '验证码发送失败') }
  finally { codeSending.value = false }
}

async function sendResetPasswordCode() {
  if (!resetPasswordForm.email) { ElMessage.warning('请填写注册邮箱'); return }
  resetCodeSending.value = true
  try {
    const response = await fetch(`${API_BASE}/auth/email/code`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: resetPasswordForm.email }) })
    if (!response.ok) throw new Error(await response.text() || '验证码发送失败')
    ElMessage.success('验证码已发送至您的邮箱，有效时间 5 分钟')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '验证码发送失败') }
  finally { resetCodeSending.value = false }
}

async function resetPassword() {
  if (!validatePassword(resetPasswordForm.newPassword)) { ElMessage.warning('密码需为 6-18 位字母与数字组合'); return }
  resettingPassword.value = true
  try {
    const response = await fetch(`${API_BASE}/auth/reset-password`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(resetPasswordForm) })
    if (!response.ok) throw new Error(await response.text() || '密码重置失败')
    resetPasswordVisible.value = false; resetPasswordForm.code = ''; resetPasswordForm.newPassword = ''
    ElMessage.success('密码已重置，请使用新密码登录')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '密码重置失败') }
  finally { resettingPassword.value = false }
}

async function registerByEmail() {
  if (!validatePassword(registerForm.password)) { ElMessage.warning('密码需为 6-18 位字母与数字组合'); return }
  loading.value = true
  try {
    const response = await fetch(`${API_BASE}/auth/register`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(registerForm) })
    if (!response.ok) throw new Error(await response.text() || '注册失败')
    await applyLoginResult(await response.json())
    ElMessage.success('注册成功，已自动登录')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '注册失败') }
  finally { loading.value = false }
}

function validatePassword(password: string) { return /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,18}$/.test(password) }
function openPasswordDialog() { passwordForm.oldPassword = ''; passwordForm.newPassword = ''; passwordForm.confirmPassword = ''; passwordDialogVisible.value = true }

async function changePassword() {
  if (!passwordForm.oldPassword) { ElMessage.warning('请输入旧密码'); return }
  if (!validatePassword(passwordForm.newPassword)) { ElMessage.warning('密码需为 6-18 位字母与数字组合'); return }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) { ElMessage.warning('两次输入密码不一致'); return }
  passwordChanging.value = true
  try {
    await api('/user/password', { method: 'PUT', body: JSON.stringify({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword }) })
    passwordDialogVisible.value = false; passwordForm.oldPassword = ''; passwordForm.newPassword = ''; passwordForm.confirmPassword = ''
    ElMessage.success('登录密码已修改')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '密码修改失败') }
  finally { passwordChanging.value = false }
}

async function applyLoginResult(data: any) {
  token.value = data.accessToken; loggedInUser.value = data.user
  const role = normalizeRole(data.user?.role)
  isApplyingAuth.value = true; selectedRole.value = role; isApplyingAuth.value = false
  localStorage.setItem(TOKEN_KEY, token.value); localStorage.setItem(ROLE_KEY, selectedRole.value)
  if (data.firstLoginPrompted === false && role === 'superAdmin') firstLoginDialogVisible.value = true
  await refreshAll()
}

function logout() {
  firstLoginDialogVisible.value = false; clearAuthStorage()
  bookedSalonIds.value = []; bookedSalons.value = []; resources.splice(0, resources.length); blogs.splice(0, blogs.length)
  salons.splice(0, salons.length); teams.splice(0, teams.length); reviewQueue.splice(0, reviewQueue.length)
  salonAppointments.splice(0, salonAppointments.length); candidates.splice(0, candidates.length)
  navigateTo('dashboard'); ElMessage.success('已退出登录')
}

function goBack() { window.history.back() }
function handleUserCommand(command: string) { if (command === 'logout') { logout(); return }; if (command === 'profile') navigateTo('profile') }

async function refreshAll() {
  await Promise.all([
    loadOverview(), loadCreatorStats(), loadResources(), loadHotResources(), loadBlogs(), loadSalons(), loadBookings(), loadTeams(),
    isContentAdmin.value ? loadAdmin() : Promise.resolve(),
    isContentAdmin.value ? loadManagedUsers() : Promise.resolve(),
    isSuperAdmin.value ? loadAdminUsers() : Promise.resolve(),
  ])
}

async function restoreSession() {
  if (!token.value) return
  try {
    const user = await api<any>('/users/me')
    loggedInUser.value = user
    const role = normalizeRole(user.role)
    isApplyingAuth.value = true; selectedRole.value = role; isApplyingAuth.value = false
    localStorage.setItem(ROLE_KEY, role)
    await refreshAll()
  } catch (error) {
    handleSessionExpired()
  }
}

const routeByView: Record<string, string> = {
  dashboard: '/', resources: '/resources', blog: '/blogs', blogEditor: '/blogs/editor',
  blogDetail: '/blogs/detail', salon: '/salons', teams: '/teams', profile: '/profile',
  admin: '/admin', userManagement: '/admin/user-management', superAdmin: '/admin/permissions',
}
const viewByRoute = Object.fromEntries(Object.entries(routeByView).map(([view, path]) => [path, view]))
function viewFromLocation(pathname = window.location.pathname) {
  if (/^\/blogs\/detail\/\d+$/.test(pathname)) return 'blogDetail'
  return viewByRoute[pathname] || 'dashboard'
}
function blogDetailIdFromLocation(pathname = window.location.pathname) {
  const match = pathname.match(/^\/blogs\/detail\/(\d+)$/)
  return match ? Number(match[1]) : null
}
function pathForView(view: string) {
  if (view === 'blogDetail' && activeBlog.value?.id) return `/blogs/detail/${activeBlog.value.id}`
  return routeByView[view]
}
function allowedView(view: string) {
  if (view === 'superAdmin') return isSuperAdmin.value
  if (view === 'admin' || view === 'userManagement') return isContentAdmin.value
  return true
}
function navigateTo(view: string) {
  let targetView = routeByView[view] ? view : 'dashboard'
  if (!allowedView(targetView)) targetView = 'dashboard'
  activeView.value = targetView
  if (targetView === 'admin' && isContentAdmin.value) void loadAdmin()
  const nextPath = pathForView(targetView)
  if (window.location.pathname !== nextPath) window.history.pushState({ view: targetView }, '', nextPath)
}
function syncViewFromLocation() {
  const routeView = viewFromLocation()
  const nextView = allowedView(routeView) ? routeView : 'dashboard'
  activeView.value = nextView
  if (nextView === 'blogDetail') void restoreBlogDetailFromRoute()
  if (nextView !== routeView && window.location.pathname !== pathForView(nextView)) {
    window.history.replaceState({ view: nextView }, '', pathForView(nextView))
  }
}

function queryString(params: Record<string, string | number | undefined>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => { if (value !== undefined && value !== '') search.set(key, String(value)) })
  return search.toString()
}

async function loadResources() {
  const query = queryString({ page: resourcePage.current, size: resourcePage.size, keyword: resourceKeyword.value.trim(), contest: resourceContest.value, type: resourceType.value })
  const data = await api<any>(`/resources?${query}`)
  const records = Array.isArray(data) ? data : data.records
  resourcePage.total = Array.isArray(data) ? records.length : data.total; resourcePage.current = Array.isArray(data) ? resourcePage.current : data.current; resourcePage.size = Array.isArray(data) ? resourcePage.size : data.size
  selectedResourceIds.value = []
  resources.splice(0, resources.length, ...records.map((item: any) => ({ id: item.id, uploaderId: item.uploaderId, contest: item.contest, type: item.type, title: item.title, summary: item.summary, downloads: item.downloadCount, uploader: item.uploader })))
}
async function loadHotResources() {
  const data = await api<any[]>('/resources/hot-ranking')
  hotResources.splice(0, hotResources.length, ...data.map((item) => ({ id: item.id, title: item.title, contest: item.contest, type: item.type, downloads: item.downloads || 0 })))
}
async function loadOverview() {
  const data = await api<Record<string, number>>('/overview')
  metrics.forEach((metric) => { metric.value = String(data[metric.key] ?? 0) })
}
async function loadCreatorStats() {
  const data = await api<any>('/blogs/creator-center')
  creatorStats.totalLikes = data.totalLikes || 0; creatorStats.totalCollects = data.totalCollects || 0; creatorStats.followers = data.followers || 0; creatorStats.works = data.works || 0
}
async function loadBlogs() {
  const query = queryString({ page: blogPage.current, size: blogPage.size, keyword: blogKeyword.value.trim(), contestTag: blogContest.value, visibility: blogVisibility.value })
  const data = await api<any>(`/blogs?${query}`)
  const records = Array.isArray(data) ? data : data.records
  blogPage.total = Array.isArray(data) ? records.length : data.total; blogPage.current = Array.isArray(data) ? blogPage.current : data.current; blogPage.size = Array.isArray(data) ? blogPage.size : data.size
  blogs.splice(0, blogs.length, ...records.map(mapBlog))
}
async function loadSalons() {
  const query = queryString({ page: salonPage.current, size: salonPage.size })
  const data = await api<any>(`/salons?${query}`)
  const records = Array.isArray(data) ? data : data.records
  salonPage.total = Array.isArray(data) ? records.length : data.total; salonPage.current = Array.isArray(data) ? salonPage.current : data.current; salonPage.size = Array.isArray(data) ? salonPage.size : data.size
  salons.splice(0, salons.length, ...records.map(mapSalon))
}
async function loadBookings() {
  const data = await api<any[]>('/salons/me/bookings')
  bookedSalonIds.value = data.map((item) => item.salon.id)
  bookedSalons.value = data.map((item) => ({ ...mapSalon(item.salon), certificateCode: item.certificateCode }))
}
async function loadTeams() {
  const query = queryString({ page: teamPage.current, size: teamPage.size, keyword: teamKeyword.value.trim(), contest: teamContest.value })
  const data = await api<any>(`/teams?${query}`)
  const records = Array.isArray(data) ? data : data.records
  teamPage.total = Array.isArray(data) ? records.length : data.total; teamPage.current = Array.isArray(data) ? teamPage.current : data.current; teamPage.size = Array.isArray(data) ? teamPage.size : data.size
  teams.splice(0, teams.length, ...records.map(mapTeam))
}
async function loadTeamApplications() {
  const query = queryString({ page: teamApplicationPage.current, size: teamApplicationPage.size })
  const data = await api<any>(`/teams/applications/manageable?${query}`)
  const records = Array.isArray(data) ? data : data.records
  teamApplicationPage.total = Array.isArray(data) ? records.length : data.total; teamApplicationPage.current = Array.isArray(data) ? teamApplicationPage.current : data.current; teamApplicationPage.size = Array.isArray(data) ? teamApplicationPage.size : data.size
  selectedManageableApplicationIds.value = []; selectedManageableApplicationDeleteIds.value = []
  teamApplications.splice(0, teamApplications.length, ...records.map(mapTeamApplication))
}
async function loadTeamMembers() {
  const query = queryString({ page: teamMemberPage.current, size: teamMemberPage.size })
  const data = await api<any>(`/teams/members/manageable?${query}`)
  const records = Array.isArray(data) ? data : data.records
  teamMemberPage.total = Array.isArray(data) ? records.length : data.total; teamMemberPage.current = Array.isArray(data) ? teamMemberPage.current : data.current; teamMemberPage.size = Array.isArray(data) ? teamMemberPage.size : data.size
  manageableTeamMembers.splice(0, manageableTeamMembers.length, ...records.map(mapTeamMemberRecord))
}
async function loadAdmin() {
  const [reviews, pool, appointments] = await Promise.all([
    api<any>(`/admin/reviews?${queryString({ page: reviewPage.current, size: reviewPage.size })}`),
    api<any[]>('/admin/matchmaking'), api<any[]>('/admin/salon-appointments'),
    loadTeamApplications(),
  ])
  const reviewRecords = Array.isArray(reviews) ? reviews : reviews.records
  reviewPage.total = Array.isArray(reviews) ? reviewRecords.length : reviews.total; reviewPage.current = Array.isArray(reviews) ? reviewPage.current : reviews.current; reviewPage.size = Array.isArray(reviews) ? reviewPage.size : reviews.size
  selectedReviewIds.value = []
  reviewQueue.splice(0, reviewQueue.length, ...reviewRecords.map((item: any) => ({ id: item.id, type: typeLabel(item.targetType), targetType: item.targetType, targetId: item.targetId, title: item.title, owner: item.owner, status: statusLabel(item.status) })))
  candidates.splice(0, candidates.length, ...pool.map((item) => ({ id: item.userId, maskedName: item.maskedName, grade: '脱敏画像', intent: '待匹配', skills: item.skills })))
  salonAppointments.splice(0, salonAppointments.length, ...appointments.map((item) => ({ id: item.id, salonTitle: item.salonTitle, userNickname: item.userNickname, certificateCode: item.certificateCode, rawStatus: item.status, status: item.status === 'BOOKED' ? '已预约' : '已取消' })))
}

function mapBlog(item: any): BlogView {
  const publishedAt = item.createdAt || item.createdTime || item.publishTime || item.publishedAt || item.updatedAt
  return {
    id: item.id, authorId: item.authorId, title: item.title,
    author: typeof item.author === 'string' ? item.author : item.author?.nickname || '匿名用户',
    alias: '匿名用户', cert: Array.isArray(item.author?.badges) ? item.author.badges.join(' / ') : '',
    contestTag: item.contestTag || '经验分享', anonymous: item.anonymous, visibility: String(item.visibility).toLowerCase(),
    commentsOpen: item.commentsOpen, content: item.content,
    createdAt: publishedAt ? formatChineseDateTime(new Date(publishedAt)) : '刚刚发布',
    social: { likes: item.social?.likes || 0, collects: item.social?.collects || 0, liked: Boolean(item.social?.liked), collected: Boolean(item.social?.collected), followed: Boolean(item.social?.followed) },
    comments: (item.comments || []).map((comment: any) => ({ id: comment.id, author: comment.author, text: comment.content, replies: (comment.replies || []).map((reply: any) => ({ id: reply.id, author: reply.author, text: reply.content })) })),
  }
}

function mapSalon(item: any): SalonView {
  const date = new Date(item.startsAt)
  const month = Number.isNaN(date.getTime()) ? '本月' : `${date.getMonth() + 1}月`
  const day = Number.isNaN(date.getTime()) ? '--' : String(date.getDate()).padStart(2, '0')
  return { id: item.id, speakerId: item.speakerId, title: item.title, speaker: item.speaker || '特邀嘉宾', date: item.startsAtText || formatChineseDateTime(date), month, day, place: item.place, capacity: item.capacity, booked: item.booked, description: item.description }
}

function formatChineseDateTime(date: Date) {
  if (Number.isNaN(date.getTime())) return '日期待定'
  return date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).replaceAll('/', '-')
}

function mapTeam(item: any): TeamView {
  const leader = item.leader || {}
  const contact = leader.wechat && leader.wechat !== '双向确认前隐藏' ? `微信：${leader.wechat}` : leader.phone && leader.phone !== '双向确认前隐藏' ? `手机：${leader.phone}` : '双向确认前隐藏'
  const applications = (item.applications || []).map((entry: any) => {
    const application = entry.application || {}; const profile = entry.applicantProfile || {}; const skills = profile.skills || []
    return { id: application.id, teamId: item.id, leaderId: leader.id, teamTitle: item.title, applicant: profile.maskedName || application.applicantNickname || `用户${application.applicantId}`, applicantId: application.applicantId, skills, skillsText: skills.join('、') || '暂无标签', rawStatus: application.status, status: statusLabel(application.status) }
  })
  const members = (item.members || []).map((member: any) => {
    const skills = member.skills || []
    return { teamId: item.id, leaderId: leader.id, teamTitle: item.title, userId: member.userId, member: member.maskedName || member.nickname || `用户${member.userId}`, rawRole: member.role, role: member.role === 'LEADER' ? '队长' : '成员', skills, skillsText: skills.join('、') || '暂无标签' }
  })
  const pendingApplication = applications.find((a: TeamApplicationView) => a.rawStatus === 'PENDING')
  const anyApplication = applications.find((a: TeamApplicationView) => a.rawStatus === 'PENDING' || a.rawStatus === 'APPROVED')
  return { id: item.id, leaderId: leader.id, contest: item.contest, title: item.title, need: item.need, skills: item.skills || [], status: statusLabel(item.status), contact, applied: !!anyApplication, unlocked: contact !== '双向确认前隐藏', applicationId: pendingApplication?.id, applications, members }
}

function mapTeamApplication(item: any): TeamApplicationView {
  const application = item.application || {}; const team = item.team || {}; const profile = item.applicantProfile || {}; const skills = profile.skills || []
  return { id: application.id, teamId: application.teamId || team.id, leaderId: team.leaderId || item.leaderId || 0, teamTitle: team.title || item.teamTitle || '未知队伍', applicant: profile.maskedName || application.applicantNickname || `用户${application.applicantId}`, applicantId: application.applicantId, skills, skillsText: skills.join('、') || '暂无标签', rawStatus: application.status, status: statusLabel(application.status) }
}

function mapTeamMemberRecord(item: any): TeamMemberView {
  const team = item.team || {}; const member = item.member || {}; const skills = member.skills || []
  return { teamId: team.id || member.teamId, leaderId: team.leaderId || item.leaderId || 0, teamTitle: team.title || item.teamTitle || '未知队伍', userId: member.userId, member: member.maskedName || member.nickname || `用户${member.userId}`, rawRole: member.role, role: member.role === 'LEADER' ? '队长' : '成员', skills, skillsText: skills.join('、') || '暂无标签' }
}

function statusLabel(status: string) { return ({ APPROVED: '已通过', PENDING: '待审核', REJECTED: '已驳回' } as Record<string, string>)[status] || status }
function typeLabel(type: string) { return ({ resource: '资源', blog: '博客', salon: '沙龙', team: '组队' } as Record<string, string>)[type] || type }

async function downloadResource(resource: ResourceView) {
  await runExclusive(actionKey('resource-download', resource.id), async () => {
    let preparing: { close: () => void } | null = null
    let downloading: { close: () => void } | null = null
    try {
      preparing = ElMessage.info({ message: `正在准备下载「${resource.title}」，大文件请耐心等待...`, duration: 0 })
      const result = await api<{ downloadUrl: string; downloadCount: number; fileName?: string; fileSize?: number; contentType?: string }>(`/resources/${resource.id}/download`, { method: 'POST' })
      preparing.close(); preparing = null
      const sizeText = formatFileSize(result.fileSize)
      downloading = ElMessage.info({ message: `${sizeText ? `${sizeText} 文件` : '文件'}下载中，请不要重复点击`, duration: 0 })
      const response = await fetchWithTimeout(absoluteApiUrl(result.downloadUrl), { headers: token.value ? { Authorization: `Bearer ${token.value}` } : {} })
      if (!response.ok) { if (response.status === 401 || response.status === 403) throw handleSessionExpired(), Error('登录已过期，请重新登录'); throw Error(await response.text() || '下载失败') }
      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = downloadFileName(response, result.fileName || resource.title)
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
      window.setTimeout(() => URL.revokeObjectURL(url), 1000)
      resource.downloads = result.downloadCount; await loadHotResources()
      ElMessage.success(`已开始下载「${resource.title}」`)
    } catch (error) { ElMessage.error(error instanceof Error ? error.message : '下载失败') }
    finally { preparing?.close(); downloading?.close() }
  })
}

function downloadFileName(response: Response, fallback: string) {
  const disposition = response.headers.get('Content-Disposition') || response.headers.get('content-disposition') || ''
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  if (encoded) {
    try { return decodeURIComponent(encoded) } catch {}
  }
  const plain = disposition.match(/filename="?([^";]+)"?/i)?.[1]
  return plain || fallback
}
function canManageResource(resource: ResourceView) { return isContentAdmin.value || resource.uploaderId === currentUser.value.id }
function openResourceEditor(resource: ResourceView) { resourceEditor.id = resource.id; resourceEditor.title = resource.title; resourceEditor.contest = resource.contest; resourceEditor.type = resource.type; resourceEditor.summary = resource.summary; resourceEditFile.value = null; resourceEditorVisible.value = true }
function handleResourceEditFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  resourceEditFile.value = input.files?.[0] || null
}
async function saveResourceEdit() {
  if (!resourceEditor.id) return
  await runExclusive('resource-edit', async () => {
    try {
      const formData = new FormData()
      formData.append('title', resourceEditor.title)
      formData.append('contest', resourceEditor.contest)
      formData.append('type', resourceEditor.type)
      formData.append('summary', resourceEditor.summary)
      if (resourceEditFile.value) formData.append('file', resourceEditFile.value)
      const response = await fetch(`${API_BASE}/resources/${resourceEditor.id}/upload`, {
        method: 'PUT',
        headers: token.value ? { Authorization: `Bearer ${token.value}` } : {},
        body: formData,
      })
      if (!response.ok) { if (response.status === 401 || response.status === 403) throw handleSessionExpired(), Error('登录已过期，请重新登录'); throw Error(await response.text() || '编辑资源失败') }
      resourceEditorVisible.value = false; resourceEditFile.value = null; await Promise.all([loadResources(), loadOverview()])
      ElMessage.success('资源已更新')
    } catch (error) { ElMessage.error(error instanceof Error ? error.message : '编辑资源失败，只有上传者或管理员可以编辑') }
  })
}

async function deleteResource(resource: ResourceView) {
  await runExclusive(actionKey('resource-delete', resource.id), async () => {
    try {
      await api(`/resources/${resource.id}`, { method: 'DELETE' })
      await Promise.all([loadResources(), loadHotResources(), loadOverview(), isContentAdmin.value ? loadAdmin() : Promise.resolve()])
      ElMessage.success(`资源已删除：${resource.title}`)
    } catch (error) { ElMessage.error(error instanceof Error ? error.message : '删除资源失败，只有上传者或管理员可以删除') }
  })
}
async function deleteSelectedResources() {
  if (selectedResourceIds.value.length === 0) return
  try {
    await confirmAction(`确定要批量删除选中的 ${selectedResourceIds.value.length} 个资源吗？`)
    await api('/resources/batch', { method: 'DELETE', body: JSON.stringify({ ids: selectedResourceIds.value }) })
    await Promise.all([loadResources(), loadOverview(), isContentAdmin.value ? loadAdmin() : Promise.resolve()])
    ElMessage.success('已批量删除所选资源')
  } catch (error) { if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '批量删除资源失败') }
}
function changeResourcePage(page: number) { resourcePage.current = page; void loadResources() }
function changeResourceSize(size: number) { resourcePage.size = size; resourcePage.current = 1; void loadResources() }
function changeBlogPage(page: number) { blogPage.current = page; void loadBlogs() }
function changeBlogSize(size: number) { blogPage.size = size; blogPage.current = 1; void loadBlogs() }
function changeSalonPage(page: number) { salonPage.current = page; void loadSalons() }
function changeSalonSize(size: number) { salonPage.size = size; salonPage.current = 1; void loadSalons() }
function changeTeamPage(page: number) { teamPage.current = page; void loadTeams() }
function changeTeamSize(size: number) { teamPage.size = size; teamPage.current = 1; void loadTeams() }
function changeTeamApplicationPage(page: number) { teamApplicationPage.current = page; void loadTeamApplications() }
function changeTeamApplicationSize(size: number) { teamApplicationPage.size = size; teamApplicationPage.current = 1; void loadTeamApplications() }
function changeTeamMemberPage(page: number) { teamMemberPage.current = page; void loadTeamMembers() }
function changeTeamMemberSize(size: number) { teamMemberPage.size = size; teamMemberPage.current = 1; void loadTeamMembers() }

function openResourceDialog() {
  const suffix = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  resourceForm.title = `蓝桥杯备赛资料${suffix}`
  resourceForm.contest = resourceContest.value || '蓝桥杯'
  resourceForm.type = resourceType.value || '学习笔记'
  resourceForm.summary = '包含历年真题解析和算法模板，适合正在备考的同学参考。'
  resourceFile.value = null
  resourceDialogVisible.value = true
}
function handleResourceFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  resourceFile.value = input.files?.[0] || null
}
async function submitResource() {
  if (!resourceForm.title.trim() || !resourceForm.contest.trim() || !resourceForm.type.trim() || !resourceForm.summary.trim() || !resourceFile.value) {
    ElMessage.warning('请填写完整的资源信息')
    return
  }
  await runExclusive('resource-submit', async () => {
    try {
      const formData = new FormData()
      formData.append('title', resourceForm.title.trim())
      formData.append('contest', resourceForm.contest.trim())
      formData.append('type', resourceForm.type.trim())
      formData.append('summary', resourceForm.summary.trim())
      formData.append('file', resourceFile.value as File)
      const response = await fetch(`${API_BASE}/resources/upload`, {
        method: 'POST',
        headers: token.value ? { Authorization: `Bearer ${token.value}` } : {},
        body: formData,
      })
      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          handleSessionExpired()
          throw new Error('登录已过期，请重新登录')
        }
        throw new Error(await response.text() || '创建资源失败')
      }
      resourceDialogVisible.value = false
      resourceFile.value = null
      await Promise.all([loadResources(), loadOverview(), isContentAdmin.value ? loadAdmin() : Promise.resolve()])
      ElMessage.success(isContentAdmin.value ? '资源已发布' : '资源已提交审核，等待管理员审核通过后公开')
    } catch (error) { ElMessage.error(error instanceof Error ? error.message : '创建资源失败') }
  })
}


function insertBlogImage() {
  const imageUrl = draftBlog.imageUrl.trim() || '/system-map.png'
  draftBlog.imageUrl = imageUrl
  if (!draftBlog.content.includes(imageUrl)) draftBlog.content = `${draftBlog.content.trim()}\n\n![竞赛经验图](${imageUrl})`
  ElMessage.success('图片 Markdown 已插入')
}
function triggerBlogImageInput() { blogImageInputRef.value?.click() }
async function handleBlogImageUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
  blogImageUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const response = await fetch(`${API_BASE}/blogs/upload-image`, {
      method: 'POST',
      headers: token.value ? { Authorization: `Bearer ${token.value}` } : {},
      body: formData,
    })
    if (!response.ok) { if (response.status === 401 || response.status === 403) throw handleSessionExpired(), Error('登录已过期，请重新登录'); throw Error(await response.text() || '图片上传失败') }
    const data = await response.json()
    draftBlog.imageUrl = data.url
    if (!draftBlog.content.includes(data.url)) draftBlog.content = `${draftBlog.content.trim()}\n\n![${file.name}](${data.url})`
    ElMessage.success('图片已上传并插入')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '图片上传失败')
  } finally {
    blogImageUploading.value = false
  }
}

function resetDraftBlog() {
  editingBlogId.value = null; blogEditorMode.value = 'markdown'
  draftBlog.title = '我的算法学习笔记'; draftBlog.contestTag = '蓝桥杯'
  draftBlog.content = '## 刷题心得\n\n- 先理解题意再编码\n- 注意边界条件处理\n- 多练习常见题型\n\n> 坚持每天刷题，保持手感很重要。'
  draftBlog.imageUrl = ''; draftBlog.visibility = 'public'; draftBlog.anonymous = false; draftBlog.commentsOpen = true
}

function canEditBlog(post: BlogView) { return isContentAdmin.value || post.authorId === currentUser.value.id }
function hasBlogSocialStats(post: BlogView) { return post.social.likes > 0 || post.social.collects > 0 }
function openBlogEditor(post?: BlogView) {
  if (post) { editingBlogId.value = post.id; draftBlog.title = post.title; draftBlog.contestTag = post.contestTag || '经验分享'; draftBlog.content = post.content; draftBlog.imageUrl = ''; draftBlog.visibility = post.visibility; draftBlog.anonymous = post.anonymous; draftBlog.commentsOpen = post.commentsOpen }
  else { resetDraftBlog() }
  navigateTo('blogEditor')
}
function goToBlogDetail(post: BlogView) { void openBlogDetail(post) }
async function openBlogDetail(post: BlogView) {
  try {
    const data = await api<any>(`/blogs/${post.id}`)
    activeBlog.value = mapBlog(data)
    activeView.value = 'blogDetail'
    const nextPath = `/blogs/detail/${post.id}`
    if (window.location.pathname !== nextPath) window.history.pushState({ view: 'blogDetail', id: post.id }, '', nextPath)
  }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '查看博客详情失败') }
}
async function restoreBlogDetailFromRoute() {
  const id = blogDetailIdFromLocation()
  if (!id || activeBlog.value?.id === id) return
  try {
    const data = await api<any>(`/blogs/${id}`)
    activeBlog.value = mapBlog(data)
  } catch (error) {
    activeBlog.value = null
    ElMessage.error(error instanceof Error ? error.message : '查看博客详情失败')
  }
}
async function publishBlog() {
  await runExclusive('blog-publish', async () => {
    try {
      const imageUrl = draftBlog.imageUrl.trim()
      const content = imageUrl && !draftBlog.content.includes(imageUrl) ? `${draftBlog.content.trim()}\n\n![竞赛经验图](${imageUrl})` : draftBlog.content
      const path = editingBlogId.value ? `/blogs/${editingBlogId.value}` : '/blogs'
      await api(path, { method: editingBlogId.value ? 'PUT' : 'POST', body: JSON.stringify({ title: draftBlog.title, content, contestTag: draftBlog.contestTag, anonymous: draftBlog.anonymous, visibility: draftBlog.visibility.toUpperCase(), commentsOpen: draftBlog.commentsOpen }) })
      await Promise.all([loadBlogs(), loadOverview(), isContentAdmin.value ? loadAdmin() : Promise.resolve()])
      navigateTo('blog'); ElMessage.success(isContentAdmin.value ? '博客已发布' : '博客已提交审核，等待管理员审核通过后公开')
    } catch (error) { ElMessage.error(error instanceof Error ? error.message : '发布博客失败') }
  })
}

async function deleteBlog(post: BlogView) {
  try {
    await confirmAction(`确定要删除博客「${post.title}」吗？`)
    await api(`/blogs/${post.id}`, { method: 'DELETE' })
    if (activeBlog.value?.id === post.id) { activeBlog.value = null; navigateTo('blog') }
    await Promise.all([loadBlogs(), loadOverview(), isContentAdmin.value ? loadAdmin() : Promise.resolve()])
    ElMessage.success('博客已删除')
  } catch (error) { if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除博客失败') }
}
async function toggleBlogAction(type: 'LIKE' | 'COLLECT') {
  if (!activeBlog.value) return; const pendingKey = type === 'LIKE' ? 'like' : 'collect'
  if (socialPending[pendingKey]) return; socialPending[pendingKey] = true
  try {
    const data = await api<any>(`/blogs/${activeBlog.value.id}/actions`, { method: 'POST', body: JSON.stringify({ type }) })
    activeBlog.value.social = mapSocial(data.stats); await Promise.all([loadBlogs(), loadCreatorStats()])
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '操作失败') }
  finally { socialPending[pendingKey] = false }
}
async function toggleFollowAuthor() {
  if (!activeBlog.value || activeBlog.value.authorId === currentUser.value.id || socialPending.follow) return
  socialPending.follow = true
  try {
    const data = await api<any>(`/blogs/${activeBlog.value.id}/follow-author`, { method: 'POST' })
    activeBlog.value.social.followed = Boolean(data.active); await loadCreatorStats()
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '关注失败') }
  finally { socialPending.follow = false }
}
function mapSocial(source: any): BlogSocialView {
  return { likes: source?.likes || 0, collects: source?.collects || 0, liked: Boolean(source?.liked), collected: Boolean(source?.collected), followed: Boolean(source?.followed) }
}
async function addComment(post: { id: number; comments: BlogComment[] }) {
  const text = commentDrafts[post.id]?.trim()
  if (!text) return
  try {
    await api(`/blogs/${post.id}/comments`, { method: 'POST', body: JSON.stringify({ content: text, anonymous: false }) })
    commentDrafts[post.id] = ''
    await Promise.all([loadBlogs(), activeBlog.value?.id === post.id ? api<any>(`/blogs/${post.id}`).then((data) => { activeBlog.value = mapBlog(data) }) : Promise.resolve()])
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '评论失败') }
}

function salonPercent(salon: SalonView) { return Math.round(salon.booked / salon.capacity * 100) }
function openSalonDialog() {
  const startsAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16)
  salonForm.title = '大三年级算法竞赛沙龙'
  salonForm.description = '邀请同学分享竞赛路线与刷题规划经验。'
  salonForm.startsAt = startsAt
  salonForm.place = '教学楼 A204'
  salonForm.capacity = 36
  salonDialogVisible.value = true
}
async function submitSalon() {
  if (!salonForm.title.trim() || !salonForm.description.trim() || !salonForm.startsAt || !salonForm.place.trim() || !salonForm.capacity) {
    ElMessage.warning('请填写完整的沙龙信息')
    return
  }
  await runExclusive('salon-submit', async () => {
    try {
      await api('/salons', { method: 'POST', body: JSON.stringify({ ...salonForm }) })
      salonDialogVisible.value = false
      await Promise.all([loadSalons(), loadOverview(), isContentAdmin.value ? loadAdmin() : Promise.resolve()])
      ElMessage.success(isContentAdmin.value ? '沙龙已发布，已开放预约' : '沙龙已提交审核，等待管理员审核通过后开放预约')
    } catch (error) { ElMessage.error(error instanceof Error ? error.message : '创建沙龙失败') }
  })
}

async function bookSalon(salon: SalonView) {
  if (salon.booked >= salon.capacity || bookedSalonIds.value.includes(salon.id)) return
  await runExclusive(actionKey('salon-book', salon.id), async () => {
    try {
      await api(`/salons/${salon.id}/book`, { method: 'POST' })
      await Promise.all([loadSalons(), loadBookings()])
      ElMessage.success(`预约成功！「${salon.title}」凭证已生成，请在个人中心查看`)
    } catch (error) { ElMessage.error(error instanceof Error ? error.message : '预约失败') }
  })
}

function canManageSalon(salon: SalonView) { return isContentAdmin.value || salon.speakerId === currentUser.value.id }
async function deleteSalon(salon: SalonView) {
  try {
    await confirmAction(`确定要删除沙龙「${salon.title}」吗？`)
    await api(`/salons/${salon.id}`, { method: 'DELETE' })
    await Promise.all([loadSalons(), loadBookings(), loadOverview(), isContentAdmin.value ? loadAdmin() : Promise.resolve()])
    ElMessage.success('沙龙已删除')
  } catch (error) { if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除沙龙失败') }
}

async function publishTeam() {
  await runExclusive('team-publish', async () => {
    try {
      await api('/teams', { method: 'POST', body: JSON.stringify({ title: draftTeam.title, contest: '自定义竞赛', need: draftTeam.need, skills: [...draftTeam.skills] }) })
      await Promise.all([loadTeams(), loadOverview(), isContentAdmin.value ? loadAdmin() : Promise.resolve()])
      ElMessage.success(isContentAdmin.value ? '组队卡片已发布，联系方式默认隐藏' : '组队卡片已提交审核，等待管理员审核')
    } catch (error) { ElMessage.error(error instanceof Error ? error.message : '发布组队失败') }
  })
}

async function applyTeam(team: TeamView) {
  await runExclusive(actionKey('team-apply', team.id), async () => {
    try {
      const data = await api<any>(`/teams/${team.id}/apply`, { method: 'POST' })
      team.applied = true; team.applicationId = data.application?.id; teamStep.value = 1; await loadTeams()
      ElMessage.info('申请已发送，等待队长同意后联系方式才会解锁')
    } catch (error) { ElMessage.error(error instanceof Error ? error.message : '申请失败') }
  })
}

async function approveTeam(team: TeamView) {
  if (!team.applicationId) return
  await runExclusive(actionKey('team-approve', team.applicationId), async () => {
    try {
      await api(`/teams/${team.id}/applications/${team.applicationId}/approve`, { method: 'POST' })
      await Promise.all([loadTeams(), loadOverview()]); teamStep.value = 3
      ElMessage.success('队长已同意，系统已双向解锁联系方式')
    } catch (error) { ElMessage.error(error instanceof Error ? error.message : '同意失败，请检查队长身份') }
  })
}

async function approveTeamApplication(application: TeamApplicationView) {
  await runExclusive(actionKey('team-application-approve', application.id), async () => {
    try {
      await api(`/teams/${application.teamId}/applications/${application.id}/approve`, { method: 'POST' })
      await Promise.all([loadTeams(), loadOverview()]); teamStep.value = 3
      ElMessage.success('已同意加入，双方联系方式已解锁')
    } catch (error) { ElMessage.error(error instanceof Error ? error.message : '同意失败，请检查队长身份') }
  })
}

function selectManageableApplications(rows: any[]) { selectedManageableApplicationIds.value = rows.filter(r => r.rawStatus === 'PENDING').map(r => r.id); selectedManageableApplicationDeleteIds.value = rows.map(r => r.id) }
function selectAdminApplications(rows: any[]) { selectedAdminApplicationIds.value = rows.filter(r => r.rawStatus === 'PENDING').map(r => r.id); selectedAdminApplicationDeleteIds.value = rows.map(r => r.id) }
async function approveSelectedManageableApplications() { await batchApproveApplications(selectedManageableApplicationIds.value, '我的申请') }
async function approveSelectedAdminApplications() { await batchApproveApplications(selectedAdminApplicationIds.value, '后台申请') }
async function batchApproveApplications(ids: number[], label: string) {
  if (ids.length === 0) return
  try {
    await confirmAction(`确定要批量同意选中的 ${ids.length} 条${label}吗？`)
    await api('/teams/applications/batch-approve', { method: 'POST', body: JSON.stringify({ applicationIds: ids }) })
    await Promise.all([loadTeams(), loadOverview()])
    selectedManageableApplicationIds.value = []; selectedManageableApplicationDeleteIds.value = []; selectedAdminApplicationIds.value = []; selectedAdminApplicationDeleteIds.value = []
    teamStep.value = 3; ElMessage.success('已批量同意申请，双方联系方式已解锁')
  } catch (error) { if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '批量同意失败') }
}
async function rejectTeamApplication(application: TeamApplicationView) { await batchRejectApplications([application.id], '申请') }
async function rejectSelectedManageableApplications() { await batchRejectApplications(selectedManageableApplicationIds.value, '申请') }
async function batchRejectApplications(ids: number[], label: string) {
  if (ids.length === 0) return
  try {
    await confirmAction(`确定要批量拒绝选中的 ${ids.length} 条${label}吗？`)
    await api('/teams/applications/batch-reject', { method: 'POST', body: JSON.stringify({ applicationIds: ids }) })
    await Promise.all([loadTeams()]); selectedManageableApplicationIds.value = []; ElMessage.success('已批量拒绝申请')
  } catch (error) { if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '批量拒绝失败') }
}
async function deleteTeamApplication(application: TeamApplicationView) { await batchDeleteApplications([application.id], '申请') }
async function deleteSelectedManageableApplications() { await batchDeleteApplications(selectedManageableApplicationDeleteIds.value, '申请') }
async function deleteSelectedAdminApplications() { await batchDeleteApplications(selectedAdminApplicationDeleteIds.value, '后台申请') }
async function batchDeleteApplications(ids: number[], label: string) {
  if (ids.length === 0) return
  try {
    await confirmAction(`确定要删除选中的 ${ids.length} 条${label}记录？删除后不可恢复。`)
    await api('/teams/applications/batch', { method: 'DELETE', body: JSON.stringify({ applicationIds: ids }) })
    await Promise.all([loadTeams(), loadOverview()])
    selectedManageableApplicationIds.value = []; selectedManageableApplicationDeleteIds.value = []; selectedAdminApplicationIds.value = []; selectedAdminApplicationDeleteIds.value = []
    ElMessage.success('已删除所选申请记录')
  } catch (error) { if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除申请记录失败') }
}
function openTeamEditor(team: TeamView) { teamEditor.id = team.id; teamEditor.title = team.title; teamEditor.contest = team.contest; teamEditor.need = team.need; teamEditor.skills = [...team.skills]; teamEditorVisible.value = true }
function canManageTeam(team: TeamView) { return isContentAdmin.value || team.leaderId === currentUser.value.id }
async function saveTeamEdit() {
  if (!teamEditor.id) return
  try {
    await api(`/teams/${teamEditor.id}`, { method: 'PUT', body: JSON.stringify({ title: teamEditor.title, contest: teamEditor.contest, need: teamEditor.need, skills: [...teamEditor.skills] }) })
    teamEditorVisible.value = false; await Promise.all([loadTeams(), loadOverview()]); ElMessage.success('队伍信息已更新')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '更新失败，请检查队长身份') }
}
async function deleteTeam(team: TeamView) {
  try {
    await api(`/teams/${team.id}`, { method: 'DELETE' })
    await Promise.all([loadTeams(), loadOverview()]); ElMessage.success('队伍已删除')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '删除失败，请检查队长身份') }
}
async function removeTeamMember(member: TeamMemberView) {
  try {
    await api(`/teams/${member.teamId}/members/${member.userId}`, { method: 'DELETE' })
    await Promise.all([loadTeams()]); ElMessage.success('已将该队员移除')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '移除队员失败，请检查队长身份') }
}
async function cancelSalonAppointment(appointment: SalonAppointmentView) {
  try {
    await api(`/admin/salon-appointments/${appointment.id}/cancel`, { method: 'PATCH' })
    await Promise.all([loadAdmin(), loadSalons(), loadBookings()]); ElMessage.success('已取消预约，释放名额')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '取消预约失败') }
}
async function deleteSalonAppointment(appointment: SalonAppointmentView) {
  try {
    await api(`/admin/salon-appointments/${appointment.id}`, { method: 'DELETE' }); await loadAdmin(); ElMessage.success('预约记录已删除')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '删除预约记录失败') }
}
async function exportSalonAppointments() {
  try {
    const response = await fetch(`${API_BASE}/salon/appointments/export`, { headers: { ...(token.value ? { Authorization: `Bearer ${token.value}` } : {}) } })
    if (!response.ok) { if (response.status === 401) throw handleSessionExpired(), Error('登录已过期，请重新登录'); const text = await response.text(); throw Error(text || '导出预约失败') }
    const blob = await response.blob(); const url = URL.createObjectURL(blob); const anchor = document.createElement('a')
    anchor.href = url; anchor.download = `沙龙预约数据-${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(anchor); anchor.click(); anchor.remove(); URL.revokeObjectURL(url); ElMessage.success('预约数据已导出')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '导出预约数据失败') }
}
async function reviewItem(row: ReviewView, status: 'APPROVED' | 'REJECTED') {
  const key = actionKey(status === 'APPROVED' ? 'review-approve' : 'review-reject', row.id)
  await runExclusive(key, async () => {
    try {
      await api(`/admin/reviews/${row.id}`, { method: 'PATCH', body: JSON.stringify({ status }) })
      await Promise.all([loadAdmin(), loadResources(), loadBlogs(), loadSalons(), loadTeams(), loadOverview()])
      ElMessage.success(`审核状态已更新为：${status === 'APPROVED' ? '已通过' : '已驳回'}`)
    } catch (error) { ElMessage.error(error instanceof Error ? error.message : '审核失败') }
  })
}

function selectReviews(rows: any[]) { selectedReviewIds.value = rows.map(r => r.id) }
async function deleteSelectedReviews() {
  if (selectedReviewIds.value.length === 0) return
  try {
    await confirmAction(`确定要批量删除选中的 ${selectedReviewIds.value.length} 条审核记录吗？`)
    await api('/admin/reviews/batch-delete', { method: 'POST', body: JSON.stringify({ ids: selectedReviewIds.value }) })
    await loadAdmin(); ElMessage.success('已批量删除选中审核记录')
  } catch (error) { if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '批量删除审核记录失败') }
}
async function reviewSelected(status: 'APPROVED' | 'REJECTED') {
  if (selectedReviewIds.value.length === 0) return; const label = status === 'APPROVED' ? '批量通过' : '批量驳回'
  try {
    await confirmAction(`确定要${label}选中的 ${selectedReviewIds.value.length} 条记录吗？`)
    await api('/admin/reviews/batch', { method: 'PATCH', body: JSON.stringify({ ids: selectedReviewIds.value, status }) })
    await Promise.all([loadAdmin(), loadResources(), loadBlogs(), loadSalons(), loadTeams(), loadOverview()]); ElMessage.success(`已${label}`)
  } catch (error) { if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : `${label}失败`) }
}
function changeReviewPage(page: number) { reviewPage.current = page; loadAdmin() }

async function transferSuperAdmin(userId: number) {
  try {
    await confirmAction('确定要转让超级管理员权限吗？转让后您将不再是超级管理员。')
    await api(`/admin/super/users/${userId}/transfer`, { method: 'POST' })
    selectedRole.value = 'admin'
    currentUser.value.role = 'admin'
    localStorage.setItem(ROLE_KEY, selectedRole.value)
    sessionStorage.setItem(ROLE_KEY, selectedRole.value)
    navigateTo('admin')
    await Promise.all([loadAdmin(), loadManagedUsers(), loadAdminUsers()])
    ElMessage.success('超级管理员权限已转让，当前账号已切换为管理员视图')
  } catch (error) { if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '转让失败') }
}



async function loadManagedUsers() {
  if (!isContentAdmin.value) return
  managedUsersLoading.value = true
  try {
    const query = queryString({ page: managedUserPage.current, size: managedUserPage.size, keyword: managedUserKeyword.value.trim(), role: managedUserRole.value })
    const data = await api<any>(`/users/page?${query}`)
    const records = Array.isArray(data) ? data : data.records
    managedUserPage.total = Array.isArray(data) ? records.length : data.total
    managedUserPage.current = Array.isArray(data) ? managedUserPage.current : data.current
    managedUserPage.size = Array.isArray(data) ? managedUserPage.size : data.size
    managedUserSelection.value = []
    managedUsers.value = records.map(mapManagedUser)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '加载用户列表失败') }
  finally { managedUsersLoading.value = false }
}
function mapManagedUser(item: any): ManagedUserView {
  return { id: item.id, studentNo: item.studentNo || '-', username: item.username || '-', nickname: item.nickname || '-', email: item.email || '-', phone: item.phone || '-', role: item.role || 'STUDENT', createdAt: item.createdAt ? formatChineseDateTime(new Date(item.createdAt)) : '-' }
}
function handleManagedUserSelectionChange(rows: ManagedUserView[]) { managedUserSelection.value = rows }
function canDeleteManagedUser(row: ManagedUserView) { return row.id !== currentUser.value.id && row.role !== 'ADMIN' && row.role !== 'SUPER_ADMIN' }
function managedUserRoleLabel(role: string) { if (role === 'SUPER_ADMIN') return '超级管理员'; if (role === 'ADMIN') return '管理员'; if (role === 'MENTOR') return '特邀专家'; return '普通学生' }
function managedUserRoleTag(role: string) { if (role === 'SUPER_ADMIN') return 'danger'; if (role === 'ADMIN') return 'warning'; if (role === 'MENTOR') return 'success'; return 'info' }
async function deleteManagedUser(row: ManagedUserView) { await deleteManagedUsers([row.id], `确定要删除用户「${row.nickname || row.username}」吗？`) }
async function deleteSelectedManagedUsers() { const ids = managedUserSelection.value.map(row => row.id); await deleteManagedUsers(ids, `此操作将永久删除选中的 ${ids.length} 位用户，是否继续？`, true) }
async function deleteManagedUsers(ids: number[], message: string, batch = false) {
  if (ids.length === 0) return
  if (ids.includes(currentUser.value.id)) { ElMessage.warning('不能删除当前登录的账号！'); return }
  try {
    await confirmAction(message)
    if (batch) await api('/users/batch', { method: 'DELETE', body: JSON.stringify(ids) })
    else await api(`/users/${ids[0]}`, { method: 'DELETE' })
    await loadManagedUsers()
    ElMessage.success(batch ? '批量删除成功' : '用户已删除')
  } catch (error) { if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除用户失败') }
}
function changeManagedUserPage(page: number) { managedUserPage.current = page; void loadManagedUsers() }
function changeManagedUserSize(size: number) { managedUserPage.size = size; managedUserPage.current = 1; void loadManagedUsers() }

async function loadAdminUsers() {
  adminUsersLoading.value = true
  try {
    const data = await api<any[]>('/admin/users'); adminUsers.value = data
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '加载管理员列表失败') }
  finally { adminUsersLoading.value = false }
}
function openNewAdminDialog() { newAdminForm.studentNo = ''; newAdminForm.username = ''; newAdminDialogVisible.value = true }
async function createAdmin() {
  if (!newAdminForm.studentNo || !newAdminForm.username) { ElMessage.warning('请填写学号和用户名'); return }
  try {
    await api('/admin/users', { method: 'POST', body: JSON.stringify(newAdminForm) }); newAdminDialogVisible.value = false; await loadAdminUsers(); ElMessage.success('管理员添加成功')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '新增管理员失败') }
}
async function removeAdminUser(row: any) {
  try {
    await confirmAction(`确定要取消管理员「${row.nickname || row.username}」的管理员权限吗？`)
    await api(`/admin/users/${row.id}`, { method: 'DELETE' }); await loadAdminUsers(); ElMessage.success('已取消该用户管理员权限')
  } catch (error) { if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '操作失败') }
}

function triggerMdFileInput() { mdFileInputRef.value?.click() }
function handleMdDrop(event: DragEvent) {
  const files = Array.from(event.dataTransfer?.files || [])
  const file = findBlogImportFile(files)
  if (file) { uploadMdFile(file, files.filter(item => item !== file)) }
  else { ElMessage.warning('请上传 .md、.txt、.docx 或 .pdf 文件') }
}
function handleMdFileChange(event: Event) {
  const input = event.target as HTMLInputElement; const files = Array.from(input.files || [])
  const file = findBlogImportFile(files)
  if (file) uploadMdFile(file, files.filter(item => item !== file))
  else if (files.length > 0) ElMessage.warning('请至少选择一个 .md、.txt、.docx 或 .pdf 文件')
  input.value = ''
}
function findBlogImportFile(files: File[]) {
  return files.find(file => /\.(md|txt|docx|pdf)$/i.test(file.name))
}
async function uploadMdFile(file: File, attachments: File[] = []) {
  mdUploading.value = true
  try {
    const formData = new FormData(); formData.append('file', file)
    attachments.filter(item => item.type.startsWith('image/') || /\.(png|jpe?g|gif|webp|bmp)$/i.test(item.name)).forEach(item => formData.append('files', item))
    const response = await fetch(`${API_BASE}/blog/upload-md`, {
      method: 'POST', headers: token.value ? { Authorization: `Bearer ${token.value}` } : {}, body: formData,
    })
    if (!response.ok) { const text = await response.text(); throw new Error(text || '解析文件失败') }
    const data = await response.json()
    if (data.title) draftBlog.title = data.title
    if (data.content) draftBlog.content = data.content
    blogEditorMode.value = file.name.toLowerCase().endsWith('.md') ? 'markdown' : 'text'
    blogEditorTab.value = 'write'
    window.requestAnimationFrame(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
    ElMessage.success(`已成功导入「${data.fileName || file.name}」，标题内容和关联图片已自动填充`)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '上传文件失败') }
  finally { mdUploading.value = false }
}

async function acknowledgeFirstLogin() {
  try { await api('/auth/first-login-acknowledged', { method: 'POST' }); firstLoginDialogVisible.value = false; ElMessage.success('首次登录提示已关闭') }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '操作失败') }
}

onMounted(() => {
  if (window.location.pathname === '/login' && !isLoggedIn.value) {
    window.history.replaceState({ view: 'login' }, '', '/login')
    window.addEventListener('popstate', syncViewFromLocation); return
  }
  syncViewFromLocation()
  window.history.replaceState({ view: activeView.value }, '', pathForView(activeView.value))
  window.addEventListener('popstate', syncViewFromLocation)
  void restoreSession()
})
onUnmounted(() => { window.removeEventListener('popstate', syncViewFromLocation) })
</script>
