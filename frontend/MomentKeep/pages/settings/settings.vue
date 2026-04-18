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
        
        <div class="setting-item" @click="resetBackground">
          <span class="setting-label">恢复默认背景</span>
          <div class="setting-value">
            <div class="arrow-icon"></div>
          </div>
        </div>

        <!-- 官方主题背景 -->
        <div class="setting-item" style="display: none;">
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
        <span class="section-title">AI设置（开发中，敬请期待）</span>
        
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
        <span class="section-title">通知设置（开发中，敬请期待）</span>
        
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
        
        <div class="setting-item" @click="openFeedbackDialog">
          <span class="setting-label">提交反馈</span>
          <div class="setting-value">
            <div class="arrow-icon"></div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 反馈提交弹窗 -->
    <div class="modal" v-if="isFeedbackDialogOpen">
      <div class="modal-content">
        <div class="modal-header">
          <h3>提交反馈</h3>
          <div class="close-icon" @click="closeFeedbackDialog">×</div>
        </div>
        <div class="modal-body">
          <view class="form-item">
            <text class="label">反馈类型</text>
            <view class="type-selector">
              <view 
                v-for="(item, index) in feedbackTypes" 
                :key="index"
                class="type-btn"
                :class="{ 'active': feedback.type === item.value }"
                @click="feedback.type = item.value"
              >{{ item.label }}</view>
            </view>
          </view>
          <view class="form-item">
            <text class="label">反馈内容</text>
            <textarea 
              v-model="feedback.content" 
              class="input-box textarea-box" 
              placeholder="请详细描述您的问题或建议..."
            ></textarea>
          </view>
          <view class="form-item">
            <text class="label">联系方式（选填）</text>
            <input 
              type="text" 
              v-model="feedback.contact" 
              class="input-box" 
              placeholder="邮箱或手机号，方便我们回复您" 
            />
          </view>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeFeedbackDialog">取消</button>
          <button class="confirm-btn" @click="submitFeedback">提交</button>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Layout from '../../components/Layout.vue'
import { useUserStore } from '../../store/user'

// 初始化用户store
const userStore = useUserStore()

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

// 反馈相关
const isFeedbackDialogOpen = ref(false)
const feedback = reactive({
  type: 'bug',
  content: '',
  contact: ''
})
const feedbackTypes = [
  { label: '功能异常', value: 'bug' },
  { label: '功能建议', value: 'suggestion' },
  { label: '其他', value: 'other' }
]

// 官方主题背景
const themeBackgrounds = [
  {
    name: '雾感森林',
    image: '/static/background/Misty Forest.jpg'
  },
  {
    name: '简约线条',
    image: '/static/background/minimalist lines.png'
  },
  {
    name: '温柔肌理',
    image: '/static/background/gentle texture.jpg'
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
      // 上传图片到服务器
      uni.uploadFile({
        url: '/api/user/background',
        filePath: res.tempFilePaths[0],
        name: 'file',
        header: {
          'Authorization': `Bearer ${userStore.getToken}`
        },
        success: (uploadRes) => {
          try {
            const data = JSON.parse(uploadRes.data)
            if (data.code === 200) {
              const backgroundUrl = data.data
              backgroundImage.value = backgroundUrl
              // 保存到本地存储
              localStorage.setItem('backgroundImage', backgroundUrl)
              // 应用背景图片
              applyBackgroundImage(backgroundUrl)
              // 清除官方主题背景
              selectedTheme.value = -1
              localStorage.setItem('selectedTheme', '-1')
              uni.showToast({ title: '背景图片已设置', icon: 'success' })
            } else {
              uni.showToast({ title: data.message || '背景图片上传失败', icon: 'none' })
            }
          } catch (error) {
            uni.showToast({ title: '背景图片上传失败', icon: 'none' })
          }
        },
        fail: () => {
          uni.showToast({ title: '网络错误，背景图片上传失败', icon: 'none' })
        }
      })
    }
  })
}

const resetBackground = async () => {
  backgroundImage.value = ''
  selectedTheme.value = -1
  localStorage.removeItem('backgroundImage')
  localStorage.setItem('selectedTheme', '-1')
  
  // 通知后端清空背景图片
  if (userStore.getToken) {
    try {
      await uni.request({
        url: '/api/user/background',
        method: 'DELETE',
        header: {
          'Authorization': `Bearer ${userStore.getToken}`
        }
      })
    } catch (error) {
      console.error('清空背景图片失败:', error)
    }
  }
  
  uni.showToast({ title: '已恢复默认背景', icon: 'success' })
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
  document.documentElement.style.setProperty('--base-font-size', sizes[index])
  
  // 同时应用到 body
  document.body.style.fontSize = sizes[index]
}

// 应用背景图片
const applyBackgroundImage = (imageUrl) => {
  // 保存到本地存储
  localStorage.setItem('backgroundImage', imageUrl)
  // 清除官方主题背景
  localStorage.setItem('selectedTheme', '-1')

  // 立即更新背景样式
  const backgroundLayer = document.querySelector('.background-layer')
  if (backgroundLayer) {
    backgroundLayer.style.backgroundImage = `url(${imageUrl})`
    backgroundLayer.style.backgroundSize = 'cover'
    backgroundLayer.style.backgroundPosition = 'center'
    backgroundLayer.style.backgroundRepeat = 'no-repeat'
  }

  // 通知Layout组件背景已更新
  window.dispatchEvent(new CustomEvent('background-updated', {
    detail: {
      type: 'custom',
      image: imageUrl,
      selectedTheme: -1
    }
  }))
}

// 应用官方主题背景
const applyThemeBackground = (index) => {
  const theme = themeBackgrounds[index]
  if (!theme || !theme.image) return

  // 保存到本地存储
  localStorage.setItem('selectedTheme', index.toString())
  // 清除自定义背景图片
  localStorage.removeItem('backgroundImage')

  // 立即更新背景样式（与自上传背景逻辑一致）
  const backgroundLayer = document.querySelector('.background-layer')
  if (backgroundLayer) {
    backgroundLayer.style.backgroundImage = `url(${theme.image})`
    backgroundLayer.style.backgroundSize = 'cover'
    backgroundLayer.style.backgroundPosition = 'center'
    backgroundLayer.style.backgroundRepeat = 'no-repeat'
  }

  // 同时更新Layout组件的响应式数据
  // 通知Layout组件背景已更新
  window.dispatchEvent(new CustomEvent('background-updated', {
    detail: {
      type: 'theme',
      image: theme.image,
      selectedTheme: index
    }
  }))
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

// 反馈相关方法
const openFeedbackDialog = () => {
  isFeedbackDialogOpen.value = true
}

const closeFeedbackDialog = () => {
  isFeedbackDialogOpen.value = false
  // 重置表单
  feedback.type = 'bug'
  feedback.content = ''
  feedback.contact = ''
}

const submitFeedback = async () => {
  if (!feedback.content.trim()) {
    uni.showToast({ title: '请输入反馈内容', icon: 'none' })
    return
  }
  
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  
  const typeLabel = feedbackTypes.find(t => t.value === feedback.type)?.label || '其他'
  const fullContent = `反馈类型：${typeLabel}\n反馈内容：${feedback.content}`
  
  try {
    const response = await uni.request({
      url: '/api/feedback',
      method: 'POST',
      header: {
        'Authorization': `Bearer ${userStore.getToken}`,
        'Content-Type': 'application/json'
      },
      data: {
        content: fullContent,
        contact: feedback.contact
      }
    })
    
    if (response.statusCode === 200 && response.data.code === 200) {
      uni.showToast({ title: '反馈提交成功', icon: 'success' })
      closeFeedbackDialog()
    } else {
      uni.showToast({ title: '反馈提交失败', icon: 'none' })
    }
  } catch (error) {
    console.error('提交反馈失败:', error)
    uni.showToast({ title: '网络错误', icon: 'none' })
  }
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
  background: rgba(242, 238, 232, 0.8);
  backdrop-filter: blur(10px);
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
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.theme-background:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.theme-background.selected {
  border: 2px solid #C2977F;
  border-radius: 8px;
  overflow: hidden;
  padding: 0;
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

/* 反馈弹窗样式 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 90%;
  max-width: 400px;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #f8f6f2;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.close-icon {
  font-size: 28px;
  color: #999;
  line-height: 1;
}

.modal-body {
  padding: 20px 24px;
}

.form-item {
  margin-bottom: 16px;
}

.form-item .label {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.type-selector {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.type-btn {
  padding: 8px 16px;
  border: 1px solid #d8c8be;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
  background-color: #fff;
  transition: all 0.3s ease;
}

.type-btn.active {
  background-color: #C2977F;
  color: #fff;
  border-color: #C2977F;
}

.input-box {
  width: 100%;
  padding: 14px 12px;
  border: 1px solid #d8c8be;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  background-color: #fafafa;
  box-sizing: border-box;
  line-height: 1.5;
  min-height: 48px;
  font-family: 'Arial', sans-serif;
}

.textarea-box {
  height: 140px;
  resize: none;
  line-height: 1.5;
  min-height: 140px;
  font-family: 'Arial', sans-serif;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  background-color: #f8f6f2;
}

.cancel-btn {
  padding: 10px 24px;
  border: 1px solid #d8c8be;
  border-radius: 8px;
  background-color: #fff;
  font-size: 14px;
  color: #666;
}

.confirm-btn {
  padding: 10px 24px;
  border: 1px solid #C2977F;
  border-radius: 8px;
  background-color: #C2977F;
  font-size: 14px;
  color: #fff;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-content {
    width: 95%;
  }
  
  .modal-footer {
    flex-direction: column;
  }
  
  .cancel-btn,
  .confirm-btn {
    width: 100%;
    text-align: center;
  }
}
</style>