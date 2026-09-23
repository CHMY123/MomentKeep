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
      v-if="isMobile && !isAIChatOpen"
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
          <div v-if="!isMobile" class="chat-icon" @click="toggleAIChat"></div>
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
            <span>你好！我是你的AI助手，有什么可以帮助你的吗？</span>
          </div>
        </div>
        <div v-for="(message, index) in aiMessages" :key="index" class="ai-message" :class="message.type === 'user' ? 'ai-message-user' : 'ai-message-bot'">
          <div v-if="message.type === 'user'" class="ai-avatar" :style="{ backgroundImage: toCssUrl(userAvatar || 'https://momentkeep.s3.bitiful.net/avatars/logo.png') }"></div>
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
      <div class="ai-input" :style="{ paddingBottom: isMobile ? '44px' : '0' }">
        <input type="text" v-model="aiInput" placeholder="输入你的问题..." />
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
const userAvatar = computed(() => userStore.getUserInfo.avatar || 'https://img.icons8.com/ios-filled/50/000000/user.png')

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
    'clock': 'icon-clock',
    'todo-list': 'icon-todo',
    'focus': 'icon-focus',
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
        aiMessages.value[loadingMessageId] = { type: 'bot', content: '抱歉，AI助手暂时无法回复。' }
      }
    } catch (error) {
      console.error('AI chat error:', error)
      // 配额超限(429)等业务错误由 request 工具抛出，直接把后端文案展示给用户
      aiMessages.value[aiMessages.value.length - 1] = {
        type: 'bot',
        content: error && error.message ? error.message : '网络错误，请稍后再试。'
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
      'Authorization': `Bearer ${userStore.getToken}`
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
const handleThemeUpdate = (event) => {
  // 主题更新事件处理，确保Layout组件响应主题变化
}

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
    window.addEventListener('theme-updated', handleThemeUpdate)
  }

  // 资料刷新放到最后、纯后台进行，不阻塞首帧。
  // 本地已有用户资料时跳过请求：否则每次切页都要多打一次 /user/profile。
  if (userStore.getToken && !userStore.getUserInfo.id) {
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
    window.removeEventListener('theme-updated', handleThemeUpdate)
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
  transition: all 0.3s ease;
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
  border-right: 1px solid var(--sidebar-border, rgba(0, 0, 0, 0.05));
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
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 97;
  transition: all 0.3s ease;
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
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 97;
  transition: all 0.3s ease;
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
  transition: all 0.3s ease;
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
  font-size: calc(16px * var(--font-scale, 1));
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
  font-size: calc(20px * var(--font-scale, 1));
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

.icon-focus::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/000000/stopwatch.png);
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
  font-size: calc(24px * var(--font-scale, 1));
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
  color: var(--primary-color, #C2977F);
  font-weight: 500;
}

/* 主内容区域样式 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
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
  border-bottom: 1px solid var(--sidebar-border, rgba(0, 0, 0, 0.05));
  position: sticky;
  top: 0;
  z-index: 10;
  transition: all 0.3s ease;
}

.header-title {
  flex: 1;
  font-size: calc(18px * var(--font-scale, 1));
  font-weight: 600;
  color: var(--primary-color, #C2977F);
}

.header-right {
  cursor: pointer;
  padding-right: 40px; /* 为微信小程序工具栏留出空间 */
}

.chat-icon {
  font-size: calc(24px * var(--font-scale, 1));
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
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.ai-title {
  font-size: calc(16px * var(--font-scale, 1));
  font-weight: 500;
  color: var(--text-color, #333333);
}

.ai-quota {
  font-size: calc(12px * var(--font-scale, 1));
  color: var(--text-muted, #999999);
  margin-left: 8px;
  margin-right: auto;
}

.close-icon {
  cursor: pointer;
  font-size: calc(24px * var(--font-scale, 1));
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
  border-radius: 50%;
  background-color: #94A7C8;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: calc(12px * var(--font-scale, 1));
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
  padding: 10px 14px;
  border-radius: 16px;
  background-color: var(--surface-strong, #FFFFFF);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
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
  font-size: calc(18px * var(--font-scale, 1));
}

.markdown-content h2 {
  font-size: calc(16px * var(--font-scale, 1));
}

.markdown-content h3 {
  font-size: calc(14px * var(--font-scale, 1));
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
  font-size: calc(12px * var(--font-scale, 1));
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
  font-size: calc(12px * var(--font-scale, 1));
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
  padding: 8px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 60px;
  box-sizing: border-box;
  position: sticky;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.ai-input input {
  flex: 1;
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid var(--border-color, #D8C8BE);
  border-radius: 22px;
  background-color: var(--surface-strong, #FFFFFF);
  font-size: calc(14px * var(--font-scale, 1));
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
  border-radius: 22px;
  font-size: calc(14px * var(--font-scale, 1));
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