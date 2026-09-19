import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    activeMenu: 'home',
    sidebarOpen: true,
    isMobile: false
  }),

  getters: {
    getActiveMenu(state) {
      return state.activeMenu
    },
    getSidebarOpen(state) {
      return state.sidebarOpen
    },
    getIsMobile(state) {
      return state.isMobile
    }
  },

  actions: {
    setActiveMenu(menuId) {
      this.activeMenu = menuId
    },
    setSidebarOpen(isOpen) {
      this.sidebarOpen = isOpen
    },
    setIsMobile(isMobile) {
      this.isMobile = isMobile
    }
  }
})