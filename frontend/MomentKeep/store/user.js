/**
 * MomentKeep 朝暮记 - 用户状态管理
 * @description 管理用户登录状态、用户信息和认证Token
 * @author MomentKeep Team
 * @since 2026-04-18
 */
import { defineStore } from 'pinia'

/**
 * 用户状态管理Store
 * @description 提供用户登录状态管理、用户信息存取、Token管理等功能
 */
export const useUserStore = defineStore('user', {
  // 状态定义 - 存储用户信息、认证令牌和登录状态
  state: () => ({
    userInfo: {
      id: '',
      username: '',
      nickname: '',
      email: '',
      phone: '',
      avatar: ''
    },
    token: '',
    isLoggedIn: false
  }),

  // Getters - 计算属性，提供只读的状态访问接口
  getters: {
    /**
     * 获取用户信息
     * @returns {Object} 用户信息对象
     */
    getUserInfo(state) {
      return state.userInfo
    },
    /**
     * 获取认证令牌
     * @returns {string} JWT令牌
     */
    getToken(state) {
      return state.token
    },
    /**
     * 获取登录状态
     * @returns {boolean} 是否已登录
     */
    getIsLoggedIn(state) {
      return state.isLoggedIn
    }
  },

  // Actions - 异步操作，处理用户登录、登出等业务逻辑
  actions: {
    /**
     * 设置用户信息并保存到本地存储
     * @description 同时更新状态和本地缓存，自动处理背景图片
     * @param {Object} info - 用户信息对象
     */
    setUserInfo(info) {
      this.userInfo = info
      this.isLoggedIn = true
      uni.setStorageSync('userInfo', info)
      if (info.backgroundImage && info.backgroundImage.trim() !== '') {
        uni.setStorageSync('backgroundImage', info.backgroundImage)
      } else {
        uni.removeStorageSync('backgroundImage')
      }
    },

    /**
     * 设置用户信息（兼容性别名）
     * @description 功能同setUserInfo，保持兼容性
     * @param {Object} user - 用户信息对象
     */
    setUser(user) {
      this.userInfo = user
      this.isLoggedIn = true
      uni.setStorageSync('userInfo', user)
      if (user.backgroundImage && user.backgroundImage.trim() !== '') {
        uni.setStorageSync('backgroundImage', user.backgroundImage)
      } else {
        uni.removeStorageSync('backgroundImage')
      }
    },

    /**
     * 设置认证令牌
     * @description 保存JWT令牌到状态和本地存储
     * @param {string} token - JWT认证令牌
     */
    setToken(token) {
      this.token = token
      uni.setStorageSync('token', token)
    },

    /**
     * 用户登出
     * @description 清除用户状态和本地存储的登录信息
     */
    logout() {
      this.userInfo = {
        id: '',
        username: '',
        nickname: '',
        email: '',
        phone: '',
        avatar: ''
      }
      this.token = ''
      this.isLoggedIn = false
      // 清除本地存储
      uni.removeStorageSync('userInfo')
      uni.removeStorageSync('token')
      uni.removeStorageSync('backgroundImage')
    },

    /**
     * 初始化用户信息
     * @description 从本地存储恢复用户登录状态，用于页面刷新后恢复登录
     */
    initUserInfo() {
      try {
        const userInfo = uni.getStorageSync('userInfo')
        const token = uni.getStorageSync('token')
        if (userInfo && token) {
          this.userInfo = userInfo
          this.token = token
          this.isLoggedIn = true
        }
      } catch (error) {
        console.error('恢复登录状态失败:', error)
      }
    }
  }
})