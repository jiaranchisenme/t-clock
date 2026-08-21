import axios, {
  type AxiosInstance,
  type InternalAxiosRequestConfig,
  type AxiosResponse,
  type Method,
  type AxiosRequestConfig,
} from 'axios'

// 统一后端响应壳：{ code, msg, data }
export interface Result<T = unknown> {
  code: number
  msg: string
  data: T
}

/**
 * 扩展后的 axios 实例：
 * - 拦截器自动剥离 Result 壳，直接返回 T
 * - 因此 get/post/put/delete 第 1 个泛型 = 业务 data 的类型（不再是 AxiosResponse）
 */
export interface TypedHttp extends Omit<AxiosInstance, 'get' | 'post' | 'put' | 'delete' | 'patch' | 'request'> {
  get<T = unknown, R = T>(url: string, config?: AxiosRequestConfig): Promise<R>
  post<T = unknown, R = T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<R>
  put<T = unknown, R = T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<R>
  delete<T = unknown, R = T>(url: string, config?: AxiosRequestConfig): Promise<R>
  patch<T = unknown, R = T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<R>
  request<T = unknown, R = T>(config: AxiosRequestConfig & { method?: Method }): Promise<R>
}

// axios 实例：baseURL 在开发环境走 Vite 代理 /api，生产环境由 nginx 转发
const base = axios.create({
  baseURL: '/api',
  timeout: 10000,
})
const http = base as unknown as TypedHttp

// 请求拦截器：可在此挂载 token（后续多用户场景）
base.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => config,
  (err) => Promise.reject(err),
)

// 响应拦截器：剥离 Result 壳，业务层只拿 data
base.interceptors.response.use(
  (resp: AxiosResponse<Result>) => {
    const body = resp.data
    if (!body || typeof body !== 'object') {
      // 非标准返回：直接回传（兼容特殊接口）
      return body as any
    }
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
