<template>
  <main class="shell">
    <iframe ref="legacyFrame" class="legacy-frame" src="/legacy/index.html" title="学科竞赛隐私组队系统" @load="onFrameLoad" />

    <el-button v-if="isSuperAdmin" class="float-btn admin-btn" type="primary" :icon="Setting" circle @click="openAdminPanel" />
    <el-button class="float-btn import-btn" type="success" :icon="Upload" circle @click="markdownPanelVisible = true" />

    <el-drawer v-model="panelVisible" title="管理员权限管理" size="820px" append-to-body @open="loadAdmins">
      <div class="drawer-body">
        <div class="toolbar">
          <el-input v-model="keyword" clearable placeholder="搜索学号、昵称、邮箱、手机号" class="search" />
          <el-button type="primary" :icon="Plus" @click="openCreateDialog">添加新管理员</el-button>
          <el-button :icon="Refresh" :loading="loading" @click="loadAdmins">刷新</el-button>
        </div>

        <el-table v-loading="loading" :data="filteredAdmins" stripe height="560">
          <el-table-column prop="studentNo" label="学号/工号" width="150" />
          <el-table-column prop="nickname" label="昵称" min-width="140" />
          <el-table-column prop="email" label="邮箱" min-width="190" />
          <el-table-column prop="phone" label="手机号" min-width="140" />
          <el-table-column label="角色" width="130">
            <template #default="{ row }">
              <el-tag :type="row.role === 'SUPER_ADMIN' ? 'danger' : 'warning'">
                {{ row.role === 'SUPER_ADMIN' ? '超级管理员' : '管理员' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="注册时间" width="180" />
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain :disabled="row.role === 'SUPER_ADMIN'" @click="openEditDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" plain :disabled="row.role === 'SUPER_ADMIN'" @click="confirmRemoveAdmin(row)">取消管理员权限</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-drawer v-model="markdownPanelVisible" title="Markdown 文件导入" size="720px" append-to-body>
      <div class="drawer-body">
        <el-upload
          drag
          accept=".md"
          :show-file-list="false"
          :http-request="uploadMarkdownFile"
          class="md-upload"
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">支持拖拽或点击上传本地 .md 文件，自动解析内容</div>
          <div class="upload-hint">解析后将自动回填标题与正文到当前博客编辑器</div>
        </el-upload>

        <el-alert v-if="markdownFileName" :title="`已解析：${markdownFileName}`" type="success" :closable="false" show-icon class="mt-12" />

        <el-form label-position="top" class="mt-12">
          <el-form-item label="标题">
            <el-input v-model="markdownDraft.title" placeholder="解析后自动填充" />
          </el-form-item>
          <el-form-item label="正文预览">
            <el-input v-model="markdownDraft.content" type="textarea" :rows="18" placeholder="Markdown 内容预览" />
          </el-form-item>
        </el-form>

        <div class="drawer-actions">
          <el-button :icon="Monitor" @click="fillEditorNow">回填到当前编辑器</el-button>
          <el-button type="primary" :icon="RefreshRight" @click="markdownPanelVisible = false">完成</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="createDialogVisible" title="添加新管理员" width="520px" append-to-body>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="学号/工号">
          <el-input v-model="adminForm.studentNo" placeholder="请输入学号或工号" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="adminForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="adminForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="adminForm.password" type="password" show-password placeholder="6-18 位字母与数字组合" @keyup.enter="createAdmin" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="createAdmin">确认添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑管理员信息" width="520px" append-to-body>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="updateAdmin">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import { Monitor, Plus, Refresh, RefreshRight, Setting, Upload, UploadFilled } from '@element-plus/icons-vue'

type Role = 'ADMIN' | 'SUPER_ADMIN'

interface AdminUser {
  id: number
  studentNo: string
  username: string
  nickname: string
  email: string
  phone: string
  role: Role
  createdAt: string
}

interface MarkdownDraft {
  title: string
  content: string
}

const API_BASE = 'http://127.0.0.1:8099/api'
const TOKEN_KEY = 'competition-hub-token'
const ROLE_KEY = 'competition-hub-role'

const legacyFrame = ref<HTMLIFrameElement | null>(null)
const token = ref('')
const currentRole = ref('')
const panelVisible = ref(false)
const markdownPanelVisible = ref(false)
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const loading = ref(false)
const saving = ref(false)
const admins = ref<AdminUser[]>([])
const keyword = ref('')
const markdownFileName = ref('')
const markdownDraft = reactive<MarkdownDraft>({ title: '', content: '' })

const adminForm = reactive({
  studentNo: '',
  nickname: '',
  email: '',
  password: 'admin123',
})

const editForm = reactive({
  id: 0,
  nickname: '',
  email: '',
  phone: '',
})

const isSuperAdmin = computed(() => currentRole.value === 'SUPER_ADMIN' || currentRole.value === 'superAdmin')

const filteredAdmins = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  if (!word) return admins.value
  return admins.value.filter((admin) =>
    [admin.studentNo, admin.nickname, admin.email, admin.phone, admin.role].some((item) => String(item ?? '').toLowerCase().includes(word)),
  )
})

let storageHandler: (() => void) | null = null
let refreshTimer: number | null = null

onMounted(() => {
  refreshSession()
  storageHandler = () => refreshSession()
  window.addEventListener('storage', storageHandler)
  refreshTimer = window.setInterval(refreshSession, 1500)
  window.addEventListener('message', onLegacyMessage)
})

onBeforeUnmount(() => {
  if (storageHandler) window.removeEventListener('storage', storageHandler)
  if (refreshTimer) window.clearInterval(refreshTimer)
  window.removeEventListener('message', onLegacyMessage)
})

function refreshSession() {
  token.value = localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || ''
  currentRole.value = parseJwtRole(token.value) || localStorage.getItem(ROLE_KEY) || ''
}

function parseJwtRole(jwt: string) {
  try {
    const payload = jwt.split('.')[1]
    if (!payload) return ''
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const decoded = JSON.parse(decodeURIComponent(escape(window.atob(normalized))))
    return String(decoded.role || '')
  } catch {
    return ''
  }
}

function onFrameLoad() {
  try {
    fillEditorNow()
  } catch {
    // legacy iframe may not be ready yet
  }
}

function onLegacyMessage(event: MessageEvent) {
  if (event.origin !== window.location.origin) return
  if (event.data?.type === 'legacy-blog-editor-ready') {
    fillEditorNow()
  }
}

async function openAdminPanel() {
  refreshSession()
  panelVisible.value = true
  await loadAdmins()
}

async function loadAdmins() {
  if (!token.value) {
    ElMessage.warning('请先以超级管理员身份登录')
    return
  }
  loading.value = true
  try {
    admins.value = await request<AdminUser[]>('/admin/users')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  adminForm.studentNo = ''
  adminForm.nickname = ''
  adminForm.email = ''
  adminForm.password = 'admin123'
  createDialogVisible.value = true
}

async function createAdmin() {
  const payload = {
    studentNo: adminForm.studentNo.trim(),
    nickname: adminForm.nickname.trim(),
    email: adminForm.email.trim(),
    password: adminForm.password.trim(),
  }
  if (!payload.studentNo || !payload.nickname || !payload.email || !payload.password) {
    ElMessage.warning('请完整填写管理员信息')
    return
  }
  saving.value = true
  try {
    await request<AdminUser>('/admin/users', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    ElMessage.success('管理员已添加')
    createDialogVisible.value = false
    await loadAdmins()
  } finally {
    saving.value = false
  }
}

function openEditDialog(row: AdminUser) {
  editForm.id = row.id
  editForm.nickname = row.nickname
  editForm.email = row.email
  editForm.phone = row.phone
  editDialogVisible.value = true
}

async function updateAdmin() {
  const payload = {
    nickname: editForm.nickname.trim(),
    email: editForm.email.trim(),
    phone: editForm.phone.trim(),
  }
  if (!payload.nickname || !payload.email) {
    ElMessage.warning('请完整填写管理员信息')
    return
  }
  saving.value = true
  try {
    await request<AdminUser>(`/admin/users/${editForm.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    ElMessage.success('管理员信息已更新')
    editDialogVisible.value = false
    await loadAdmins()
  } finally {
    saving.value = false
  }
}

async function confirmRemoveAdmin(row: AdminUser) {
  await ElMessageBox.confirm('确定要取消该用户的管理员权限吗？', '取消管理员权限', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
  await request(`/admin/users/${row.id}`, { method: 'DELETE' })
  ElMessage.success('管理员权限已取消')
  await loadAdmins()
}

async function uploadMarkdownFile(options: UploadRequestOptions) {
  const file = options.file as File
  const formData = new FormData()
  formData.append('file', file)
  try {
    const response = await fetch(`${API_BASE}/blog/upload-md`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token.value}`,
      },
      body: formData,
    })
    if (!response.ok) {
      throw new Error(await response.text())
    }
    const data = await response.json()
    markdownFileName.value = file.name
    markdownDraft.title = String(data.title || '')
    markdownDraft.content = String(data.content || '')
    await nextTick()
    fillEditorNow()
    ElMessage.success('Markdown 已解析并回填')
    markdownPanelVisible.value = true
    options.onSuccess?.(data)
  } catch (error) {
    options.onError?.(error as any)
    ElMessage.error(error instanceof Error && error.message ? error.message : 'Markdown 上传失败')
  }
}

function fillEditorNow() {
  const frame = legacyFrame.value?.contentWindow
  const doc = frame?.document
  if (!doc) {
    ElMessage.warning('当前编辑器尚未加载完成')
    return
  }
  const titleInput = findVisibleInput(doc, ['标题', '标题（必填）', '文章标题'])
  const contentArea = findVisibleTextarea(doc)
  if (titleInput) {
    setNativeValue(titleInput, markdownDraft.title)
  }
  if (contentArea) {
    setNativeValue(contentArea, markdownDraft.content)
  }
  if (!titleInput && !contentArea) {
    ElMessage.info('请先切换到博客发布或编辑页面，再执行回填')
    return
  }
  ElMessage.success('已回填到当前博客编辑器')
}

function findVisibleInput(doc: Document, hints: string[]) {
  const inputs = Array.from(doc.querySelectorAll('input')) as HTMLInputElement[]
  return inputs.find((input) => isVisible(input) && hints.some((hint) => (input.getAttribute('placeholder') || '').includes(hint)))
    || inputs.find((input) => isVisible(input) && input.type !== 'hidden')
    || null
}

function findVisibleTextarea(doc: Document) {
  const textareas = Array.from(doc.querySelectorAll('textarea')) as HTMLTextAreaElement[]
  return textareas.find((item) => isVisible(item)) || null
}

function isVisible(el: HTMLElement) {
  const style = window.getComputedStyle(el)
  return style.display !== 'none' && style.visibility !== 'hidden' && !el.hasAttribute('disabled')
}

function setNativeValue(el: HTMLInputElement | HTMLTextAreaElement, value: string) {
  const setter = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(el), 'value')?.set
  setter?.call(el, value)
  el.dispatchEvent(new Event('input', { bubbles: true }))
  el.dispatchEvent(new Event('change', { bubbles: true }))
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${token.value}`,
      ...(init.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...(init.headers || {}),
    },
  })
  if (!response.ok) {
    const text = await response.text()
    if (response.status === 401 || response.status === 403) {
      ElMessage.error('当前账号没有超级管理员权限，或登录已过期')
    }
    throw new Error(text || `HTTP ${response.status}`)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
</script>

<style scoped>
.shell {
  width: 100vw;
  height: 100vh;
  margin: 0;
  overflow: hidden;
  background: #eef3f8;
}

.legacy-frame {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
}

.float-btn {
  position: fixed;
  right: 24px;
  z-index: 20;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.22);
}

.admin-btn {
  bottom: 84px;
}

.import-btn {
  bottom: 24px;
}

.drawer-body {
  min-height: 100%;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}

.search {
  flex: 1;
}

.md-upload {
  width: 100%;
}

.upload-icon {
  font-size: 42px;
  color: #409eff;
  margin-bottom: 10px;
}

.upload-text {
  font-size: 15px;
  font-weight: 600;
}

.upload-hint {
  margin-top: 8px;
  color: #667085;
  font-size: 13px;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.mt-12 {
  margin-top: 12px;
}
</style>
