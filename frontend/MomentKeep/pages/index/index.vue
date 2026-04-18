<template>
  <Layout>
    <div class="home-container">
      <!-- 欢迎区域 -->
      <div class="welcome-section">
        <h1 class="welcome-title">
          你好，{{ userStore.userInfo.nickname || '朋友' }}
        </h1>
        <p class="welcome-subtitle">朝有目标，暮有记录</p>
        <p class="welcome-subtitle">每一天都值得被认真对待</p>
      </div>
      
      <!-- 快捷功能区 -->
      <div class="quick-actions">
        <div class="action-card" @click="navigateTo('/pages/daily-checkin/daily-checkin')">
          <div class="action-icon icon-clock"></div>
          <h3 class="action-title">每日打卡</h3>
          <p class="action-subtitle">记录每一个瞬间</p>
        </div>
        <div class="action-card" @click="navigateTo('/pages/todo/todo')">
          <div class="action-icon icon-todo"></div>
          <h3 class="action-title">今日待办</h3>
          <p class="action-subtitle">完成每一个目标</p>
        </div>
        <div class="action-card" @click="navigateTo('/pages/countdown/countdown')">
          <div class="action-icon icon-time"></div>
          <h3 class="action-title">未来倒计时</h3>
          <p class="action-subtitle">期待每一个重要时刻</p>
        </div>
      </div>
      
      <!-- 最近倒计时 -->
      <div class="countdown-section">
        <div class="section-header">
          <h2 class="section-title">最近倒计时</h2>
          <span class="section-more" @click="navigateTo('/pages/countdown/countdown')">查看全部</span>
        </div>
        <div class="countdown-list">
          <div v-for="(countdown, index) in countdowns" :key="index" class="countdown-card">
            <div class="countdown-time">
              <span class="time-number">{{ countdown.days }}</span>
              <span class="time-unit">天</span>
            </div>
            <div class="countdown-info">
              <h4 class="countdown-title">{{ countdown.title }}</h4>
              <span class="countdown-date">{{ countdown.date }}</span>
            </div>
          </div>
          <div v-if="countdowns.length === 0" class="empty-countdown">
            <span>暂无倒计时</span>
          </div>
        </div>
      </div>
      
      <!-- 今日统计 -->
      <div class="stats-section">
        <div class="section-header">
          <h2 class="section-title">今日统计</h2>
        </div>
        <div class="stats-grid">
          <div class="stat-item">
            <span class="stat-value stat-icon" :class="{ 'checked': checkinStats.early }">
              {{ checkinStats.early ? '√' : '×' }}
            </span>
            <span class="stat-label">早起打卡</span>
          </div>
          <div class="stat-item">
            <span class="stat-value stat-icon" :class="{ 'checked': checkinStats.sleep }">
              {{ checkinStats.sleep ? '√' : '×' }}
            </span>
            <span class="stat-label">睡眠打卡</span>
          </div>
          <div class="stat-item">
            <span class="stat-value stat-number">{{ checkinStats.meal }}</span>
            <span class="stat-label">用餐次数</span>
          </div>
          <div class="stat-item">
            <span class="stat-value stat-icon" :class="{ 'checked': checkinStats.exercise }">
              <span class="icon-run"></span>
            </span>
            <span class="stat-label">运动打卡</span>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
/**
 * MomentKeep 朝暮记 - 首页
 * @description 应用主页面，展示欢迎信息、快捷功能入口、最近倒计时和今日打卡统计
 * @author MomentKeep Team
 * @since 2026-04-18
 */
import { ref, onMounted } from 'vue'
import Layout from '../../components/Layout.vue'
import { useUserStore } from '../../store/user'
import { useCache } from '../../utils/cache'

const userStore = useUserStore()
const { getCache, setCache, fetchWithCache } = useCache()

// 倒计时数据
const countdowns = ref([])

// 打卡统计
const checkinStats = ref({
  early: false,
  sleep: false,
  meal: 0,
  exercise: false
})

// 加载状态
const loading = ref(false)

/**
 * 跳转到指定页面
 * @param {string} path - 页面路径
 */
const navigateTo = (path) => {
  uni.navigateTo({
    url: path
  })
}

/**
 * 获取最近倒计时
 * @description 从API获取倒计时列表，计算剩余天数并缓存
 */
const fetchCountdowns = async () => {
  if (!userStore.getToken) {
    return
  }

  const fetchFn = async () => {
    const response = await uni.request({
      url: '/api/countdown',
      header: {
        'Authorization': `Bearer ${userStore.getToken}`
      }
    })

    if (response.statusCode === 200 && response.data.code === 200) {
      const countdownList = response.data.data || []
      const now = new Date()
      const sortedCountdowns = countdownList
        .filter(countdown => countdown && countdown.targetTime)
        .map(countdown => {
          const targetDate = new Date(countdown.targetTime)
          const diffTime = targetDate - now
          const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
          return {
            id: countdown.id,
            title: countdown.title,
            days: diffDays > 0 ? diffDays : 0,
            date: countdown.targetTime.split('T')[0]
          }
        })
        .sort((a, b) => a.days - b.days)
        .slice(0, 3)
      return sortedCountdowns
    }
    return []
  }

  try {
    loading.value = true
    const cachedCountdowns = getCache('countdowns')
    if (cachedCountdowns !== null) {
      countdowns.value = cachedCountdowns
    }
    const data = await fetchWithCache('countdowns', fetchFn)
    countdowns.value = data
  } catch (error) {
    console.error('获取倒计时失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 获取今日打卡统计
 * @description 调用API获取今日打卡记录，统计各类打卡完成情况
 */
const fetchCheckinStats = async () => {
  if (!userStore.getToken) {
    return
  }

  const date = new Date().toISOString().split('T')[0]
  const cacheKey = `checkinStats_${date}`

  const fetchFn = async () => {
    const response = await uni.request({
      url: `/api/checkin/by-date?date=${date}`,
      header: {
        'Authorization': `Bearer ${userStore.getToken}`
      }
    })

    if (response.statusCode === 200 && response.data.code === 200) {
      return response.data.data || []
    }
    return []
  }

  try {
    const checkinList = await fetchFn()
    setCache(cacheKey, checkinList)

    checkinStats.value = {
      early: false,
      sleep: false,
      meal: 0,
      exercise: false
    }

    for (const checkin of checkinList) {
      switch (checkin.type) {
        case 'early':
          checkinStats.value.early = true
          break
        case 'sleep':
          checkinStats.value.sleep = true
          break
        case 'meal':
          checkinStats.value.meal += 1
          break
        case 'exercise':
          checkinStats.value.exercise = true
          break
      }
    }
  } catch (error) {
    const cachedStats = getCache(cacheKey)
    if (cachedStats) {
      checkinStats.value = {
        early: false,
        sleep: false,
        meal: 0,
        exercise: false
      }
      for (const checkin of cachedStats) {
        switch (checkin.type) {
          case 'early':
            checkinStats.value.early = true
            break
          case 'sleep':
            checkinStats.value.sleep = true
            break
          case 'meal':
            checkinStats.value.meal += 1
            break
          case 'exercise':
            checkinStats.value.exercise = true
            break
        }
      }
    }
    console.error('获取打卡统计失败:', error)
  }
}

/**
 * 组件挂载时初始化数据
 * @description 检查登录状态并加载倒计时和打卡统计数据
 */
onMounted(async () => {
  // 检查是否登录
  if (!userStore.getToken) {
    return
  }

  // 初始化数据
  await fetchCountdowns()
  await fetchCheckinStats()
})
</script>

<style scoped>
.home-container {
  width: 100%;
}

/* 欢迎区域 */
.welcome-section {
  text-align: center;
  margin-bottom: 30px;
}

.welcome-title {
  font-size: 24px;
  font-weight: 600;
  color: #C2977F;
  margin-bottom: 8px;
  display: block;
}

.welcome-subtitle {
  font-size: 14px;
  color: #666666;
}

/* 快捷功能区 */
.quick-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30px;
}

.action-card {
  flex: 1;
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 20px;
  margin: 0 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  cursor: pointer;
}

.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-card .action-icon {
  font-size: 32px;
  margin-bottom: 12px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 图标样式 */
.icon-clock::before {
  content: "🕒";
}

.icon-todo::before {
  content: "";
  width: 32px;
  height: 32px;
  display: inline-block;
  background-image: url(https://img.icons8.com/color/96/000000/todo-list.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-time::before {
  content: "";
  width: 32px;
  height: 32px;
  display: inline-block;
  background-image: url(https://img.icons8.com/color/96/000000/hourglass.png);
  background-size: contain;
  background-repeat: no-repeat;
}

/* 统计图标 */
.icon-sunny::before {
  content: "";
  width: 24px;
  height: 24px;
  display: inline-block;
  background-image: url(https://img.icons8.com/color/48/000000/sun.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-moon::before {
  content: "";
  width: 24px;
  height: 24px;
  display: inline-block;
  background-image: url(https://img.icons8.com/color/48/000000/moon.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-run::before {
  content: "";
  width: 24px;
  height: 24px;
  display: inline-block;
  background-image: url(https://img.icons8.com/color/48/000000/running.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
}

.stat-icon.checked {
  color: #4CAF50;
}

.stat-icon:not(.checked) {
  color: #F44336;
}

.stat-number {
  font-size: 24px;
  font-weight: 600;
  color: #C2977F;
}

.action-title {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  display: block;
  margin-bottom: 4px;
}

.action-subtitle {
  font-size: 12px;
  color: #999999;
}

/* 通用区域样式 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  border-bottom: 1px solid #94A7C8;
  padding-bottom: 4px;
}

.section-more {
  font-size: 14px;
  color: #94A7C8;
  cursor: pointer;
}

/* 倒计时区域 */
.countdown-section {
  margin-bottom: 30px;
}

.countdown-list {
  display: flex;
  overflow-x: auto;
  padding-bottom: 10px;
}

.countdown-card {
  min-width: 120px;
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 16px;
  margin-right: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.countdown-time {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.time-number {
  font-size: 24px;
  font-weight: 600;
  color: #C2977F;
}

.time-unit {
  font-size: 14px;
  color: #666666;
  margin-left: 4px;
}

.countdown-title {
  font-size: 14px;
  font-weight: 500;
  color: #333333;
  margin-bottom: 4px;
  text-align: center;
}

.countdown-date {
  font-size: 12px;
  color: #999999;
}

.empty-countdown {
  flex: 1;
  text-align: center;
  padding: 40px 0;
  color: #999999;
  font-size: 14px;
}

/* 统计区域 */
.stats-section {
  margin-bottom: 30px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-item {
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #C2977F;
  display: block;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666666;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .quick-actions {
    flex-direction: column;
  }
  
  .action-card {
    margin: 8px 0;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>