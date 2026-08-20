import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'

// 统一后端响应壳：{ code, msg, data }
export interface Result<T = unknown> {
  code: number
  msg: string
  data: T
}

// axios 实例：baseURL 在开发环境走 Vite 代理 /api，生产环境由 nginx 转发
const http: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器：可在此挂载 token（后续多用户场景）
http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => config,
  (err) => Promise.reject(err),
)

// 响应拦截器：剥离 Result 壳，业务层只拿 data
http.interceptors.response.use(
  (resp: AxiosResponse<Result>) => {
    const body = resp.data
    if (body.code !== 0) {
      // 业务错误统一抛出，View 层通过 catch 处理
      return Promise.reject(new Error(body.msg || '业务错误'))
    }
    return body.data as any
  },
  (err) => {
    // HTTP 层错误兜底
    return Promise.reject(new Error(err.message || '网络异常'))
  },
)

export default http
