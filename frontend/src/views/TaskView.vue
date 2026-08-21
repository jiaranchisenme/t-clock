<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useTaskStore } from '../stores/task'
import { useTimerStore } from '../stores/timer'
import type { Task } from '../api/task'

const taskStore = useTaskStore()
const timerStore = useTimerStore()

// ----- 新增草稿 -----
const draftTitle = ref('')
const draftDesc = ref('')
const addErr = ref('')

// ----- 编辑状态 -----
const editingId = ref<number | null>(null)
const editTitle = ref('')
const editDesc = ref('')
const editErr = ref('')

// ----- 确认弹窗 -----
const confirmOpen = ref(false)
const confirmText = ref('')
let confirmAction: (() => Promise<void>) | null = null

// ----- Toast -----
const toast = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null
function showToast(msg: string) {
  toast.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2500)
}

onMounted(() => taskStore.load())

// ----- 新增（需求 1/2） -----
function validateTitle(v: string): string {
  const s = (v ?? '').trim()
  if (!s) return '任务名称不能为空'
  if (s.length > 100) return '任务名称最多 100 字'
  return ''
}

async function handleAdd() {
  const err = validateTitle(draftTitle.value)
  addErr.value = err
  if (err) {
    showToast(err)
    return
  }
  try {
    await taskStore.add(draftTitle.value.trim(), draftDesc.value.trim() || undefined)
    draftTitle.value = ''
    draftDesc.value = ''
    addErr.value = ''
    showToast('任务已添加')
  } catch (e) {
    showToast('添加失败')
    console.warn(e)
  }
}

// ----- 编辑（需求 6/7） -----
function startEdit(t: Task) {
  editingId.value = t.id
  editTitle.value = t.title
  editDesc.value = t.description ?? ''
  editErr.value = ''
}

function cancelEdit() {
  editingId.value = null
  editErr.value = ''
}

async function saveEdit(id: number) {
  const err = validateTitle(editTitle.value)
  editErr.value = err
  if (err) {
    showToast(err)
    return
  }
  try {
    await taskStore.patch(id, {
      title: editTitle.value.trim(),
      description: editDesc.value.trim() || undefined,
    })
    editingId.value = null
    editErr.value = ''
    showToast('已保存')
  } catch (e) {
    showToast('保存失败')
    console.warn(e)
  }
}

// ----- 状态切换（需求 4） -----
async function toggleStatus(t: Task) {
  const next = t.status === 0 ? 1 : 0
  try {
    await taskStore.patch(t.id, { status: next })
    // 若把当前专注任务标完成，自动解除关联
    if (next === 1 && timerStore.activeTaskId === t.id) {
      timerStore.setActiveTask(null)
      showToast('任务已完成，已解除当前专注')
    }
  } catch (e) {
    showToast('切换失败')
    console.warn(e)
  }
}

// ----- 删除（需求 8/9，带确认弹窗） -----
function askDelete(id: number) {
  confirmText.value = '确定删除该任务？'
  confirmOpen.value = true
  confirmAction = async () => {
    try {
      await taskStore.remove(id)
      showToast('已删除')
    } catch (e) {
      showToast('删除失败')
      console.warn(e)
    }
  }
}

function askClearAll() {
  if (taskStore.list.length === 0) {
    showToast('当前没有任务')
    return
  }
  confirmText.value = `确定清空全部 ${taskStore.list.length} 个任务？此操作不可恢复`
  confirmOpen.value = true
  confirmAction = async () => {
    try {
      await taskStore.clearAll()
      showToast('已清空全部任务')
    } catch (e) {
      showToast('清空失败')
      console.warn(e)
    }
  }
}

async function doConfirm() {
  confirmOpen.value = false
  if (confirmAction) await confirmAction()
  confirmAction = null
}

// ----- 设为当前专注（需求 10） -----
function handleSetFocus(t: Task) {
  try {
    taskStore.setCurrentFocus(t.id)
    showToast(
      timerStore.activeTaskId === t.id ? '已设为当前专注任务' : '已取消当前专注'
    )
  } catch (e) {
    showToast((e as Error).message)
  }
}

// ----- 派生 -----
const unfinished = computed(() => taskStore.list.filter((t) => t.status === 0).length)
const finished = computed(() => taskStore.list.filter((t) => t.status === 1).length)

function fmtTime(s: string): string {
  if (!s) return ''
  // 兼容 ISO 与 LocalDateTime 格式
  return s.replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <section class="page">
    <div class="header">
      <div class="header-text">
        <h2>📝 学习任务清单</h2>
        <p class="hint">未完成 <b>{{ unfinished }}</b> · 已完成 <b>{{ finished }}</b></p>
      </div>
      <button class="btn-pill ghost danger-ghost" @click="askClearAll" :disabled="taskStore.list.length === 0">
        🗑️ 清空全部
      </button>
    </div>

    <!-- 新增表单 · 卡片式 -->
    <div class="card add-card">
      <div class="add-form">
        <input
          v-model="draftTitle"
          class="input"
          type="text"
          placeholder="任务名称（例如：背 50 个单词）"
          maxlength="100"
          @keydown.enter="handleAdd"
        />
        <input
          v-model="draftDesc"
          class="input desc"
          type="text"
          placeholder="描述（可选）"
          maxlength="500"
          @keydown.enter="handleAdd"
        />
        <button class="btn-pill primary" @click="handleAdd">＋ 添加</button>
      </div>
      <small v-if="addErr" class="err">{{ addErr }}</small>
    </div>

    <!-- 任务列表 -->
    <ul class="list" v-if="taskStore.list.length > 0">
      <li
        v-for="t in taskStore.list"
        :key="t.id"
        class="item card"
        :class="{ done: t.status === 1, active: timerStore.activeTaskId === t.id }"
      >
        <!-- 普通态 -->
        <div v-if="editingId !== t.id" class="item-main">
          <label class="check" @click.prevent="toggleStatus(t)">
            <input type="checkbox" :checked="t.status === 1" />
            <span class="checkmark">
              <svg v-if="t.status === 1" viewBox="0 0 16 16" width="14" height="14" fill="none">
                <path d="M3 8.5L6.5 12L13 5" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </span>
          </label>
          <div class="content">
            <div class="title">{{ t.title }}</div>
            <div v-if="t.description" class="desc-text">{{ t.description }}</div>
            <div class="meta">
              <span class="time">🕒 {{ fmtTime(t.createdAt) }}</span>
              <span v-if="timerStore.activeTaskId === t.id" class="tag">当前专注</span>
            </div>
          </div>
          <div class="actions">
            <button
              v-if="t.status === 0"
              class="mini"
              :class="{ active: timerStore.activeTaskId === t.id }"
              @click="handleSetFocus(t)"
            >{{ timerStore.activeTaskId === t.id ? '取消专注' : '设为专注' }}</button>
            <button class="mini" @click="startEdit(t)">编辑</button>
            <button class="mini danger" @click="askDelete(t.id)">删除</button>
          </div>
        </div>

        <!-- 编辑态 -->
        <div v-else class="edit-form">
          <input v-model="editTitle" class="input" type="text" placeholder="任务名称" maxlength="100" />
          <input v-model="editDesc" class="input" type="text" placeholder="描述（可选）" maxlength="500" />
          <small v-if="editErr" class="err">{{ editErr }}</small>
          <div class="edit-actions">
            <button class="mini" @click="cancelEdit">取消</button>
            <button class="mini primary" @click="saveEdit(t.id)">保存</button>
          </div>
        </div>
      </li>
    </ul>

    <!-- 空状态 · 卡片插图 -->
    <div v-else class="empty-card card">
      <div class="empty-illustration" aria-hidden="true">
        <svg viewBox="0 0 120 120" width="120" height="120" fill="none">
          <circle cx="60" cy="60" r="52" fill="var(--brand-50)"/>
          <rect x="36" y="34" width="48" height="56" rx="8" fill="#fff" stroke="var(--brand-100)" stroke-width="2"/>
          <rect x="44" y="46" width="32" height="4" rx="2" fill="var(--brand-100)"/>
          <rect x="44" y="56" width="24" height="4" rx="2" fill="var(--brand-100)"/>
          <rect x="44" y="66" width="28" height="4" rx="2" fill="var(--brand-100)"/>
          <path d="M54 78L58 82L68 72" stroke="var(--leaf)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="84" cy="40" r="12" fill="var(--amber-50)"/>
          <path d="M80 40 L83 43 L89 37" stroke="var(--amber)" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <h3 class="empty-title">{{ taskStore.loading ? '正在加载任务…' : '还没有任务哦' }}</h3>
      <p class="empty-desc">{{ taskStore.loading ? '稍等一下～' : '添加一个学习任务，从一小步开始专注吧 🌱' }}</p>
    </div>

    <!-- 确认弹窗（不使用 confirm） -->
    <transition name="fade">
      <div v-if="confirmOpen" class="modal-mask" @click.self="confirmOpen = false">
        <div class="modal card">
          <div class="modal-icon" aria-hidden="true">⚠️</div>
          <h3>请确认</h3>
          <p class="confirm-text">{{ confirmText }}</p>
          <div class="modal-actions">
            <button class="btn-pill" @click="confirmOpen = false">取消</button>
            <button class="btn-pill" :class="confirmText.includes('删除') || confirmText.includes('清空') ? 'danger' : 'primary'" @click="doConfirm">确定</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- Toast -->
    <transition name="fade">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </transition>
  </section>
</template>

<style scoped>
.page {
  max-width: 760px;
  margin: 0 auto;
  padding: 20px 18px 48px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---------- 顶部 ---------- */
.header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.header-text h2 {
  margin: 0;
}
.header-text .hint {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}
.header-text .hint b {
  color: var(--text-h);
  font-weight: 600;
}
.danger-ghost {
  color: var(--danger);
  border-color: transparent;
}
.danger-ghost:hover:not(:disabled) {
  background: var(--danger-50);
  color: var(--danger);
  border-color: transparent;
}

/* ---------- 新增卡片 ---------- */
.add-card {
  padding: 16px;
}
.add-form {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
.input {
  flex: 1;
  min-width: 140px;
}
.input.desc {
  flex: 2;
  min-width: 180px;
}
.err {
  color: var(--danger);
  font-size: 12px;
  display: block;
  margin: 8px 2px 0;
}

/* ---------- 列表 ---------- */
.list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.item {
  padding: 14px 16px;
  transition: transform 0.18s, box-shadow 0.18s, border-color 0.18s;
}
.item:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow);
}
.item.active {
  border-color: var(--accent-border);
  background: linear-gradient(180deg, #ffffff 0%, var(--brand-50) 100%);
}
.item.done {
  background: var(--surface-muted);
  opacity: 0.88;
}
.item.done .title {
  text-decoration: line-through;
  color: var(--text-muted);
}
.item.done .desc-text {
  color: var(--text-muted);
}
.item-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

/* 自定义 checkbox */
.check {
  display: inline-block;
  width: 22px;
  height: 22px;
  cursor: pointer;
  margin-top: 2px;
  position: relative;
  flex-shrink: 0;
}
.check input {
  opacity: 0;
  width: 0;
  height: 0;
}
.checkmark {
  position: absolute;
  inset: 0;
  border: 2px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  transition: all 0.18s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.check:hover .checkmark {
  border-color: var(--brand);
}
.check input:checked + .checkmark {
  background: var(--leaf);
  border-color: var(--leaf);
  box-shadow: 0 2px 6px -2px rgba(34, 197, 94, 0.6);
}

/* 内容 */
.content {
  flex: 1;
  min-width: 0;
}
.title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-h);
  word-break: break-word;
  line-height: 1.4;
}
.desc-text {
  font-size: 13px;
  color: var(--text);
  margin-top: 4px;
  word-break: break-word;
  line-height: 1.55;
}
.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.time {
  font-size: 12px;
  color: var(--text-muted);
}

/* 操作按钮 */
.actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.mini {
  padding: 5px 12px;
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: var(--r-pill);
  cursor: pointer;
  font-size: 12px;
  transition: all 0.15s;
  color: var(--text);
}
.mini:hover {
  border-color: var(--brand);
  color: var(--brand);
  background: var(--brand-50);
}
.mini.active {
  background: var(--brand);
  color: #fff;
  border-color: var(--brand);
}
.mini.active:hover {
  background: var(--brand-600);
  color: #fff;
  border-color: var(--brand-600);
}
.mini.primary {
  background: var(--brand);
  color: #fff;
  border-color: var(--brand);
}
.mini.primary:hover {
  background: var(--brand-600);
  color: #fff;
}
.mini.danger {
  color: var(--danger);
  border-color: transparent;
  background: transparent;
}
.mini.danger:hover {
  background: var(--danger-50);
  color: var(--danger);
}

/* 编辑 */
.edit-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.edit-form .input { width: 100%; }
.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* ---------- 空状态卡片 ---------- */
.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 40px 20px 32px;
  gap: 8px;
}
.empty-illustration {
  margin-bottom: 4px;
}
.empty-title {
  margin: 8px 0 0;
  color: var(--text-h);
  font-size: 16px;
  font-weight: 600;
}
.empty-desc {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

/* ---------- 确认弹窗 ---------- */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}
.modal {
  min-width: 300px;
  max-width: 420px;
  width: 100%;
  padding: 24px;
  text-align: center;
  animation: pop 0.22s cubic-bezier(0.3, 0.9, 0.3, 1);
}
.modal-icon {
  font-size: 32px;
  margin-bottom: 4px;
}
.modal h3 {
  margin: 0 0 8px;
  font-size: 18px;
}
.confirm-text {
  font-size: 14px;
  color: var(--text);
  margin: 0 0 20px;
  line-height: 1.6;
}
.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}
.btn-pill.danger {
  background: var(--danger);
  color: #fff;
  border-color: var(--danger);
}
.btn-pill.danger:hover {
  background: #dc2626;
  border-color: #dc2626;
  color: #fff;
}

/* ---------- Toast ---------- */
.toast {
  position: fixed;
  bottom: 36px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--text-h);
  color: var(--bg);
  padding: 10px 20px;
  border-radius: var(--r-pill);
  font-size: 13px;
  box-shadow: var(--shadow);
  z-index: 1100;
  letter-spacing: 0.01em;
}

/* ---------- 动画 ---------- */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.25s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
@keyframes pop {
  from { transform: scale(0.94); opacity: 0; }
  to   { transform: scale(1);    opacity: 1; }
}

/* ---------- 响应式：移动端 ---------- */
@media (max-width: 520px) {
  .page { padding: 14px 12px 40px; gap: 12px; }
  .add-card { padding: 12px; }
  .add-form { gap: 8px; }
  .add-form .input.desc { flex: 1 1 100%; }
  .item { padding: 12px; }
  .item-main { flex-wrap: wrap; }
  .actions { width: 100%; justify-content: flex-start; margin-top: 4px; }
  .modal {
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: 20px;
    min-width: 0;
    animation: slide-up 0.25s cubic-bezier(0.3, 0.9, 0.3, 1);
  }
  @keyframes slide-up {
    from { transform: translateY(24px); opacity: 0; }
    to   { transform: translateY(0);    opacity: 1; }
  }
}
</style>
