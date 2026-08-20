import http from './http'

// 任务实体
export interface Task {
  id: number
  title: string
  description?: string
  status: 0 | 1 // 0=未完成 1=已完成
  createdAt: string
  updatedAt: string
}

export function listTasks() {
  return http.get<Task[], Task[]>('/tasks')
}

export function createTask(payload: { title: string; description?: string }) {
  return http.post<Task, Task>('/tasks', payload)
}

export function updateTask(id: number, payload: Partial<Task>) {
  return http.put<Task, Task>(`/tasks/${id}`, payload)
}

export function deleteTask(id: number) {
  return http.delete<void, void>(`/tasks/${id}`)
}

export function clearAllTasks() {
  return http.delete<void, void>('/tasks')
}
