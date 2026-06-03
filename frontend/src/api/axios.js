import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// 요청 인터셉터: accessToken 자동 첨부
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 응답 인터셉터: ApiResponse<T> 자동 언래핑 + 401 처리
api.interceptors.response.use(
  (res) => {
    const body = res.data
    // { success, data, error } 형태면 언래핑
    if (body && typeof body.success === 'boolean') {
      if (!body.success) {
        return Promise.reject(new Error(body.error || '오류가 발생했습니다.'))
      }
      return { ...res, data: body.data }
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.clear()
      window.location.href = '/login'
    }
    // 서버 에러 메시지 꺼내기
    const msg = error.response?.data?.error || error.message || '오류가 발생했습니다.'
    return Promise.reject(new Error(msg))
  }
)

export default api
