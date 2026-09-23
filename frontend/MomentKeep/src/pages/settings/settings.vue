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

        <!-- 非 H5 端如实说明：动态换肤依赖浏览器 CSS 变量，小程序/App 不支持 -->
        <div v-if="!supportsDomTheme" class="platform-hint">
          <span>动态换肤与字号调整依赖浏览器能力，仅在网页端生效；你的偏好会被保存，在网页端登录后自动应用。</span>
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
      </div>
      
      <!-- AI设置 -->
      <div class="setting-section">
        <span class="section-title">AI 设置（暂未开放）</span>
        
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
        <span class="section-title">通知设置（暂未开放）</span>
        
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
              placeholder="说说遇到的问题或想法…"
            ></textarea>
          </view>
          <view class="form-item">
            <text class="label">联系方式（选填）</text>
            <input 
              type="text" 
              v-model="feedback.contact" 
              class="input-box" 
              placeholder="邮箱或手机号，方便回复你" 
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
import { post, del, buildUrl, toCssUrl } from '../../utils/request'
import { applyFontScale } from '../../utils/fontScale'
import { applyTheme as applyThemeVars } from '../../utils/theme'

// 初始化用户store
const userStore = useUserStore()

// 主题设置
const themeIndex = ref(0) // 0: 浅色, 1: 深色, 2: 柔和
const fontSizeIndex = ref(0) // 0: 标准, 1: 偏大, 2: 偏小
const backgroundImage = ref('')

/**
 * 当前端是否支持动态换肤 / 改字号
 *
 * @description applyTheme / applyFontSize 依赖 document.documentElement 上的 CSS 变量，
 * 属浏览器专有机制：小程序与 App 端没有 document，调用后界面不会有任何变化。
 * 该标记用于给用户如实提示——而不是像原来那样无论是否真正生效都弹"已更新"。
 *
 * 这里直接复用 applyTheme / applyFontSize 内部的同一判据（typeof document），
 * 使"是否支持"与"调用后是否真的生效"永远一致。
 * 采用运行时探测而非 #ifdef 条件编译：条件编译注释会被 Vetur 之类的静态分析工具
 * 误判为"重复声明"，而 typeof 判断在 uni-app 各端都安全（typeof 不存在的变量不抛错）。
 */
const supportsDomTheme = typeof document !== 'undefined' && !!document.documentElement

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

// 方法
const changeTheme = (index) => {
  themeIndex.value = index
  // 保存到本地存储（换端登录后仍会读到该偏好）
  uni.setStorageSync('themeIndex', index.toString())

  if (!supportsDomTheme) {
    // 非 H5 端无法动态换肤：如实告知，避免"点了没反应却说已更新"
    uni.showToast({ title: '已保存，仅网页端即时生效', icon: 'none' })
    return
  }

  applyTheme(index)
  uni.showToast({ title: '主题已更新', icon: 'success' })
}

const changeFontSize = (index) => {
  fontSizeIndex.value = index
  // 保存到本地存储
  uni.setStorageSync('fontSizeIndex', index.toString())

  if (!supportsDomTheme) {
    uni.showToast({ title: '已保存，仅网页端即时生效', icon: 'none' })
    return
  }

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
        url: buildUrl('/user/background'),
        filePath: res.tempFilePaths[0],
        name: 'file',
        header: {
        },
        success: (uploadRes) => {
          try {
            const data = JSON.parse(uploadRes.data)
            if (data.code === 200) {
              const backgroundUrl = data.data
              backgroundImage.value = backgroundUrl
              // 保存到本地存储
              uni.setStorageSync('backgroundImage', backgroundUrl)
              // 应用背景图片
              applyBackgroundImage(backgroundUrl)
              uni.showToast({ title: '背景图片已设置', icon: 'success' })
            } else {
              uni.showToast({ title: data.message || '背景图片上传失败', icon: 'none' })
            }
          } catch (error) {
            uni.showToast({ title: '背景图片上传失败', icon: 'none' })
          }
        },
        fail: () => {
          // 注意：这里是上传失败回调，作用域内没有 error 变量。
          // 之前写 error.message 会在失败时抛 ReferenceError，反而把提示吞掉。
          uni.showToast({ title: '网络错误，背景图片上传失败', icon: 'none' })
        }
      })
    }
  })
}

const resetBackground = async () => {
  backgroundImage.value = ''
  uni.removeStorageSync('backgroundImage')

  // 通知 Layout 立即撤下背景图。
  // 原先这里只清缓存不通知，Layout 上的旧背景图会一直留到下次挂载才消失。
  // #ifdef H5
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('background-updated', { detail: { type: 'default' } }))
  }
  // #endif
  
  // 通知后端清空背景图片
  if (userStore.getToken) {
    try {
      await del('/user/background', {}, {
      })
    } catch (error) {
      console.error('清空背景图片失败:', error)
    }
  }
  
  uni.showToast({ title: '已恢复默认背景', icon: 'success' })
}

/**
 * 应用主题
 *
 * @description 变量表与写入实现都在 utils/theme.js——放在公共模块里，
 * 应用启动时（App.vue）才能恢复上次选择的主题，否则刷新后会退回浅色。
 */
const applyTheme = (index) => {
  if (!applyThemeVars(index)) return

  // 通知 Layout 组件主题已更新
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('theme-updated', {
      detail: {
        themeIndex: index
      }
    }))
  }
}

/**
 * 应用字号偏好
 *
 * @description 原实现只改了 --base-font-size 与 body 的 font-size：
 * 前者全项目仅 20 处引用，后者会被各组件自己硬编码的 px 字号覆盖，
 * 所以设置后界面几乎没有变化（就是"形同虚设"的由来）。
 * 现在改写全项目共用的 --font-scale，所有 calc(Npx * var(--font-scale)) 同步生效。
 */
const applyFontSize = (index) => applyFontScale(index)

// 应用背景图片
const applyBackgroundImage = (imageUrl) => {
  // 保存到本地存储
  uni.setStorageSync('backgroundImage', imageUrl)

  // 确保在浏览器环境中
  if (typeof document !== 'undefined') {
    // 立即更新背景样式
    const backgroundLayer = document.querySelector('.background-layer')
    if (backgroundLayer) {
      backgroundLayer.style.backgroundImage = toCssUrl(imageUrl)
      backgroundLayer.style.backgroundSize = 'cover'
      backgroundLayer.style.backgroundPosition = 'center'
      backgroundLayer.style.backgroundRepeat = 'no-repeat'
    }

    // 通知Layout组件背景已更新
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('background-updated', {
        detail: {
          type: 'custom',
          image: imageUrl
        }
      }))
    }
  }
}

const toggleAIAutoFill = (event) => {
  aiAutoFill.value = event.detail.value
  // 这里可以调用API保存AI设置
}

const toggleTodayNoPopup = (event) => {
  todayNoPopup.value = event.detail.value
  // 这里可以调用API保存设置
}

const toggleNotifications = (event) => {
  notifications.value = event.detail.value
  // 这里可以调用API保存通知设置
}

const toggleCheckinReminder = (event) => {
  checkinReminder.value = event.detail.value
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
    const response = await post('/feedback', {
      content: fullContent,
      contact: feedback.contact
    }, {
    })
    
    if (response.code === 200) {
      uni.showToast({ title: '反馈提交成功', icon: 'success' })
      closeFeedbackDialog()
    } else {
      uni.showToast({ title: '反馈提交失败', icon: 'none' })
    }
  } catch (error) {
    console.error('提交反馈失败:', error)
    uni.showToast({ title: error.message || '网络错误', icon: 'none' })
  }
}

// 生命周期
onMounted(() => {
  // 从本地存储加载设置
  const savedThemeIndex = uni.getStorageSync('themeIndex')
  const savedFontSizeIndex = uni.getStorageSync('fontSizeIndex')
  const savedBackgroundImage = uni.getStorageSync('backgroundImage')
  
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
  border-radius: var(--radius-md);
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-md);
}

.section-title {
  font-size: var(--fs-md);
  font-weight: 500;
  color: var(--text-color, #333333);
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
  border-bottom: 1px solid var(--border-subtle);
  cursor: pointer;
  transition: var(--transition-interactive);
}

.setting-item:hover {
  background-color: rgba(148, 167, 200, 0.1);
  padding-left: 12px;
  border-radius: var(--radius-sm);
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-label {
  font-size: var(--fs-body);
  color: var(--text-color, #333333);
}

.setting-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.setting-value span {
  font-size: var(--fs-body);
  color: var(--text-muted, #999999);
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
  transition: var(--transition-interactive);
  border-radius: var(--radius-sm);
  overflow: hidden;
  position: relative;
}

.theme-background:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.theme-background.selected {
  border: 2px solid #C2977F;
  border-radius: var(--radius-sm);
  overflow: hidden;
  padding: 0;
}

.theme-background img {
  width: 100%;
  height: 80px;
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
}

.theme-name {
  font-size: var(--fs-xs);
  color: var(--text-secondary, #666666);
  display: block;
}

/* 平台能力提示（非 H5 端的动态换肤说明） */
.platform-hint {
  margin-top: 12px;
  padding: 12px 12px;
  background-color: rgba(194, 151, 127, 0.1);
  border-radius: var(--radius-sm);
}

.platform-hint span {
  font-size: var(--fs-xs);
  line-height: 1.6;
  color: #8a7a6d;
}

/* 主题选项按钮 */
.theme-options {
  display: flex;
  gap: 8px;
}

.theme-btn {
  padding: 8px 12px;
  border: 1px solid var(--border-color, var(--border-color, #D8C8BE));
  border-radius: var(--radius-lg);
  background-color: var(--surface-strong, #FFFFFF);
  font-size: var(--fs-xs);
  color: var(--text-secondary, #666666);
  cursor: pointer;
  transition: var(--transition-interactive);
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
  padding: 8px 12px;
  border: 1px solid var(--border-color, var(--border-color, #D8C8BE));
  border-radius: var(--radius-lg);
  background-color: var(--surface-strong, #FFFFFF);
  font-size: var(--fs-xs);
  color: var(--text-secondary, #666666);
  cursor: pointer;
  transition: var(--transition-interactive);
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
  border-radius: var(--radius-lg);
}

.switch-slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: var(--surface-strong, #FFFFFF);
  transition: .4s;
  border-radius: var(--radius-circle);
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
  color: var(--text-muted, #999999);
}

.arrow-icon::before {
  content: ">";
  font-size: var(--fs-xl);
  font-weight: var(--fw-bold); /* 原为 bold(700)，归一为 600 */
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
  background-color: var(--surface-strong, #FFFFFF);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
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
  font-size: var(--fs-lg);
  font-weight: 500;
  color: #333;
}

.close-icon {
  font-size: var(--fs-3xl);
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
  font-size: var(--fs-body);
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
  border: 1px solid var(--border-color, var(--border-color, #D8C8BE));
  border-radius: var(--radius-lg);
  font-size: var(--fs-body);
  color: #666;
  background-color: var(--surface-strong, #FFFFFF);
  transition: var(--transition-interactive);
}

.type-btn.active {
  background-color: #C2977F;
  color: #fff;
  border-color: #C2977F;
}

.input-box {
  width: 100%;
  padding: 16px 12px;
  border: 1px solid var(--border-color, var(--border-color, #D8C8BE));
  border-radius: var(--radius-sm);
  font-size: var(--fs-body);
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
  padding: 12px 24px;
  border: 1px solid var(--border-color, var(--border-color, #D8C8BE));
  border-radius: var(--radius-sm);
  background-color: var(--surface-strong, #FFFFFF);
  font-size: var(--fs-body);
  color: #666;
}

.confirm-btn {
  padding: 12px 24px;
  border: 1px solid #C2977F;
  border-radius: var(--radius-sm);
  background-color: #C2977F;
  font-size: var(--fs-body);
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