<template>
  <div class="layout-container">
    <!-- 背景层 -->
    <div class="background-layer" :style="currentBackgroundStyle"></div>
    
    <!-- 侧边栏遮罩层 -->
    <div 
      v-if="isMobile && isSidebarOpen" 
      class="sidebar-overlay"
      @click="toggleSidebar"
    ></div>
    
    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ 'sidebar-active': isSidebarOpen, 'sidebar-mobile': isMobile }">
      <div class="user-info">
        <div class="avatar" @click="navigateToProfile" :style="{ backgroundImage: `url(${userAvatar})` }"></div>
        <div class="user-name">{{ userName }}</div>
      </div>
      
      <div class="menu-list">
        <div 
          v-for="item in menuItems" 
          :key="item.id"
          class="menu-item"
          :class="{ 'menu-item-active': activeMenu === item.id }"
          @click="navigateTo(item.path, item.id)"
        >
          <div class="menu-icon" :class="getIconClass(item.icon)"></div>
          <span :class="{ 'menu-text-active': activeMenu === item.id }"> {{ item.name }}</span>
        </div>
      </div>
    </div>
    
    <!-- 悬浮侧边栏切换按钮 -->
    <div 
      v-if="isMobile"
      class="floating-toggle-btn"
      :class="{ 'btn-hidden': isSidebarOpen }"
      @click="toggleSidebar"
    >
      <div class="hamburger-icon"></div>
    </div>
    
    <!-- 主内容区域 -->
    <div class="main-content" :class="{ 'main-content-active': isSidebarOpen && !isMobile }">
      <div class="header">
        <div v-if="!isMobile" class="menu-icon sidebar-icon" @click="toggleSidebar"></div>
        <div class="header-title">{{ currentPageTitle }}</div>
        <div class="header-right">
          <div class="chat-icon" @click="toggleAIChat"></div>
        </div>
      </div>
      
      <!-- 页面内容 -->
      <div class="page-content">
        <slot></slot>
      </div>
    </div>
    
    <!-- AI聊天侧边栏 -->
    <div class="ai-sidebar" :class="{ 'ai-sidebar-active': isAIChatOpen }">
      <div class="ai-header">
        <span class="ai-title">AI助手</span>
        <div class="close-icon" @click="toggleAIChat">×</div>
      </div>
      <div class="ai-content">
        <div class="ai-message ai-message-bot">
          <div class="ai-avatar" :style="{ backgroundImage: `url('/static/avatar/AI assistant.png')` }"></div>
          <div class="ai-message-content">
            <span>你好！我是你的AI助手，有什么可以帮助你的吗？</span>
          </div>
        </div>
        <div v-for="(message, index) in aiMessages" :key="index" class="ai-message" :class="message.type === 'user' ? 'ai-message-user' : 'ai-message-bot'">
          <div v-if="message.type === 'user'" class="ai-avatar" :style="{ backgroundImage: `url(${userAvatar})` }"></div>
          <div v-else class="ai-avatar" :style="{ backgroundImage: `url('/static/avatar/AI assistant.png')` }"></div>
          <div class="ai-message-content">
            <div v-if="message.loading" class="loading-indicator">
              <span class="loading-dot"></span>
              <span class="loading-dot"></span>
              <span class="loading-dot"></span>
            </div>
            <div v-else class="markdown-content" v-html="marked(message.content)"></div>
          </div>
        </div>
      </div>
      <div class="ai-input">
        <input type="text" v-model="aiInput" placeholder="输入你的问题..." />
        <button @click="sendAIMessage">发送</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '../store/user'
import { useAppStore } from '../store/app'
import { post, get } from '../utils/request'
import { marked } from 'marked'

const userStore = useUserStore()
const appStore = useAppStore()

const isSidebarOpen = ref(false) // 默认关闭侧边栏
const isAIChatOpen = ref(false)
const aiMessages = ref([])
const aiInput = ref('')
const currentPageTitle = ref('朝暮记')
const isMobile = ref(false)

const activeMenu = computed(() => appStore.activeMenu)

const themeBackgrounds = [
  { name: '雾感森林', image: '/static/background/Misty Forest.jpg' },
  { name: '简约线条', image: '/static/background/minimalist lines.png' },
  { name: '温柔肌理', image: '/static/background/gentle texture.jpg' }
]

const currentBackgroundStyle = ref({})

const updateBackgroundStyle = () => {
  try {
    const backgroundImage = uni.getStorageSync('backgroundImage')
    const selectedTheme = uni.getStorageSync('selectedTheme')

    let bgImage = ''
    if (backgroundImage) {
      bgImage = backgroundImage
    } else if (selectedTheme !== null && parseInt(selectedTheme) >= 0 && parseInt(selectedTheme) < themeBackgrounds.length) {
      bgImage = themeBackgrounds[parseInt(selectedTheme)].image
    }

    if (bgImage) {
      currentBackgroundStyle.value = {
        backgroundImage: `url(${bgImage})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat'
      }
    } else {
      // 添加默认背景色，避免微信小程序中背景变成纯白
      currentBackgroundStyle.value = {
        backgroundColor: '#F2EEE8'
      }
    }
  } catch (e) {
    console.error('读取背景设置失败:', e)
    // 出错时也添加默认背景色
    currentBackgroundStyle.value = {
      backgroundColor: '#F2EEE8'
    }
  }
}

updateBackgroundStyle()

const userAvatar = computed(() => userStore.getUserInfo.avatar || 'https://img.icons8.com/ios-filled/50/000000/user.png')
const userName = computed(() => userStore.getUserInfo.nickname || userStore.getUserInfo.username || '用户')

// 菜单配置
const menuItems = [
  { id: 'home', name: '首页', icon: 'home', path: 'pages/index/index' },
  { id: 'checkin', name: '每日打卡', icon: 'clock', path: 'pages/daily-checkin/daily-checkin' },
  { id: 'todo', name: '今日待办', icon: 'todo-list', path: 'pages/todo/todo' },
  { id: 'countdown', name: '未来倒计时', icon: 'time', path: 'pages/countdown/countdown' },
  { id: 'settings', name: '设置', icon: 'settings', path: 'pages/settings/settings' },
  { id: 'about', name: '关于我们', icon: 'info', path: 'pages/about/about' }
]

// 获取图标类名
const getIconClass = (iconName) => {
  const iconClasses = {
    'home': 'icon-home',
    'clock': 'icon-clock',
    'todo-list': 'icon-todo',
    'time': 'icon-time',
    'settings': 'icon-settings',
    'info': 'icon-info'
  }
  return iconClasses[iconName] || 'icon-default'
}

// 方法
const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value
}

const toggleAIChat = () => {
  isAIChatOpen.value = !isAIChatOpen.value
  if (isAIChatOpen.value && userStore.getToken) {
    loadChatHistory()
  }
}

const navigateTo = (path, menuId) => {
  appStore.setActiveMenu(menuId)
  // 使用uni.reLaunch避免页面栈溢出和switchTab错误
  uni.reLaunch({
    url: `/${path}`
  })
  if (isMobile.value) {
    isSidebarOpen.value = false
  }
}

const navigateToProfile = () => {
  // 个人资料页面使用reLaunch
  uni.reLaunch({
    url: '/pages/profile/profile'
  })
  // 在移动端自动关闭侧边栏
  if (isMobile.value) {
    isSidebarOpen.value = false
  }
}

const sendAIMessage = async () => {
  if (aiInput.value.trim() && userStore.getToken) {
    const userMessage = aiInput.value.trim()
    aiMessages.value.push({ type: 'user', content: userMessage })
    aiInput.value = ''

    try {
      // 添加一个临时的加载消息
      const loadingMessageId = aiMessages.value.length
      aiMessages.value.push({ type: 'bot', content: '', loading: true })

      const response = await post('/ai/chat', { message: userMessage }, {
        'Authorization': `Bearer ${userStore.getToken}`
      })

      if (response.code === 200) {
        const aiResponse = response.data
        // 替换加载消息为实际的AI回复
        aiMessages.value[loadingMessageId] = { type: 'bot', content: aiResponse }
        
        // 模拟流式输出
        const message = aiMessages.value[loadingMessageId]
        const fullContent = message.content
        message.content = ''
        message.loading = false
        
        let index = 0
        const interval = setInterval(() => {
          if (index < fullContent.length) {
            message.content += fullContent[index]
            index++
          } else {
            clearInterval(interval)
          }
        }, 50)
      } else {
        aiMessages.value[loadingMessageId] = { type: 'bot', content: '抱歉，AI助手暂时无法回复。' }
      }
    } catch (error) {
      console.error('AI chat error:', error)
      aiMessages.value[aiMessages.value.length - 1] = { type: 'bot', content: '网络错误，请稍后再试。' }
    }
  } else if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
  }
}

const loadChatHistory = async () => {
  if (!userStore.getToken) return

  try {
    const response = await get('/ai/history', {}, {
      'Authorization': `Bearer ${userStore.getToken}`
    })

    if (response.code === 200 && response.data) {
      const history = response.data
      aiMessages.value = []
      for (const msg of history) {
        if (msg.role === 'user') {
          aiMessages.value.push({ type: 'user', content: msg.content })
        } else if (msg.role === 'assistant') {
          aiMessages.value.push({ type: 'bot', content: msg.content })
        }
      }
    }
  } catch (error) {
    console.error('Load chat history error:', error)
  }
}

// 检测是否为移动端
const checkMobile = () => {
  try {
    // 使用uni-app的API获取设备信息
    const systemInfo = uni.getSystemInfoSync()
    // 小程序环境或屏幕宽度小于768px视为移动端
    isMobile.value = systemInfo.platform === 'devtools' || systemInfo.platform === 'mp-weixin' || systemInfo.screenWidth < 768
  } catch (e) {
    // 降级方案：使用window对象
    if (typeof window !== 'undefined') {
      isMobile.value = window.innerWidth < 768
    } else {
      // 默认视为移动端
      isMobile.value = true
    }
  }
  // 移动端默认关闭侧边栏
  if (isMobile.value) {
    isSidebarOpen.value = false
  } else {
    isSidebarOpen.value = true
  }
}

// 处理背景更新事件
const handleBackgroundUpdate = (event) => {
  const { type, image, selectedTheme } = event.detail
  if (type === 'theme' && image) {
    currentBackgroundStyle.value = {
      backgroundImage: `url(${image})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat'
    }
    uni.setStorageSync('selectedTheme', selectedTheme.toString())
    uni.removeStorageSync('backgroundImage')
  } else if (type === 'custom' && image) {
    currentBackgroundStyle.value = {
      backgroundImage: `url(${image})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat'
    }
    uni.setStorageSync('backgroundImage', image)
    uni.setStorageSync('selectedTheme', '-1')
  }
}

// 生命周期
onMounted(async () => {
  updateBackgroundStyle()
  userStore.initUserInfo()

  if (userStore.getToken) {
    try {
      const response = await get('/user/profile', {}, {
        'Authorization': `Bearer ${userStore.getToken}`
      })
      if (response.code === 200) {
        const profile = response.data
        userStore.setUserInfo(profile)
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }

  checkMobile()
  updateActiveMenu() // 直接调用，不延迟

  // 只在浏览器环境中添加事件监听器
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', checkMobile)
    window.addEventListener('background-updated', handleBackgroundUpdate)
  }
})

const updateActiveMenu = () => {
  try {
    const pages = getCurrentPages()
    if (pages && pages.length > 0) {
      const currentPage = pages[pages.length - 1]
      if (currentPage && currentPage.route) {
        const currentPath = currentPage.route

        const menuItem = menuItems.find(item => item.path === currentPath)
        if (menuItem) {
          currentPageTitle.value = menuItem.name
          appStore.setActiveMenu(menuItem.id)
        }
      }
    }
  } catch (error) {
    console.error('获取当前页面路径失败:', error)
  }
}

onUnmounted(() => {
  // 移除窗口大小监听
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', checkMobile)
    window.removeEventListener('background-updated', handleBackgroundUpdate)
  }
})
</script>

<style scoped>
/* 布局容器 */
.layout-container {
  display: flex;
  height: 100vh;
  position: relative;
  color: #333333;
  transition: all 0.3s ease;
  overflow: hidden;
  background-color: #F2EEE8;
}

/* 背景层 */
.background-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  filter: blur(8px);
  transition: background-image 0.3s ease, opacity 0.3s ease;
  background-color: #F2EEE8;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

/* 确保内容层在背景之上 */
.sidebar,
.main-content,
.sidebar-overlay,
.floating-toggle-btn,
.ai-sidebar {
  position: relative;
  z-index: 1;
}

/* 侧边栏遮罩层 */
.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 98;
  transition: opacity 0.3s ease;
}

/* 侧边栏样式 */
.sidebar {
  width: 240px;
  background: rgba(242, 238, 232, 0.8);
  backdrop-filter: blur(10px);
  box-shadow: 1px 0 4px rgba(0, 0, 0, 0.1);
  border-right: 1px solid rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease, background 0.3s ease, color 0.3s ease, border-color 0.3s ease;
  display: flex;
  flex-direction: column;
  padding-top: 20px;
  position: relative;
  z-index: 99;
  color: #333333;
}

.sidebar-active {
  transform: translateX(0);
}

/* 悬浮侧边栏切换按钮 */
.floating-toggle-btn {
  position: fixed;
  top: 20px;
  left: 20px;
  width: 44px;
  height: 44px;
  background-color: #C2977F;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 97;
  transition: all 0.3s ease;
}

.floating-toggle-btn:hover {
  background-color: #94A7C8;
  transform: scale(1.05);
}

.floating-toggle-btn.btn-hidden {
  opacity: 0;
  pointer-events: none;
  transform: translateX(-60px);
}

.hamburger-icon {
  width: 20px;
  height: 14px;
  position: relative;
}

.hamburger-icon::before,
.hamburger-icon::after {
  content: '';
  position: absolute;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: white;
  transition: all 0.3s ease;
}

.hamburger-icon::before {
  top: 0;
}

.hamburger-icon::after {
  bottom: 0;
}

.hamburger-icon {
  background-color: white;
  height: 2px;
  width: 20px;
  position: relative;
}

.hamburger-icon::before {
  content: '';
  position: absolute;
  top: -6px;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: white;
}

.hamburger-icon::after {
  content: '';
  position: absolute;
  top: 6px;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: white;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  margin-bottom: 12px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background-color: #f0f0f0;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.user-name {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
}

.menu-list {
  flex: 1;
  padding: 0 20px;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.menu-item:hover {
  background-color: rgba(148, 167, 200, 0.1);
}

.menu-item-active {
  background-color: rgba(194, 151, 127, 0.1);
}

.menu-icon {
  margin-right: 12px;
  font-size: 20px;
  transition: color 0.3s ease;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 图标样式 */
.icon-home::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/000000/home.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-clock::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/000000/clock.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-todo::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/000000/checkmark--v1.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-time::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/000000/alarm-clock.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-settings::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/000000/settings.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-info::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/000000/info.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-default::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/000000/list.png);
  background-size: contain;
  background-repeat: no-repeat;
}

/* 侧边栏图标 */
.sidebar-icon::before {
  content: "";
  width: 24px;
  height: 24px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/000000/menu.png);
  background-size: contain;
  background-repeat: no-repeat;
}

/* 聊天图标 */
.chat-icon {
  font-size: 24px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-icon::before {
  content: "";
  width: 24px;
  height: 24px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/000000/chat.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.menu-text-active {
  color: #C2977F;
  font-weight: 500;
}

/* 主内容区域样式 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  background: transparent;
  color: #333333;
}

.header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  background: rgba(242, 238, 232, 0.8);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 10;
  transition: all 0.3s ease;
}

.header-title {
  flex: 1;
  font-size: 18px;
  font-weight: 600;
  color: #C2977F;
}

.header-right {
  cursor: pointer;
}

.chat-icon {
  font-size: 24px;
}

.page-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

/* AI聊天侧边栏样式 */
.ai-sidebar {
  position: fixed;
  right: -300px;
  top: 0;
  width: 300px;
  height: 100vh;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  box-shadow: -1px 0 4px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  z-index: 100;
  color: #333333;
}

.ai-sidebar-active {
  right: 0;
}

.ai-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.ai-title {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
}

.close-icon {
  cursor: pointer;
  font-size: 24px;
}

.ai-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.ai-message {
  display: flex;
  margin-bottom: 16px;
}

.ai-message-bot {
  flex-direction: row;
}

.ai-message-user {
  flex-direction: row-reverse;
}

.ai-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #94A7C8;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 500;
  margin: 0 8px;
}

.ai-message-bot .ai-avatar {
  background-color: #94A7C8;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
}

.ai-message-bot .ai-avatar::before {
  display: none;
}

.ai-message-bot .ai-avatar::after {
  display: none;
}

.ai-message-user .ai-avatar {
  background-color: #C2977F;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
}

.ai-message-content {
  flex: 1;
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 16px;
  background-color: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  color: #333333;
}

.ai-message-user .ai-message-content {
  background-color: #C2977F;
  color: white;
}

/* 加载指示器 */
.loading-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
}

.loading-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #94A7C8;
  animation: pulse 1.5s infinite ease-in-out;
}

.loading-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.6;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

/* Markdown内容样式 */
.markdown-content {
  line-height: 1.6;
}

.markdown-content h1, .markdown-content h2, .markdown-content h3 {
  font-weight: 600;
  margin: 12px 0 8px 0;
  color: inherit;
}

.markdown-content h1 {
  font-size: 18px;
}

.markdown-content h2 {
  font-size: 16px;
}

.markdown-content h3 {
  font-size: 14px;
}

.markdown-content p {
  margin: 8px 0;
}

.markdown-content ul, .markdown-content ol {
  margin: 8px 0;
  padding-left: 20px;
}

.markdown-content li {
  margin: 4px 0;
}

.markdown-content strong {
  font-weight: 600;
}

.markdown-content em {
  font-style: italic;
}

.markdown-content code {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 2px 4px;
  border-radius: 4px;
  font-size: 12px;
  font-family: monospace;
}

.markdown-content pre {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.markdown-content pre code {
  background-color: transparent;
  padding: 0;
  font-size: 12px;
}

.markdown-content a {
  color: #C2977F;
  text-decoration: underline;
}

/* 用户消息的Markdown样式 */
.ai-message-user .markdown-content {
  color: white;
}

.ai-message-user .markdown-content a {
  color: #F2EEE8;
}

.ai-message-user .markdown-content code {
  background-color: rgba(255, 255, 255, 0.1);
}

.ai-message-user .markdown-content pre {
  background-color: rgba(255, 255, 255, 0.1);
}

.ai-message-user .markdown-content pre code {
  background-color: transparent;
}

.ai-input {
  padding: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 64px;
  box-sizing: border-box;
}

.ai-input input {
  flex: 1;
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #D8C8BE;
  border-radius: 22px;
  background-color: white;
  font-size: 14px;
  color: #333333;
  outline: none;
  box-sizing: border-box;
}

.ai-input input:focus {
  border-color: #C2977F;
}

.ai-input button {
  flex-shrink: 0;
  height: 44px;
  padding: 0 20px;
  background-color: #C2977F;
  color: white;
  border: none;
  border-radius: 22px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-input button:hover {
  background-color: #94A7C8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: 99;
    transform: translateX(-100%);
  }
  
  .sidebar-active {
    transform: translateX(0);
  }
  
  .main-content {
    margin-left: 0;
  }
  
  .header {
    padding-left: 70px;
  }
  
  .page-content {
    padding: 16px;
  }
}

@media (min-width: 769px) {
  .sidebar {
    transform: translateX(0) !important;
  }
  
  .floating-toggle-btn {
    display: none;
  }
}
</style>