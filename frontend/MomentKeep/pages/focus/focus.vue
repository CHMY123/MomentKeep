<template>
  <Layout>
    <div class="focus-container">
      <!-- 模式选择标签 -->
      <div class="mode-tabs">
        <div 
          class="mode-tab" 
          :class="{ 'active': currentMode === 'stopwatch' }"
          @click="switchMode('stopwatch')"
        >
          正向计时
        </div>
        <div 
          class="mode-tab" 
          :class="{ 'active': currentMode === 'countdown' }"
          @click="switchMode('countdown')"
        >
          倒计时
        </div>
        <div 
          class="mode-tab" 
          :class="{ 'active': currentMode === 'pomodoro' }"
          @click="switchMode('pomodoro')"
        >
          番茄钟
        </div>
      </div>

      <!-- 计时器显示 -->
      <div class="timer-display">
        <div class="timer-time">{{ formatTime(displayTime) }}</div>
        <div class="timer-status">{{ timerStatusText }}</div>
      </div>

      <!-- 鼓励语显示 -->
      <div class="encouragement" v-if="showEncouragement">
        <div class="encouragement-text">{{ currentEncouragement }}</div>
      </div>

      <!-- 时间设置（仅倒计时和番茄钟） -->
      <div class="time-settings" v-if="currentMode !== 'stopwatch' && !isRunning">
        <div class="time-input-group" v-if="currentMode === 'countdown'">
          <input 
            type="number" 
            v-model.number="countdownMinutes" 
            min="1" 
            max="180"
            class="time-input"
          />
          <span class="time-unit">分钟</span>
        </div>
        <div class="time-presets" v-if="currentMode === 'pomodoro'">
          <div 
            class="time-preset" 
            :class="{ 'active': pomodoroMinutes === 25 }"
            @click="setPomodoroTime(25)"
          >
            25分钟
          </div>
          <div 
            class="time-preset" 
            :class="{ 'active': pomodoroMinutes === 45 }"
            @click="setPomodoroTime(45)"
          >
            45分钟
          </div>
          <div 
            class="time-preset" 
            :class="{ 'active': pomodoroMinutes === 60 }"
            @click="setPomodoroTime(60)"
          >
            60分钟
          </div>
        </div>
      </div>

      <!-- 控制按钮 -->
      <div class="timer-controls">
        <button 
          class="control-btn start-btn" 
          v-if="!isRunning && currentMode !== 'pomodoro'"
          @click="startTimer"
        >
          {{ isPaused ? '继续' : '开始' }}
        </button>
        <button 
          class="control-btn pause-btn" 
          v-if="isRunning && currentMode !== 'pomodoro'"
          @click="pauseTimer"
        >
          暂停
        </button>
        <button 
          class="control-btn reset-btn" 
          v-if="!isRunning || isPaused"
          @click="resetTimer"
        >
          重置
        </button>
        
        <!-- 番茄钟控制 -->
        <template v-if="currentMode === 'pomodoro'">
          <button 
            class="control-btn start-btn" 
            v-if="!isRunning"
            @click="startPomodoro"
          >
            开始专注
          </button>
          <button 
            class="control-btn pause-btn" 
            v-if="isRunning && !isBreakTime"
            @click="pausePomodoro"
          >
            暂停
          </button>
          <button 
            class="control-btn break-btn" 
            v-if="isRunning && isBreakTime"
            @click="skipBreak"
          >
            跳过休息
          </button>
        </template>
      </div>

      <!-- 关联待办 -->
      <div class="related-todo" v-if="relatedTodo">
        <div class="related-todo-label">当前专注任务</div>
        <div class="related-todo-item">
          <span class="todo-title">{{ relatedTodo.title }}</span>
          <span class="todo-remove" @click="removeRelatedTodo">×</span>
        </div>
      </div>

      <!-- 选择待办列表（未关联时显示） -->
      <div class="todo-selector" v-if="!relatedTodo">
        <div class="todo-selector-title">请选择专注任务（必须选择）</div>
        <div v-if="uncompletedTodos.length > 0" class="todo-selector-list">
          <div 
            v-for="todo in uncompletedTodos" 
            :key="todo.id"
            class="todo-selector-item"
            @click="selectTodo(todo)"
          >
            {{ todo.title }}
          </div>
        </div>
        <div v-else class="no-todos-message">
          暂无未完成的待办事项，请先添加待办
        </div>
      </div>

      <!-- 专注记录 -->
      <div class="focus-records">
        <div class="records-title">今日专注记录</div>
        <div class="records-list">
          <div 
            v-for="record in todayRecords" 
            :key="record.id"
            class="record-item"
          >
            <div class="record-info">
              <span class="record-mode">{{ getModeName(record.mode) }}</span>
              <span class="record-duration">{{ formatDuration(record.duration) }}</span>
              <span class="record-todo" v-if="record.todoTitle">- {{ record.todoTitle }}</span>
            </div>
            <span class="record-time">{{ formatRecordTime(record.startTime) }}</span>
          </div>
          <div v-if="todayRecords.length === 0" class="no-records">
            暂无专注记录
          </div>
        </div>
      </div>

      <!-- 历史专注数据 -->
      <div class="focus-stats">
        <div class="stats-title">专注统计</div>
        <div class="stats-cards">
          <div class="stat-card">
            <div class="stat-label">今日专注</div>
            <div class="stat-value">{{ formatDuration(todayTotalDuration) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">本周专注</div>
            <div class="stat-value">{{ formatDuration(weekTotalDuration) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">累计专注</div>
            <div class="stat-value">{{ formatDuration(totalDuration) }}</div>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useUserStore } from '../../store/user'
import { get, post } from '../../utils/request'
import Layout from '../../components/Layout.vue'

const userStore = useUserStore()

// 计时模式
const currentMode = ref('stopwatch')
const modes = ['stopwatch', 'countdown', 'pomodoro']

// 计时状态
const isRunning = ref(false)
const isPaused = ref(false)
const isBreakTime = ref(false)
const displayTime = ref(0) // 秒
const elapsedTime = ref(0) // 总 elapsed time in seconds

// 倒计时设置
const countdownMinutes = ref(25)
const countdownSeconds = ref(0)

// 番茄钟设置
const pomodoroMinutes = ref(25)
const pomodoroBreakMinutes = ref(5)
const pomodoroRound = ref(1)
const maxPomodoros = ref(4)

// 鼓励语
const showEncouragement = ref(false)
const currentEncouragement = ref('')
const encouragements = {
  start: [
    '专注当下，你会发现不一样的自己！',
    '开始专注吧，每一次努力都在积累！',
    '专注是一种力量，坚持就是胜利！',
    '用心专注，成就更好的自己！'
  ],
  running: [
    '保持专注，你正在超越大多数人！',
    '坚持就是胜利，继续加油！',
    '专注的你最美丽，继续保持！',
    '每分每秒都在创造价值！'
  ],
  half: [
    '已经完成一半了，继续坚持！',
    '不错哦，继续保持专注！',
    '你已经进入状态了！',
    '加油！胜利就在眼前！'
  ],
  almost: [
    '马上就完成了，再坚持一下！',
    '最后冲刺阶段，不要放弃！',
    '胜利就在眼前，继续加油！',
    '你做得很好，马上就完成了！'
  ],
  completed: [
    '太棒了！你完成了这次专注！',
    '恭喜你，又完成了一次专注！',
    '专注力满满，为你点赞！',
    '做得好！继续保持！'
  ],
  break: [
    '休息一下吧，你值得！',
    '休息是为了更好地出发！',
    '放松一下，短暂休息！',
    '休息时光，好好享受！'
  ]
}

// 关联待办
const todos = ref([])
const relatedTodo = ref(null)

// 专注记录
const todayRecords = ref([])
const todayTotalDuration = ref(0)
const weekTotalDuration = ref(0)
const totalDuration = ref(0)

// 计时器 interval
let timerInterval = null

// 计算属性
const uncompletedTodos = computed(() => {
  return todos.value.filter(todo => !todo.completed)
})

const timerStatusText = computed(() => {
  if (currentMode.value === 'pomodoro') {
    return isBreakTime.value ? '休息中' : (isRunning.value ? '专注中' : '准备开始')
  }
  return isRunning.value ? '进行中' : (isPaused.value ? '已暂停' : '准备开始')
})

// 切换模式
const switchMode = (mode) => {
  if (isRunning.value) return
  currentMode.value = mode
  resetTimer()
}

// 设置番茄钟时间
const setPomodoroTime = (minutes) => {
  pomodoroMinutes.value = minutes
}

// 格式化时间显示
const formatTime = (seconds) => {
  const hrs = Math.floor(seconds / 3600)
  const mins = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  if (hrs > 0) {
    return `${hrs.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 格式化时长
const formatDuration = (seconds) => {
  if (seconds < 60) {
    return `${seconds}秒`
  }
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  if (mins < 60) {
    return secs > 0 ? `${mins}分${secs}秒` : `${mins}分钟`
  }
  const hrs = Math.floor(mins / 60)
  const remainingMins = mins % 60
  return `${hrs}小时${remainingMins}分钟`
}

// 格式化记录时间
const formatRecordTime = (dateStr) => {
  const date = new Date(dateStr)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 获取模式名称
const getModeName = (mode) => {
  const modeNames = {
    stopwatch: '正向计时',
    countdown: '倒计时',
    pomodoro: '番茄钟'
  }
  return modeNames[mode] || mode
}

// 显示鼓励语
const showEncouragementMessage = (type) => {
  const messages = encouragements[type]
  if (messages && messages.length > 0) {
    const randomIndex = Math.floor(Math.random() * messages.length)
    currentEncouragement.value = messages[randomIndex]
    showEncouragement.value = true
    setTimeout(() => {
      showEncouragement.value = false
    }, 3000)
  }
}

// 开始计时
const startTimer = () => {
  // 检查是否已选择待办事项
  if (!relatedTodo.value) {
    uni.showToast({ title: '请先选择一个待办事项', icon: 'none' })
    return
  }
  
  if (currentMode.value === 'countdown') {
    displayTime.value = countdownMinutes.value * 60
  } else {
    displayTime.value = 0
  }
  isRunning.value = true
  isPaused.value = false
  
  showEncouragementMessage('start')
  
  timerInterval = setInterval(() => {
    if (currentMode.value === 'stopwatch') {
      displayTime.value++
      elapsedTime.value++
    } else if (currentMode.value === 'countdown') {
      displayTime.value--
      elapsedTime.value++
      if (displayTime.value <= 0) {
        stopTimer()
        showEncouragementMessage('completed')
      }
    }
    
    // 动态鼓励语
    if (currentMode.value === 'stopwatch' || currentMode.value === 'countdown') {
      const totalSeconds = currentMode.value === 'countdown' 
        ? countdownMinutes.value * 60 
        : elapsedTime.value
      const progress = totalSeconds / 60 // 每分钟检查一次
      
      if (progress === 5) showEncouragementMessage('half')
      if (progress === 10) showEncouragementMessage('almost')
    }
  }, 1000)
}

// 暂停计时
const pauseTimer = () => {
  isPaused.value = true
  isRunning.value = false
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

// 停止计时
const stopTimer = () => {
  isRunning.value = false
  isPaused.value = false
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
  
  // 保存记录
  if (elapsedTime.value > 0) {
    saveRecord()
  }
}

// 重置计时
const resetTimer = () => {
  stopTimer()
  displayTime.value = 0
  elapsedTime.value = 0
  countdownSeconds.value = 0
}

// 开始番茄钟
const startPomodoro = () => {
  // 检查是否已选择待办事项
  if (!relatedTodo.value) {
    uni.showToast({ title: '请先选择一个待办事项', icon: 'none' })
    return
  }
  
  isRunning.value = true
  isBreakTime.value = false
  displayTime.value = pomodoroMinutes.value * 60
  elapsedTime.value = 0
  
  showEncouragementMessage('start')
  
  timerInterval = setInterval(() => {
    displayTime.value--
    elapsedTime.value++
    
    if (displayTime.value <= 0) {
      if (!isBreakTime.value) {
        // 专注时间结束，开始休息
        isBreakTime.value = true
        displayTime.value = pomodoroBreakMinutes.value * 60
        showEncouragementMessage('break')
        saveRecord()
        elapsedTime.value = 0
      } else {
        // 休息时间结束，开始下一个番茄钟
        isBreakTime.value = false
        displayTime.value = pomodoroMinutes.value * 60
        pomodoroRound.value++
        showEncouragementMessage('start')
        elapsedTime.value = 0
        
        if (pomodoroRound.value > maxPomodoros.value) {
          // 完成所有番茄钟
          stopTimer()
          showEncouragementMessage('completed')
          pomodoroRound.value = 1
        }
      }
    }
    
    // 动态鼓励语
    if (!isBreakTime.value) {
      const progress = elapsedTime.value / 60
      if (progress === Math.floor(pomodoroMinutes.value / 2)) {
        showEncouragementMessage('half')
      }
      if (progress === pomodoroMinutes.value - 2) {
        showEncouragementMessage('almost')
      }
    }
  }, 1000)
}

// 暂停番茄钟
const pausePomodoro = () => {
  pauseTimer()
}

// 跳过休息
const skipBreak = () => {
  if (isBreakTime.value) {
    isBreakTime.value = false
    displayTime.value = pomodoroMinutes.value * 60
    elapsedTime.value = 0
    if (timerInterval) {
      clearInterval(timerInterval)
      timerInterval = null
    }
    startPomodoro()
  }
}

// 选择待办
const selectTodo = (todo) => {
  relatedTodo.value = todo
}

// 移除关联待办
const removeRelatedTodo = () => {
  relatedTodo.value = null
}

// 保存专注记录
const saveRecord = async () => {
  if (!userStore.getToken) return
  
  try {
    const recordData = {
      mode: currentMode.value,
      duration: elapsedTime.value,
      todoId: relatedTodo.value.id,
      todoTitle: relatedTodo.value.title,
      startTime: new Date().toISOString()
    }
    
    await post('/focus/record', recordData, {
      'Authorization': `Bearer ${userStore.getToken}`
    })
    
    // 重新获取记录
    await fetchRecords()
  } catch (error) {
    console.error('保存专注记录失败:', error)
  }
}

// 获取今日待办列表
const fetchTodos = async () => {
  if (!userStore.getToken) return
  
  try {
    const response = await get('/todo/today', {}, {
      'Authorization': `Bearer ${userStore.getToken}`
    })
    
    if (response.code === 200) {
      todos.value = response.data || []
    }
  } catch (error) {
    console.error('获取待办列表失败:', error)
  }
}

// 获取专注记录
const fetchRecords = async () => {
  if (!userStore.getToken) return
  
  try {
    const response = await get('/focus/records', {}, {
      'Authorization': `Bearer ${userStore.getToken}`
    })
    
    if (response.code === 200) {
      todayRecords.value = response.data.todayRecords || []
      todayTotalDuration.value = response.data.todayTotal || 0
      weekTotalDuration.value = response.data.weekTotal || 0
      totalDuration.value = response.data.total || 0
    }
  } catch (error) {
    console.error('获取专注记录失败:', error)
  }
}

// 初始化
onMounted(() => {
  userStore.initUserInfo()
  fetchTodos()
  fetchRecords()
})

// 清理
onUnmounted(() => {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
  // 如果正在计时，保存记录
  if (isRunning.value && elapsedTime.value > 0) {
    saveRecord()
  }
})
</script>

<style scoped>
.focus-container {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

/* 模式选择标签 */
.mode-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 30px;
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 6px;
}

.mode-tab {
  flex: 1;
  text-align: center;
  padding: 12px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
}

.mode-tab.active {
  background-color: #C2977F;
  color: white;
}

.mode-tab:hover:not(.active) {
  background-color: rgba(194, 151, 127, 0.2);
}

/* 计时器显示 */
.timer-display {
  text-align: center;
  margin-bottom: 24px;
  padding: 40px 20px;
  background-color: #F2EEE8;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.timer-time {
  font-size: 64px;
  font-weight: 600;
  color: #333;
  font-family: 'SF Mono', 'Menlo', 'Monaco', monospace;
  letter-spacing: 2px;
}

.timer-status {
  font-size: 14px;
  color: #666;
  margin-top: 8px;
}

/* 鼓励语 */
.encouragement {
  text-align: center;
  margin-bottom: 24px;
  padding: 16px;
  background: linear-gradient(135deg, #C2977F 0%, #D8C8BE 100%);
  border-radius: 12px;
  animation: fadeIn 0.5s ease;
}

.encouragement-text {
  color: white;
  font-size: 14px;
  font-weight: 500;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 时间设置 */
.time-settings {
  margin-bottom: 24px;
}

.time-input-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.time-input {
  width: 80px;
  padding: 12px;
  border: 1px solid #D8C8BE;
  border-radius: 8px;
  font-size: 18px;
  text-align: center;
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

.time-unit {
  font-size: 14px;
  color: #666;
}

.time-presets {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.time-preset {
  padding: 10px 20px;
  border: 1px solid #D8C8BE;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

.time-preset.active {
  background-color: #C2977F;
  border-color: #C2977F;
  color: white;
}

.time-preset:hover:not(.active) {
  border-color: #C2977F;
  color: #C2977F;
}

/* 控制按钮 */
.timer-controls {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 30px;
}

.control-btn {
  padding: 14px 32px;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 100px;
}

.start-btn {
  background-color: #C2977F;
  color: white;
}

.start-btn:hover {
  background-color: #B08A72;
}

.pause-btn {
  background-color: #94A7C8;
  color: white;
}

.pause-btn:hover {
  background-color: #7A92B0;
}

.reset-btn {
  background-color: #F2EEE8;
  color: #666;
  border: 1px solid #D8C8BE;
}

.reset-btn:hover {
  background-color: #E8DDD5;
}

.break-btn {
  background-color: #7DC5A8;
  color: white;
}

.break-btn:hover {
  background-color: #5FB08E;
}

/* 关联待办 */
.related-todo {
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 24px;
}

.related-todo-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.related-todo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background-color: white;
  border-radius: 8px;
}

.todo-title {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.todo-remove {
  color: #999;
  font-size: 18px;
  cursor: pointer;
  padding: 0 8px;
}

.todo-remove:hover {
  color: #C2977F;
}

/* 待办选择器 */
.todo-selector {
  margin-bottom: 24px;
}

.todo-selector-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.todo-selector-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.todo-selector-item {
  padding: 8px 16px;
  background-color: white;
  border: 1px solid #D8C8BE;
  border-radius: 20px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-selector-item:hover {
  border-color: #C2977F;
  color: #C2977F;
}

.todo-selector-footer {
  margin-top: 12px;
  text-align: center;
}

.custom-input-btn {
  padding: 8px 16px;
  border: 1px solid #C2977F;
  border-radius: 20px;
  font-size: 13px;
  color: #C2977F;
  background-color: transparent;
  cursor: pointer;
  transition: all 0.3s ease;
}

.custom-input-btn:hover {
  background-color: #C2977F;
  color: white;
}

/* 自定义专注内容输入 */
.custom-focus-input {
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 24px;
}

.custom-input-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.custom-input-group {
  display: flex;
  gap: 12px;
}

.custom-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #D8C8BE;
  border-radius: 8px;
  font-size: 14px;
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

.save-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  color: white;
  background-color: #C2977F;
  cursor: pointer;
  transition: all 0.3s ease;
}

.save-btn:hover {
  background-color: #B08A72;
}

/* 专注记录 */
.focus-records {
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 24px;
}

.records-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

.records-list {
  max-height: 200px;
  overflow-y: auto;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background-color: white;
  border-radius: 8px;
  margin-bottom: 8px;
}

.record-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.record-mode {
  font-size: 13px;
  color: #C2977F;
  font-weight: 500;
}

.record-duration {
  font-size: 13px;
  color: #333;
}

.record-todo {
  font-size: 12px;
  color: #666;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-time {
  font-size: 12px;
  color: #999;
}

.no-records {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 20px;
}

/* 专注统计 */
.focus-stats {
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 16px;
}

.stats-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-card {
  background-color: white;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #C2977F;
}
</style>
