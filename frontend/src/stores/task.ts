import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Task } from '../api/task'

export const useTaskStore = defineStore('task', () => {
  const list = ref<Task[]>([])
  const draft = ref<Partial<Task>>({ title: '', description: '' })

  function setList(tasks: Task[]) {
    list.value = tasks
  }

  function resetDraft() {
    draft.value = { title: '', description: '' }
  }

  return { list, draft, setList, resetDraft }
})
