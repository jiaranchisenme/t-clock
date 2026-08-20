import { defineStore } from 'pinia'
import { ref } from 'vue'
import { listTasks, createTask, updateTask, deleteTask, clearAllTasks, type Task } from '../api/task'

export const useTaskStore = defineStore('task', () => {
  const list = ref<Task[]>([])
  const draft = ref<Partial<Task>>({ title: '', description: '' })
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

  async function patch(id: number, payload: Partial<Task>) {
    const updated = await updateTask(id, payload)
    const idx = list.value.findIndex((t) => t.id === id)
    if (idx >= 0) list.value[idx] = { ...list.value[idx], ...updated }
    return updated
  }

  async function remove(id: number) {
    await deleteTask(id)
    list.value = list.value.filter((t) => t.id !== id)
  }

  async function clearAll() {
    await clearAllTasks()
    list.value = []
  }

  function resetDraft() {
    draft.value = { title: '', description: '' }
  }

  return { list, draft, loading, load, add, patch, remove, clearAll, resetDraft }
})
