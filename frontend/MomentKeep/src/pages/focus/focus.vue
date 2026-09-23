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
        <div class="time-stepper" v-if="currentMode === 'countdown'">
          <div
            class="stepper-btn"
            :class="{ disabled: countdownMinutes <= minCountdownMinutes }"
            @click="adjustCountdownMinutes(-countdownStep)"
          >−</div>
          <div class="stepper-value">
            <span class="stepper-number">{{ countdownMinutes }}</span>
            <span class="stepper-unit">分钟</span>
          </div>
          <div
            class="stepper-btn"
            :class="{ disabled: countdownMinutes >= maxCountdownMinutes }"
            @click="adjustCountdownMinutes(countdownStep)"
          >+</div>
        </div>
        <div class="time-hint" v-if="currentMode === 'countdown'">
          点击两侧按钮调整时长，每次 {{ countdownStep }} 分钟（{{ minCountdownMinutes }}–{{ maxCountdownMinutes }} 分钟）
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
            class="todo-selector-item stagger-item"
            @click="selectTodo(todo)"
          >
            {{ todo.title }}
          </div>
        </div>
        <div v-else class="no-todos-message">
          还没有待办 · 先去「今日待办」添加一件
        </div>
      </div>

      <!-- 专注记录 -->
      <div class="focus-records">
        <div class="records-title">今日专注记录</div>
        <div class="records-list">
          <div 
            v-for="record in todayRecords" 
            :key="record.id"
            class="record-item stagger-item"
          >
            <div class="record-info">
              <span class="record-mode">{{ getModeName(record.mode) }}</span>
              <span class="record-duration">{{ formatDuration(record.duration) }}</span>
              <span class="record-todo" v-if="record.todoTitle">- {{ record.todoTitle }}</span>
            </div>
            <span class="record-time">{{ formatRecordTime(record.startTime) }}</span>
          </div>
          <div v-if="todayRecords.length === 0" class="no-records">
            今天还没有专注记录
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '../../store/user'
import { get, post } from '../../utils/request'
// 注意：本文件已有同名的 formatTime（格式化时长），时间格式化需用别名导入
import { toLocalDateTime, parseDateTime, formatTime as formatClockTime } from '../../utils/datetime'
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

/** 倒计时时长步进参数：替代原先的裸 input 输入框 */
const countdownStep = 5
const minCountdownMinutes = 5
const maxCountdownMinutes = 180

/**
 * 调整倒计时时长
 * @param {number} delta 增量（分钟），可为负
 */
const adjustCountdownMinutes = (delta) => {
  const next = countdownMinutes.value + delta
  countdownMinutes.value = Math.min(maxCountdownMinutes, Math.max(minCountdownMinutes, next))
}

// 番茄钟设置
const pomodoroMinutes = ref(25)
const pomodoroBreakMinutes = ref(5)
const pomodoroRound = ref(1)
const maxPomodoros = ref(4)

// 过程提示
const showEncouragement = ref(false)
const currentEncouragement = ref('')

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
// 鼓励语提示的定时器句柄（必须在卸载时清理，否则会在组件销毁后修改状态）
let encouragementTimer = null

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
  return formatClockTime(dateStr)
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
  const totalMinutes = pomodoroMinutes.value
  const elapsedMinutes = Math.floor(calcElapsedSeconds() / 60)
  const remainingMinutes = Math.max(0, totalMinutes - elapsedMinutes)
  const round = pomodoroRound.value

  /*
   * 原实现是从 6 组、每组 4 条（共 30 条）的固定鼓励语里随机取一句：
   * 内容与真实进度无关，句式可整体替换（把"专注"换成"跑步"依然成立），
   * 属于情绪填充。现在按当前会话的真实数据生成，每句话都对应一个事实。
   */
  const texts = {
    start: `第 ${round} 轮 / 共 ${maxPomodoros.value} 轮 · 本轮 ${totalMinutes} 分钟`,
    running: `已专注 ${elapsedMinutes} 分钟 · 还剩 ${remainingMinutes} 分钟`,
    half: `已过一半 · 本轮还剩约 ${remainingMinutes} 分钟`,
    almost: `还剩约 ${Math.max(1, remainingMinutes)} 分钟 · 本轮即将结束`,
    completed: `完成第 ${round} 轮 · 本轮 ${totalMinutes} 分钟`,
    break: `休息 ${pomodoroBreakMinutes.value} 分钟 · 之后进入第 ${Math.min(round + 1, maxPomodoros.value)} 轮`
  }

  const text = texts[type]
  if (!text) return

  currentEncouragement.value = text
  showEncouragement.value = true
  if (encouragementTimer) {
    clearTimeout(encouragementTimer)
  }
  encouragementTimer = setTimeout(() => {
    showEncouragement.value = false
    encouragementTimer = null
  }, 3000)
}

// 开始计时
/* ------------------------------ 墙钟计时基准 ------------------------------ */
// 小程序 / App 切到后台时 setInterval 会被系统节流甚至暂停，
// 早期实现用「每秒自增/自减」累加，回到前台后显示会明显偏少且无法补偿。
// 现在每次 tick 都按时间戳重算，恢复前台后第一次 tick 即可自动纠正。

/** 本轮运行开始的绝对时间戳（毫秒），0 表示当前未在计时 */
let runStartedAt = 0
/** 本轮运行之前已累计的秒数（暂停后再继续时作为基数） */
let baseElapsedSeconds = 0
/** 已触发过的鼓励语阈值，避免同一节点重复提示 */
const firedMilestones = new Set()

/** 按墙钟计算当前累计已用秒数 */
const calcElapsedSeconds = () => {
  if (!runStartedAt) return baseElapsedSeconds
  return baseElapsedSeconds + Math.floor((Date.now() - runStartedAt) / 1000)
}

/**
 * 以当前时刻为起点重新开始一个计时阶段
 * @param {number} initialSeconds 该阶段的初始秒数
 */
const resetClock = (initialSeconds) => {
  baseElapsedSeconds = 0
  runStartedAt = Date.now()
  displayTime.value = initialSeconds
  elapsedTime.value = 0
  firedMilestones.clear()
}

/**
 * 按已用分钟数触发鼓励语
 * @description 早期用浮点等值判断（progress === 5），只要该秒被跳过就永远不触发，
 *              这里改为 >= 判断并用集合去重
 * @param {number} elapsedSeconds 已用秒数
 */
const fireElapsedMilestones = (elapsedSeconds) => {
  const minutes = Math.floor(elapsedSeconds / 60)
  if (minutes >= 5 && !firedMilestones.has('half')) {
    firedMilestones.add('half')
    showEncouragementMessage('half')
  }
  if (minutes >= 10 && !firedMilestones.has('almost')) {
    firedMilestones.add('almost')
    showEncouragementMessage('almost')
  }
}

/** 秒表 / 倒计时的 tick：只按时间戳重算显示，不再自增自减 */
const tickTimer = () => {
  const elapsed = calcElapsedSeconds()
  elapsedTime.value = elapsed

  if (currentMode.value === 'countdown') {
    displayTime.value = Math.max(countdownMinutes.value * 60 - elapsed, 0)
    if (displayTime.value <= 0) {
      stopTimer()
      showEncouragementMessage('completed')
      return
    }
  } else {
    displayTime.value = elapsed
  }

  fireElapsedMilestones(elapsed)
}

/** 开始 / 继续计时 */
const startTimer = () => {
  // 检查是否已选择待办事项
  if (!relatedTodo.value) {
    uni.showToast({ title: '请先选择一个待办事项', icon: 'none' })
    return
  }

  // 模板上「继续」按钮走的是同一个方法，这里必须区分「全新开始」与「继续」：
  // 早期实现无论哪种情况都会把 displayTime 重置（倒计时回到满值、秒表归零），
  // 导致暂停后继续时显示与实际已用时长不一致
  const isResume = isPaused.value

  isRunning.value = true
  isPaused.value = false

  if (isResume) {
    // 继续：基数已在 pauseTimer 中结算好，这里只重新锚定时间戳
    runStartedAt = Date.now()
  } else {
    resetClock(currentMode.value === 'countdown' ? countdownMinutes.value * 60 : 0)
  }

  showEncouragementMessage('start')

  timerInterval = setInterval(tickTimer, 1000)
}

// 暂停计时
const pauseTimer = () => {
  isPaused.value = true
  isRunning.value = false
  // 结算：把本轮已用时长并入基数并解除时间戳锚点，继续时从该基数往下走
  baseElapsedSeconds = calcElapsedSeconds()
  runStartedAt = 0
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

// 停止计时
const stopTimer = () => {
  isRunning.value = false
  isPaused.value = false
  // 停表即结束本轮：基数与锚点都要清掉（elapsedTime 保留给 saveRecord 使用）
  runStartedAt = 0
  baseElapsedSeconds = 0
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
  firedMilestones.clear()
}

/** 番茄钟 tick：同样按时间戳重算，专注 / 休息的阶段切换在这里完成 */
const tickPomodoro = () => {
  const elapsed = calcElapsedSeconds()

  if (!isBreakTime.value) {
    elapsedTime.value = elapsed
    displayTime.value = Math.max(pomodoroMinutes.value * 60 - elapsed, 0)

    // 阶段内的两个鼓励语节点（用 >= 判断，避免浮点等值被跳过）
    const halfMinutes = Math.floor(pomodoroMinutes.value / 2)
    if (elapsed >= halfMinutes * 60 && !firedMilestones.has('half')) {
      firedMilestones.add('half')
      showEncouragementMessage('half')
    }
    const almostMinutes = pomodoroMinutes.value - 2
    if (almostMinutes > 0 && elapsed >= almostMinutes * 60 && !firedMilestones.has('almost')) {
      firedMilestones.add('almost')
      showEncouragementMessage('almost')
    }
  } else {
    displayTime.value = Math.max(pomodoroBreakMinutes.value * 60 - elapsed, 0)
  }

  if (displayTime.value > 0) return

  if (!isBreakTime.value) {
    // 专注阶段结束：先结算（saveRecord 读取 elapsedTime），再进入休息阶段
    isBreakTime.value = true
    saveRecord()
    resetClock(pomodoroBreakMinutes.value * 60)
    showEncouragementMessage('break')
    return
  }

  // 休息阶段结束：进入下一轮
  isBreakTime.value = false
  pomodoroRound.value++
  if (pomodoroRound.value > maxPomodoros.value) {
    // 全部番茄钟完成：先把已用时长清零，避免 stopTimer 内重复保存一条记录
    elapsedTime.value = 0
    stopTimer()
    showEncouragementMessage('completed')
    pomodoroRound.value = 1
  } else {
    resetClock(pomodoroMinutes.value * 60)
    showEncouragementMessage('start')
  }
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
  // 重置为一个全新的专注阶段（同时锚定时间戳）
  resetClock(pomodoroMinutes.value * 60)

  showEncouragementMessage('start')

  timerInterval = setInterval(tickPomodoro, 1000)
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
  // 关联待办可能在计时过程中被移除（点了 ×），此时记录无处归属，直接跳过，
  // 否则读取 relatedTodo.value.id 会抛错（页面卸载时保存尤其容易触发）
  if (!relatedTodo.value) return
  
  try {
    const recordData = {
      mode: currentMode.value,
      duration: elapsedTime.value,
      todoId: relatedTodo.value.id,
      todoTitle: relatedTodo.value.title,
      // 后端不会覆盖该字段，必须提交本地时间，否则记录会早 8 小时且凌晨时段被算到前一天
      startTime: toLocalDateTime()
    }
    
    await post('/focus/record', recordData, {
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
  if (encouragementTimer) {
    clearTimeout(encouragementTimer)
    encouragementTimer = null
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
  gap: 12px;
  margin-bottom: 32px;
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 8px;
}

.mode-tab {
  flex: 1;
  text-align: center;
  padding: 12px;
  border-radius: var(--radius-sm);
  font-size: var(--fs-body);
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: var(--transition-interactive);
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
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.timer-time {
  font-size: calc(64px * var(--font-scale, 1));
  font-weight: 600;
  color: #333;
  font-family: 'SF Mono', 'Menlo', 'Monaco', monospace;
  letter-spacing: 2px;
}

.timer-status {
  font-size: var(--fs-body);
  color: #666;
  margin-top: 8px;
}

/* 鼓励语 */
.encouragement {
  text-align: center;
  margin-bottom: 24px;
  padding: 16px;
  background: linear-gradient(135deg, #C2977F 0%, #D8C8BE 100%);
  border-radius: var(--radius-md);
  animation: fadeIn 0.5s ease;
}

.encouragement-text {
  color: white;
  font-size: var(--fs-body);
  font-weight: var(--fw-normal); /* 正文应为 400；原 500 是把"标签的重量"用在了描述文字上 */
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 时间设置 */
/* 时长设置区：套上与全站一致的实色卡片，避免在背景图上浮着看不清 */
.time-settings {
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 16px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-md);
}

/* 倒计时时长步进器：− / 数值 / +（替代原先样式突兀的裸 input） */
.time-stepper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.stepper-btn {
  width: 40px;
  height: 40px;
  line-height: 38px;
  text-align: center;
  font-size: calc(22px * var(--font-scale, 1));
  color: #C2977F;
  background-color: var(--surface-strong, #FFFFFF)FF;
  border: 1px solid var(--border-color, #D8C8BE);
  border-radius: var(--radius-circle);
  cursor: pointer;
  user-select: none;
  transition: var(--transition-interactive);
}

.stepper-btn:active {
  background-color: #EDE4DC;
}

.stepper-btn.disabled {
  color: #CCCCCC;
  border-color: var(--border-color-light, #E8E4DE);
  cursor: not-allowed;
}

.stepper-value {
  display: flex;
  align-items: baseline;
  justify-content: center;
  min-width: 96px;
}

.stepper-number {
  font-size: var(--fs-3xl);
  font-weight: 500;
  color: var(--text-color, #333333);
}

.stepper-unit {
  font-size: var(--fs-sm);
  color: var(--text-secondary, #666666);
  margin-left: 8px;
}

.time-hint {
  margin-top: 12px;
  font-size: var(--fs-xs);
  color: var(--text-muted, #999999);
  text-align: center;
}

.time-presets {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.time-preset {
  padding: 12px 20px;
  border: 1px solid var(--border-color, #D8C8BE);
  border-radius: var(--radius-lg);
  font-size: var(--fs-body);
  color: #666;
  cursor: pointer;
  transition: var(--transition-interactive);
  /* 卡片已提供背景，这里用实色白底：backdrop-filter 仅 H5 生效，
     去掉可保证三端观感一致 */
  background-color: var(--surface-strong, #FFFFFF)FF;
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
  margin-bottom: 32px;
}

.control-btn {
  padding: 16px 32px;
  border: none;
  border-radius: var(--radius-pill);
  font-size: var(--fs-md);
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition-interactive);
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
  background-color: var(--surface-color, #F2EEE8);
  color: #666;
  border: 1px solid var(--border-color, #D8C8BE);
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
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 16px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-md);
}

.related-todo-label {
  font-size: var(--fs-xs);
  color: #666;
  margin-bottom: 8px;
}

.related-todo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 12px;
  background-color: var(--surface-strong, #FFFFFF);
  border-radius: var(--radius-sm);
}

.todo-title {
  font-size: var(--fs-body);
  color: #333;
  font-weight: 500;
}

.todo-remove {
  color: #999;
  font-size: var(--fs-lg);
  cursor: pointer;
  padding: 0 8px;
}

.todo-remove:hover {
  color: #C2977F;
}

/* 待办选择器 */
/* 「请选择专注任务」原先没有任何背景，文字直接浮在页面背景图上导致看不清，
   这里套上与全站一致的实色卡片并加深标题颜色 */
.todo-selector {
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 16px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-md);
}

.todo-selector-title {
  font-size: var(--fs-body);
  font-weight: 500;
  color: var(--text-color, #333333);
  margin-bottom: 12px;
}

.todo-selector-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.todo-selector-item {
  padding: 8px 16px;
  background-color: var(--surface-strong, #FFFFFF);
  border: 1px solid var(--border-color, #D8C8BE);
  border-radius: var(--radius-lg);
  font-size: var(--fs-sm);
  color: #666;
  cursor: pointer;
  transition: var(--transition-interactive);
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
  border-radius: var(--radius-lg);
  font-size: var(--fs-sm);
  color: #C2977F;
  background-color: transparent;
  cursor: pointer;
  transition: var(--transition-interactive);
}

.custom-input-btn:hover {
  background-color: #C2977F;
  color: white;
}

/* 自定义专注内容输入 */
.custom-focus-input {
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 16px;
  margin-bottom: 24px;
}

.custom-input-title {
  font-size: var(--fs-body);
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
  border: 1px solid var(--border-color, #D8C8BE);
  border-radius: var(--radius-sm);
  font-size: var(--fs-body);
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

.save-btn {
  padding: 12px 24px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--fs-body);
  color: white;
  background-color: #C2977F;
  cursor: pointer;
  transition: var(--transition-interactive);
}

.save-btn:hover {
  background-color: #B08A72;
}

/* 专注记录 */
.focus-records {
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 16px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-md);
}

.records-title {
  font-size: var(--fs-md);
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
  padding: 12px 12px;
  background-color: var(--surface-strong, #FFFFFF);
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
}

.record-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.record-mode {
  font-size: var(--fs-sm);
  color: #C2977F;
  font-weight: 500;
}

.record-duration {
  font-size: var(--fs-sm);
  color: #333;
}

.record-todo {
  font-size: var(--fs-xs);
  color: #666;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-time {
  font-size: var(--fs-xs);
  color: #999;
}

.no-records {
  text-align: center;
  color: #999;
  font-size: var(--fs-body);
  padding: 20px;
}

/* 专注统计 */
.focus-stats {
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 16px;
  box-shadow: var(--shadow-md);
}

.stats-title {
  font-size: var(--fs-md);
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
  background-color: var(--surface-strong, #FFFFFF);
  border-radius: var(--radius-sm);
  padding: 16px;
  text-align: center;
}

.stat-label {
  font-size: var(--fs-xs);
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: var(--fs-md);
  font-weight: 600;
  color: #C2977F;
}
</style>
