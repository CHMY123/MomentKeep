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
            <span class="stat-value">{{ checkinStats.early ? '✓' : '✗' }}</span>
            <span class="stat-label">早起打卡</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ checkinStats.sleep ? '✓' : '✗' }}</span>
            <span class="stat-label">睡眠打卡</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ checkinStats.meal }}</span>
            <span class="stat-label">用餐次数</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ checkinStats.exercise ? '✓' : '✗' }}</span>
            <span class="stat-label">运动打卡</span>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Layout from '../../components/Layout.vue'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()

// 倒计时数据
const countdowns = ref([
  {
    title: '生日',
    days: 10,
    date: '2026-04-23'
  },
  {
    title: '旅行',
    days: 30,
    date: '2026-05-13'
  },
  {
    title: '考试',
    days: 60,
    date: '2026-06-13'
  }
])

// 打卡统计
const checkinStats = ref({
  early: true,
  sleep: false,
  meal: 2,
  exercise: false
})

// 方法
const navigateTo = (path) => {
  uni.navigateTo({
    url: path
  })
}

// 生命周期
onMounted(() => {
  // 初始化数据
  // 这里可以从API获取实际数据
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
  content: "✅";
}

.icon-time::before {
  content: "⏰";
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