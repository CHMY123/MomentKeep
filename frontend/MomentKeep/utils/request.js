/**
 * MomentKeep 朝暮记 - 网络请求工具
 * @description 提供统一的网络请求接口，支持不同环境的API基础URL切换
 * @author MomentKeep Team
 * @since 2026-04-18
 */

// API基础URL配置
const API_CONFIG = {
  // 开发环境（H5）
  development: '/api',
  // 微信小程序环境
  'mp-weixin': 'http://localhost:8080/api',
  // APP环境
  app: 'http://10.252.41.76:8080/api'
}

/**
 * 获取API基础URL
 * @returns {string} API基础URL
 */
const getApiBaseUrl = () => {
  // #ifdef APP-PLUS
  return API_CONFIG.app;
  // #endif

  // #ifdef MP-WEIXIN
  return API_CONFIG['mp-weixin'];
  // #endif

  // #ifdef H5
  return API_CONFIG.development;
  // #endif
  
  return API_CONFIG.development;
}

/**
 * 统一网络请求方法
 * @param {Object} options - 请求配置
 * @returns {Promise} 请求结果
 */
export const request = async (options) => {
  // 获取API基础URL
  const baseUrl = getApiBaseUrl()
  
  // 构建完整的请求URL
  const url = options.url.startsWith('http') ? options.url : baseUrl + options.url
  
  // 合并请求配置
  const config = {
    ...options,
    url
  }
  
  try {
    const response = await uni.request(config)
    
    // 统一处理响应
    if (response.statusCode === 200) {
      return response.data
    } else {
      throw new Error(`HTTP ${response.statusCode}: ${response.data.message || '请求失败'}`)
    }
  } catch (error) {
    console.error('网络请求失败:', error)
    throw error
  }
}

/**
 * GET请求
 * @param {string} url - 请求URL
 * @param {Object} params - 请求参数
 * @param {Object} header - 请求头
 * @returns {Promise} 请求结果
 */
export const get = (url, params = {}, header = {}) => {
  // 构建查询字符串
  const queryString = Object.keys(params)
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
 * POST请求
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
    header: {
      'Content-Type': 'application/json',
      ...header
    }
  })
}

/**
 * PUT请求
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
    header: {
      'Content-Type': 'application/json',
      ...header
    }
  })
}

/**
 * DELETE请求
 * @param {string} url - 请求URL
 * @param {Object} params - 请求参数
 * @param {Object} header - 请求头
 * @returns {Promise} 请求结果
 */
export const del = (url, params = {}, header = {}) => {
  // 构建查询字符串
  const queryString = Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  
  const fullUrl = queryString ? `${url}?${queryString}` : url
  
  return request({
    url: fullUrl,
    method: 'DELETE',
    header
  })
}