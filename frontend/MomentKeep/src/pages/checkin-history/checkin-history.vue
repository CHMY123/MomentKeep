<template>
  <view class="history-page">
    <!-- 筛选卡片 -->
    <view class="filter-card">
      <view class="filter-row">
        <text class="filter-label">类型</text>
        <view class="chip-group">
          <view
            v-for="item in typeOptions"
            :key="item.value"
            class="chip"
            :class="{ active: filters.type === item.value }"
            @click="selectType(item.value)"
          >
            <text class="chip-text" :class="{ active: filters.type === item.value }">{{ item.label }}</text>
          </view>
        </view>
      </view>

      <view class="filter-row" v-if="subTypeOptions.length > 0">
        <text class="filter-label">细分</text>
        <view class="chip-group">
          <view
            class="chip"
            :class="{ active: !filters.subType }"
            @click="selectSubType('')"
          >
            <text class="chip-text" :class="{ active: !filters.subType }">全部</text>
          </view>
          <view
            v-for="item in subTypeOptions"
            :key="item"
            class="chip"
            :class="{ active: filters.subType === item }"
            @click="selectSubType(item)"
          >
            <text class="chip-text" :class="{ active: filters.subType === item }">{{ item }}</text>
          </view>
        </view>
      </view>

      <view class="filter-row">
        <text class="filter-label">时间</text>
        <view class="chip-group">
          <view
            v-for="item in rangeOptions"
            :key="item.value"
            class="chip"
            :class="{ active: activeQuickRange === item.value }"
            @click="selectQuickRange(item.value)"
          >
            <text class="chip-text" :class="{ active: activeQuickRange === item.value }">{{ item.label }}</text>
          </view>
        </view>
      </view>

      <view class="filter-row">
        <text class="filter-label">自定义</text>
        <view class="date-range">
          <picker
            mode="date"
            :value="filters.startDate"
            :end="filters.endDate || today"
            @change="onStartDateChange"
          >
            <view class="date-box">
              <text class="date-text" :class="{ placeholder: !filters.startDate }">
                {{ filters.startDate || '开始日期' }}
              </text>
            </view>
          </picker>
          <text class="date-sep">至</text>
          <picker
            mode="date"
            :value="filters.endDate"
            :start="filters.startDate"
            :end="today"
            @change="onEndDateChange"
          >
            <view class="date-box">
              <text class="date-text" :class="{ placeholder: !filters.endDate }">
                {{ filters.endDate || '结束日期' }}
              </text>
            </view>
          </picker>
        </view>
      </view>
    </view>

    <!-- 统计条 -->
    <view class="summary-bar">
      <text class="summary-text">共 {{ total }} 条记录</text>
      <text class="summary-reset" @click="resetFilters">重置筛选</text>
    </view>

    <!-- 记录列表：按日期分组，触底自动加载下一页 -->
    <scroll-view
      class="list-scroll"
      scroll-y
      :style="{ height: scrollHeight + 'px' }"
      @scrolltolower="loadMore"
    >
      <view v-if="loading && records.length === 0" class="state-block">
        <text class="state-text">加载中…</text>
      </view>

      <view v-else-if="records.length === 0" class="state-block">
        <text class="state-text">没有符合条件的打卡记录</text>
        <text class="state-hint">可以换个类型或调整时间范围再试试</text>
      </view>

      <view v-for="group in groups" :key="group.date" class="day-group">
        <view class="day-header">
          <text class="day-title">{{ group.dateLabel }}</text>
          <text class="day-count">{{ group.items.length }} 条</text>
        </view>
        <view class="day-body">
          <view v-for="item in group.items" :key="item.id" class="record-item">
            <view class="record-left">
              <text class="record-type">{{ formatTypeLabel(item) }}</text>
              <text v-if="item.note" class="record-note">{{ item.note }}</text>
            </view>
            <text class="record-time">{{ formatTime(item.checkinTime) }}</text>
          </view>
        </view>
      </view>

      <view v-if="records.length > 0" class="list-footer">
        <text class="footer-text">
          {{ loadingMore ? '加载中…' : (hasMore ? '上拉加载更多' : '没有更多了') }}
        </text>
      </view>
      <view class="list-bottom-space"></view>
    </scroll-view>
  </view>
</template>

<script setup>
/**
 * 历史打卡记录页
 *
 * @description 替代原先内嵌在「每日打卡」页的文字明细：
 * 1. 支持按打卡类型 / 细分类型 / 时间范围筛选，数据由后端分页接口返回，不再一次性拉全量；
 * 2. 按日期分组展示，触底自动加载下一页；
 * 3. 使用系统导航栏（自带返回按钮），因此本页不复用带侧边栏的 Layout。
 */
import { ref, reactive, computed, onMounted, nextTick, getCurrentInstance } from 'vue'
import { get } from '../../utils/request'
import { useUserStore } from '../../store/user'
import { toLocalDate, localDateOffset, formatTime, formatDate, formatDateWithWeek } from '../../utils/datetime'

/** 每页条数（后端上限 100） */
const PAGE_SIZE = 20

const TYPE_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: 'early', label: '早起' },
  { value: 'sleep', label: '睡眠' },
  { value: 'meal', label: '用餐' },
  { value: 'exercise', label: '运动' }
]

const MEAL_SUBTYPES = ['早餐', '午餐', '晚餐', '宵夜']
const EXERCISE_SUBTYPES = ['跑步', '健身', '瑜伽', '游泳']

const RANGE_OPTIONS = [
  { value: '7d', label: '近7天' },
  { value: '30d', label: '近30天' },
  { value: 'all', label: '全部' }
]

const TYPE_LABELS = {
  early: '早起打卡',
  sleep: '睡眠打卡',
  meal: '用餐打卡',
  exercise: '运动打卡'
}

const instance = getCurrentInstance()
const userStore = useUserStore()
const today = toLocalDate()

/**
 * 请求序号：快速切换筛选条件时，只接受最后一次请求的响应。
 * 否则先发出的慢响应可能后到达，把新筛选的结果覆盖掉。
 */
let requestSeq = 0

const typeOptions = TYPE_OPTIONS
const rangeOptions = RANGE_OPTIONS

const filters = reactive({
  type: 'all',
  subType: '',
  startDate: '',
  endDate: ''
})
const activeQuickRange = ref('30d')

const records = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)
const scrollHeight = ref(400)

/** 细分类型仅在选中「用餐 / 运动」时有意义 */
const subTypeOptions = computed(() => {
  if (filters.type === 'meal') return MEAL_SUBTYPES
  if (filters.type === 'exercise') return EXERCISE_SUBTYPES
  return []
})

/** 按日期分组（records 已按时间倒序，分组后顺序保持） */
const groups = computed(() => {
  const result = []
  const mapper = new Map()

  for (const item of records.value) {
    const date = formatDate(item.checkinTime) || '未知日期'
    if (!mapper.has(date)) {
      const group = {
        date,
        dateLabel: formatDateWithWeek(item.checkinTime) || date,
        items: []
      }
      mapper.set(date, group)
      result.push(group)
    }
    mapper.get(date).items.push(item)
  }

  return result
})

/** 列表类型文案 */
const formatTypeLabel = item => {
  const base = TYPE_LABELS[item.type] || '打卡'
  if (item.type === 'meal' && item.mealType) return `${base} · ${item.mealType}`
  if (item.type === 'exercise' && item.exerciseType) return `${base} · ${item.exerciseType}`
  return base
}

/** 构造查询参数（空值一律不传，避免后端把空串当成筛选条件） */
const buildParams = targetPage => {
  const params = { page: targetPage, size: PAGE_SIZE }
  if (filters.startDate) params.startDate = filters.startDate
  if (filters.endDate) params.endDate = filters.endDate
  if (filters.type && filters.type !== 'all') params.type = filters.type
  if (filters.subType) params.subType = filters.subType
  return params
}

/**
 * 拉取记录
 * @param {boolean} reset true=重置到第一页（筛选变化时），false=追加下一页
 */
const fetchRecords = async (reset = true) => {
  const seq = ++requestSeq

  if (reset) {
    page.value = 1
    records.value = []
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const response = await get('/checkin/history/page', buildParams(page.value))
    // 已被更新的请求取代，丢弃本次结果（避免旧筛选覆盖新筛选）
    if (seq !== requestSeq) return
    if (response.code === 200 && response.data) {
      const list = response.data.list || []
      records.value = reset ? list : records.value.concat(list)
      total.value = response.data.total || 0
      hasMore.value = records.value.length < total.value
    }
  } catch (error) {
    if (seq !== requestSeq) return
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    if (seq === requestSeq) {
      loading.value = false
      loadingMore.value = false
    }
  }
}

/** 触底加载下一页 */
const loadMore = () => {
  if (!hasMore.value || loading.value || loadingMore.value) return
  page.value += 1
  fetchRecords(false)
}

/** 按可用的窗口高度计算列表滚动区高度（筛选卡高度会随细分类型出现而变化） */
const updateScrollHeight = () => {
  nextTick(() => {
    const query = uni.createSelectorQuery().in(instance.proxy)
    query.select('.filter-card').boundingClientRect()
    query.select('.summary-bar').boundingClientRect()
    query.exec(res => {
      let windowHeight = 600
      try {
        windowHeight = uni.getSystemInfoSync().windowHeight || 600
      } catch (e) {
        // 取不到系统信息时用默认值，保证列表仍可滚动
      }
      const filterHeight = res && res[0] ? res[0].height : 0
      const summaryHeight = res && res[1] ? res[1].height : 0
      scrollHeight.value = Math.max(240, Math.floor(windowHeight - filterHeight - summaryHeight - 20))
    })
  })
}

/** 快捷时间范围 */
const applyQuickRange = value => {
  activeQuickRange.value = value
  if (value === '7d') {
    filters.startDate = localDateOffset(-6)
    filters.endDate = today
  } else if (value === '30d') {
    filters.startDate = localDateOffset(-29)
    filters.endDate = today
  } else {
    filters.startDate = ''
    filters.endDate = ''
  }
}

const selectQuickRange = value => {
  applyQuickRange(value)
  fetchRecords()
}

const selectType = value => {
  filters.type = value
  // 切换类型后原来的子类型筛选不再有意义
  filters.subType = ''
  updateScrollHeight()
  fetchRecords()
}

const selectSubType = value => {
  filters.subType = value
  // 子类型行本身不改变显隐，但 chips 可能换行导致高度变化，这里一并重算
  updateScrollHeight()
  fetchRecords()
}

const onStartDateChange = event => {
  filters.startDate = event.detail.value
  activeQuickRange.value = ''
  fetchRecords()
}

const onEndDateChange = event => {
  filters.endDate = event.detail.value
  activeQuickRange.value = ''
  fetchRecords()
}

/** 恢复默认筛选（近30天 + 全部类型） */
const resetFilters = () => {
  filters.type = 'all'
  filters.subType = ''
  applyQuickRange('30d')
  updateScrollHeight()
  fetchRecords()
}

onMounted(() => {
  // 本页可被直接打开（pages.json 里注册过），必须先校验登录态，
  // 否则会先发一次注定 401 的请求，靠 request 工具被动跳转、页面闪现空态
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/login/login' })
    }, 800)
    return
  }

  applyQuickRange('30d')
  updateScrollHeight()
  fetchRecords()
})
</script>

<style scoped>
.history-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  box-sizing: border-box;
  padding: 12px;
  background-color: #f8f6f2;
}

/* 筛选卡：与全站卡片风格统一 */
.filter-card {
  background-color: #f2eee8;
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.filter-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
}

.filter-row:last-child {
  margin-bottom: 0;
}

.filter-label {
  flex-shrink: 0;
  width: 44px;
  padding-top: 5px;
  font-size: 12px;
  color: #888888;
}

.chip-group {
  display: flex;
  flex-wrap: wrap;
  flex: 1;
  gap: 8px;
}

.chip {
  padding: 5px 12px;
  background-color: #ffffff;
  border: 1px solid #d8c8be;
  border-radius: 14px;
}

.chip.active {
  background-color: #c2977f;
  border-color: #c2977f;
}

.chip-text {
  font-size: 12px;
  color: #666666;
}

.chip-text.active {
  color: #ffffff;
}

.date-range {
  display: flex;
  align-items: center;
  flex: 1;
  gap: 8px;
}

.date-box {
  flex: 1;
  padding: 5px 10px;
  background-color: #ffffff;
  border: 1px solid #d8c8be;
  border-radius: 8px;
  text-align: center;
}

.date-text {
  font-size: 12px;
  color: #333333;
}

.date-text.placeholder {
  color: #aaaaaa;
}

.date-sep {
  font-size: 12px;
  color: #999999;
}

/* 统计条 */
.summary-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 4px;
}

.summary-text {
  font-size: 12px;
  color: #666666;
}

.summary-reset {
  font-size: 12px;
  color: #c2977f;
}

/* 列表 */
.list-scroll {
  width: 100%;
}

.state-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.state-text {
  font-size: 14px;
  color: #999999;
}

.state-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #bbbbbb;
}

.day-group {
  margin-bottom: 12px;
}

.day-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 4px;
}

.day-title {
  font-size: 13px;
  font-weight: 500;
  color: #c2977f;
}

.day-count {
  font-size: 11px;
  color: #aaaaaa;
}

.day-body {
  background-color: #f2eee8;
  border-radius: 12px;
  padding: 6px 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.record-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #e8e4de;
}

.record-item:last-child {
  border-bottom: none;
}

.record-left {
  display: flex;
  flex-direction: column;
  flex: 1;
  margin-right: 10px;
}

.record-type {
  font-size: 14px;
  color: #333333;
}

.record-note {
  margin-top: 4px;
  font-size: 12px;
  color: #999999;
}

.record-time {
  flex-shrink: 0;
  font-size: 13px;
  color: #888888;
}

.list-footer {
  padding: 12px 0;
  text-align: center;
}

.footer-text {
  font-size: 12px;
  color: #aaaaaa;
}

/* 给底部留出安全区，避免最后一条被系统手势条遮挡 */
.list-bottom-space {
  height: 24px;
}
</style>
