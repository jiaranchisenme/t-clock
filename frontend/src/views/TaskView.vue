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
      <h2>学习任务清单</h2>
      <button class="btn danger-ghost" @click="askClearAll">清空全部</button>
    </div>
    <p class="hint">未完成 {{ unfinished }} · 已完成 {{ finished }}</p>

    <!-- 新增表单 -->
    <div class="add-form">
      <input
        v-model="draftTitle"
        class="input"
        type="text"
        placeholder="任务名称"
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
      <button class="btn primary" @click="handleAdd">添加</button>
    </div>
    <small v-if="addErr" class="err">{{ addErr }}</small>

    <!-- 任务列表 -->
    <ul class="list" v-if="taskStore.list.length > 0">
      <li
        v-for="t in taskStore.list"
        :key="t.id"
        class="item"
        :class="{ done: t.status === 1, active: timerStore.activeTaskId === t.id }"
      >
        <!-- 普通态 -->
        <div v-if="editingId !== t.id" class="item-main">
          <label class="check" @click.prevent="toggleStatus(t)">
            <input type="checkbox" :checked="t.status === 1" />
            <span class="checkmark"></span>
          </label>
          <div class="content">
            <div class="title">{{ t.title }}</div>
            <div v-if="t.description" class="desc-text">{{ t.description }}</div>
            <div class="meta">
              <span class="time">创建于 {{ fmtTime(t.createdAt) }}</span>
              <span v-if="timerStore.activeTaskId === t.id" class="focus-tag">当前专注</span>
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
          <input v-model="editDesc" class="input desc" type="text" placeholder="描述" maxlength="500" />
          <small v-if="editErr" class="err">{{ editErr }}</small>
          <div class="edit-actions">
            <button class="mini" @click="cancelEdit">取消</button>
            <button class="mini primary" @click="saveEdit(t.id)">保存</button>
          </div>
        </div>
      </li>
    </ul>

    <!-- 空状态 -->
    <p v-else class="empty">{{ taskStore.loading ? '加载中...' : '暂无任务，添加一个开始吧' }}</p>

    <!-- 确认弹窗（不使用 confirm） -->
    <transition name="fade">
      <div v-if="confirmOpen" class="modal-mask">
        <div class="modal">
          <h3>请确认</h3>
          <p class="confirm-text">{{ confirmText }}</p>
          <div class="modal-actions">
            <button class="btn" @click="confirmOpen = false">取消</button>
            <button class="btn danger" @click="doConfirm">确定</button>
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
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px;
  position: relative;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header h2 {
  margin: 0;
}
.hint {
  color: var(--text-muted, #6b6b78);
  font-size: 13px;
  margin: 4px 0 16px;
}
.add-form {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.input {
  padding: 8px 12px;
  border: 1px solid var(--border, #e2e2e8);
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  flex: 1;
  min-width: 120px;
}
.input.desc {
  flex: 2;
}
.input:focus {
  border-color: var(--brand, #6c4bd6);
}
.btn {
  padding: 8px 16px;
  border: 1px solid var(--border, #e2e2e8);
  background: var(--surface, #fff);
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.15s;
}
.btn:hover {
  border-color: var(--brand, #6c4bd6);
}
.btn.primary {
  background: var(--brand, #6c4bd6);
  color: #fff;
  border-color: var(--brand, #6c4bd6);
}
.btn.danger {
  background: #e53935;
  color: #fff;
  border-color: #e53935;
}
.btn.danger-ghost {
  background: transparent;
  color: #e53935;
  border-color: #e53935;
}
.err {
  color: #e53935;
  font-size: 12px;
  display: block;
  margin: 4px 0 8px;
}
.list {
  list-style: none;
  padding: 0;
  margin: 16px 0 0;
}
.item {
  border: 1px solid var(--border, #e2e2e8);
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: var(--surface, #fff);
  transition: all 0.15s;
}
.item.active {
  border-color: var(--brand, #6c4bd6);
  box-shadow: 0 0 0 2px rgba(108, 75, 214, 0.12);
}
.item.done {
  background: var(--bg-muted, #f7f7fa);
  opacity: 0.65;
}
.item.done .title {
  text-decoration: line-through;
  color: var(--text-muted, #9a9aa8);
}
.item.done .desc-text {
  color: var(--text-muted, #9a9aa8);
}
.item-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.check {
  display: inline-block;
  width: 20px;
  height: 20px;
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
  border: 2px solid var(--border, #cfcfd8);
  border-radius: 6px;
  transition: all 0.15s;
}
.check input:checked + .checkmark {
  background: var(--brand, #6c4bd6);
  border-color: var(--brand, #6c4bd6);
}
.check input:checked + .checkmark::after {
  content: '';
  position: absolute;
  left: 6px;
  top: 2px;
  width: 5px;
  height: 10px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
.content {
  flex: 1;
  min-width: 0;
}
.title {
  font-size: 15px;
  font-weight: 500;
  word-break: break-all;
}
.desc-text {
  font-size: 13px;
  color: var(--text-muted, #6b6b78);
  margin-top: 4px;
  word-break: break-all;
}
.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  flex-wrap: wrap;
}
.time {
  font-size: 11px;
  color: var(--text-muted, #9a9aa8);
}
.focus-tag {
  font-size: 11px;
  background: var(--brand, #6c4bd6);
  color: #fff;
  padding: 1px 8px;
  border-radius: 999px;
}
.actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  flex-wrap: wrap;
}
.mini {
  padding: 4px 10px;
  border: 1px solid var(--border, #e2e2e8);
  background: var(--surface, #fff);
  border-radius: 999px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.15s;
}
.mini:hover {
  border-color: var(--brand, #6c4bd6);
  color: var(--brand, #6c4bd6);
}
.mini.active {
  background: var(--brand, #6c4bd6);
  color: #fff;
  border-color: var(--brand, #6c4bd6);
}
.mini.primary {
  background: var(--brand, #6c4bd6);
  color: #fff;
  border-color: var(--brand, #6c4bd6);
}
.mini.danger {
  color: #e53935;
  border-color: #e53935;
  background: transparent;
}
.mini.danger:hover {
  background: #e53935;
  color: #fff;
}
.edit-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.empty {
  text-align: center;
  color: var(--text-muted, #9a9aa8);
  padding: 48px 0;
  font-size: 14px;
}
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal {
  background: var(--surface, #fff);
  border-radius: 12px;
  padding: 24px;
  min-width: 320px;
  max-width: 90vw;
}
.modal h3 {
  margin: 0 0 8px;
}
.confirm-text {
  font-size: 14px;
  color: var(--text-muted, #6b6b78);
  margin: 0 0 16px;
}
.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
.toast {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  background: #333;
  color: #fff;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1100;
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.25s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
