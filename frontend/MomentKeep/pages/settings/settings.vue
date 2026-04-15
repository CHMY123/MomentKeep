<template>
  <Layout>
    <div class="settings-container">
      <!-- 个性化设置 -->
      <div class="setting-section">
        <span class="section-title">个性化设置</span>
        
        <div class="setting-item">
          <span class="setting-label">主题模式</span>
          <div class="setting-value">
            <div class="theme-options">
              <button 
                v-for="(theme, index) in ['浅色', '深色', '柔和']" 
                :key="index"
                class="theme-btn"
                :class="{ 'active': themeIndex === index }"
                @click="changeTheme(index)"
              >{{ theme }}</button>
            </div>
          </div>
        </div>
        
        <div class="setting-item">
          <span class="setting-label">字体大小</span>
          <div class="setting-value">
            <div class="font-options">
              <button 
                v-for="(size, index) in ['标准', '偏大', '偏小']" 
                :key="index"
                class="font-btn"
                :class="{ 'active': fontSizeIndex === index }"
                @click="changeFontSize(index)"
              >{{ size }}</button>
            </div>
          </div>
        </div>
        
        <div class="setting-item" @click="chooseBackground">
          <span class="setting-label">背景图片</span>
          <div class="setting-value">
            <span>{{ backgroundImage ? '已设置' : '未设置' }}</span>
            <div class="arrow-icon"></div>
          </div>
        </div>
        
        <div class="setting-item">
          <span class="setting-label">官方主题背景</span>
          <div class="theme-backgrounds">
            <div 
              v-for="(theme, index) in themeBackgrounds" 
              :key="index"
              class="theme-background"
              :class="{ 'selected': selectedTheme === index }"
              @click="selectThemeBackground(index)"
            >
              <img :src="theme.image" :alt="theme.name" />
              <span class="theme-name">{{ theme.name }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- AI设置 -->
      <div class="setting-section">
        <span class="section-title">AI设置</span>
        
        <div class="setting-item">
          <span class="setting-label">AI自动填充提示词</span>
          <div class="setting-value">
            <label class="switch">
              <input type="checkbox" :checked="aiAutoFill" @change="toggleAIAutoFill" />
              <span class="switch-slider"></span>
            </label>
          </div>
        </div>
        
        <div class="setting-item">
          <span class="setting-label">今日内免弹出</span>
          <div class="setting-value">
            <label class="switch">
              <input type="checkbox" :checked="todayNoPopup" @change="toggleTodayNoPopup" />
              <span class="switch-slider"></span>
            </label>
          </div>
        </div>
      </div>
      
      <!-- 通知设置 -->
      <div class="setting-section">
        <span class="section-title">通知设置</span>
        
        <div class="setting-item">
          <span class="setting-label">推送通知</span>
          <div class="setting-value">
            <label class="switch">
              <input type="checkbox" :checked="notifications" @change="toggleNotifications" />
              <span class="switch-slider"></span>
            </label>
          </div>
        </div>
        
        <div class="setting-item">
          <span class="setting-label">打卡提醒</span>
          <div class="setting-value">
            <label class="switch">
              <input type="checkbox" :checked="checkinReminder" @change="toggleCheckinReminder" />
              <span class="switch-slider"></span>
            </label>
          </div>
        </div>
      </div>
      
      <!-- 关于设置 -->
      <div class="setting-section">
        <span class="section-title">关于</span>
        
        <div class="setting-item" @click="navigateTo('/pages/about/about')">
          <span class="setting-label">关于我们</span>
          <div class="setting-value">
            <div class="arrow-icon"></div>
          </div>
        </div>
        
        <div class="setting-item">
          <span class="setting-label">版本</span>
          <div class="setting-value">
            <span>1.0.0</span>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Layout from '../../components/Layout.vue'

// 主题设置
const themeIndex = ref(0) // 0: 浅色, 1: 深色, 2: 柔和
const fontSizeIndex = ref(0) // 0: 标准, 1: 偏大, 2: 偏小
const backgroundImage = ref('')
const selectedTheme = ref(0)

// AI设置
const aiAutoFill = ref(true)
const todayNoPopup = ref(false)

// 通知设置
const notifications = ref(true)
const checkinReminder = ref(true)

// 官方主题背景
const themeBackgrounds = [
  {
    name: '雾感森林',
    image: 'https://img.icons8.com/color/200/000000/forest.png'
  },
  {
    name: '简约线条',
    image: 'https://img.icons8.com/color/200/000000/line.png'
  },
  {
    name: '温柔肌理',
    image: 'https://img.icons8.com/color/200/000000/nature.png'
  }
]

// 方法
const changeTheme = (index) => {
  themeIndex.value = index
  // 保存到本地存储
  localStorage.setItem('themeIndex', index.toString())
  // 应用主题
  applyTheme(index)
  uni.showToast({ title: '主题已更新', icon: 'success' })
}

const changeFontSize = (index) => {
  fontSizeIndex.value = index
  // 保存到本地存储
  localStorage.setItem('fontSizeIndex', index.toString())
  // 应用字体大小
  applyFontSize(index)
  uni.showToast({ title: '字体大小已更新', icon: 'success' })
}

const chooseBackground = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      backgroundImage.value = res.tempFilePaths[0]
      // 保存到本地存储
      localStorage.setItem('backgroundImage', res.tempFilePaths[0])
      // 应用背景图片
      applyBackgroundImage(res.tempFilePaths[0])
      uni.showToast({ title: '背景图片已设置', icon: 'success' })
    }
  })
}

const selectThemeBackground = (index) => {
  selectedTheme.value = index
  // 保存到本地存储
  localStorage.setItem('selectedTheme', index.toString())
  // 应用官方主题背景
  applyThemeBackground(index)
  uni.showToast({ title: '主题背景已更新', icon: 'success' })
}

// 应用主题
const applyTheme = (index) => {
  const themes = {
    0: { // 浅色
      '--bg-color': '#F8F6F2',
      '--text-color': '#333333',
      '--primary-color': '#C2977F',
      '--secondary-color': '#94A7C8',
      '--sidebar-bg': '#F2EEE8',
      '--sidebar-border': 'rgba(0, 0, 0, 0.05)'
    },
    1: { // 深色
      '--bg-color': '#2D2D2D',
      '--text-color': '#E5E5E5',
      '--primary-color': '#D8C8BE',
      '--secondary-color': '#6B7280',
      '--sidebar-bg': '#1E1E1E',
      '--sidebar-border': 'rgba(255, 255, 255, 0.1)'
    },
    2: { // 柔和
      '--bg-color': '#F0EAE1',
      '--text-color': '#4A4A4A',
      '--primary-color': '#A8846B',
      '--secondary-color': '#8A9BB0',
      '--sidebar-bg': '#E8E1D6',
      '--sidebar-border': 'rgba(0, 0, 0, 0.05)'
    }
  }
  
  const theme = themes[index]
  for (const [key, value] of Object.entries(theme)) {
    document.documentElement.style.setProperty(key, value)
  }
}

// 应用字体大小
const applyFontSize = (index) => {
  const sizes = {
    0: '16px', // 标准
    1: '18px', // 偏大
    2: '14px'  // 偏小
  }
  document.documentElement.style.fontSize = sizes[index]
}

// 应用背景图片
const applyBackgroundImage = (imageUrl) => {
  document.body.style.backgroundImage = `url(${imageUrl})`
  document.body.style.backgroundSize = 'cover'
  document.body.style.backgroundPosition = 'center'
  document.body.style.backgroundRepeat = 'no-repeat'
}

// 应用官方主题背景
const applyThemeBackground = (index) => {
  const theme = themeBackgrounds[index]
  if (theme) {
    document.body.style.backgroundImage = `url(${theme.image})`
    document.body.style.backgroundSize = 'cover'
    document.body.style.backgroundPosition = 'center'
    document.body.style.backgroundRepeat = 'no-repeat'
  }
}

const toggleAIAutoFill = (event) => {
  aiAutoFill.value = event.target.checked
  // 这里可以调用API保存AI设置
}

const toggleTodayNoPopup = (event) => {
  todayNoPopup.value = event.target.checked
  // 这里可以调用API保存设置
}

const toggleNotifications = (event) => {
  notifications.value = event.target.checked
  // 这里可以调用API保存通知设置
}

const toggleCheckinReminder = (event) => {
  checkinReminder.value = event.target.checked
  // 这里可以调用API保存提醒设置
}

const navigateTo = (path) => {
  uni.navigateTo({
    url: path
  })
}

// 生命周期
onMounted(() => {
  // 从本地存储加载设置
  const savedThemeIndex = localStorage.getItem('themeIndex')
  const savedFontSizeIndex = localStorage.getItem('fontSizeIndex')
  const savedBackgroundImage = localStorage.getItem('backgroundImage')
  const savedSelectedTheme = localStorage.getItem('selectedTheme')
  
  // 应用保存的设置
  if (savedThemeIndex !== null) {
    const index = parseInt(savedThemeIndex)
    themeIndex.value = index
    applyTheme(index)
  }
  
  if (savedFontSizeIndex !== null) {
    const index = parseInt(savedFontSizeIndex)
    fontSizeIndex.value = index
    applyFontSize(index)
  }
  
  if (savedBackgroundImage !== null) {
    backgroundImage.value = savedBackgroundImage
    applyBackgroundImage(savedBackgroundImage)
  }
  
  if (savedSelectedTheme !== null) {
    const index = parseInt(savedSelectedTheme)
    selectedTheme.value = index
    applyThemeBackground(index)
  }
})
</script>

<style scoped>
.settings-container {
  padding: 20px;
}

/* 设置区域 */
.setting-section {
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  margin-bottom: 16px;
  border-bottom: 1px solid #94A7C8;
  padding-bottom: 8px;
  display: block;
}

/* 设置项 */
.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s ease;
}

.setting-item:hover {
  background-color: rgba(148, 167, 200, 0.1);
  padding-left: 12px;
  border-radius: 8px;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-label {
  font-size: 14px;
  color: #333333;
}

.setting-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.setting-value span {
  font-size: 14px;
  color: #999999;
}

/* 主题背景选择 */
.theme-backgrounds {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.theme-background {
  min-width: 100px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.theme-background:hover {
  transform: translateY(-2px);
}

.theme-background.selected {
  border: 2px solid #C2977F;
  border-radius: 8px;
  padding: 4px;
}

.theme-background img {
  width: 100%;
  height: 80px;
  border-radius: 8px;
  margin-bottom: 8px;
}

.theme-name {
  font-size: 12px;
  color: #666666;
  display: block;
}

/* 主题选项按钮 */
.theme-options {
  display: flex;
  gap: 8px;
}

.theme-btn {
  padding: 6px 12px;
  border: 1px solid #D8C8BE;
  border-radius: 16px;
  background-color: white;
  font-size: 12px;
  color: #666666;
  cursor: pointer;
  transition: all 0.3s ease;
}

.theme-btn.active {
  background-color: #C2977F;
  color: white;
  border-color: #C2977F;
}

/* 字体选项按钮 */
.font-options {
  display: flex;
  gap: 8px;
}

.font-btn {
  padding: 6px 12px;
  border: 1px solid #D8C8BE;
  border-radius: 16px;
  background-color: white;
  font-size: 12px;
  color: #666666;
  cursor: pointer;
  transition: all 0.3s ease;
}

.font-btn.active {
  background-color: #C2977F;
  color: white;
  border-color: #C2977F;
}

/* 开关样式 */
.switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.switch-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #D8C8BE;
  transition: .4s;
  border-radius: 24px;
}

.switch-slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .switch-slider {
  background-color: #C2977F;
}

input:checked + .switch-slider:before {
  transform: translateX(24px);
}

/* 箭头图标 */
.arrow-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999999;
}

.arrow-icon::before {
  content: ">";
  font-size: 20px;
  font-weight: bold;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .setting-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .setting-value {
    width: 100%;
    justify-content: flex-end;
  }
  
  .theme-backgrounds {
    flex-wrap: wrap;
  }
  
  .theme-background {
    min-width: calc(33.33% - 8px);
  }
  
  .theme-options,
  .font-options {
    flex-wrap: wrap;
  }
  
  .theme-btn,
  .font-btn {
    flex: 1;
    min-width: 80px;
    text-align: center;
  }
}
</style>