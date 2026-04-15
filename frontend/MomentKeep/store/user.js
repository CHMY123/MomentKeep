import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
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
  
  getters: {
    getUserInfo: (state) => state.userInfo,
    getToken: (state) => state.token,
    getIsLoggedIn: (state) => state.isLoggedIn
  },
  
  actions: {
    setUserInfo(info) {
      this.userInfo = info
      this.isLoggedIn = true
      // 保存到本地存储
      uni.setStorageSync('userInfo', info)
    },
    
    setUser(user) {
      this.userInfo = user
      this.isLoggedIn = true
      // 保存到本地存储
      uni.setStorageSync('userInfo', user)
    },
    
    setToken(token) {
      this.token = token
      // 保存到本地存储
      uni.setStorageSync('token', token)
    },
    
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
    },
    
    // 初始化用户信息
    initUserInfo() {
      const userInfo = uni.getStorageSync('userInfo')
      const token = uni.getStorageSync('token')
      if (userInfo && token) {
        this.userInfo = userInfo
        this.token = token
        this.isLoggedIn = true
      }
    }
  }
})