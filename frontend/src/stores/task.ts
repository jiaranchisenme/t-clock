import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { listTasks, createTask, updateTask, deleteTask, clearAllTasks, type Task } from '../api/task'
import { useTimerStore } from './timer'

/**
 * Task Store：任务清单 CRUD + 当前专注任务管理。
 * - 设为当前专注任务时，仅允许未完成任务（PRD 4.2.3）
 * - 当前任务 ID 写入后续 session（由 TimerView onComplete 传递）
 */
export const useTaskStore = defineStore('task', () => {
  const list = ref<Task[]>([])
  const draft = ref<{ title: string; description: string }>({ title: '', description: '' })
  const loading = ref(false)

  // 容错加载：异常时 list 保持空，主页不崩
  async function load() {
    loading.value = true
    try {
      list.value = await listTasks()
    } catch (e) {
      console.warn('加载任务失败', e)
      list.value = []
    } finally {
      loading.value = false
    }
  }

  async function add(title: string, description?: string) {
    const created = await createTask({ title, description })
    list.value.push(created)
    return created
  }

  async function patch(id: number, payload: Partial<Pick<Task, 'title' | 'description' | 'status'>>) {
    const updated = await updateTask(id, payload)
    const idx = list.value.findIndex((t) => t.id === id)
    if (idx >= 0) list.value[idx] = { ...list.value[idx], ...updated }
    return updated
  }

  async function remove(id: number) {
    await deleteTask(id)
    list.value = list.value.filter((t) => t.id !== id)
    // 若删的是当前专注任务，清空 timer 关联
    const timer = useTimerStore()
    if (timer.activeTaskId === id) timer.setActiveTask(null)
  }

  async function clearAll() {
    await clearAllTasks()
    list.value = []
    // 清空全部时也清 timer 关联
    const timer = useTimerStore()
    timer.setActiveTask(null)
  }

  function resetDraft() {
    draft.value = { title: '', description: '' }
  }

  /**
   * 把一个未完成任务设为当前专注任务（需求 10）
   * - 已完成任务拒绝
   * - 再次点击同一任务可取消（单选 toggle 体验）
   */
  function setCurrentFocus(id: number) {
    const timer = useTimerStore()
    const task = list.value.find((t) => t.id === id)
    if (!task) return
    if (task.status === 1) {
      throw new Error('已完成的任务不能设为当前专注任务')
    }
    // toggle：再次点击同一任务 → 取消
    timer.setActiveTask(timer.activeTaskId === id ? null : id)
  }

  const activeTaskId = computed(() => useTimerStore().activeTaskId)

  return { list, draft, loading, activeTaskId, load, add, patch, remove, clearAll, resetDraft, setCurrentFocus }
})
