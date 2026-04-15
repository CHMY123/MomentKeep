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
        <div 
          v-for="(countdown, index) in countdowns" 
          :key="index"
          class="countdown-card"
          :style="{ borderLeftColor: countdown.color }"
        >
          <div class="countdown-header">
            <span class="countdown-title">{{ countdown.title }}</span>
            <div class="countdown-actions">
              <div class="action-icon edit-icon" @click="editCountdown(index)"></div>
              <div class="action-icon delete-icon" @click="deleteCountdown(index)"></div>
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
          <div class="countdown-info">
            <span class="countdown-date">{{ countdown.date }}</span>
            <span v-if="countdown.description" class="countdown-description">{{ countdown.description }}</span>
          </div>
        </div>
        <div v-if="countdowns.length === 0" class="empty-countdown">
          <span>暂无倒计时</span>
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
            <input type="date" v-model="formData.date" placeholder="选择日期" />
            <input type="time" v-model="formData.time" placeholder="选择时间" />
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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import Layout from '../../components/Layout.vue'

// 倒计时数据
const countdowns = ref([
  {
    title: '生日',
    description: '我的生日',
    date: '2026-04-23',
    time: '12:00',
    color: '#C2977F',
    days: 10,
    hours: 0,
    minutes: 0
  },
  {
    title: '旅行',
    description: '去海边旅行',
    date: '2026-05-13',
    time: '08:00',
    color: '#94A7C8',
    days: 30,
    hours: 0,
    minutes: 0
  },
  {
    title: '考试',
    description: '期末考试',
    date: '2026-06-13',
    time: '09:00',
    color: '#D8C8BE',
    days: 60,
    hours: 0,
    minutes: 0
  }
])

// 表单数据
const formData = reactive({
  title: '',
  description: '',
  date: '',
  time: '',
  color: '#C2977F'
})

// 弹窗状态
const isDialogOpen = ref(false)
const isEdit = ref(false)
const editIndex = ref(-1)

// 颜色选项
const colorOptions = ['#C2977F', '#94A7C8', '#D8C8BE', '#333333', '#4A5568', '#718096']

// 弹窗引用
const countdownDialog = ref(null)

// 定时器
let timer = null

// 方法
const showAddDialog = () => {
  isEdit.value = false
  editIndex.value = -1
  formData.title = ''
  formData.description = ''
  formData.date = ''
  formData.time = ''
  formData.color = '#C2977F'
  isDialogOpen.value = true
}

const editCountdown = (index) => {
  isEdit.value = true
  editIndex.value = index
  const countdown = countdowns.value[index]
  formData.title = countdown.title
  formData.description = countdown.description
  formData.date = countdown.date
  formData.time = countdown.time
  formData.color = countdown.color
  isDialogOpen.value = true
}

const saveCountdown = () => {
  if (!formData.title.trim() || !formData.date) {
    uni.showToast({ title: '请填写标题和日期', icon: 'none' })
    return
  }
  
  const targetDate = new Date(`${formData.date} ${formData.time || '00:00'}`)
  const now = new Date()
  const diff = targetDate - now
  
  if (diff <= 0) {
    uni.showToast({ title: '目标时间必须在未来', icon: 'none' })
    return
  }
  
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  
  const countdown = {
    title: formData.title,
    description: formData.description,
    date: formData.date,
    time: formData.time || '00:00',
    color: formData.color,
    days,
    hours,
    minutes
  }
  
  if (isEdit.value) {
    countdowns.value[editIndex.value] = countdown
  } else {
    countdowns.value.push(countdown)
  }
  
  closeCountdownDialog()
  // 这里可以调用API保存倒计时
  uni.showToast({ title: isEdit.value ? '倒计时更新成功' : '倒计时添加成功', icon: 'success' })
}

const deleteCountdown = (index) => {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这个倒计时吗？',
    success: (res) => {
      if (res.confirm) {
        countdowns.value.splice(index, 1)
        // 这里可以调用API删除倒计时
        uni.showToast({ title: '倒计时删除成功', icon: 'success' })
      }
    }
  })
}

const closeCountdownDialog = () => {
  isDialogOpen.value = false
}

const updateCountdowns = () => {
  countdowns.value.forEach(countdown => {
    const targetDate = new Date(`${countdown.date} ${countdown.time}`)
    const now = new Date()
    const diff = targetDate - now
    
    if (diff > 0) {
      countdown.days = Math.floor(diff / (1000 * 60 * 60 * 24))
      countdown.hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
      countdown.minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    } else {
      countdown.days = 0
      countdown.hours = 0
      countdown.minutes = 0
    }
  })
  
  // 按时间排序
  countdowns.value.sort((a, b) => {
    const dateA = new Date(`${a.date} ${a.time}`)
    const dateB = new Date(`${b.date} ${b.time}`)
    return dateA - dateB
  })
}

// 生命周期
onMounted(() => {
  // 初始化数据
  // 这里可以从API获取实际倒计时数据
  
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
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 20px;
  transition: all 0.3s ease;
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
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border-left: 4px solid #C2977F;
  transition: all 0.3s ease;
}

.countdown-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.countdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.countdown-title {
  font-size: 18px;
  font-weight: 600;
  color: #333333;
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

.time-item {
  text-align: center;
}

.time-number {
  font-size: 24px;
  font-weight: 600;
  color: #C2977F;
  display: block;
}

.time-unit {
  font-size: 12px;
  color: #666666;
}

.countdown-info {
  font-size: 14px;
  color: #666666;
}

.countdown-date {
  display: block;
  margin-bottom: 4px;
}

.countdown-description {
  font-size: 12px;
  color: #999999;
  line-height: 1.4;
}

.empty-countdown {
  text-align: center;
  padding: 40px 0;
  color: #999999;
  font-size: 14px;
  background-color: #F2EEE8;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 图标样式 */
.add-icon {
  font-size: 24px;
  color: white;
}

.add-icon::before {
  content: "+";
  font-weight: bold;
}

/* 操作图标 */
.countdown-actions {
  display: flex;
  gap: 12px;
}

.action-icon {
  font-size: 20px;
  cursor: pointer;
  transition: color 0.3s ease;
}

.edit-icon {
  color: #94A7C8;
}

.edit-icon::before {
  content: "✏️";
}

.delete-icon {
  color: #D8C8BE;
}

.delete-icon::before {
  content: "🗑️";
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
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
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
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  margin: 0;
}

.close-icon {
  font-size: 24px;
  cursor: pointer;
  color: #999999;
  transition: color 0.3s ease;
}

.close-icon:hover {
  color: #333333;
}

.modal-body {
  margin-bottom: 20px;
}

.modal-body input,
.modal-body textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #D8C8BE;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 12px;
  box-sizing: border-box;
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
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn {
  background-color: #F2EEE8;
  color: #666666;
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
  font-size: 14px;
  color: #666666;
  margin-bottom: 8px;
}

.color-options {
  display: flex;
  gap: 12px;
}

.color-option {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.color-option:hover {
  transform: scale(1.1);
}

.color-option.selected {
  border-color: #333333;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .countdown-time {
    gap: 12px;
  }
  
  .time-number {
    font-size: 20px;
  }
}
</style>