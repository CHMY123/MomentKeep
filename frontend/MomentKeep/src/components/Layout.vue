<template>
  <div class="layout-container">
    <!-- 背景层 -->
    <div class="background-layer" :class="{ 'has-custom-bg': !!currentBackgroundStyle.backgroundImage }" :style="currentBackgroundStyle"></div>
    
    <!-- 侧边栏遮罩层 -->
    <div 
      v-if="isMobile && isSidebarOpen" 
      class="sidebar-overlay"
      @click="toggleSidebar"
    ></div>
    
    <!-- 侧边栏 -->
    <div 
      class="sidebar" 
      :class="{ 'sidebar-active': isSidebarOpen, 'sidebar-mobile': isMobile }" 
      :style="{ paddingTop: isMobile ? (20 + safeAreaTop) + 'px' : '20px' }"
      @touchstart="handleSidebarTouchStart"
      @touchmove="handleSidebarTouchMove"
      @touchend="handleSidebarTouchEnd"
    >
      <div class="user-info">
        <div class="avatar" @click="navigateToProfile" :style="{ backgroundImage: toCssUrl(userAvatar) }"></div>
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
    
    <!-- 悬浮聊天按钮 -->
    <div 
      v-if="AI_FEATURE_ENABLED && isMobile && !isAIChatOpen"
      class="floating-chat-btn"
      @click="toggleAIChat"
    >
      <div class="chat-icon"></div>
    </div>
    
    <!-- 主内容区域 -->
    <div class="main-content" :class="{ 'main-content-active': isSidebarOpen && !isMobile }">
      <div class="header" :style="{ paddingTop: isMobile ? safeAreaTop + 'px' : '0', height: isMobile ? (56 + safeAreaTop) + 'px' : '56px' }">
        <div v-if="!isMobile" class="menu-icon sidebar-icon" @click="toggleSidebar"></div>
        <div class="header-title">{{ currentPageTitle }}</div>
        <div class="header-right">
          <div v-if="AI_FEATURE_ENABLED && !isMobile" class="chat-icon" @click="toggleAIChat"></div>
        </div>
      </div>
      
      <!-- 页面内容 -->
      <div class="page-content">
        <slot></slot>
      </div>
    </div>
    
    <!-- 侧边栏遮罩层（小程序和APP端） -->
    <!-- #ifdef MP-WEIXIN || APP-PLUS -->
    <div 
      v-if="isSidebarOpen || isAIChatOpen" 
      class="sidebar-overlay"
      @click="handleOverlayClick"
    ></div>
    <!-- #endif -->
    
    <!-- AI聊天侧边栏 -->
    <div 
      class="ai-sidebar" 
      :class="{ 'ai-sidebar-active': isAIChatOpen }"
      @touchstart="handleTouchStart"
      @touchmove="handleTouchMove"
      @touchend="handleTouchEnd"
    >
      <div class="ai-header" :style="{ height: isMobile ? (48 + safeAreaTop) + 'px' : '48px', paddingTop: isMobile ? safeAreaTop + 'px' : '0' }">
        <span class="ai-title">AI助手</span>
        <span v-if="aiQuota" class="ai-quota">今日剩余 {{ aiQuota.remaining }}/{{ aiQuota.limit }} 次</span>
        <!-- #ifdef H5 -->
        <div class="close-icon" @click="toggleAIChat">×</div>
        <!-- #endif -->
      </div>
      <div class="ai-content">
        <div class="ai-message ai-message-bot">
          <div class="ai-avatar" :style="{ backgroundImage: `url('https://momentkeep.s3.bitiful.net/avatars/AI%20assistant.png')` }"></div>
          <div class="ai-message-content">
            <span>想聊点什么？可以问打卡、待办或专注相关的问题。</span>
          </div>
        </div>
        <div v-for="(message, index) in aiMessages" :key="index" class="ai-message stagger-item" :class="message.type === 'user' ? 'ai-message-user' : 'ai-message-bot'">
          <div v-if="message.type === 'user'" class="ai-avatar" :style="{ backgroundImage: toCssUrl(userAvatar || 'https://momentkeep.s3.bitiful.net/logos/logo.png') }"></div>
          <div v-else class="ai-avatar" :style="{ backgroundImage: `url('https://momentkeep.s3.bitiful.net/avatars/AI%20assistant.png')` }"></div>
          <div class="ai-message-content">
            <div v-if="message.loading" class="loading-indicator">
              <span class="loading-dot"></span>
              <span class="loading-dot"></span>
              <span class="loading-dot"></span>
            </div>
            <!--
              只保留一个 v-else（外层容器），平台差异放在内部用条件编译区分：
              若把 #ifdef/#endif 夹在两个 v-else 之间，预处理前会出现「一个 v-if 两个 v-else」，
              模板与 lint 都会报错。
              H5：marked 渲染 Markdown 后必须用 DOMPurify 消毒再交给 v-html——
                  marked 自身不做安全过滤，直接 v-html 会让 AI 回复 / 历史里的
                  <img onerror=...> 在页面执行，而 token 存于 localStorage，可被窃取。
              小程序 / App：不支持 v-html，退化为纯文本渲染（换行由 CSS 保留）。
            -->
            <div v-else class="markdown-content">
              <!-- #ifdef H5 -->
              <div v-html="renderSafeMarkdown(message.content)"></div>
              <!-- #endif -->
              <!-- #ifndef H5 -->
              <text>{{ message.content }}</text>
              <!-- #endif -->
            </div>
          </div>
        </div>
      </div>
      <!--
        这里原本内联注入 paddingBottom: 44px（想把输入栏垫到手机底部指示条之上），
        但 .ai-input 同时写死了 height: 60px，而全局 reset 是 box-sizing: border-box ——
        44px 的内边距把内容区压到只剩 16px，却要塞进两个 44px 高的子元素，
        于是小程序 / App 上整行被压扁、子元素溢出错位（H5 因为多一层容器侥幸看不出来）。
        改为由 CSS 用 min-height + 安全区内边距处理，这里不再注入。
      -->
      <div class="ai-input">
        <input type="text" v-model="aiInput" placeholder="输入问题…" />
        <button @click="sendAIMessage">发送</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '../store/user'
import { useAppStore } from '../store/app'
import { post, get, toCssUrl } from '../utils/request'
import { useCache } from '../utils/cache'
// H5 专用：Markdown 渲染与消毒。小程序 / App 端不支持 v-html，无需引入这两个包
// #ifdef H5
import { marked } from 'marked'
import DOMPurify from 'dompurify'

/**
 * AI 助手功能开关。
 *
 * @description 当前阶段 AI 功能暂不开放，界面入口先隐藏；全部实现代码保留，
 *              改回 true 即可恢复（悬浮按钮重新出现、面板可正常打开）。
 *              用开关而不是删代码：需求回摆时不必从 git 里捞回来。
 */
const AI_FEATURE_ENABLED = false
// #endif

const userStore = useUserStore()
const appStore = useAppStore()
const { getCache, setCache, fetchWithCache } = useCache()

/**
 * 渲染并消毒 AI 回复中的 Markdown（仅 H5 使用）
 * @description marked 只负责语法转换、不做安全过滤，必须再用 DOMPurify 清洗，
 *              否则模型回复或历史记录中的 HTML 会在页面上执行
 * @param {string} content 原始回复内容
 * @returns {string} 可安全交给 v-html 的 HTML
 */
// #ifdef H5
const renderSafeMarkdown = (content) => {
  if (!content) return ''
  try {
    return DOMPurify.sanitize(marked(content), { USE_PROFILES: { html: true } })
  } catch (e) {
    console.error('Markdown 渲染失败，降级为纯文本:', e)
    // 此处必须转义后再返回：降级路径同样不能把原文直接交给 v-html
    return String(content).replace(/[&<>"']/g, ch => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    }[ch]))
  }
}
// #endif

const isSidebarOpen = ref(false) // 默认关闭侧边栏
const isAIChatOpen = ref(false)
const aiMessages = ref([])
const aiInput = ref('')
const currentPageTitle = ref('朝暮记')
const isMobile = ref(false)
const statusBarHeight = ref(0) // 状态栏高度
const safeAreaTop = ref(0) // 安全区域顶部高度
/** 今日 AI 配额 { limit, used, remaining }，用于展示"今日剩余 N 次" */
const aiQuota = ref(null)
/** 打字机定时器句柄，必须在页面卸载时清理，否则会在组件销毁后继续改状态 */
let typingTimer = null

const activeMenu = computed(() => appStore.activeMenu)

const currentBackgroundStyle = ref({})

const updateBackgroundStyle = () => {
  try {
    const backgroundImage = uni.getStorageSync('backgroundImage')

    let bgImage = ''
    if (backgroundImage) {
      bgImage = backgroundImage
    }

    if (bgImage) {
      currentBackgroundStyle.value = {
        backgroundImage: toCssUrl(bgImage),
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat'
      }
    } else {
      // 添加默认背景色，避免微信小程序中背景变成纯白。
      // 【必须用主题变量】这里是内联样式，优先级高于所有样式表规则：
      // 写死浅米色会让它盖住深色主题的页面底色（实测深色下内容区约 43% 的像素仍是这个浅色）。
      // 变量取不到时（小程序 / App 端不换肤）兜底值仍是原本的浅米色。
      currentBackgroundStyle.value = {
        backgroundColor: 'var(--surface-color, #F2EEE8)'
      }
    }
  } catch (e) {
    console.error('读取背景设置失败:', e)
    // 出错时也添加默认背景色（同样用主题变量，理由见上）
    currentBackgroundStyle.value = {
      backgroundColor: 'var(--surface-color, #F2EEE8)'
    }
  }
}

updateBackgroundStyle()

/**
 * 用户信息中的背景图一旦变化就立即重新应用。
 *
 * @description 原先只在 setup 与 onMounted 里各读一次本地缓存，而 onMounted 中的
 * 读取发生在 await 拉取用户资料之前，拿到资料后不再刷新，
 * 因此登录后背景图要等组件重挂载（切页）或刷新才出现。
 * 这里监听 store 而非 storage：所有写入路径（登录、拉资料、改资料）都会经过 setUserInfo，
 * 而 setUserInfo 是先同步写 storage 再触发响应式更新，所以这里读到的缓存值一定是最新的。
 */
watch(() => userStore.getUserInfo.backgroundImage, () => {
  updateBackgroundStyle()
})

// 侧边栏头像（模板直接引用该 setup 绑定）。
// 注意：此声明一旦缺失，模板会退化为读取实例属性（undefined），
// 表现为头像空白，并在控制台报 "Property userAvatar was accessed during render but is not defined"。
const userAvatar = computed(() => userStore.getUserInfo.avatar || 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAADu0lEQVR4AexYS0hUYRQ+5zcXFWREr03vqKgoKrKIolU5Cq0Cm2lROQZGtC2hgqQHVIs20cLKaxQ51xa1EJ0ZFawgLFDoQQ+zVesiWvRQ857+MyWKzP+4c68vmMs9zPF833l8/z/3Xu8ImOJHXsBEb2B+B/I7EHAF8l+hgAsYOD30HUi7daUp13FSCedZyq37mjH2E05dWmKBJx5VIDQBXbW1hUm37ioBNsseFYCwAwDnZIx9hDhjUthl5kJIRygCWtw7K74UFXYi4Ek5F0pTnSiFVTOXc1QkP/FQBAjyLsmmW6TZnlsQPBZry1fyAgtIurejclUPKDsoAASoCuOaCCwASBxTzGgMe4TVRpKBEEgA1dQIufrFhh5qGKE4U0PNMCLCyNAQ2tYu2SC/CtM1FC3Eua2rlqzSkgxgIAHeoLfdUN8IexisRjABKF4aJzQQCMUHA0ULCy1qAAdn9r8mIs9AU8JE5M0WM18pCRZAIAH79lX9lBdxr0Wf7BSE3h3l5b+yg3bRQAK4BZI4z5+5mCA8l0veyBwfAkamDfuRWEUDEFwZjlh6MqckFm+0ZCtpgQVw5ZKez6cB6Cn7dkbJfzl2bB0rFAFYU+NFopW7BdFWAnijakhEXQUCN0huGeeoeH7ioQgYarg3VtkVOVCx0ROwTj6kqoDoHlvG98T6SDRevKe8QilwqI6fz1AFcGNEpLLy+LuSaPxmJFZ5iC3jHzzyljHmhGmhCwhzOJtaeQHZVqnjUf1sfuNKJ5xtbOxzLBs3aCyUHUi5zuZUor5avsC3JBPO974++ibA+0QIz9nY5xhjSddpTjU6pzgn6PCcn7OA9oZbC5KNztlUwnkvC3UD0mUALEWEWaA4GJN3pLL/D75uzk27zpn0w7vzFSnGsG8B/AIiV+/EHyF6kOCC/F9ojbGLioCwRj43LlL/wMekW3+ca6uoqrgvAR0NtXNTqxc9kcWuA2ARhHZgEQLdSK9e/LjlgTPPT1lrAekHt5b9xsIXiLjTTwNfXIRd6EFn+31nuW2elYCmptoZnlfQhgjWhW0HGM2T18iKgQJo5Z6jsWx/WwmY9qPwGhfOVmAsYtyLe9rUNgpoSdzeJAtW2RQLk8M9ubepplGAQHHYVGSscAHC+IOZUYC8Z8fGakBjXaT9Jo5WQDJRv1Te53N+yJiam3FcmZlBQ9QKkHlLpU30qZ3BIMDTJo+LMhxcqOujFVAaq7wj36LQt0XjoeWURo+6OQvQJU4WTLsDk2VI3Rx5AbrVGQ9syu/AXwAAAP//8SRC4gAAAAZJREFUAwAgDj1wJiPQ2AAAAABJRU5ErkJggg==')

const userName = computed(() => userStore.getUserInfo.nickname || userStore.getUserInfo.username || '用户')

// 菜单配置
const menuItems = [
  { id: 'home', name: '首页', icon: 'home', path: 'pages/index/index' },
  { id: 'checkin', name: '每日打卡', icon: 'clock', path: 'pages/daily-checkin/daily-checkin' },
  { id: 'todo', name: '今日待办', icon: 'todo-list', path: 'pages/todo/todo' },
  { id: 'focus', name: '专注计时', icon: 'focus', path: 'pages/focus/focus' },
  { id: 'countdown', name: '未来倒计时', icon: 'time', path: 'pages/countdown/countdown' },
  { id: 'settings', name: '设置', icon: 'settings', path: 'pages/settings/settings' },
  { id: 'about', name: '关于我们', icon: 'info', path: 'pages/about/about' }
]

// 获取图标类名
const getIconClass = (iconName) => {
  const iconClasses = {
    'home': 'icon-home',
    'clock': 'icon-checkin',
    'todo-list': 'icon-todo',
    'focus': 'icon-focus',
    'time': 'icon-countdown',
    'settings': 'icon-settings',
    'info': 'icon-about'
  }
  return iconClasses[iconName] || 'icon-default'
}

// 方法
const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value
}

const toggleAIChat = () => {
  // AI 功能关闭期间：任何入口都不展开面板
  if (!AI_FEATURE_ENABLED) return
  isAIChatOpen.value = !isAIChatOpen.value
  if (isAIChatOpen.value && userStore.getToken) {
    loadChatHistory()
    loadAiQuota()
  }
  if (!isAIChatOpen.value && typingTimer) {
    // 面板关闭时停止打字机动画，避免无意义的定时任务
    clearInterval(typingTimer)
    typingTimer = null
  }
}

// 查询今日 AI 剩余次数
const loadAiQuota = async () => {
  if (!userStore.getToken) return
  try {
    const response = await get('/ai/quota')
    if (response.code === 200) {
      aiQuota.value = response.data
    }
  } catch (error) {
    console.error('查询 AI 配额失败:', error)
  }
}

// 滑动关闭相关变量
const touchStartX = ref(0)
const touchStartY = ref(0)
const touchCurrentX = ref(0)
const touchCurrentY = ref(0)
const isTouching = ref(false)

// 触摸开始
const handleTouchStart = (e) => {
  touchStartX.value = e.touches[0].clientX
  touchStartY.value = e.touches[0].clientY
  touchCurrentX.value = touchStartX.value
  isTouching.value = true
}

// 触摸移动
const handleTouchMove = (e) => {
  if (!isTouching.value) return
  touchCurrentX.value = e.touches[0].clientX
}

// 触摸结束
const handleTouchEnd = () => {
  if (!isTouching.value) return
  isTouching.value = false
  
  const deltaX = touchCurrentX.value - touchStartX.value
  
  // AI侧边栏：右滑关闭（从右向左滑动）
  if (isAIChatOpen.value && deltaX < -50) {
    toggleAIChat()
  }
}

// 左侧边栏触摸事件处理
const handleSidebarTouchStart = (e) => {
  touchStartX.value = e.touches[0].clientX
  touchStartY.value = e.touches[0].clientY
  touchCurrentX.value = touchStartX.value
  isTouching.value = true
}

const handleSidebarTouchMove = (e) => {
  if (!isTouching.value) return
  touchCurrentX.value = e.touches[0].clientX
}

const handleSidebarTouchEnd = () => {
  if (!isTouching.value) return
  isTouching.value = false
  
  const deltaX = touchCurrentX.value - touchStartX.value
  
  // 左侧边栏：左滑关闭（从左向右滑动）
  if (isSidebarOpen.value && deltaX > 50) {
    toggleSidebar()
  }
}

// 遮罩层点击处理（关闭侧边栏）
const handleOverlayClick = () => {
  if (isAIChatOpen.value) {
    toggleAIChat()
  } else if (isSidebarOpen.value) {
    toggleSidebar()
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

      // token 由 request 工具自动注入，无需手动传 Authorization
      const response = await post('/ai/chat', { message: userMessage })

      if (response.code === 200) {
        // 后端返回结构：{ reply, quota: { limit, used, remaining } }
        const aiResponse = (response.data && response.data.reply) || ''
        if (response.data && response.data.quota) {
          aiQuota.value = response.data.quota
        }

        // 替换加载消息为实际的AI回复
        aiMessages.value[loadingMessageId] = { type: 'bot', content: aiResponse }

        // 模拟流式输出（定时器句柄统一保存，便于在卸载/关闭时清理）
        const message = aiMessages.value[loadingMessageId]
        const fullContent = message.content
        message.content = ''
        message.loading = false

        if (typingTimer) {
          clearInterval(typingTimer)
        }
        let index = 0
        typingTimer = setInterval(() => {
          if (index < fullContent.length) {
            message.content += fullContent[index]
            index++
          } else {
            clearInterval(typingTimer)
            typingTimer = null
          }
        }, 50)
      } else {
        aiMessages.value[loadingMessageId] = { type: 'bot', content: '这次没收到回复，请重试；也可能是今日次数已用完。' }
      }
    } catch (error) {
      console.error('AI chat error:', error)
      // 配额超限(429)等业务错误由 request 工具抛出，直接把后端文案展示给用户
      aiMessages.value[aiMessages.value.length - 1] = {
        type: 'bot',
        content: error && error.message ? error.message : '网络连接失败，请检查网络后重试。'
      }
      // 失败时同步一次配额，保证展示的剩余次数与实际一致
      loadAiQuota()
    }
  } else if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
  }
}

const loadChatHistory = async () => {
  if (!userStore.getToken) return

  const cacheKey = `ai_chat_history_${userStore.userInfo?.id || 'guest'}`

  const fetchFn = async () => {
    const response = await get('/ai/history', {}, {
    })

    if (response.code === 200 && response.data) {
      return response.data
    }
    return []
  }

  try {
    const history = await fetchWithCache(cacheKey, fetchFn, 3600000) // 缓存1小时
    aiMessages.value = []
    for (const msg of history) {
      if (msg.role === 'user') {
        aiMessages.value.push({ type: 'user', content: msg.content })
      } else if (msg.role === 'assistant') {
        aiMessages.value.push({ type: 'bot', content: msg.content })
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
    
    // 获取状态栏高度和安全区域顶部高度
    statusBarHeight.value = systemInfo.statusBarHeight || 0
    if (systemInfo.safeArea && systemInfo.safeArea.top) {
      safeAreaTop.value = systemInfo.safeArea.top || 0
    } else {
      safeAreaTop.value = statusBarHeight.value
    }
  } catch (e) {
    // 降级方案：使用window对象
    if (typeof window !== 'undefined') {
      isMobile.value = window.innerWidth < 768
      // 浏览器环境默认状态栏高度为0
      statusBarHeight.value = 0
      safeAreaTop.value = 0
    } else {
      // 默认视为移动端
      isMobile.value = true
      statusBarHeight.value = 20 // 默认状态栏高度
      safeAreaTop.value = 20 // 默认安全区域顶部高度
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
  const { type, image } = event.detail || {}

  if (type === 'custom' && image) {
    currentBackgroundStyle.value = {
      backgroundImage: toCssUrl(image),
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat'
    }
    uni.setStorageSync('backgroundImage', image)
    return
  }

  // 恢复默认背景：以存储为准重新计算，避免残留旧背景图
  if (type === 'default') {
    uni.removeStorageSync('backgroundImage')
    updateBackgroundStyle()
  }
}

// 处理主题更新事件

// 生命周期
onMounted(async () => {
  // ⚠️ 顺序至关重要：布局判定与页面标题必须在任何 await 之前同步完成。
  // 侧边栏切换用的是 uni.reLaunch，它会销毁重建页面（Layout 随之重建），
  // 若这两步排在 await 拉取用户资料之后，每次切页都要等这个网络请求返回
  // 才把标题从"朝暮记"改成页面名、把布局切成移动端形态 ——
  // 用户会先看到几百毫秒的中间态（桌面版布局 + 应用名标题），切页一多就很疲劳。
  checkMobile()
  // 标题与侧边栏高亮：立即尝试，并在页面栈尚未就绪时自动重试（见 syncPageTitle 注释）
  syncPageTitle()

  updateBackgroundStyle()
  userStore.initUserInfo()

  // 事件监听同样不应受网络影响
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', checkMobile)
    window.addEventListener('background-updated', handleBackgroundUpdate)
  }

  // 资料刷新放到最后、纯后台进行，不阻塞首帧。
  // 本地已有用户资料时跳过请求：否则每次切页都要多打一次 /user/profile。
  if (userStore.getToken && !userStore.getUserInfo.id) {
    try {
      const response = await get('/user/profile', {}, {
      })
      if (response.code === 200) {
        const profile = response.data
        userStore.setUserInfo(profile)
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }
})

/**
 * 依据当前页面栈校准页面标题与侧边栏高亮
 * @returns {boolean} 是否成功匹配到当前页面
 */
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
          return true
        }
      }
    }
  } catch (error) {
    console.error('获取当前页面路径失败:', error)
  }
  return false
}

/** 标题校准重试定时器（组件销毁时需清理） */
let titleRetryTimer = null

/**
 * 校准标题与侧边栏高亮（带重试）
 *
 * @description 导航过程中 getCurrentPages() 存在短暂空窗期：实测 uni.reLaunch 切换页面后
 * 约 70ms 内该数组为空，之后才指向新页面（先关闭旧页面、再挂载新页面）。
 * 若只在 onMounted 里调用一次，恰好落进这个窗口就会匹配失败，
 * 而没有任何后续时机再纠正它，标题便会一直停在初始值「朝暮记」——
 * 各页面挂载时机不同，所以只有首页、每日打卡这类较重页面会偶发中招。
 * 因此改为：立即尝试，未命中则按递增间隔重试，命中即停。
 *
 * @param {number} [attempt] 当前重试轮次（递归内部使用）
 */
const syncPageTitle = (attempt = 0) => {
  if (updateActiveMenu()) return

  // 间隔贴近实测空窗期（约 75ms）：早于该窗口的重试注定失败，故先密后疏
  const retryDelays = [0, 30, 80, 150, 400, 900]
  if (attempt >= retryDelays.length) return

  if (titleRetryTimer) clearTimeout(titleRetryTimer)
  titleRetryTimer = setTimeout(() => {
    titleRetryTimer = null
    syncPageTitle(attempt + 1)
  }, retryDelays[attempt])
}

onUnmounted(() => {
  // 清理打字机定时器，避免组件销毁后仍在修改状态
  if (typingTimer) {
    clearInterval(typingTimer)
    typingTimer = null
  }

  // 清理标题重试定时器
  if (titleRetryTimer) {
    clearTimeout(titleRetryTimer)
    titleRetryTimer = null
  }

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
  color: var(--text-color, #333333);
  transition: var(--transition-interactive);
  overflow: hidden;
  background-color: var(--surface-color, #F2EEE8);
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
  background-color: var(--surface-color, #F2EEE8);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

/*
 * 自定义背景图之上的柔和遮罩。
 *
 * 为什么需要：页面上有一部分文字直接压在背景上（欢迎语、区块标题、"查看全部"等），
 * 一旦用户设置了色彩丰富的自定义背景图，这些文字与卡片边缘都会难以辨认
 * （实测卡片本身是不透明的，问题出在压在图上的文字对比度）。
 * 该遮罩在保留背景图观感的前提下把对比度拉回来；只在设置了自定义背景时生效，
 * 默认的纯色背景不受影响。
 */
.background-layer.has-custom-bg::after {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(248, 246, 242, 0.62);
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
  background: var(--sidebar-bg, rgba(242, 238, 232, 0.8));
  backdrop-filter: blur(10px);
  box-shadow: 1px 0 4px rgba(0, 0, 0, 0.1);
  border-right: 1px solid var(--sidebar-border, var(--border-subtle));
  transition: transform 0.3s ease, background 0.3s ease, color 0.3s ease, border-color 0.3s ease;
  display: flex;
  flex-direction: column;
  padding-top: 20px;
  position: relative;
  z-index: 99;
  color: var(--text-color, #333333);
}

.sidebar-active {
  transform: translateX(0);
}

/* 悬浮侧边栏切换按钮 */
.floating-toggle-btn {
  position: fixed;
  top: 60px;
  left: 20px;
  width: 44px;
  height: 44px;
  background-color: var(--primary-color, #C2977F);
  border-radius: var(--radius-circle);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-xl);
  z-index: 97;
  transition: var(--transition-interactive);
  /* 确保在小程序中固定显示 */
  position: fixed !important;
  z-index: 9999 !important;
}

.floating-toggle-btn:hover {
  background-color: var(--secondary-color, #94A7C8);
  transform: scale(1.05);
}

.floating-toggle-btn.btn-hidden {
  opacity: 0;
  pointer-events: none;
  transform: translateX(-60px);
}

/* 悬浮聊天按钮 */
.floating-chat-btn {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 56px;
  height: 56px;
  background-color: var(--primary-color, #C2977F);
  border-radius: var(--radius-circle);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-xl);
  z-index: 97;
  transition: var(--transition-interactive);
  /* 确保在小程序中固定在屏幕底部 */
  position: fixed !important;
  z-index: 9999 !important;
}

.floating-chat-btn:hover {
  background-color: var(--secondary-color, #94A7C8);
  transform: scale(1.05);
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
  background-color: var(--surface-strong, #FFFFFF);
  transition: var(--transition-interactive);
}

.hamburger-icon::before {
  top: 0;
}

.hamburger-icon::after {
  bottom: 0;
}

.hamburger-icon {
  background-color: var(--surface-strong, #FFFFFF);
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
  background-color: var(--surface-strong, #FFFFFF);
}

.hamburger-icon::after {
  content: '';
  position: absolute;
  top: 6px;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: var(--surface-strong, #FFFFFF);
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-circle);
  margin-bottom: 12px;
  cursor: pointer;
  box-shadow: var(--shadow-lg);
  background-color: #f0f0f0;
  /* 必须用 cover：圆形头像若用 contain，图片非正方形时会在上下（或左右）
     留出空隙，露出下面的 background-color，表现为贴合不自然的浅色"白边"。
     cover 会等比放大到铺满并裁掉溢出部分，配合 overflow: hidden 正好被圆形裁切。
     资料页的 <image> 用的就是 object-fit: cover，两侧保持一致。 */
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.user-name {
  font-size: var(--fs-md);
  font-weight: 500;
  color: var(--text-color, #333333);
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
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition-interactive);
}

.menu-item:hover {
  background-color: rgba(148, 167, 200, 0.1);
}

.menu-item-active {
  background-color: rgba(194, 151, 127, 0.1);
}

.menu-icon {
  margin-right: 12px;
  font-size: var(--fs-xl);
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
  display: block;
  width: 24px;
  height: 24px;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  background-image: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAACfUlEQVR4AexWTWsTURS9Z9IP0eIHigtdKfiJuwGLxEwxSdMm6sKVIuofsAiCC3HVLqprqX9A927TTVJJNoXs/Qf6M9rxnVTKkJI7mXkvTVvekBde3r3nnHvunSETyDG/vIFJD9BPwE/AsgNjvYWehuFpLssaVfjYDCwuFq/snD/T5eJercIiOBYDj6vRnendqZ4AIRf3PLOocyjUuYF6OartCrYFcnVf1ex5xtj+maONUwONSvQWAZoQOTdYH88YY85gzOa3KwNBvRptCPDNFFMwa9inwJx+rogTbWuS+vz8WVOQ6TpWhlU9eA7BCjHEDsay/rYyUKtF12RudtsUtJRVuI8x2D5HVnAiP7eB+qNScWoHPQB3E3yZtsSSg1yZgInkXAYa5YXXKGBLIJcSXPm2hoNcy+WFZ3kIMhuoV0rr5vH7IYIZcXZhBoh/Llejj1kpRzYQhuG0efC+A8GnrCKj5JvbCYHgCzWoNQqGOQG/0lalcv/i5QtzW+bBe5OWaxunBrWoOQpXqgG+AszKqR5EHo5C6CKHWtSkdhqfaoB//XwFEOB6GpHzuNGkNmvQuFUDgHw23fj/WqDRjCfW1w6wrrGrBjTgUYl5A5OehJ+An4BlB/wtZNlAa7jTCTwolmSp8URdzLGuOkHg1MDzl6/k3fsP6nphchL61lunBqyryUHgDeRomlOIn4DTduYg8xPI0TSnkBM+Acgfp+3KQQaJ/2owfQKx/NLAhxGLBW1NRzXQbHe/isRrcSy/NZIDMQcHe5rx2mars6HRqQYIbLa6q5vtzr1mq4O0dfPWbaRdN0xOGg/je5rdVdagrVQDGvgoxLyBSU/h2E/gHwAAAP//ZtIgHAAAAAZJREFUAwDC96RhKnMAlQAAAABJRU5ErkJggg==");
}

.icon-checkin::before {
  content: "";
  display: block;
  width: 24px;
  height: 24px;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  background-image: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAE6ElEQVR4AexYbWxTVRh+3tuOZUEw8eOHE36YqFE0oCEo4NaOdbp1EwcmOmWwhSguYlA2YLLvbgwdRvmB8QOi0aAx8YdAwKwhtuA2gW1KEILhh+AnJJIwjKKZq1uP76lpuXe39+72dmtD0tP79pz3Pe/H85xz7m1zFVzjLUMg3RuY2YFrdQeoxOOq9nrce0o9rne9nvw5iRKRMf/Hco5C9yqOJ5aEL1tHqKTIvV4h+pAIy0FUAygD3oKCWVarP+JyzQYpgzKWZA4Fu2VOq/FqP1sEFIgKdRIGcZ1wjOWpbWZjp1N4CJiu9iGIcrVudWyLAASc+gI0U28zsBBl62dIQ0g/H99ij0D8XGmxWibgzc+/mW+8ZXzjtgggdzxaPgJLS4tcPisCiEfHxwuIXJlb1iguXnTD+HkjfUICRUXzr/d6XDsxjS4SKXv5vHfw+b1Fn5AYFLUBloR9oWkEyo3k5hrKaNYl+YSStTVOcRRTAqVF+eVZmH6GiJ5jIaSoRWoR1WRxbYkBJs2UgBD0NIF0q81HSJ+S9CYji4jrq89KXFtiMMoj7YYESjzuBbwSmselDJBCBH4QqQoKAeKPnLMi0leoHTlerarHRFRRtiRvvtqmHhsSUETYMAgSLCMQsjD3IEKiLRLBsTIHdyD+GOUQRAuM5gwJgOg+oyBpJyJ2IYAv2G0cS0QgIvMMJlgMCfCqTJDVvGaqZg0JpApAsnUyBOyu4IwZM7H2xVpUP7MG2dlx/hpZTJzADljMaMFNgn9jx1soW1qOJ5+qRM3adRai4ruknEBOTg46t72OW2fNjiHyPFwMRXHE9EQGKSWQlTUNvs4u3H7HnRqM3Z/vRzg8prFZVVJGQFEUNLb6cO/ceRpsp06ewPu73tHYElFSRmDjy014YOFiDbYfzp2Fr2kzRkdHNfZElKQIyLPb0OLD3XPuMa255vkX4C70aHzO//oLGjfVYWRkRGNPVLFNYO68+1FX34A8VwG2bd+BBxdpVzcKpGLFSix7/ImoGumHhi6hYVMtrlz5M6In82WbgGtJYayuw+FAs28LFi5+KGaTg2JvGapWPyuHMZGgGzasx+WhoZgtmYFtAgf27dGcXUVxoKmtI3bO890FWFe7UYNteHgYTfUbcOHCeY09GcU2gZ9/+hGdvua4JCqrVqO+sUXzLzMUCsHXvBnnzn6fDF5drKKzJGD4eqAfHS2NGBu7+gx3Op1Ysapa88Mkn/GvdrTh9KmTCWS35poUAVni+DeDeKWj1fSHaPtrXRgcOCbdJ12SJiAR9R89gq3tbUwijPFt59tv4nDwi/HmSdMnhYBE03/0K3R1tjOJq8fp008+xv69n8npKZNJIyARHunrQX3dS5C9JLP7g/ekeUrFkAAJccJO5TPfneZ7og19PYfthMePEeLb+BOAIQGhOAyDjJJNld0MiyEBf+DLAX75E5gqUFbzCohgBItBgCEB9hcj+Gclv8L6ncfpuQR+C4mRSi4uWOJeZgQQDA5cDOHv2zjyI5aUXfyyS167QvTXXRKDWWFTAjIwEDj+R3egp2rMEbpRiPByIdDKu9I+FSLColmE8VjY+e9N/mBvjawtMZjJhASiwQcPHrvsD/bt8wd7tnQHen2mYnPef6h3q/9QzwFZK1p3ot4ygYkSpWs+QyBdKx+tm9mB6Eqkq/8PAAD//2RBzoYAAAAGSURBVAMA9qzLcL0G+agAAAAASUVORK5CYII=");
}

.icon-todo::before {
  content: "";
  display: block;
  width: 24px;
  height: 24px;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  background-image: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAD+0lEQVR4AexXa2xMWxT+9pne3GivxL3V+uFe9wb38ePqg1KptkFH044G8apXKREiISEqISFCSEtKPUJCRNW7iT+CTtW0aGukXkUj+EH41xlBkFaY6dn2nkx3Ss9zzEwjOSfnO2fP2nutb317r+msSvjBL0tAXx+gdQLWCXznDpguIcfEjD/z7JnjIwEe26weQwLys7ISHPbs6nx79mtIP72QYLsaCfDYnCM/J/tsbm5GohExugIc9qwZ+Jk8AshsAvIbInxxDkJIoU2OaXPkZBbo0WkKmJyTOZRCqmJBE/QChXuecSZSYjs7meWgFVtLAGHJnyBAnFaASM5xbplIxxkHG7Knwq0qIN8+Ph2EZCj4RNVEQMYFclFhVRVA5K4UFR/8n5SMqdNnYv7CYoGZhXORNiYdNptNzS1ku1YuqgLY7vcSIEkStpbuxI5de7FsxUrMKyoWWLx0ObZs34EDhysxYMCvISer6EhIr1y616kKoADBN5ejYApGpY35xvr1xz+GDMGC4iVfGyP4SVWAEmdS6kglcy9bisF1vRxDMJgSMDB+oKDwtLdjfclqgda7d8RcQuKgwLhfbCwrsUUoLa9A2a49uthWVo48R0HA1+jDlABbTIyI++H9O7Q9uC/g8bSLuZjgukm5eexLvhhJyakYkZSii9RRaVi1pgT//PufiKU3kPQWfM88+x6F5M5+iQ37mRLQ5feLwP3i4jAime1qEPE9yssfXOeqq8Wp45V4+KAVbQ/Zaeng3p3b2F9RjqdPHgsevYEpAV6vV8QbPPh3lJWzug5idPpYMef1eALjj52dOH2iChtK1mD92tW62LRhHWprLgZ8jT5MCAButbgNxW25ecPQunAsMiWg4UodKo8cgt/nU+W+dOE8qo4eUZ0P94QpAZz8XPUZzJ01Dbt3lrLyOCZwYF8FFhROx0H29vk+86VRgWkBPKuhw4bjl/79+VAglv3NHzb874j0QoJEYWBKQJ/1QgqJd5tMCbB6oe5tC+Pb1AlYvZBOP2T1QnqlafVCOv2Q1QvpldAP1QsRSluVBPVJL0TpfaVcuE31d4BKNlWnzo4O1F+5zP5ZOSZQw7rQt2/e8Jhhh1YuqgKcrmu3KNAc9mzMBqTUzXJpUXNTFcAcZMi+YoBGrzdmpD1vtoEdBHIRs7EheyrcWgLgbHA/k4HNCn5RMskbL9U3P9ci0xTAHWtdjWV+yTeIUlpNQSNT5JwoiAAHxUn6SU50upr2BM2qL10B3LOuzu111jfOcboa41lZ/SWja0IkwGNzjpr660XOpqZXnFsPhgT0DFLT4H5Z62q+pokQ53nsnlxGxqYFGAkazTWWgGjuthKXdQJKuxJN2xcAAAD//21EnYAAAAAGSURBVAMALuCSf9yg5/YAAAAASUVORK5CYII=");
}

.icon-focus::before {
  content: "";
  display: block;
  width: 24px;
  height: 24px;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  background-image: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAE70lEQVR4AexYXWgcVRQ+Z3atkVZMUn9a64PgiwTyYBA06iZLdzWdTWPEWogoMVq0YksTxZ8HlVYroqLQ2gcVSdoYrIlBrKbmp2zIZrFahEUfUmtEUJ+0AbcRktR0d/Z6zrYbutmZnTPZG6WQ4Z7MnfN99zvnuzObXcaAS/xYMfB/38CVO7DoDmBDONBohupfjoTr9lwcnGOM+EihbWi9A9TkFz4wjiLCqwC4++LgHGNk6nPQeBi6tDYFgzdSk03uetjMXHeejKHNgHEZXC0rCeDD9Fop142nzcBcCn5SoJJuBQHUmVnlP+nOkzG0GYjFYjMKMlsUqFGKcYcYzUDmfuL+I2vPnaXNAJcajn4dG4rGwxRBhwgzh7m6QqsBXU150dFqIBIMrotsDLSZobo3zHBdtxmqHzwfdYdMymWxQOAaLw26cbUYiIQDzdRgAvzqDzCMg4j4AgK2IoJ5PvARpBxjsApPMzcSumuzW3MSvCQD9wSDN9NOxwCMI9RgjaQg8XjUAPoGIqH6AXPjHTdJ1jlxDCfALW+GAg/4fZnvELDejeuII2xG9H/PWo4cF8Czga1VVato1w8gGv2AeGUx/YqKSuAoxmENJC16rPaxdlGuDejZwMz1a5+iXd9po5WXeqj1Uejp+ywbPM8DbS4QsZ21baCiKU8G6Jm9m5p/p6jiBTDSdC9QU9kwGwU/kWgda28KBxpoKh5iA8FgsIxUewBQtKa8vAJyR0VlZW7qckaDTBysra29woW4AIuaYXaZP9MBCNfxfDmDDKwvX+3fLq0hNYCGwnapaOk8fJ40kMJ1iAyY4eBttPvrXNU0ERBwfbamQE9kAFTmPoGWXoqwpswAwu16uxOoCWvKDCj8zx6fBWvCmjIDCCUb2PXMc+7fygvd00RYU2QAAa4iSfFARLAsK4/fYDZC50eHYcvWFjAMXx5mdyGtKTIACk7bFXHKKaWgv/dwAXx5WRk89sST8H5XN9xSc2sBnpdQ8HvetcOFzADAnw7rHdM9hzph7+4XYWqq0PuGDTfAa2++DTt2Pe24HlCJagoNqBPOlZyRE98ch8fbHobuzg9hfn6+gBhpaobaOwMF+WxCwQ8gOEQG6A3DsEDLlpJOpeDT3o9hW+uDMD42WsBJJv8qyHFCWlNk4KzliymAwi3kSsI4k0zCW6/vhWc7dsIvP09COp2Go18egclTP9ooqHMpnBuzAQpSRkHGJkHvcaZBqV4byHPq1MkJaN+xHZrNMLx3YJ/tevof8Ek0mvjbFlyUFBngNZYPXiFhbS+kWNMuuIayjD12mF3OsEva5Y4di/8KoD6ww/Tm1P7hWOw3qabYAAuetYyX6FGK83xZQqk41RDvPvfgyQB9FmbmLKORTCR4sdZQKsHaVMPTY+rJADdMBWZ807N19KwO8bWOUKBGWJO1vep5NsAFBhKJuTWV4030k2E/fS4ynFtaqAzdzXfXVMQbWXMpGksywIX6+8EaGo13ZCyriox0Uy7/1xsligzm9vDawdF4O2sV4RaFlmwgpzo8dnySjLRZvnPXkpEWii7CJuiconN2XJhzrovmLcwdjI638tosoYQ/JRvI1R4Z+TZJRvootg1Gx6unpmdXYxqrOXhODVczRtHH3Ny6Us/aDCxuJJFIpL6KxSY4eL4Y13W9bAZ0Neims2LAbYeWG7/k78C/AAAA//9GxX2gAAAABklEQVQDAJOxyHCOLZ15AAAAAElFTkSuQmCC");
}

.icon-countdown::before {
  content: "";
  display: block;
  width: 24px;
  height: 24px;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  background-image: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAADkklEQVR4AexZO0wUURR9740hRiyI0khLRyG1EdjlE3CpLCSxAxMTKytDoSYWJmqBVlYkJmJnAjUrhI98jDUWdFhiIRoKaQg7z3se7maZ7Cz3/TQkbubu3LnvvnPumTnZYkeJM/75L+BfP0CbJ6BGB3onSoOF2dJQ38coQdjgoJvCnovdeGOop08o9VZKcUsKWYgShA0Ow0UqOAdbgNRqggMYokdpdZuLo7iNWqYz3F7fvlSm77kYbAEfljbXtdCTQuuvXHDrPsIGB7i4e9kCCDAtL62/nF9e76wkh5eJaJdqQQ5gARPY4CDQlIJ12AioAS4sfP4pUnm3VvBNCMtgOuA4CQBPeWWtTHaaRu4VWk8bLEcQZwHgO0r2H+DxI3cJrcUOMFz2Vvd4CVhc/HLgZaU0HTcY1Wkczl4CwGceP9kAuVXQnvLqxierPQ2avQUAEzaAHZBzAr3Yw+k9rSeIAGMDsoMQmvHzRz3Ua/acNh1jPYgA8MAOdGdfI28W1DOF3mY9NmvBBIC0kuw/pgF3kDcKrF38tvek0ZprLagAYwuyR2MrHVtndnv70HXYRvuCCgAB7EF3egp5faCGtfpaiDy4AAxlbKLFFnITlJuauQj7ZSGATwybVNLK+LGV9CFy1PgI/M4oAkC/sLq5BdtQ/gI5naMc0QRgWtimdXfvOfJYEVVArKHrcaMK+HWl/elBR/ujesLQeTQBI/093fQPxiQN/BA5naMcUQSMdXW1JCp5J4QkfGly1ESEDxGER4V1hBTdNWTKTa1WCJcEF1Dq770upYB1TkyJGtZOFANcBBUwPHy1VSj1xzrZ6aQStBbaSkEFJJW2Z3SnO7OjV6+xFtpKwQTAHjTg/eqweWfqmURv3rptPYiA5tbJjiSNlcye7JLDdRAB5yptr+jO5lonOxd6sSdbd7n2FlAaKJSElPesyWlPCCt5CTA2UPqN9fDVDfSrZDCq1w5nLwGwAb3o6HDgNVtCWMlZgLN1zOh1X7ASbFhXskmdBIyMXLskfKyTnZCwDGa2zrhmCygWi+dLg30zo4OF70ml5YePdbJzAQuYwAYHuLI9eddsAReSo6KUclxI0Z4H5l0nbHCAi4vFFiCEuskFFd6NfC7F5bJ58cbFzOuz4WILMC/e0vQO/dMwRy811qKEFnOCOAxXnrpMnS2A9qXzKxsz5eW1MXoRV4wShA0OcFGwDhsBLMC/3XTmBfwGAAD//6rhOu8AAAAGSURBVAMAHLracE+2pCYAAAAASUVORK5CYII=");
}

.icon-settings::before {
  content: "";
  display: block;
  width: 24px;
  height: 24px;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  background-image: url("data:image/png;base64,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");
}

.icon-about::before {
  content: "";
  display: block;
  width: 24px;
  height: 24px;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  background-image: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAFEElEQVR4AexYbWxTZRQ+53aMfTnBRMEPpkQFfzmGidtY13a0Y7TGJSYmYPyhP/gjiRp140OGmxA/sjgNiYYfJn6gIrAQ2YR1XXrVdZvij6kBUTQRBZ3TH34kxjDdbl/OuaXLLX3v7b3tlhWyNz295z3vc55znnvf3vZWgct8zAuY6ws4fwWuqCsQanBXhwKe9pDf81oo4N0fDHj62dhPxDztTWvr755J0TlvoZDfvSLo9+4kOw0u13EA7ADEzQDwAAI2sbGfiGGHS1E+J0HfhgKetnXrPMshx5G1gHvc7sV0VvcAKicRYRfZSge93AGAuws0OBXye59nLshyOBZQW1tbHFzr2SqKlB/orD4GgIWQ7UAsBoTtzBUKeFuZ2ymVIwE+n69sUUnhPlTwRQBcDDM2dK7ORSUL3qQaRU5obQto8vluKymIf4EI9zsp4ASLiBtKXOI4f67s5tkS0FxXd5XLFT8GgLfDbA+ESgGu3mB1dbmdUnYE4GSxqxsQV9ghZMyyigrYvrMD3j/co9u2tna48aZlvGTLEGEllBUdIDCSWb4yCgj6Pa8g3Q4tWQyLiAhPP7ML3B4flJdfrVu9twHaOnYbUJldoglS7RcyIS0FrA+4fYj4eCYS4/q11y2BiptvMYZ0n2OlpWW67+BtS7Cx3muFtxSAoHSAw7Fk6VLTDKs1WRKdPASBz8rWkjEl6Vx61M8+oKX6S3N4/v13p0HTNHZTjGNjY7+kxOxMaPt6uRczrKkARSgbzZKs4v9NTMDhQwdACDENY//g/neA16aDDhyrXkwFCIRmBzVSoG+/8To8sulh2PvqHt3Yf2/fWykYJxPq5V4zvFRAMOCroUt3vVmSnfjP587C0Z4PdGPfTo4Zhnq5gXuSrUsFAGg1MvBcxjCurZLVlwsQKLmVyNLNY6uq7oKWbTt0Y98caXNFkfckFwCQk4A7K6vguc4uaPA36sZ+ZdVqm53KYXRPSP9yIahcAIIUDDZHzZq6NGR1zZq0mKOASU9yAY6Y08GFCxemBQsLs39sSCMzBOQCBPxmwOSHa9KTXADkoQCTnqQC6Isj/64Aip9kW0EqQBHxr2TguYwJENKepAJ+//u8Sgnjc9lwSm3a//3R4U9SYhcnUgGjo6OTKKD3IiYPDqLHrAmpgAQ4fiRxzId3815MBfSpw/20jSJz377o4V7M+jAVwAn0O34LH53a+NhYWso5+nWaFswcoOcgbYcVzFJAvzp0gkQctCKQrR37sAe++frk9BL7kTD9KzMdsedQ7XcjH4+cskJbCuDEeMHkZvohZUnCOKNNTJyH1icehU0PPagb+06fxrgm1X7SyCvzMwqIRD77M/5/vJGeEc/ICKxi47+OAZsVRromxBmuybWl64ZgRgGMjQwNjU+5IEBn5Q+ez6ZxDa7FNe3UsSWAiQYGYj8umJhaDgKiPJ8VE3CUa3Atu/y2BTBh78jIP6XXDK4XIt7J85k0umW/1KcONnMNJ7yOBDBxdzdoYXVoKxVcLYTgO5TG8SyNcsUh5gpHY63EIcgcvRwLSLJTwS/DamwjxCdvpW31FJntrUUNqyS+hXP7orENzJXkdXrMWkCyUN9Hn57tUwdfJmucUv4q04TWKADo1iu66AN5JGHkU4zXGEMNB8JqrItzkzzZHnMWYCw8MHDi34g6HA1HB/dSgy1hdfC+hMVaOMZrjDHm5OrPqIBcm8kmf15ANmdtJnMu+ytwAQAA///5d0rsAAAABklEQVQDABdNvnAprejRAAAAAElFTkSuQmCC");
}

.icon-default::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAEE0lEQVR4AexZTUxTQRCeqQj+RKMHFNQrilyJAQRapOWnJWJi5GoMXo0/Bz1hwODJeNBEE0z04E0TT6h9CG2hJYghqZp4wYSYeDBBQwKJ/ESFjrNtgX1/hVceLcZumLc7u7PfzLe7fbtvccA/nnIEsj2B/9cMNLtdJ3xu12Wfx9m1KcLYwoeVWV1zBhoanCUc7Cufx0UOhDFAuA+AnZsijC18eN2uBZZe4RvWSKYE2srK8n0eZ2ce4ScAbIEMJkTYwXJa+OYYOsrLy7ebuTclMFdc+AgAuxCgALKUEr6x+8C+3T1mIRgS8NY7GwHhAmyRhIjtPndNs1E4OgItNTX7ucNTnTHBIAFWTc/93uUPhHEzRGBjjKoJKKz1T+h44q2o2Kut1xFYKnC08egXyYZE8MwfDNcrgaF3o6OjC3KbnWWB/ToUeasEInVE9FzGRsBDsKfgnFwnyjoCiFAiGlaEYCpvZvbiip6hQt7MXDsQzICcCI7KqijrCHClyoiQoi+j0Xmuz+if8MlL6b3aKR5T6wAGBHiqZCuCr7KayTICflH7o8NqHQwIEBmQ0nbLjE7Ii0hyhaCPd8sEK8VpqZgWAd7mb/CZaNLncZEt4nZNCkxLkSeNLRNora7eA0C3AeFgEmPjGWMhUncC2xqcZQICnl+1JHI7hfcaTAfPMoHekZGfRHiTf17f03Fo2IeAsbBDYBu2p6h0pGgzbVKC4Tu8Mxf57TpSBMNFAtPUYYoGCwRSoGSxKUcgi4Mfd53WDIh3dm4fiI9f8pHbB5IDsZ5MvKtz+4B2/8jtA+tZO1vUJq3X6FbikhaBdPYB7jPuddfqbhU2OhiWCSTO7Na/B/gIzh/keGujAWv7GxFY0hppddR8q2rb7dKR+LNJAiOAmKTGi0YEVDcBCKi6J0p3HyCCzwDUGfe67gfxrK0aI+LEqpYoGRBAzV0MVTbV1hYnzBNPcXa3+j3AfUqV4PCLBMLaT29d3RFArJUt+bYuKuuirCdA9FE0rAjizm0F+GBFz1QhL/ZQ54rgg7ZOR0AJRfopPt2yKZ7l02fI66mrrKqq2im32FkW2C31zpM+tzPMS7dVxhYxcWwhuU6UdQREJU+d/mod4RT/dkf3786ft+UqxeBKRmCTA0cA0RmPI/ngpUMxip1PqqrMkIC4hQaCxyrLbCqIPW9Cw2NGIRgSEIaU/+0SM+f/hwktO8L+eeXQvR/Ts1fMIjAloCgTv5Rg5CrPRCPLlBmAYb0dlXzVwr+DJiUQuRaNRv+YQZoSWO7gD4YHWAoXF/F4bInO8JX3dQDiHdV+EdjCR2xpsZR9FrEMLMdhlq9JYLlj/9DQeN9gpJdH5K4/EOnaDBHYwkff4AhvesueU+frJpAaJnutOQLZG/uE578AAAD//ymZ/dcAAAAGSURBVAMABpX2f+qNZD8AAAAASUVORK5CYII=);
  background-size: contain;
  background-repeat: no-repeat;
}

/* 侧边栏图标 */
.sidebar-icon::before {
  content: "";
  width: 24px;
  height: 24px;
  display: inline-block;
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAABxElEQVR4AexYsU7DQAz1JQx8Bn/BUkqBSNAgxHcwwMiCIgrqwsyPUECBocPBxNIvQEgMDGxMIIaiYKfqFl+qXu6SVo7kVPLFfn5+Uc+5ABb8EgJ1CygKiAKWHZBXyLKB1uGigHULLROUKrAbbex3dzaTOGr3fBphEnYZPyMBTHIbQnCvFFwCqHOfRpiEjU27AcMVcGt7nc4aJjng1v351SHVwuGxBLiApvlZAo9avwNkr3UXnGXwNqmluBKWQP74OGgjiQTtoiZL1J9q5bUwNyOBVOvPdPjcR+vVZH2qgak9dxsJ5E80/CYE6hZo6RVQtJ3jjpzEUXWjBOY7jbda61WoZ1QAgQahg1ECd/grCMOX7nb7zJZEwCWg7RuBnI4SSqljDn9WP0tg1gQ2z2UKVm3iKZYl8Avwgbuv41EiG1ARNsYS0FqPweEokQGcrHx9H9kUT7EsAVqkbRxHCCejxMPw6fpuNPohHBszErBJ7CtWCPjqNIez/Aq4GCXwQ730hAOngISwuc5P/UYFMImcSkw7Nf+vnErM37sqIuVUIpVTiSpeJD6H8W+UD2vOihAo1MKjUxTw2OxCKFGgsC0enQuvwD8AAAD//wq7s+kAAAAGSURBVAMArR9UfwB0ugYAAAAASUVORK5CYII=);
  background-size: contain;
  background-repeat: no-repeat;
}

/* 聊天图标 */
.chat-icon {
  font-size: var(--fs-2xl);
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
  background-image: url(data:image/png;base64,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);
  background-size: contain;
  background-repeat: no-repeat;
}

.menu-text-active {
  color: var(--primary-color, #C2977F);
  font-weight: 500;
}

/* 主内容区域样式 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  transition: var(--transition-interactive);
  background: transparent;
  color: var(--text-color, #333333);
}

.header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  background: var(--sidebar-bg, rgba(242, 238, 232, 0.8));
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--sidebar-border, var(--border-subtle));
  position: sticky;
  top: 0;
  z-index: 10;
  transition: var(--transition-interactive);
}

.header-title {
  flex: 1;
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--primary-color, #C2977F);
}

.header-right {
  cursor: pointer;
  padding-right: 40px; /* 为微信小程序工具栏留出空间 */
}

.chat-icon {
  font-size: var(--fs-2xl);
}

/*
 * 悬浮聊天按钮里的图标：由"蓝色位图"改为"纯 CSS 白色气泡"。
 *
 * 原来的 ::before 用的是一张内嵌的蓝色 PNG，而按钮底色是暖棕
 * （--primary-color / #C2977F），蓝配棕既突兀又与整体暖色调不搭。
 * 改成纯 CSS 绘制后：颜色完全可控（白色在暖棕底上对比最清晰）、
 * 不依赖任何位图、也不会因为图标源变更而再次跑偏。
 *
 * 选择器刻意带上 .floating-chat-btn 前缀：.chat-icon 这个类名在别处
 * 也出现过（浅色底上），不加限定的话白色气泡在浅底上会直接看不见。
 */
.floating-chat-btn .chat-icon::before {
  content: "";
  width: 22px;
  height: 16px;
  display: inline-block;
  background-color: #FFFFFF;
  /* 覆盖先前那条蓝色位图规则（同为 ::before，靠位置在后胜出） */
  background-image: none;
  /* 左下角收紧，形成"对话气泡"的指向感 */
  border-radius: 6px 6px 6px 2px;
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
  transition: var(--transition-interactive);
  display: flex;
  flex-direction: column;
  z-index: 100;
  color: var(--text-color, #333333);
  box-sizing: border-box;
  padding-bottom: 0;
}

.ai-sidebar-active {
  right: 0;
}

.ai-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-subtle);
}

.ai-title {
  font-size: var(--fs-md);
  font-weight: 500;
  color: var(--text-color, #333333);
}

.ai-quota {
  font-size: var(--fs-xs);
  color: var(--text-muted, #999999);
  margin-left: 8px;
  margin-right: auto;
}

.close-icon {
  cursor: pointer;
  font-size: var(--fs-2xl);
}

.ai-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  min-height: 0;
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
  border-radius: var(--radius-circle);
  background-color: #94A7C8;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-xs);
  font-weight: 500;
  margin: 0 8px;
}

.ai-message-bot .ai-avatar {
  background-color: #94A7C8;
  border-radius: var(--radius-circle);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  /* 头像统一用 cover，避免非正方形图片在圆形内留出空隙露出底色 */
  background-size: cover;
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
  /* 此处显示的是用户头像，同样需要 cover 才能铺满圆形 */
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.ai-message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  background-color: var(--surface-strong, #FFFFFF);
  box-shadow: var(--shadow-sm);
  color: var(--text-color, #333333);
  align-self: flex-start;
  word-wrap: break-word;
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
  border-radius: var(--radius-circle);
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
  /* 超长串强制折行，避免撑破 AI 面板 */
  word-break: break-word;
  overflow-wrap: anywhere;
}

/* 非 H5 端走纯文本渲染，需要保留换行；H5 端由 Markdown 生成块级标签，无需 pre-wrap */
/* #ifndef H5 */
.markdown-content {
  white-space: pre-wrap;
}
/* #endif */

.markdown-content h1, .markdown-content h2, .markdown-content h3 {
  font-weight: 600;
  margin: 12px 0 8px 0;
  color: inherit;
}

.markdown-content h1 {
  font-size: var(--fs-lg);
}

.markdown-content h2 {
  font-size: var(--fs-md);
}

.markdown-content h3 {
  font-size: var(--fs-body);
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
  border-radius: var(--radius-xs);
  font-size: var(--fs-xs);
  font-family: monospace;
}

.markdown-content pre {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 12px;
  border-radius: var(--radius-sm);
  overflow-x: auto;
  margin: 8px 0;
}

.markdown-content pre code {
  background-color: transparent;
  padding: 0;
  font-size: var(--fs-xs);
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

/* 小程序 / App：<button> 自带 ::after 边框，会与自定义圆角叠加成"双框"，必须清掉 */
.ai-input button::after {
  border: none;
}

.ai-input {
  /*
   * height → min-height：写死高度时，一旦子元素较高就会被压扁并溢出错位
   * （小程序 / App 上原先正是如此，H5 因多一层容器侥幸未暴露）。
   * 同时改为由 CSS 撑开安全区内边距，不再依赖模板注入的 padding-bottom，
   * 避免"内边距挤压内容区"与"固定高度"互相冲突。
   */
  min-height: 60px;
  padding: 8px 16px;
  padding-bottom: calc(8px + constant(safe-area-inset-bottom));
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  box-sizing: border-box;
  position: sticky;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

.ai-input input {
  flex: 1;
  width: 100%;
  height: 44px;
  padding: 0 16px;
  border: 1px solid var(--border-color, var(--border-color, #D8C8BE));
  border-radius: var(--radius-pill);
  background-color: var(--surface-strong, #FFFFFF);
  font-size: var(--fs-body);
  color: var(--text-color, #333333);
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
  border-radius: var(--radius-pill);
  font-size: var(--fs-body);
  cursor: pointer;
  transition: var(--transition-interactive);
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
    padding-left: 72px;
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

/* ==== 设计修订（覆盖规则，勿手改上面旧值） ==== */
/*
 * 背景层此前恒加 blur(8px)：默认的纯色/浅色背景下这是无意义的模糊，
 * 只有用户设置了自定义背景图时才需要它来柔化、提升压在图上的文字对比度。
 * （.has-custom-bg 由模板按"是否设置了背景图"绑定）
 */
.background-layer {
  filter: none;
}
.background-layer.has-custom-bg {
  filter: blur(8px);
}

</style>