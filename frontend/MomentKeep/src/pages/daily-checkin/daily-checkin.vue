<template>
  <Layout>
    <div class="checkin-container">
      <!-- 打卡卡片 -->
      <div class="checkin-cards">
        <!-- 早起打卡 -->
        <div class="checkin-card" :class="{ 'checked': checkins.early }">
          <div class="checkin-header">
            <div class="checkin-icon icon-sunny" :class="{ 'checked': checkins.early }"></div>
            <div class="checkin-title">早起打卡</div>
            <div class="checkin-status" v-if="checkins.early">已打卡</div>
          </div>
          <div class="checkin-time" v-if="checkins.early">
            <div>{{ checkins.earlyTime }}</div>
          </div>
          <div class="checkin-actions" v-if="checkins.early">
            <button class="checkin-btn cancel-btn" @click="cancelCheckin('early')">
              取消打卡
            </button>
          </div>
          <button 
            v-else-if="dataReady"
            class="checkin-btn" 
            @click="checkin('early')"
          >
            打卡
          </button>
          <!-- 加载期间不显示可点击的"打卡"按钮：既避免状态跳变，也避免误触重复打卡 -->
          <div v-else class="checkin-loading">加载中…</div>
        </div>
        
        <!-- 睡眠打卡 -->
        <div class="checkin-card" :class="{ 'checked': checkins.sleep }">
          <div class="checkin-header">
            <div class="checkin-icon icon-moon" :class="{ 'checked': checkins.sleep }"></div>
            <div class="checkin-title">睡眠打卡</div>
            <div class="checkin-status" v-if="checkins.sleep">已打卡</div>
          </div>
          <div class="checkin-time" v-if="checkins.sleep">
            <div>{{ checkins.sleepTime }}</div>
          </div>
          <div class="checkin-actions" v-if="checkins.sleep">
            <button class="checkin-btn cancel-btn" @click="cancelCheckin('sleep')">
              取消打卡
            </button>
          </div>
          <button 
            v-else-if="dataReady"
            class="checkin-btn" 
            @click="checkin('sleep')"
          >
            打卡
          </button>
          <div v-else class="checkin-loading">加载中…</div>
        </div>
        
        <!-- 用餐打卡 -->
        <div class="checkin-card" :class="{ 'checked': checkins.meals.length > 0 }">
          <div class="checkin-header">
            <div class="checkin-icon icon-restaurant" :class="{ 'checked': checkins.meals.length > 0 }"></div>
            <div class="checkin-title">用餐打卡</div>
            <div class="checkin-status" v-if="checkins.meals.length > 0">已打卡 {{ checkins.meals.length }} 次</div>
          </div>
          <!-- 加载期间先显示加载态：避免先渲染"选类型 + 打卡按钮"再跳变成已打卡记录 -->
          <div class="checkin-loading" v-if="!dataReady">加载中…</div>
          <div class="meal-types" v-else-if="checkins.meals.length === 0 || showAddMealType">
            <div 
              v-for="type in mealTypes" 
              :key="type"
              class="meal-type"
              :class="{ 'selected': selectedMealType === type }"
              @click="selectMealType(type)"
            >
              <div>{{ type }}</div>
            </div>
            <div class="meal-type custom" :class="{ 'selected': selectedMealType && !mealTypes.includes(selectedMealType) }" @click="showCustomMeal">
              <div>自定义</div>
            </div>
          </div>
          <div class="checkin-records" v-else>
            <div v-for="(meal, index) in checkins.meals" :key="index" class="checkin-record stagger-item">
              <div class="checkin-record-info">
                <div class="checkin-time">{{ meal.time }}</div>
                <div class="meal-type-text">{{ meal.type }}</div>
              </div>
              <button class="checkin-record-cancel" @click="cancelMealCheckin(index)">
                ×
              </button>
            </div>
          </div>
          <div class="checkin-actions" v-if="checkins.meals.length > 0 && !showAddMealType">
            <button class="checkin-btn" @click="resetMealCheckin">
              再次打卡
            </button>
          </div>
          <button 
            v-if="showAddMealType"
            class="checkin-btn" 
            @click="checkin('meal')"
            :disabled="!selectedMealType"
          >
            打卡
          </button>
          <button 
            v-if="dataReady && !showAddMealType && checkins.meals.length === 0"
            class="checkin-btn" 
            @click="checkin('meal')"
            :disabled="!selectedMealType"
          >
            打卡
          </button>
        </div>
        
        <!-- 运动打卡 -->
        <div class="checkin-card" :class="{ 'checked': checkins.exercises.length > 0 }">
          <div class="checkin-header">
            <div class="checkin-icon icon-run" :class="{ 'checked': checkins.exercises.length > 0 }"></div>
            <div class="checkin-title">运动打卡</div>
            <div class="checkin-status" v-if="checkins.exercises.length > 0">已打卡 {{ checkins.exercises.length }} 次</div>
          </div>
          <!-- 加载期间先显示加载态：避免先渲染"选类型 + 打卡按钮"再跳变成已打卡记录 -->
          <div class="checkin-loading" v-if="!dataReady">加载中…</div>
          <div class="exercise-types" v-else-if="checkins.exercises.length === 0 || showAddExerciseType">
            <div 
              v-for="type in exerciseTypes" 
              :key="type"
              class="exercise-type"
              :class="{ 'selected': selectedExerciseType === type }"
              @click="selectExerciseType(type)"
            >
              <div>{{ type }}</div>
            </div>
            <div class="exercise-type custom" :class="{ 'selected': selectedExerciseType && !exerciseTypes.includes(selectedExerciseType) }" @click="showCustomExercise">
              <div>自定义</div>
            </div>
          </div>
          <div class="checkin-records" v-else>
            <div v-for="(exercise, index) in checkins.exercises" :key="index" class="checkin-record stagger-item">
              <div class="checkin-record-info">
                <div class="checkin-time">{{ exercise.time }}</div>
                <div class="exercise-type-text">{{ exercise.type }}</div>
              </div>
              <button class="checkin-record-cancel" @click="cancelExerciseCheckin(index)">
                ×
              </button>
            </div>
          </div>
          <div class="checkin-actions" v-if="checkins.exercises.length > 0 && !showAddExerciseType">
            <button class="checkin-btn" @click="resetExerciseCheckin">
              再次打卡
            </button>
          </div>
          <button 
            v-if="showAddExerciseType"
            class="checkin-btn" 
            @click="checkin('exercise')"
            :disabled="!selectedExerciseType"
          >
            打卡
          </button>
          <button 
            v-if="dataReady && !showAddExerciseType && checkins.exercises.length === 0"
            class="checkin-btn" 
            @click="checkin('exercise')"
            :disabled="!selectedExerciseType"
          >
            打卡
          </button>
        </div>
      </div>
      
      <!-- 今日打卡时间分布 -->
      <div class="time-distribution-section">
        <div class="section-title">今日打卡时间分布</div>
        <div class="distribution-card">
          <div class="distribution-tagline">{{ distributionTagline }}</div>
          <div class="time-slots">
            <div class="time-slot">
              <div class="time-slot-header">
                <div class="time-slot-title">清晨 0–6 点</div>
              </div>
              <div class="time-slot-content">
                <div v-if="timeDistribution.morning.length > 0" class="checkin-items">
                  <div v-for="(item, index) in timeDistribution.morning" :key="index" class="checkin-item stagger-item">
                    <span class="checkin-type">{{ item.type }}</span>
                    <span class="checkin-time">{{ item.time }}</span>
                  </div>
                </div>
                <div v-else class="empty-checkin">{{ dataReady ? '暂无打卡' : '加载中…' }}</div>
              </div>
            </div>
            
            <div class="time-slot">
              <div class="time-slot-header">
                <div class="time-slot-title">上午 7–12 点</div>
              </div>
              <div class="time-slot-content">
                <div v-if="timeDistribution.midMorning.length > 0" class="checkin-items">
                  <div v-for="(item, index) in timeDistribution.midMorning" :key="index" class="checkin-item stagger-item">
                    <span class="checkin-type">{{ item.type }}</span>
                    <span class="checkin-time">{{ item.time }}</span>
                  </div>
                </div>
                <div v-else class="empty-checkin">{{ dataReady ? '暂无打卡' : '加载中…' }}</div>
              </div>
            </div>
            
            <div class="time-slot">
              <div class="time-slot-header">
                <div class="time-slot-title">下午 13–18 点</div>
              </div>
              <div class="time-slot-content">
                <div v-if="timeDistribution.afternoon.length > 0" class="checkin-items">
                  <div v-for="(item, index) in timeDistribution.afternoon" :key="index" class="checkin-item stagger-item">
                    <span class="checkin-type">{{ item.type }}</span>
                    <span class="checkin-time">{{ item.time }}</span>
                  </div>
                </div>
                <div v-else class="empty-checkin">{{ dataReady ? '暂无打卡' : '加载中…' }}</div>
              </div>
            </div>
            
            <div class="time-slot">
              <div class="time-slot-header">
                <div class="time-slot-title">晚上 19–24 点</div>
              </div>
              <div class="time-slot-content">
                <div v-if="timeDistribution.evening.length > 0" class="checkin-items">
                  <div v-for="(item, index) in timeDistribution.evening" :key="index" class="checkin-item stagger-item">
                    <span class="checkin-type">{{ item.type }}</span>
                    <span class="checkin-time">{{ item.time }}</span>
                  </div>
                </div>
                <div v-else class="empty-checkin">{{ dataReady ? '暂无打卡' : '加载中…' }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 数据可视化 -->
      <div class="stats-section">
        <div class="section-title">打卡数据统计</div>
        <div class="stats-cards">
          <div class="stat-card">
            <div class="stat-title">早起打卡</div>
            <div class="stat-value">{{ dataReady ? stats.earlyRate + '%' : '—' }}</div>
            <div class="stat-chart">
              <div class="chart-bar" :style="{ width: stats.earlyRate + '%' }"></div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-title">睡眠打卡</div>
            <div class="stat-value">{{ dataReady ? stats.sleepRate + '%' : '—' }}</div>
            <div class="stat-chart">
              <div class="chart-bar" :style="{ width: stats.sleepRate + '%' }"></div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-title">用餐打卡</div>
            <div class="stat-value">{{ dataReady ? stats.mealCount + '次/天' : '—' }}</div>
            <div class="stat-chart">
              <div class="chart-bar" :style="{ width: (stats.mealCount / 4) * 100 + '%' }"></div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-title">运动打卡</div>
            <div class="stat-value">{{ dataReady ? stats.exerciseRate + '%' : '—' }}</div>
            <div class="stat-chart">
              <div class="chart-bar" :style="{ width: stats.exerciseRate + '%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 历史打卡时间分布 -->
      <div class="history-distribution-section">
        <div class="section-title">历史打卡时间分布</div>
        <div class="distribution-card">
          <!-- 卡片头：左侧切换统计范围，右侧进入完整历史记录页 -->
          <div class="chart-card-head">
            <div class="range-switch">
              <div
                class="range-item"
                :class="{ active: historyRange === '30d' }"
                @click="switchHistoryRange('30d')"
              >近30天</div>
              <div
                class="range-item"
                :class="{ active: historyRange === 'all' }"
                @click="switchHistoryRange('all')"
              >全部</div>
            </div>
            <div class="chart-head-right">
              <span class="chart-total">共 {{ historySummary.total || 0 }} 次</span>
              <span class="view-all" @click="goCheckinHistory">查看全部 ›</span>
            </div>
          </div>

          <!--
            口径切换：全部 / 早起 / 睡眠 / 用餐 / 运动。
            各口径的数据在首次请求时已一并取回，切换是纯前端行为，不产生网络请求。
          -->
          <div class="scope-switch">
            <div
              v-for="scope in CHART_SCOPES"
              :key="scope.key"
              class="scope-item"
              :class="{ active: chartScope === scope.key }"
              @click="switchChartScope(scope.key)"
            >{{ scope.label }}</div>
          </div>

          <!--
            柱状 + 折线 组合图：
            柱 = 当前口径各时段的次数（左轴），折线 = 打卡率（右轴 0~100%）。
            两个序列量纲不同、含义独立（量 vs 坚持度），因此双轴在此是正确用法。
          -->
          <checkin-time-chart
            :buckets="chartBuckets"
            :rate="chartRate"
            :scope-label="chartScopeLabel"
          />
          <div class="distribution-tagline">{{ historyTagline }}</div>
        </div>
      </div>

      <!-- 自定义用餐类型弹窗 -->
      <div class="modal" v-if="isCustomMealDialogOpen">
        <div class="modal-content">
          <div class="modal-header">
            <h3>自定义用餐类型</h3>
            <div class="close-icon" @click="closeCustomMealDialog">×</div>
          </div>
          <div class="modal-body">
            <input type="text" v-model="customMealType" placeholder="请输入用餐类型" />
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="closeCustomMealDialog">取消</button>
            <button class="confirm-btn" @click="confirmCustomMeal">确定</button>
          </div>
        </div>
      </div>
      
      <!-- 自定义运动类型弹窗 -->
      <div class="modal" v-if="isCustomExerciseDialogOpen">
        <div class="modal-content">
          <div class="modal-header">
            <h3>自定义运动类型</h3>
            <div class="close-icon" @click="closeCustomExerciseDialog">×</div>
          </div>
          <div class="modal-body">
            <input type="text" v-model="customExerciseType" placeholder="请输入运动类型" />
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="closeCustomExerciseDialog">取消</button>
            <button class="confirm-btn" @click="confirmCustomExercise">确定</button>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
/**
 * MomentKeep 朝暮记 - 每日打卡页面
 * @description 提供早起、睡眠、用餐、运动等打卡功能，以及打卡数据统计和时间分布分析
 * @author MomentKeep Team
 * @since 2026-04-18
 */
import { ref, computed, onMounted, watch, reactive } from 'vue'
import Layout from '../../components/Layout.vue'
import CheckinTimeChart from '../../components/CheckinTimeChart.vue'
import { useUserStore } from '../../store/user'
import { get, post, put, del } from '../../utils/request'
import { useCache } from '../../utils/cache'
import { toLocalDateTime, toLocalDate, localDateOffset } from '../../utils/datetime'

const userStore = useUserStore()
const { getCache, setCache, removeCache, fetchWithCache } = useCache()

// 图表滚动容器引用
const chartScrollView = ref(null)

// 打卡状态
const checkins = reactive({
  early: false,
  earlyTime: '',
  sleep: false,
  sleepTime: '',
  meals: [],
  exercises: []
})

// 用餐类型选项
const mealTypes = ['早餐', '午餐', '晚餐', '宵夜']
const selectedMealType = ref('')
const customMealType = ref('')
const isCustomMealDialogOpen = ref(false)
const showAddMealType = ref(false)

// 运动类型选项
const exerciseTypes = ['跑步', '健身', '瑜伽', '游泳']
const selectedExerciseType = ref('')
const customExerciseType = ref('')
const isCustomExerciseDialogOpen = ref(false)
const showAddExerciseType = ref(false)

// 统计数据
const stats = reactive({
  earlyRate: 0,
  sleepRate: 0,
  mealCount: 0,
  exerciseRate: 0
})

// 加载状态（仅用于打卡/取消打卡等"动作进行中"）
const loading = ref(false)

/**
 * 首屏数据是否已就绪
 *
 * @description 用于在初次加载期间隐藏"打卡按钮 / 暂无打卡 / 0%"这类误导性状态。
 * 否则用户会先看到"可以打卡、没有记录"的形态，几百毫秒后整页跳变成真实数据，
 * 每次切页都经历一次跳变，很容易疲劳。loading 只描述动作进行中，不能复用为它。
 */
const dataReady = ref(false)

// 历史打卡：统计范围（'30d' 近30天 / 'all' 全部历史）
const historyRange = ref('30d')
// 历史打卡时间分布汇总（由后端聚合返回）：
//   total / activeDays / buckets / typeCounts / typeBuckets / bucketActiveDays / typeBucketDays
//   buckets          -> 柱状（各时段次数）
//   bucketActiveDays ÷ activeDays -> 折线（打卡率），两者是两个互相独立的维度
//   type*            -> 供口径切换器使用，同一次响应里已含各类型数据，切换无需再请求
const historySummary = ref({
  total: 0,
  activeDays: 0,
  buckets: [],
  typeCounts: {},
  typeBuckets: {},
  bucketActiveDays: [],
  typeBucketDays: {}
})

/** 可切换的统计口径：全部 / 各类打卡 */
const CHART_SCOPES = [
  { key: 'all', label: '全部' },
  { key: 'early', label: '早起' },
  { key: 'sleep', label: '睡眠' },
  { key: 'meal', label: '用餐' },
  { key: 'exercise', label: '运动' }
]

/** 当前图表口径 */
const chartScope = ref('all')

/** 当前口径名称（图例与提示文案用） */
const chartScopeLabel = computed(
  () => (CHART_SCOPES.find(item => item.key === chartScope.value) || CHART_SCOPES[0]).label
)

/**
 * 柱状数据：当前口径下 12 个时段的次数
 *
 * @description 切换口径不重新请求接口——同一次响应里已带各类型的分布，前端换一份数据即可，切换是即时的。
 */
const chartBuckets = computed(() => {
  const summary = historySummary.value
  const allBuckets = Array.isArray(summary.buckets) ? summary.buckets : []
  if (chartScope.value === 'all') return allBuckets

  const typeCounts = (summary.typeBuckets || {})[chartScope.value]
  if (!Array.isArray(typeCounts)) return allBuckets.map(item => ({ label: item.label, count: 0 }))
  return allBuckets.map((item, index) => ({ label: item.label, count: Number(typeCounts[index]) || 0 }))
})

/**
 * 折线数据：当前口径下各时段的打卡率（%）
 *
 * @description 打卡率 = 该时段有打卡的天数 ÷ 区间内活跃天数。
 * 它回答的是"这个时段我有多坚持"，与柱状回答的"这个时段量有多大"互相独立，
 * 因此折线不是把柱状重画一遍。区间内没有任何打卡时返回空数组，图表退化为纯柱状图。
 */
const chartRate = computed(() => {
  const summary = historySummary.value
  const activeDays = Number(summary.activeDays) || 0
  if (!activeDays) return []

  const raw = chartScope.value === 'all'
    ? summary.bucketActiveDays
    : (summary.typeBucketDays || {})[chartScope.value]
  if (!Array.isArray(raw)) return []

  // 保留一位小数：百分比取整会丢失区分度（例如 8.3% 与 8.4%）
  return raw.map(days => Math.round((Number(days) || 0) / activeDays * 1000) / 10)
})

/** 切换图表口径（纯前端切换，不重新请求） */
const switchChartScope = (key) => {
  chartScope.value = key
}
/**
 * 请求序号：快速来回切换「近30天 / 全部」时，只接受最后一次请求的响应，
 * 否则先发出的慢响应可能后到达，把新范围的数据覆盖掉
 */
let historyRequestSeq = 0

// 时间分布数据
const timeDistribution = computed(() => {
  const distribution = {
    morning: [], // 0-6点
    midMorning: [], // 7-12点
    afternoon: [], // 13-18点
    evening: [] // 19-24点
  }
  
  // 处理早起打卡
  if (checkins.early && checkins.earlyTime) {
    const hour = parseInt(checkins.earlyTime.split(':')[0])
    if (hour >= 0 && hour < 7) {
      distribution.morning.push({ type: '早起打卡', time: checkins.earlyTime })
    } else if (hour >= 7 && hour < 13) {
      distribution.midMorning.push({ type: '早起打卡', time: checkins.earlyTime })
    } else if (hour >= 13 && hour < 19) {
      distribution.afternoon.push({ type: '早起打卡', time: checkins.earlyTime })
    } else {
      distribution.evening.push({ type: '早起打卡', time: checkins.earlyTime })
    }
  }
  
  // 处理睡眠打卡
  if (checkins.sleep && checkins.sleepTime) {
    const hour = parseInt(checkins.sleepTime.split(':')[0])
    if (hour >= 0 && hour < 7) {
      distribution.morning.push({ type: '睡眠打卡', time: checkins.sleepTime })
    } else if (hour >= 7 && hour < 13) {
      distribution.midMorning.push({ type: '睡眠打卡', time: checkins.sleepTime })
    } else if (hour >= 13 && hour < 19) {
      distribution.afternoon.push({ type: '睡眠打卡', time: checkins.sleepTime })
    } else {
      distribution.evening.push({ type: '睡眠打卡', time: checkins.sleepTime })
    }
  }
  
  // 处理用餐打卡
  for (const meal of checkins.meals) {
    if (meal.time) {
      const hour = parseInt(meal.time.split(':')[0])
      const type = `用餐打卡 (${meal.type})`
      if (hour >= 0 && hour < 7) {
        distribution.morning.push({ type, time: meal.time })
      } else if (hour >= 7 && hour < 13) {
        distribution.midMorning.push({ type, time: meal.time })
      } else if (hour >= 13 && hour < 19) {
        distribution.afternoon.push({ type, time: meal.time })
      } else {
        distribution.evening.push({ type, time: meal.time })
      }
    }
  }
  
  // 处理运动打卡
  for (const exercise of checkins.exercises) {
    if (exercise.time) {
      const hour = parseInt(exercise.time.split(':')[0])
      const type = `运动打卡 (${exercise.type})`
      if (hour >= 0 && hour < 7) {
        distribution.morning.push({ type, time: exercise.time })
      } else if (hour >= 7 && hour < 13) {
        distribution.midMorning.push({ type, time: exercise.time })
      } else if (hour >= 13 && hour < 19) {
        distribution.afternoon.push({ type, time: exercise.time })
      } else {
        distribution.evening.push({ type, time: exercise.time })
      }
    }
  }
  
  return distribution
})

// 智能互动文案
const distributionTagline = computed(() => {
  // 首屏加载期间不要下"今天还没有打卡"的结论
  if (!dataReady.value) {
    return '正在加载今日打卡数据…'
  }

  // 统计总打卡次数
  const totalCheckins = 
    (checkins.early ? 1 : 0) + 
    (checkins.sleep ? 1 : 0) + 
    checkins.meals.length + 
    checkins.exercises.length
  
  if (totalCheckins === 0) {
    return '今天还没有打卡，点上面的按钮记一条。'
  }
  
  // 早起打卡时间分析
  let earlyHour = null
  if (checkins.early && checkins.earlyTime) {
    earlyHour = parseInt(checkins.earlyTime.split(':')[0])
  }
  
  // 睡眠打卡时间分析
  let sleepHour = null
  if (checkins.sleep && checkins.sleepTime) {
    sleepHour = parseInt(checkins.sleepTime.split(':')[0])
  }
  
  // 用餐打卡分布分析
  const mealHours = checkins.meals.map(meal => {
    if (meal.time) {
      return parseInt(meal.time.split(':')[0])
    }
    return null
  }).filter(hour => hour !== null)
  
  // 运动打卡时间分析
  const exerciseHours = checkins.exercises.map(exercise => {
    if (exercise.time) {
      return parseInt(exercise.time.split(':')[0])
    }
    return null
  }).filter(hour => hour !== null)
  
  // 早起打卡集中在6-8点
  if (earlyHour >= 6 && earlyHour < 8) {
    return '6–8 点完成早起打卡。'
  }
  
  // 睡眠打卡在22点后
  if (sleepHour >= 22 || sleepHour < 1) {
    return '22 点后完成睡眠打卡。'
  }
  
  // 用餐打卡分布均匀
  if (mealHours.length >= 3) {
    const hasMorningMeal = mealHours.some(hour => hour >= 6 && hour < 12)
    const hasAfternoonMeal = mealHours.some(hour => hour >= 12 && hour < 18)
    const hasEveningMeal = mealHours.some(hour => hour >= 18 || hour < 1)
    
    if (hasMorningMeal && hasAfternoonMeal && hasEveningMeal) {
      return '早、中、晚三餐都已打卡。'
    }
  }
  
  // 运动集中在傍晚
  if (exerciseHours.length > 0) {
    const eveningExercises = exerciseHours.filter(hour => hour >= 17 && hour < 22)
    if (eveningExercises.length === exerciseHours.length) {
      return '17–22 点完成运动打卡。'
    }
  }
  
  // 整体打卡时间分散
  const allHours = []
  if (earlyHour !== null) allHours.push(earlyHour)
  if (sleepHour !== null) allHours.push(sleepHour)
  allHours.push(...mealHours)
  allHours.push(...exerciseHours)
  
  if (allHours.length >= 4) {
    const timeRanges = [
      allHours.filter(h => h >= 0 && h < 7).length > 0,
      allHours.filter(h => h >= 7 && h < 13).length > 0,
      allHours.filter(h => h >= 13 && h < 19).length > 0,
      allHours.filter(h => h >= 19 || h < 1).length > 0
    ]
    
    if (timeRanges.filter(Boolean).length >= 3) {
      return '打卡分散在 3 个以上时段。'
    }
  }
  
  // 打卡较少
  if (totalCheckins < 3) {
    return '轻松生活，慢慢来也很好，享受属于自己的时光～'
  }
  
  // 默认文案
  return '每一天的打卡都是对生活的热爱，继续保持这份热情！'
})

// 历史打卡智能文案（基于服务端聚合结果，不再依赖明细列表）
const historyTagline = computed(() => {
  // 首屏加载期间不要下"还没有记录"的结论：数据到达时文案会突变
  if (!dataReady.value) {
    return '正在加载历史数据…'
  }

  const totalCount = historySummary.value.total || 0

  if (totalCount === 0) {
    return '还没有历史打卡记录。'
  }

  // 有打卡记录的时段数量（12 个 2 小时桶）
  const coveredBuckets = chartBuckets.value.filter(bucket => Number(bucket.count) > 0).length

  if (coveredBuckets >= 8) {
    return `${totalCount} 次打卡遍布全天，生活节奏丰富而规律，继续保持！`
  }
  if (coveredBuckets >= 4) {
    return `${totalCount} 次打卡分布在 ${coveredBuckets} 个时段，节奏不错，继续加油！`
  }
  return `${totalCount} 次打卡记录着你的生活点滴，继续保持这份热情！`
})

/**
 * 拉取历史打卡时间分布汇总
 * @description 聚合在服务端完成（GROUP BY 小时），前端不再拉取明细列表；
 *              「近30天」与「全部」共用同一接口，仅日期区间不同
 */
const fetchHistorySummary = async () => {
  if (!userStore.getToken) return

  const seq = ++historyRequestSeq
  // 近30天 = 含今天在内的 30 天
  const params = historyRange.value === '30d'
    ? { startDate: localDateOffset(-29), endDate: toLocalDate() }
    : {}

  try {
    const response = await get('/checkin/time-distribution/summary', params)
    // 已被更新的请求取代，丢弃本次结果
    if (seq !== historyRequestSeq) return
    if (response.code === 200 && response.data) {
      historySummary.value = response.data
    }
  } catch (error) {
    if (seq !== historyRequestSeq) return
    console.error('获取历史打卡时间分布失败:', error)
  }
}

/**
 * 切换统计范围
 * @param {'30d'|'all'} range 统计范围
 */
const switchHistoryRange = (range) => {
  if (historyRange.value === range) return
  historyRange.value = range
  fetchHistorySummary()
}

/** 跳转到完整历史打卡记录页 */
const goCheckinHistory = () => {
  uni.navigateTo({ url: '/pages/checkin-history/checkin-history' })
}

/**
 * 获取当前日期字符串
 * @description 使用本地时区获取YYYY-MM-DD格式的日期字符串
 * @returns {string} 日期字符串
 */
const getCurrentDate = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 获取今日打卡缓存键
 * @returns {string} 缓存键
 */
const getTodayCacheKey = () => {
  const date = getCurrentDate()
  return `daily_checkins_${date}`
}

/**
 * 执行打卡操作
 * @description 根据打卡类型调用API保存打卡记录，更新本地状态和缓存
 * @param {string} type - 打卡类型（early/sleep/meal/exercise）
 */
/** 上次提交时间：连点「打卡」时防止重复写入 */
let lastSubmitAt = 0

const checkin = async (type) => {
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }

  // 连点防护：800ms 内的重复点击直接忽略，避免重复写入打卡记录
  if (Date.now() - lastSubmitAt < 800) return
  lastSubmitAt = Date.now()

  const now = new Date()
  const time = now.getHours().toString().padStart(2, '0') + ':' + now.getMinutes().toString().padStart(2, '0')

  const checkinData = {
    type: type,
    // 本地时间；后端保存时也会用服务端时间覆盖，这里主要是保持口径一致
    checkinTime: toLocalDateTime(now)
  }

  if (type === 'meal') {
    checkinData.mealType = selectedMealType.value
  } else if (type === 'exercise') {
    checkinData.exerciseType = selectedExerciseType.value
  }

  try {
    loading.value = true

    const response = await post('/checkin', checkinData, {
    })

    if (response.code === 200) {
      switch (type) {
        case 'early':
          checkins.early = true
          checkins.earlyTime = time
          break
        case 'sleep':
          checkins.sleep = true
          checkins.sleepTime = time
          break
        case 'meal':
          checkins.meals.push({
            time: time,
            type: selectedMealType.value
          })
          break
        case 'exercise':
          checkins.exercises.push({
            time: time,
            type: selectedExerciseType.value
          })
          break
      }

      const date = getCurrentDate()
      const cacheKey = getTodayCacheKey()
      const checkinList = []
      if (checkins.early) {
        checkinList.push({ type: 'early', checkinTime: toLocalDateTime() })
      }
      if (checkins.sleep) {
        checkinList.push({ type: 'sleep', checkinTime: toLocalDateTime() })
      }
      for (const meal of checkins.meals) {
        checkinList.push({ type: 'meal', checkinTime: toLocalDateTime(), mealType: meal.type })
      }
      for (const exercise of checkins.exercises) {
        checkinList.push({ type: 'exercise', checkinTime: toLocalDateTime(), exerciseType: exercise.type })
      }
      setCache(cacheKey, {
        data: checkinList,
        date: date
      })
      removeCache('checkinStats_' + date)
      // 同步失效统计卡片实际使用的缓存键（两者此前不一致，导致打卡后统计不更新）
      removeCache('checkin_stats')

      await fetchCheckinStats()

      // 打卡成功后重置添加状态
      if (type === 'meal') {
        showAddMealType.value = false
        selectedMealType.value = ''
      } else if (type === 'exercise') {
        showAddExerciseType.value = false
        selectedExerciseType.value = ''
      }

      uni.showToast({ title: '打卡成功', icon: 'success' })
    } else if (response.code === 403) {
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
    } else {
      uni.showToast({ title: '打卡失败', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: error.message || '网络错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 选择用餐类型
 */
const selectMealType = (type) => {
  selectedMealType.value = type
}

/**
 * 显示自定义用餐类型弹窗
 */
const showCustomMeal = () => {
  isCustomMealDialogOpen.value = true
}

/**
 * 关闭自定义用餐类型弹窗
 */
const closeCustomMealDialog = () => {
  isCustomMealDialogOpen.value = false
  customMealType.value = ''
}

/**
 * 确认自定义用餐类型
 */
const confirmCustomMeal = () => {
  if (customMealType.value.trim()) {
    selectedMealType.value = customMealType.value
    closeCustomMealDialog()
  } else {
    uni.showToast({ title: '请输入用餐类型', icon: 'none' })
  }
}

/**
 * 选择运动类型
 */
const selectExerciseType = (type) => {
  selectedExerciseType.value = type
}

/**
 * 显示自定义运动类型弹窗
 */
const showCustomExercise = () => {
  isCustomExerciseDialogOpen.value = true
}

/**
 * 关闭自定义运动类型弹窗
 */
const closeCustomExerciseDialog = () => {
  isCustomExerciseDialogOpen.value = false
  customExerciseType.value = ''
}

/**
 * 确认自定义运动类型
 */
const confirmCustomExercise = () => {
  if (customExerciseType.value.trim()) {
    selectedExerciseType.value = customExerciseType.value
    closeCustomExerciseDialog()
  } else {
    uni.showToast({ title: '请输入运动类型', icon: 'none' })
  }
}

/**
 * 取消打卡
 * @description 调用API删除指定类型的打卡记录，更新本地状态
 * @param {string} type - 打卡类型
 */
const cancelCheckin = async (type) => {
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }

  try {
    loading.value = true

    const date = getCurrentDate()
    const response = await del(`/checkin?type=${type}&date=${date}`, {}, {
    })

    if (response.code === 200) {
      switch (type) {
        case 'early':
          checkins.early = false
          checkins.earlyTime = ''
          break
        case 'sleep':
          checkins.sleep = false
          checkins.sleepTime = ''
          break
        case 'meal':
          checkins.meals = []
          break
        case 'exercise':
          checkins.exercises = []
          break
      }

      const date = getCurrentDate()
      const cacheKey = getTodayCacheKey()
      const checkinList = []
      if (checkins.early) {
        checkinList.push({ type: 'early', checkinTime: toLocalDateTime() })
      }
      if (checkins.sleep) {
        checkinList.push({ type: 'sleep', checkinTime: toLocalDateTime() })
      }
      for (const meal of checkins.meals) {
        checkinList.push({ type: 'meal', checkinTime: toLocalDateTime(), mealType: meal.type })
      }
      for (const exercise of checkins.exercises) {
        checkinList.push({ type: 'exercise', checkinTime: toLocalDateTime(), exerciseType: exercise.type })
      }
      setCache(cacheKey, {
        data: checkinList,
        date: date
      })

      await fetchCheckinStats()

      uni.showToast({ title: '取消打卡成功', icon: 'success' })
    } else if (response.statusCode === 403) {
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
    } else {
      uni.showToast({ title: '取消打卡失败', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: error.message || '网络错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 重置用餐打卡
 */
const resetMealCheckin = () => {
  // 显示添加新用餐打卡的类型选择器
  showAddMealType.value = true
  selectedMealType.value = ''
}

// 重置运动打卡
const resetExerciseCheckin = () => {
  // 显示添加新运动打卡的类型选择器
  showAddExerciseType.value = true
  selectedExerciseType.value = ''
}



/**
 * 处理打卡类型选择变化
 * @param {Object} e - picker事件对象
 */
// 取消特定的用餐打卡
const cancelMealCheckin = async (index) => {
  // 检查是否登录
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }
  
  try {
    loading.value = true
    
    const date = getCurrentDate()
    /*
     * 带上这一条记录的时间，后端只删它。
     * 不带的话后端会删除"今天该类型的全部记录"——用户有两条用餐记录时，
     * 点其中一条的 × 会把另一条也删掉（数据静默丢失）。
     * 取不到时间则不传，自动退回旧行为。
     */
    const targetMeal = (checkins.meals && checkins.meals[index]) || {}
    const mealTime = targetMeal.checkinTime
      ? `&time=${encodeURIComponent(targetMeal.checkinTime)}`
      : ''
    const response = await del(`/checkin?type=meal&date=${date}${mealTime}`, {}, {
    })
    
    if (response.code === 200) {
      // 更新本地状态
      checkins.meals.splice(index, 1)

      // 更新缓存
      const date = getCurrentDate()
      const cacheKey = getTodayCacheKey()
      const checkinList = []
      if (checkins.early) {
        checkinList.push({ type: 'early', checkinTime: toLocalDateTime() })
      }
      if (checkins.sleep) {
        checkinList.push({ type: 'sleep', checkinTime: toLocalDateTime() })
      }
      for (const meal of checkins.meals) {
        checkinList.push({ type: 'meal', checkinTime: toLocalDateTime(), mealType: meal.type })
      }
      for (const exercise of checkins.exercises) {
        checkinList.push({ type: 'exercise', checkinTime: toLocalDateTime(), exerciseType: exercise.type })
      }
      setCache(cacheKey, {
        data: checkinList,
        date: date
      })

      // 重新获取统计数据
      await fetchCheckinStats()

      uni.showToast({ title: '取消打卡成功', icon: 'success' })
    } else if (response.code === 403) {
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
    } else {
      uni.showToast({ title: '取消打卡失败', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: error.message || '网络错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 取消特定的运动打卡
const cancelExerciseCheckin = async (index) => {
  // 检查是否登录
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }
  
  try {
    loading.value = true
    
    const date = getCurrentDate()
    // 同上：只取消这一条，而不是"今天运动打卡全部取消"
    const targetExercise = (checkins.exercises && checkins.exercises[index]) || {}
    const exerciseTime = targetExercise.checkinTime
      ? `&time=${encodeURIComponent(targetExercise.checkinTime)}`
      : ''
    const response = await del(`/checkin?type=exercise&date=${date}${exerciseTime}`, {}, {
    })
    
    if (response.code === 200) {
      // 更新本地状态
      checkins.exercises.splice(index, 1)

      // 更新缓存
      const date = getCurrentDate()
      const cacheKey = getTodayCacheKey()
      const checkinList = []
      if (checkins.early) {
        checkinList.push({ type: 'early', checkinTime: toLocalDateTime() })
      }
      if (checkins.sleep) {
        checkinList.push({ type: 'sleep', checkinTime: toLocalDateTime() })
      }
      for (const meal of checkins.meals) {
        checkinList.push({ type: 'meal', checkinTime: toLocalDateTime(), mealType: meal.type })
      }
      for (const exercise of checkins.exercises) {
        checkinList.push({ type: 'exercise', checkinTime: toLocalDateTime(), exerciseType: exercise.type })
      }
      setCache(cacheKey, {
        data: checkinList,
        date: date
      })

      // 重新获取统计数据
      await fetchCheckinStats()

      uni.showToast({ title: '取消打卡成功', icon: 'success' })
    } else if (response.code === 403) {
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
    } else {
      uni.showToast({ title: '取消打卡失败', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: error.message || '网络错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 获取打卡记录
const fetchCheckins = async () => {
  const date = getCurrentDate()
  const cacheKey = getTodayCacheKey()

  if (!userStore.getToken) {
    updateCheckinsFromList([])
    return
  }

  try {
    const response = await get(`/checkin/by-date?date=${date}`, {}, {
    })

    if (response.code === 200) {
      const checkinData = response.data || []
      updateCheckinsFromList(checkinData)
      setCache(cacheKey, checkinData)
    } else {
      updateCheckinsFromList([])
    }
  } catch (error) {
    console.error('获取打卡数据失败:', error)
    updateCheckinsFromList([])
  }
}

const updateCheckinsFromList = (checkinList) => {
  checkins.early = false
  checkins.earlyTime = ''
  checkins.sleep = false
  checkins.sleepTime = ''
  checkins.meals = []
  checkins.exercises = []

  if (!checkinList || !Array.isArray(checkinList)) {
    return
  }

  for (const checkin of checkinList) {
    const checkinTime = new Date(checkin.checkinTime)
    const time = checkinTime.getHours().toString().padStart(2, '0') + ':' + checkinTime.getMinutes().toString().padStart(2, '0')

    switch (checkin.type) {
      case 'early':
        checkins.early = true
        checkins.earlyTime = time
        break
      case 'sleep':
        checkins.sleep = true
        checkins.sleepTime = time
        break
      case 'meal':
        checkins.meals.push({
          time: time,
          type: checkin.mealType || ''
        })
        break
      case 'exercise':
        checkins.exercises.push({
          time: time,
          type: checkin.exerciseType || ''
        })
        break
    }
  }
}

// 获取统计数据
const fetchCheckinStats = async () => {
  // 检查是否登录
  if (!userStore.getToken) {
    return
  }

  const cacheKey = 'checkin_stats'
  /*
   * 读取前先失效。
   *
   * 统计卡片依赖"打卡/取消打卡"刚刚产生的结果，而各写路径分散在多处
   * （打卡、再次打卡、取消、按类型取消……），此前只在其中一处清了缓存，
   * 其余路径会拿到最长 5 分钟的旧统计（打卡率、次数与真实数据不符）。
   * 与其在每处写完后逐一补失效（新增写路径时又容易漏），
   * 不如让这个幂等的小接口每次读取都以服务端为准 —— 代价是每次进入页面多一次
   * 轻量请求，换取"统计永远与刚做的操作一致"，这笔账划得来。
   */
  removeCache(cacheKey)

  const fetchFn = async () => {
    const response = await get('/checkin/stats', {}, {
    })
    
    if (response.code === 200) {
      return response.data || {}
    } else if (response.code === 403) {
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
    }
    return {}
  }

  try {
    const statsData = await fetchWithCache(cacheKey, fetchFn)
    stats.earlyRate = statsData.earlyRate || 0
    stats.sleepRate = statsData.sleepRate || 0
    stats.mealCount = statsData.mealCount || 0
    stats.exerciseRate = statsData.exerciseRate || 0
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 生命周期
onMounted(async () => {
  // 检查是否登录
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }

  // 初始化数据：三个请求并发，全部结束后才撤下加载态
  try {
    await Promise.all([fetchCheckins(), fetchCheckinStats(), fetchHistorySummary()])
  } finally {
    dataReady.value = true
  }
})
</script>

<style scoped>
.checkin-container {
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
  box-sizing: border-box;
}

/* 首屏加载占位：保持与卡片/按钮相近的高度，避免加载完成时产生跳变 */
.checkin-loading {
  padding: 12px 0;
  text-align: center;
  font-size: var(--fs-body);
  color: var(--text-muted, #999999);
}

/* 打卡卡片 */
.checkin-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.checkin-card {
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-md);
  transition: var(--transition-interactive);
}

.checkin-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.checkin-card.checked {
  background-color: var(--surface-color, #F2EEE8);
  border: 1px solid rgba(194, 151, 127, 0.3);
}

.checkin-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  position: relative;
}

.checkin-status {
  position: absolute;
  top: 0;
  right: 0;
  background-color: #C2977F;
  color: white;
  font-size: var(--fs-xs);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.checkin-icon {
  font-size: var(--fs-2xl);
  margin-right: 12px;
  color: var(--text-secondary, #666666);
  transition: color 0.3s ease;
}

.checkin-icon.checked {
  color: #C2977F;
}

/* 图标样式已迁到全局 src/styles/icons.css（自绘 base64 PNG） */
.checkin-title {
  font-size: var(--fs-md);
  font-weight: 500;
  color: var(--text-color, #333333);
}

.checkin-time {
  margin-bottom: 16px;
  font-size: var(--fs-body);
  color: var(--text-secondary, #666666);
}

.meal-type-text, .exercise-type-text {
  display: block;
  font-size: var(--fs-xs);
  color: var(--text-muted, #999999);
  margin-top: 4px;
}

.checkin-btn {
  width: 100%;
  padding: 12px;
  background-color: #94A7C8;
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--fs-body);
  font-weight: 500;
  transition: var(--transition-interactive);
  cursor: pointer;
}

.checkin-btn:hover:not(:disabled) {
  background-color: #7A8BA8;
}

.checkin-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.checkin-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.cancel-btn {
  background-color: #f0f0f0;
  color: var(--text-secondary, #666666);
}

.cancel-btn:hover {
  background-color: #e0e0e0;
  color: var(--text-color, #333333);
}

.checkin-records {
  margin-bottom: 16px;
}

.checkin-record {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 12px;
  background-color: rgba(194, 151, 127, 0.05);
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
}

.checkin-record-info {
  flex: 1;
  /* 横向展示"时间 + 事件"（此前是上下两行）：
     时间有 min-width 撑开对齐，事件紧随其后 */
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12px;
}

.checkin-record .checkin-time {
  font-size: var(--fs-body);
  color: var(--text-color, #333333);
  min-width: 50px;
  /* 旧上下堆叠布局遗留的 16px 下边距：改为横向排列后，
     在 align-items: center 下它会把时间整体顶高 8px（半倍边距），与事件错位 */
  margin-bottom: 0;
}

.checkin-record .meal-type-text,
.checkin-record .exercise-type-text {
  font-size: var(--fs-xs);
  color: var(--text-secondary, #666666);
  margin-top: 0;
}

.checkin-record-cancel {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-circle);
  border: 1px solid #e0e0e0;
  background-color: var(--surface-strong, #FFFFFF);
  color: var(--text-muted, #999999);
  font-size: var(--fs-lg);
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition-interactive);
}

.checkin-record-cancel:hover {
  background-color: var(--danger-color, #B5544A);
  border-color: var(--danger-color, #B5544A);
  color: white;
}

.checked-btn {
  background-color: var(--primary-color, #C2977F);
}

/* 用餐类型选择 */
.meal-types, .exercise-types {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.meal-type, .exercise-type {
  padding: 8px 12px;
  background-color: var(--surface-strong, #FFFFFF);
  border: 1px solid var(--sidebar-border, var(--border-color, var(--border-color, #D8C8BE)));
  border-radius: var(--radius-sm);
  text-align: center;
  cursor: pointer;
  transition: var(--transition-interactive);
  font-size: var(--fs-body);
  color: var(--text-secondary, #666666);
}

.meal-type:hover, .exercise-type:hover {
  border-color: var(--primary-color, #C2977F);
  color: var(--primary-color, #C2977F);
}

.meal-type.selected, .exercise-type.selected {
  border-color: var(--primary-color, #C2977F);
  color: var(--primary-color, #C2977F);
  background-color: rgba(194, 151, 127, 0.1);
  font-weight: 500;
}

.meal-type.custom, .exercise-type.custom {
  border-style: dashed;
}

/* 数据统计 */
.stats-section {
  background-color: var(--sidebar-bg, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-md);
}

/* 历史打卡时间分布 */
.history-distribution-section {
  background-color: var(--sidebar-bg, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-md);
  margin-top: 20px;
}

.section-title {
  font-size: var(--fs-md);
  font-weight: 500;
  color: var(--text-color, #333333);
  margin-bottom: 16px;
  border-bottom: 1px solid var(--secondary-color, #94A7C8);
  padding-bottom: 8px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-card {
  background-color: var(--surface-strong, #FFFFFF);
  border-radius: var(--radius-sm);
  padding: 16px;
  box-shadow: var(--shadow-sm);
}

.stat-title {
  font-size: var(--fs-body);
  color: var(--text-secondary, #666666);
  margin-bottom: 8px;
}

.stat-value {
  font-size: var(--fs-xl);
  font-weight: 600;
  color: var(--primary-color, #C2977F);
  margin-bottom: 8px;
}

.stat-chart {
  height: 8px;
  background-color: #F0F0F0;
  border-radius: var(--radius-xs);
  overflow: hidden;
}

.chart-bar {
  height: 100%;
  background-color: var(--primary-color, #C2977F);
  border-radius: var(--radius-xs);
  transition: width 0.5s ease;
}

/* 时间分布模块样式 */
.time-distribution-section {
  background-color: var(--sidebar-bg, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-md);
  margin-bottom: 32px;
}

.distribution-card {
  background-color: var(--surface-strong, #FFFFFF)FF;
  border-radius: var(--radius-sm);
  padding: 16px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #E8D5C4;
}

.distribution-tagline {
  font-size: var(--fs-body);
  color: var(--primary-color, #C2977F);
  text-align: center;
  margin-bottom: 16px;
  line-height: 1.4;
  font-weight: 500;
  padding: 12px 12px;
  background-color: rgba(194, 151, 127, 0.1);
  border-radius: var(--radius-sm);
}

/* 历史分布卡片头：左=统计范围切换，右=总量 + 查看全部入口 */
.chart-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap; /* 窄屏自动换行，不撑宽卡片 */
  gap: 12px;
  margin-bottom: 12px;
}

.range-switch {
  display: flex;
  padding: 2px;
  background-color: rgba(194, 151, 127, 0.12);
  border-radius: var(--radius-lg);
}

.range-item {
  padding: 4px 16px;
  font-size: var(--fs-xs);
  color: #8a7a6d;
  border-radius: var(--radius-pill);
  cursor: pointer;
  white-space: nowrap;
  transition: var(--transition-interactive);
}

.range-item.active {
  color: #ffffff;
  background-color: var(--primary-color, #C2977F);
}

/* 图表口径切换（全部 / 早起 / 睡眠 / 用餐 / 运动） */
.scope-switch {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.scope-item {
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-color-light, var(--border-color-light, #E8E4DE));
  background-color: var(--surface-strong, #FFFFFF);
  color: var(--text-secondary, #666666);
  font-size: var(--fs-xs);
  white-space: nowrap;
  cursor: pointer;
  transition: var(--transition-interactive);
}

.scope-item.active {
  color: #ffffff;
  border-color: var(--primary-color, #C2977F);
  background-color: var(--primary-color, #C2977F);
}

.chart-head-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chart-total {
  font-size: var(--fs-xs);
  color: var(--text-muted, #999999);
}

.view-all {
  font-size: var(--fs-xs);
  color: var(--primary-color, #C2977F);
  cursor: pointer;
  white-space: nowrap;
}

/* 历史卡片里文案在图表下方，作为最后一个元素不需要下外边距 */
.history-distribution-section .distribution-tagline:last-child {
  margin-top: 12px;
  margin-bottom: 0;
}

.time-slots {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.time-slot {
  border: 1px solid #E8D5C4;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background-color: var(--surface-strong, #FFFFFF);
  box-shadow: var(--shadow-sm);
}

.time-slot-header {
  background-color: var(--surface-color, #F2EEE8);
  padding: 12px 16px;
  border-bottom: 1px solid #E8D5C4;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.time-slot-title {
  font-size: var(--fs-body);
  font-weight: 600;
  color: var(--text-color, #333333);
}

.time-slot-content {
  padding: 16px;
}

.checkin-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.checkin-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: var(--surface-strong, #FFFFFF)FF;
  border-radius: var(--radius-xs);
  border: 1px solid #E8D5C4;
  font-size: var(--fs-body);
}

.checkin-type {
  color: var(--text-color, #333333);
  font-weight: 500;
  flex: 1;
}

.checkin-time {
  color: var(--text-secondary, #666666);
  font-size: var(--fs-xs);
  white-space: nowrap;
  margin-left: 12px;
}

.empty-checkin {
  text-align: center;
  color: var(--text-muted, #999999);
  font-size: var(--fs-body);
  padding: 12px 0;
  font-style: italic;
}

/* 类型分组样式 */
.type-group {
  margin-bottom: 12px;
}

.type-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background-color: rgba(194, 151, 127, 0.1);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background-color 0.3s ease;
  border: 1px solid #E8D5C4;
  box-shadow: var(--shadow-sm);
}

.type-group-header:hover {
  background-color: rgba(194, 151, 127, 0.2);
}

.type-name {
  font-weight: 500;
  color: var(--text-color, #333333);
}

.type-count {
  font-size: var(--fs-xs);
  color: #666;
  margin-left: 8px;
}

.expand-icon {
  font-size: var(--fs-xs);
  color: #666;
  transition: transform 0.3s ease;
}

.type-group-content {
  margin-top: 8px;
  margin-left: 12px;
  padding-left: 12px;
  border-left: 2px solid rgba(194, 151, 127, 0.3);
}

.more-checkins {
  margin-top: 8px;
  text-align: center;
}

.more-btn {
  background-color: var(--surface-color, #F2EEE8);
  border: 1px solid #C2977F;
  color: #C2977F;
  padding: 8px 12px;
  border-radius: var(--radius-xs);
  font-size: var(--fs-xs);
  cursor: pointer;
  transition: var(--transition-interactive);
}

.more-btn:hover {
  background-color: #C2977F;
  color: white;
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
  width: 90%;
  max-width: 400px;
  box-shadow: var(--shadow-xl);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h3 {
  font-size: var(--fs-md);
  font-weight: 500;
  color: var(--text-color, #333333);
}

.close-icon {
  font-size: var(--fs-xl);
  cursor: pointer;
  color: var(--text-muted, #999999);
}

.modal-body {
  padding: 20px;
}

.modal-body input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color, var(--border-color, #D8C8BE));
  border-radius: var(--radius-sm);
  font-size: var(--fs-body);
  color: var(--text-color, #333333);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  gap: 12px;
}

.modal-footer button {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  border-radius: var(--radius-xs);
  font-size: var(--fs-body);
  cursor: pointer;
  transition: var(--transition-interactive);
}

.cancel-btn {
  background-color: var(--surface-strong, #FFFFFF);
  color: var(--text-color, #333333);
}

.confirm-btn {
  background-color: #C2977F;
  color: white;
  border-color: #C2977F;
}

.confirm-btn:hover {
  background-color: #A8846B;
  border-color: #A8846B;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .checkin-cards {
    grid-template-columns: 1fr;
  }
  
  .stats-cards {
    grid-template-columns: 1fr;
  }
}

/* ==== 设计修订（覆盖规则，勿手改上面旧值） ==== */
/* 时间范围/统计口径胶囊同样过扁，纵向内边距提到 10px */
.range-item,
.scope-item {
  padding-top: 10px;
  padding-bottom: 10px;
}

</style>