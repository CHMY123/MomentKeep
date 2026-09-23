<template>
  <Layout>
    <div class="countdown-container">
      <!-- 添加倒计时 -->
      <button class="add-countdown-btn" @click="showAddDialog">
        <div class="add-icon"></div>
        <span>添加倒计时</span>
      </button>
      
      <!-- 倒计时列表 -->
      <div class="countdown-list">
        <!-- 加载态：原先 loading 只赋值不使用，首次进入会先闪一下"暂无倒计时" -->
        <div v-if="loading && countdowns.length === 0" class="list-state">
          <span class="state-text">正在加载倒计时…</span>
        </div>

        <div 
          v-for="countdown in countdowns" 
          :key="countdown.id"
          class="countdown-card stagger-item"
          :class="{ 'is-urgent': !countdown.isPast && countdown.days <= 3, 'is-past': countdown.isPast }"
          :style="{ borderLeftColor: countdown.color }"
        >
          <div class="countdown-header">
            <span class="countdown-title">{{ countdown.title }}</span>
            <div class="countdown-actions">
              <div class="action-icon edit-icon" @click="editCountdown(countdown)"></div>
              <div class="action-icon delete-icon" @click="deleteCountdown(countdown.id)"></div>
            </div>
          </div>
          <div class="countdown-time">
            <div class="time-item">
              <span class="time-number">{{ countdown.days }}</span>
              <span class="time-unit">天</span>
            </div>
            <div class="time-item">
              <span class="time-number">{{ countdown.hours }}</span>
              <span class="time-unit">时</span>
            </div>
            <div class="time-item">
              <span class="time-number">{{ countdown.minutes }}</span>
              <span class="time-unit">分</span>
            </div>
          </div>
          <div class="countdown-status" v-if="countdown.isPast">
            已过去
          </div>
          <div class="countdown-info">
            <span class="countdown-date">{{ formatDate(countdown.targetTime) }}</span>
            <span v-if="countdown.description" class="countdown-description">{{ countdown.description }}</span>
          </div>
        </div>
        <!-- 仅在"加载完成且确实没有数据"时才提示空态，避免加载中误报 -->
        <div v-if="!loading && countdowns.length === 0" class="empty-countdown">
          <div class="empty-graphic-list"></div>
          <span>还没有倒计时 · 点击上方按钮添加</span>
        </div>
      </div>
      
      <!-- 添加/编辑倒计时弹窗 -->
      <div class="modal" v-if="isDialogOpen">
        <div class="modal-content">
          <div class="modal-header">
            <h3>{{ isEdit ? '编辑倒计时' : '添加倒计时' }}</h3>
            <div class="close-icon" @click="closeCountdownDialog">×</div>
          </div>
          <div class="modal-body">
            <input type="text" v-model="formData.title" placeholder="输入标题" />
            <textarea v-model="formData.description" placeholder="输入描述（可选）" rows="3" />
            <div class="date-picker">
              <picker :value="yearIndex" :range="years" @change="onYearChange">
                <view class="picker-box">{{ formData.year }}年</view>
              </picker>
              <picker :value="monthIndex" :range="months" @change="onMonthChange">
                <view class="picker-box">{{ formData.month }}月</view>
              </picker>
              <picker :value="dayIndex" :range="dayOptions" @change="onDayChange">
                <view class="picker-box">{{ formData.day }}日</view>
              </picker>
            </div>
            <div class="color-picker">
              <label class="color-label">选择颜色</label>
              <div class="color-options">
                <div 
                  v-for="color in colorOptions" 
                  :key="color"
                  class="color-option"
                  :class="{ 'selected': formData.color === color }"
                  :style="{ backgroundColor: color }"
                  @click="formData.color = color"
                ></div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="closeCountdownDialog">取消</button>
            <button class="confirm-btn" @click="saveCountdown">保存</button>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import Layout from '../../components/Layout.vue'
import { useUserStore } from '../../store/user'
import { get, post, put, del } from '../../utils/request'
import { useCache } from '../../utils/cache'
import { toLocalDateTime, parseDateTime } from '../../utils/datetime'

const userStore = useUserStore()
const { getCache, setCache, removeCache, fetchWithCache } = useCache()

// 倒计时数据
const countdowns = ref([])

// 表单数据
const formData = reactive({
  id: null,
  title: '',
  description: '',
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  day: new Date().getDate(),
  color: '#C2977F'
})

// 弹窗状态
const isDialogOpen = ref(false)
const isEdit = ref(false)

// 颜色选项
const colorOptions = ['#C2977F', '#94A7C8', '#D8C8BE', '#333333', '#4A5568', '#718096']

// 年份范围 1978-2035
const years = []
for (let i = 1978; i <= 2035; i++) {
  years.push(i)
}

// 月份范围
const months = []
for (let i = 1; i <= 12; i++) {
  months.push(i)
}

// picker索引
const yearIndex = ref(years.indexOf(new Date().getFullYear()))
const monthIndex = ref(new Date().getMonth())
const dayIndex = ref(new Date().getDate() - 1)

// 日期选项
const dayOptions = ref([])

// picker选择处理
const onYearChange = (e) => {
  yearIndex.value = e.detail.value
  formData.year = years[e.detail.value]
  updateDayOptions()
}

const onMonthChange = (e) => {
  monthIndex.value = e.detail.value
  formData.month = months[e.detail.value]
  updateDayOptions()
}

const onDayChange = (e) => {
  dayIndex.value = e.detail.value
  formData.day = dayOptions.value[e.detail.value]
}

// 更新日期选项
const updateDayOptions = () => {
  const year = formData.year
  const month = formData.month
  
  // 默认显示31天
  let days = 31
  
  // 处理大小月
  if ([4, 6, 9, 11].includes(month)) {
    days = 30
  } else if (month === 2) {
    // 处理闰年
    if ((year % 4 === 0 && year % 100 !== 0) || year % 400 === 0) {
      days = 29
    } else {
      days = 28
    }
  }
  
  const options = []
  for (let i = 1; i <= days; i++) {
    options.push(i)
  }
  
  dayOptions.value = options
  
  // 确保选中的日期有效
  if (formData.day > days) {
    formData.day = days
  }
}

// 初始化日期选项
updateDayOptions()

// 定时器
let timer = null

// 加载状态
const loading = ref(false)

// 格式化日期
const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 计算倒计时
const calculateTimeLeft = (targetTime) => {
  // 用 parseDateTime 兼容 "2026-09-20 00:00:00" 这类含空格的时间串（iOS 原生 Date 无法解析）
  const target = parseDateTime(targetTime) || new Date()
  const now = new Date()
  const diff = target - now
  
  const absDiff = Math.abs(diff)
  const days = Math.floor(absDiff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((absDiff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((absDiff % (1000 * 60 * 60)) / (1000 * 60))
  
  return { days, hours, minutes, isPast: diff <= 0 }
}

// 获取倒计时列表
const fetchCountdowns = async () => {
  const fetchFn = async () => {
    if (!userStore.getToken) {
      return []
    }

    const response = await get('/countdown', {}, {
    })

    if (response.code === 200) {
      const data = response.data || []
      return data.map(countdown => {
        const timeLeft = calculateTimeLeft(countdown.targetTime)
        return {
          ...countdown,
          days: timeLeft.days,
          hours: timeLeft.hours,
          minutes: timeLeft.minutes,
          isPast: timeLeft.isPast
        }
      })
    }
    return []
  }

  try {
    loading.value = true
    const cachedData = getCache('countdowns_detail')
    if (cachedData !== null) {
      countdowns.value = cachedData
    }
    const data = await fetchWithCache('countdowns_detail', fetchFn)
    countdowns.value = data
  } catch (error) {
    uni.showToast({ title: error.message || '网络错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 方法
const showAddDialog = () => {
  isEdit.value = false
  formData.id = null
  formData.title = ''
  formData.description = ''
  formData.year = new Date().getFullYear()
  formData.month = new Date().getMonth() + 1
  formData.day = new Date().getDate()
  formData.color = '#C2977F'
  
  // 更新日期选项
  updateDayOptions()
  
  isDialogOpen.value = true
}



const editCountdown = (countdown) => {
  isEdit.value = true
  formData.id = countdown.id
  formData.title = countdown.title
  formData.description = countdown.description
  
  const targetDate = parseDateTime(countdown.targetTime) || new Date()
  formData.year = targetDate.getFullYear()
  formData.month = targetDate.getMonth() + 1
  formData.day = targetDate.getDate()
  
  // 更新日期选项
  updateDayOptions()
  
  formData.color = countdown.color
  isDialogOpen.value = true
}

// 保存倒计时的核心逻辑
const saveCountdownWithTargetDate = async (targetDate) => {
  // 检查是否有 token
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }
  
  try {
    let response
    if (isEdit.value) {
      response = await put('/countdown', {
        id: formData.id,
        title: formData.title,
        description: formData.description,
        targetTime: toLocalDateTime(targetDate),
        color: formData.color
      }, {
      })
    } else {
      response = await post('/countdown', {
        title: formData.title,
        description: formData.description,
        targetTime: toLocalDateTime(targetDate),
        color: formData.color
      }, {
      })
    }
    
    if (response.code === 200) {
      const newCountdown = response.data
      const timeLeft = calculateTimeLeft(newCountdown.targetTime)
      const countdownWithTime = {
        ...newCountdown,
        days: timeLeft.days,
        hours: timeLeft.hours,
        minutes: timeLeft.minutes,
        isPast: timeLeft.isPast
      }
      
      if (isEdit.value) {
        const index = countdowns.value.findIndex(c => c.id === formData.id)
        if (index !== -1) {
          countdowns.value[index] = countdownWithTime
        }
      } else {
        countdowns.value.push(countdownWithTime)
      }
      
      closeCountdownDialog()
      uni.showToast({ title: isEdit.value ? '倒计时更新成功' : '倒计时添加成功', icon: 'success' })
    } else if (response.code === 403) {
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
    } else {
      uni.showToast({ title: '保存失败，请重试', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: error.message || '网络错误', icon: 'none' })
  }
}

// 保存倒计时
/** 上次提交时间：连点「保存」时防止重复创建 */
let lastSubmitAt = 0

const saveCountdown = async () => {
  if (!formData.title.trim() || !formData.year || !formData.month || !formData.day) {
    uni.showToast({ title: '请填写标题和日期', icon: 'none' })
    return
  }

  // 连点防护：600ms 内的重复点击直接忽略
  if (Date.now() - lastSubmitAt < 600) return
  lastSubmitAt = Date.now()
  
  // 构建目标日期，时间部分设置为零点零分零秒
  const year = formData.year
  const month = formData.month.toString().padStart(2, '0')
  const day = formData.day.toString().padStart(2, '0')
  const targetDate = new Date(`${year}-${month}-${day}T00:00:00`)
  const now = new Date()
  const diff = targetDate - now
  
  if (diff <= 0) {
    // 先关闭模态框，避免确认提示被遮挡
    const wasOpen = isDialogOpen.value
    isDialogOpen.value = false
    
    // 弹出确认对话框
    uni.showModal({
      title: '提示',
      content: '这个日期已经过去了，仍要添加吗？',
      success: (res) => {
        if (res.confirm) {
          // 用户确认，继续保存
          saveCountdownWithTargetDate(targetDate)
        } else {
          // 用户取消，重新打开模态框
          if (wasOpen) {
            isDialogOpen.value = true
          }
        }
      }
    })
    return
  }
  
  // 时间在未来，直接保存
  saveCountdownWithTargetDate(targetDate)
}

const deleteCountdown = (id) => {
  // 检查是否有 token
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }
  
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这个倒计时吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          const response = await del(`/countdown/${id}`, {}, {
          })
          
          if (response.code === 200) {
            const index = countdowns.value.findIndex(c => c.id === id)
            if (index !== -1) {
              countdowns.value.splice(index, 1)
            }
            uni.showToast({ title: '倒计时删除成功', icon: 'success' })
          } else if (response.code === 403) {
            uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
            setTimeout(() => {
              uni.navigateTo({ url: '/pages/login/login' })
            }, 1000)
          } else {
            uni.showToast({ title: '删除失败，请重试', icon: 'none' })
          }
        } catch (error) {
          uni.showToast({ title: error.message || '网络错误', icon: 'none' })
        }
      }
    }
  })
}

const closeCountdownDialog = () => {
  isDialogOpen.value = false
}

const updateCountdowns = () => {
  countdowns.value.forEach(countdown => {
    const timeLeft = calculateTimeLeft(countdown.targetTime)
    countdown.days = timeLeft.days
    countdown.hours = timeLeft.hours
    countdown.minutes = timeLeft.minutes
  })
  
  // 按时间排序
  countdowns.value.sort((a, b) => {
    const dateA = parseDateTime(a.targetTime)
    const dateB = parseDateTime(b.targetTime)
    return dateA - dateB
  })
}

// 生命周期
onMounted(() => {
  // 初始化用户信息
  userStore.initUserInfo()
  
  // 初始化数据
  fetchCountdowns()
  
  // 启动定时器
  timer = setInterval(updateCountdowns, 60000) // 每分钟更新一次
  updateCountdowns()
})

onUnmounted(() => {
  // 清除定时器
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.countdown-container {
  padding: 20px;
}

/* 添加倒计时按钮 */
.add-countdown-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background-color: #C2977F;
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--fs-md);
  font-weight: 500;
  margin-bottom: 20px;
  transition: var(--transition-interactive);
}

.add-countdown-btn:hover {
  background-color: #A8846B;
}

/* 倒计时列表 */
.countdown-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.countdown-card {
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-md);
  border-left: 4px solid #C2977F;
  transition: var(--transition-interactive);
}

.countdown-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

/*
 * 紧急度即视觉重量 —— 本页的视线锚点
 *
 * 此前所有倒计时卡片的留白、字号、字重完全一致：不管还剩 3 天还是 300 天，
 * 观感上同样重要，整页没有落点，用户得逐张读数字才知道哪个要先做。
 * 这里用「剩余天数」这一真实数据驱动层级：
 *   剩余 ≤3 天 → 加大留白、放大数字、加粗左色条，在尺寸上"跳出来"
 *   已过期     → 降透明度、左色条转灰、数字回落到常规字重，退为背景信息
 *
 * 【两点实现说明】
 * 1) 用数据而不是 :nth-child 判断——列表并未按紧急度排序，
 *    按位置放大等于随机放大某一张。
 * 2) 用 em 而非固定 px 做放大——时间的基准字号由卡片自身决定，
 *    写死 px 可能反而比原值更小（那就成了"越紧急越小"）。
 *    1.15em 永远在原有基础上放大，与基准值无关。
 */
.countdown-card.is-urgent {
  padding: 24px;
  border-left-width: 6px;
  box-shadow: var(--shadow-lg, 0 4px 16px rgba(0, 0, 0, 0.08));
}

.countdown-card.is-urgent .countdown-title {
  font-size: 1.12em;
  font-weight: var(--fw-bold, 600);
}

.countdown-card.is-urgent .time-number {
  font-size: 1.15em;
  color: var(--primary-color, #C2977F);
}

.countdown-card.is-past {
  opacity: 0.72;
  /* 卡片左色条是由模版 :style 内联设置的，内联优先于样式表，必须 !important 才能压过去 */
  border-left-color: #9A9A9A !important;
  box-shadow: var(--shadow-sm, 0 1px 4px rgba(0, 0, 0, 0.05));
}

.countdown-card.is-past .time-number {
  color: var(--text-color, #333333);
  font-weight: var(--fw-normal, 400);
  opacity: 0.55;
}

.countdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.countdown-title {
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--text-color, #333333);
  flex: 1;
}

.countdown-actions {
  display: flex;
  gap: 12px;
}

.countdown-actions uni-icons {
  cursor: pointer;
  transition: color 0.3s ease;
}

.countdown-actions uni-icons:hover {
  color: #C2977F;
}

.countdown-time {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
}

.countdown-status {
  color: var(--danger-color, #B5544A);
  font-size: var(--fs-body);
  margin-bottom: 12px;
  font-weight: 500;
}

.time-item {
  text-align: center;
}

.time-number {
  font-size: var(--fs-2xl);
  font-weight: 600;
  color: #C2977F;
  display: block;
}

.time-unit {
  font-size: var(--fs-xs);
  color: var(--text-secondary, #666666);
}

.countdown-info {
  font-size: var(--fs-body);
  color: var(--text-secondary, #666666);
}

.countdown-date {
  display: block;
  margin-bottom: 4px;
}

.countdown-description {
  font-size: var(--fs-xs);
  color: var(--text-muted, #999999);
  line-height: 1.4;
}

/* 加载态提示 */
.list-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
}

.state-text {
  font-size: var(--fs-body);
  color: var(--text-muted, #999999);
  text-align: center;
  line-height: 1.6;
}

.empty-countdown {
  text-align: center;
  padding: 40px 0;
  color: var(--text-muted, #999999);
  font-size: var(--fs-body);
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
}

/* 图标样式 */
.add-icon {
  font-size: var(--fs-2xl);
  color: white;
}

.add-icon::before {
  content: "+";
  font-weight: var(--fw-bold); /* 原为 bold(700)，归一为 600 */
}

/* 操作图标 */
.countdown-actions {
  display: flex;
  gap: 12px;
}

.action-icon {
  font-size: var(--fs-xl);
  cursor: pointer;
  transition: color 0.3s ease;
}

.edit-icon {
  color: #94A7C8;
}

.delete-icon {
  color: #D8C8BE;
}

.edit-icon:hover {
  color: #7A8BA8;
}

.delete-icon:hover {
  color: #B8A89E;
}

/* 模态框样式 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: var(--surface-strong, #FFFFFF);
  border-radius: var(--radius-md);
  padding: 20px;
  width: 90%;
  max-width: 400px;
  box-shadow: var(--shadow-xl);
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modal-header h3 {
  font-size: var(--fs-md);
  font-weight: 500;
  color: var(--text-color, #333333);
  margin: 0;
}

.close-icon {
  font-size: var(--fs-2xl);
  cursor: pointer;
  color: var(--text-muted, #999999);
  transition: color 0.3s ease;
}

.close-icon:hover {
  color: var(--text-color, #333333);
}

.modal-body {
  margin-bottom: 20px;
}

.modal-body input,
.modal-body textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border-color, #D8C8BE);
  border-radius: var(--radius-sm);
  font-size: var(--fs-body);
  margin-bottom: 12px;
  box-sizing: border-box;
  line-height: 1.6;
  min-height: 60px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.date-picker {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.date-picker picker {
  flex: 1;
  max-width: 100px;
  margin: 0 4px;
}

.date-picker picker:first-child {
  margin-left: 0;
}

.picker-box {
  padding: 12px;
  border: 1px solid var(--border-color, #D8C8BE);
  border-radius: var(--radius-sm);
  font-size: var(--fs-body);
  background-color: var(--surface-strong, #FFFFFF)FF;
  text-align: center;
}

.modal-body textarea {
  resize: vertical;
  min-height: 80px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.modal-footer button {
  padding: 8px 16px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--fs-body);
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition-interactive);
}

.cancel-btn {
  background-color: var(--surface-color, #F2EEE8);
  color: var(--text-secondary, #666666);
}

.cancel-btn:hover {
  background-color: #E8E0D8;
}

.confirm-btn {
  background-color: #C2977F;
  color: white;
}

.confirm-btn:hover {
  background-color: #A8846B;
}

/* 颜色选择器 */
.color-picker {
  margin-top: 16px;
}

.color-label {
  display: block;
  font-size: var(--fs-body);
  color: var(--text-secondary, #666666);
  margin-bottom: 8px;
}

.color-options {
  display: flex;
  gap: 12px;
}

.color-option {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-circle);
  cursor: pointer;
  transition: var(--transition-interactive);
  border: 2px solid transparent;
}

.color-option:hover {
  transform: scale(1.1);
}

.color-option.selected {
  border-color: var(--text-color, #333333);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .countdown-time {
    gap: 12px;
  }
  
  .time-number {
    font-size: var(--fs-xl);
  }
}

/* ==== 设计修订（覆盖规则，勿手改上面旧值） ==== */
/* 同上：编辑 / 删除图标热区补齐到 44px（视觉不变） */
.action-icon {
  padding: 10px;
  margin: -10px;
}

</style>