import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    activeMenu: 'home',
    isMobile: false
  }),

  getters: {
    getActiveMenu(state) {
      return state.activeMenu
    },
  },

  actions: {
    setActiveMenu(menuId) {
      this.activeMenu = menuId
    },
  }
})