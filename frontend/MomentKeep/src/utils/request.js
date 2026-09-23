/**
 * MomentKeep 朝暮记 - 网络请求工具
 * @description 统一封装网络请求：环境化 baseURL、自动注入 token、统一错误处理与登录态失效跳转
 * @author MomentKeep Team
 */
import { useCache } from './cache'

/**
 * 各端 API 基础地址
 * - 通过 .env 文件注入，避免把服务器地址硬编码进源码
 * - H5 默认使用同源相对路径 /api，由部署时的 Nginx 反向代理到后端
 * - 小程序 / App 必须配置为完整地址（且需要 HTTPS 域名）
 */
const ENV = import.meta.env || {}

const API_BASE_URL = {
  h5: ENV.VITE_API_BASE_URL || '/api',
  'mp-weixin': ENV.VITE_API_BASE_URL_MP || ENV.VITE_API_BASE_URL || '/api',
  app: ENV.VITE_API_BASE_URL_APP || ENV.VITE_API_BASE_URL || '/api'
}

/** 登录页路径，用于登录态失效后跳转 */
const LOGIN_PAGE = '/pages/login/login'

/** 业务成功状态码 */
const CODE_SUCCESS = 200

/** 默认请求超时（毫秒）：断网/服务不可用时避免请求长时间挂起 */
const DEFAULT_TIMEOUT = 15000

/** 防止并发请求同时触发跳转 */
let isRedirecting = false

/**
 * 获取 API 基础 URL
 * @returns {string} API 基础 URL
 */
export const getApiBaseUrl = () => {
  // #ifdef APP-PLUS
  return API_BASE_URL.app
  // #endif

  // #ifdef MP-WEIXIN
  return API_BASE_URL['mp-weixin']
  // #endif

  // #ifdef H5
  return API_BASE_URL.h5
  // #endif

  return API_BASE_URL.h5
}

/**
 * 拼接完整请求地址（导出给 uni.uploadFile 等场景复用，避免各页面硬编码域名）
 * @param {string} url 接口路径，如 /user/avatar
 * @returns {string} 完整 URL
 */
export const buildUrl = (url) => {
  if (!url) return getApiBaseUrl()
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return getApiBaseUrl() + url
}

/**
 * 把资源地址安全地嵌入 CSS url()
 *
 * @description 用户上传的图片会保留原始文件名（例如「..._微信图片_20240922234533.jpg」）。
 * 直接拼成 url(https://.../微信图片.jpg) 时，未加引号的非 ASCII 字符会让整条声明解析失败，
 * 表现为 background-image 完全不生效——而同一地址用 <image src> 却能正常显示。
 * 这里统一加双引号，并只对非 ASCII 字符和双引号做百分号编码：
 * 已是 %xx 形式的字符属于 ASCII，不会被二次编码。
 *
 * @param {string} url 资源地址
 * @returns {string} 可直接赋给 background-image 的 url(...) 字符串；地址为空时返回 ''
 */
export const toCssUrl = (url) => {
  if (!url) return ''
  const safe = String(url).replace(/[^\x20-\x7E]|["]/g, (char) => encodeURIComponent(char))
  return `url("${safe}")`
}

/**
 * 读取本地保存的 token
 * @returns {string} token
 */
export const getToken = () => {
  try {
    return uni.getStorageSync('token') || ''
  } catch (e) {
    return ''
  }
}

/**
 * 登录态失效处理：清除本地登录信息并跳转登录页
 */
const handleUnauthorized = () => {
  try {
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
    uni.removeStorageSync('backgroundImage')
    // 业务缓存键不含用户标识，必须一并清空，
    // 否则同一设备换账号登录后会先回显上一个账号的数据
    useCache().clearAllCache()
  } catch (e) {
    console.error('清除登录信息失败:', e)
  }

  if (isRedirecting) return
  isRedirecting = true

  uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
  setTimeout(() => {
    const pages = getCurrentPages()
    const current = pages.length ? pages[pages.length - 1].route : ''
    if (!current.includes('login')) {
      uni.reLaunch({ url: LOGIN_PAGE })
    }
    isRedirecting = false
  }, 800)
}

/**
 * 统一网络请求方法
 * @param {Object} options - 请求配置
 * @returns {Promise<Object>} 后端统一响应体 { code, message, data, timestamp }
 */
export const request = async (options) => {
  const token = getToken()

  // 自动注入认证头（页面显式传入的同名头优先）
  const headers = {
    ...(options.header || {})
  }
  if (token && !headers.Authorization) {
    headers.Authorization = `Bearer ${token}`
  }
  if (!headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  const config = {
    ...options,
    url: buildUrl(options.url),
    header: headers,
    timeout: options.timeout || DEFAULT_TIMEOUT
  }

  /*
   * AI 长请求单独放宽超时。
   * 后端对 DeepSeek 的读超时是 60s，而本文件默认 15s —— 若沿用默认值，
   * 前端会先 abort、提示"网络连接失败"，而后端可能已经成功调用并扣掉了
   * 每日额度（客户端断开不会触发配额归还），用户既看不到回复又白耗一次额度。
   * 这里按 URL 放宽到 90s，留出网络与模型生成的余量。
   */
  if (String(options.url || '').indexOf('/ai/chat') !== -1) {
    const aiTimeout = 90000
    if (!options.timeout || Number(options.timeout) < aiTimeout) {
      options.timeout = aiTimeout
    }
    /*
     * 注意：config 已在上面按 options.timeout 组装完毕，只改 options 不会生效，
     * 必须一并覆盖已生成的配置对象——否则这段代码看着对、实际不起作用。
     */
    if (!config.timeout || Number(config.timeout) < aiTimeout) {
      config.timeout = aiTimeout
    }
  }

  let response
  try {
    response = await uni.request(config)
  } catch (error) {
    // 网络层失败（断网、超时、DNS 失败等）
    console.error('网络请求失败:', error)
    throw new Error('网络连接失败，请检查网络后重试')
  }

  const statusCode = response && response.statusCode

  // 登录态失效
  if (statusCode === 401) {
    handleUnauthorized()
    throw new Error('登录已过期，请重新登录')
  }

  if (statusCode !== 200) {
    const message = (response && response.data && response.data.message) || `请求失败(${statusCode})`
    throw new Error(message)
  }

  const body = response.data

  // 后端约定：HTTP 200 + body.code 表示业务结果
  if (body && typeof body === 'object' && body.code !== undefined && body.code !== CODE_SUCCESS) {
    const error = new Error(body.message || '请求失败')
    error.code = body.code
    throw error
  }

  return body
}

/**
 * GET 请求
 * @param {string} url - 请求URL
 * @param {Object} params - 请求参数
 * @param {Object} header - 请求头
 * @returns {Promise} 请求结果
 */
export const get = (url, params = {}, header = {}) => {
  // 构建查询字符串
  const queryString = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')

  const fullUrl = queryString ? `${url}?${queryString}` : url

  return request({
    url: fullUrl,
    method: 'GET',
    header
  })
}

/**
 * POST 请求
 * @param {string} url - 请求URL
 * @param {Object} data - 请求数据
 * @param {Object} header - 请求头
 * @returns {Promise} 请求结果
 */
export const post = (url, data = {}, header = {}) => {
  return request({
    url,
    method: 'POST',
    data,
    header
  })
}

/**
 * PUT 请求
 * @param {string} url - 请求URL
 * @param {Object} data - 请求数据
 * @param {Object} header - 请求头
 * @returns {Promise} 请求结果
 */
export const put = (url, data = {}, header = {}) => {
  return request({
    url,
    method: 'PUT',
    data,
    header
  })
}

/**
 * DELETE 请求
 * @param {string} url - 请求URL
 * @param {Object} params - 请求参数
 * @param {Object} header - 请求头
 * @returns {Promise} 请求结果
 */
export const del = (url, params = {}, header = {}) => {
  // 构建查询字符串
  const queryString = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')

  const fullUrl = queryString ? `${url}?${queryString}` : url

  return request({
    url: fullUrl,
    method: 'DELETE',
    header
  })
}
