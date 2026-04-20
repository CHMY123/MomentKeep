/**
 * MomentKeep 朝暮记 - 缓存工具模块
 * @description 提供数据缓存管理功能，支持带过期时间的缓存存取
 * @author MomentKeep Team
 * @since 2026-04-18
 */

// 缓存前缀 - 用于避免与其他存储键冲突
const CACHE_PREFIX = 'momentkeep_cache_'
// 默认缓存时间 - 5分钟（毫秒）
const DEFAULT_CACHE_TIME = 5 * 60 * 1000

/**
 * 缓存工具Hook
 * @description 提供统一的缓存读写接口，支持缓存过期自动清理
 * @returns {Object} 缓存操作方法集合
 */
export const useCache = () => {
  /**
   * 获取缓存数据
   * @description 从本地存储读取缓存，自动检查过期时间
   * @param {string} key - 缓存键
   * @returns {*} 缓存数据，过期或不存在返回null
   */
  const getCache = (key) => {
    try {
      const cacheData = uni.getStorageSync(CACHE_PREFIX + key)
      if (!cacheData) return null

      const { data, timestamp, expireTime } = cacheData
      const now = Date.now()
      const effectiveExpireTime = expireTime || DEFAULT_CACHE_TIME

      // 检查缓存是否过期
      if (now - timestamp > effectiveExpireTime) {
        uni.removeStorageSync(CACHE_PREFIX + key)
        return null
      }

      return data
    } catch (e) {
      console.error('读取缓存失败:', e)
      return null
    }
  }

  /**
   * 设置缓存数据
   * @description 将数据写入本地存储，带时间戳
   * @param {string} key - 缓存键
   * @param {*} data - 要缓存的数据
   */
  const setCache = (key, data) => {
    try {
      uni.setStorageSync(CACHE_PREFIX + key, {
        data,
        timestamp: Date.now()
      })
    } catch (e) {
      console.error('写入缓存失败:', e)
    }
  }

  /**
   * 删除指定缓存
   * @param {string} key - 缓存键
   */
  const removeCache = (key) => {
    try {
      uni.removeStorageSync(CACHE_PREFIX + key)
    } catch (e) {
      console.error('删除缓存失败:', e)
    }
  }

  /**
   * 带缓存的请求方法
   * @description 优先返回缓存数据，后台异步更新数据，实现缓存优先、后台更新的策略
   * @param {string} key - 缓存键
   * @param {Function} fetchFn - 数据获取函数
   * @param {number} cacheTime - 缓存时间（毫秒），默认5分钟
   * @returns {Promise} 返回数据Promise
   */
  const fetchWithCache = async (key, fetchFn, cacheTime = DEFAULT_CACHE_TIME) => {
    const cachedData = getCache(key)

    // 缓存命中则立即返回，后台异步更新
    if (cachedData !== null) {
      setTimeout(async () => {
        try {
          const freshData = await fetchFn()
          setCacheWithTime(key, freshData, cacheTime)
        } catch (e) {
          console.error('后台更新数据失败:', e)
        }
      }, 0)
      return cachedData
    }

    // 缓存未命中则直接请求
    const freshData = await fetchFn()
    setCacheWithTime(key, freshData, cacheTime)
    return freshData
  }

  /**
   * 设置带过期时间的缓存
   * @param {string} key - 缓存键
   * @param {*} data - 缓存数据
   * @param {number} cacheTime - 过期时间（毫秒）
   */
  const setCacheWithTime = (key, data, cacheTime) => {
    try {
      uni.setStorageSync(CACHE_PREFIX + key, {
        data,
        timestamp: Date.now(),
        expireTime: cacheTime
      })
    } catch (e) {
      console.error('写入缓存失败:', e)
    }
  }

  // 导出缓存操作方法
  return {
    getCache,
    setCache,
    removeCache,
    fetchWithCache,
    setCacheWithTime
  }
}