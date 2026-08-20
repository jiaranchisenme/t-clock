import http from './http'

// 计时器配置入参 DTO
export interface TimerConfigDTO {
  workDuration: number
  breakDuration: number
}

export function getTimerConfig() {
  return http.get<TimerConfigDTO, TimerConfigDTO>('/timer/config')
}

export function updateTimerConfig(payload: TimerConfigDTO) {
  return http.put<TimerConfigDTO, TimerConfigDTO>('/timer/config', payload)
}
