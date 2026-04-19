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
            v-else
            class="checkin-btn" 
            @click="checkin('early')"
          >
            打卡
          </button>
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
            v-else
            class="checkin-btn" 
            @click="checkin('sleep')"
          >
            打卡
          </button>
        </div>
        
        <!-- 用餐打卡 -->
        <div class="checkin-card" :class="{ 'checked': checkins.meals.length > 0 }">
          <div class="checkin-header">
            <div class="checkin-icon icon-restaurant" :class="{ 'checked': checkins.meals.length > 0 }"></div>
            <div class="checkin-title">用餐打卡</div>
            <div class="checkin-status" v-if="checkins.meals.length > 0">已打卡 {{ checkins.meals.length }} 次</div>
          </div>
          <div class="meal-types" v-if="checkins.meals.length === 0">
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
            <div v-for="(meal, index) in checkins.meals" :key="index" class="checkin-record">
              <div class="checkin-record-info">
                <div class="checkin-time">{{ meal.time }}</div>
                <div class="meal-type-text">{{ meal.type }}</div>
              </div>
              <button class="checkin-record-cancel" @click="cancelMealCheckin(index)">
                ×
              </button>
            </div>
          </div>
          <div class="checkin-actions" v-if="checkins.meals.length > 0">
            <button class="checkin-btn" @click="resetMealCheckin">
              再次打卡
            </button>
          </div>
          <button 
            v-else
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
          <div class="exercise-types" v-if="checkins.exercises.length === 0">
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
            <div v-for="(exercise, index) in checkins.exercises" :key="index" class="checkin-record">
              <div class="checkin-record-info">
                <div class="checkin-time">{{ exercise.time }}</div>
                <div class="exercise-type-text">{{ exercise.type }}</div>
              </div>
              <button class="checkin-record-cancel" @click="cancelExerciseCheckin(index)">
                ×
              </button>
            </div>
          </div>
          <div class="checkin-actions" v-if="checkins.exercises.length > 0">
            <button class="checkin-btn" @click="resetExerciseCheckin">
              再次打卡
            </button>
          </div>
          <button 
            v-else
            class="checkin-btn" 
            @click="checkin('exercise')"
            :disabled="!selectedExerciseType"
          >
            打卡
          </button>
        </div>
      </div>
      
      <!-- 数据可视化 -->
      <div class="stats-section">
        <div class="section-title">打卡数据统计</div>
        <div class="stats-cards">
          <div class="stat-card">
            <div class="stat-title">早起打卡</div>
            <div class="stat-value">{{ stats.earlyRate }}%</div>
            <div class="stat-chart">
              <div class="chart-bar" :style="{ width: stats.earlyRate + '%' }"></div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-title">睡眠打卡</div>
            <div class="stat-value">{{ stats.sleepRate }}%</div>
            <div class="stat-chart">
              <div class="chart-bar" :style="{ width: stats.sleepRate + '%' }"></div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-title">用餐打卡</div>
            <div class="stat-value">{{ stats.mealCount }}次/天</div>
            <div class="stat-chart">
              <div class="chart-bar" :style="{ width: (stats.mealCount / 4) * 100 + '%' }"></div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-title">运动打卡</div>
            <div class="stat-value">{{ stats.exerciseRate }}%</div>
            <div class="stat-chart">
              <div class="chart-bar" :style="{ width: stats.exerciseRate + '%' }"></div>
            </div>
          </div>
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
import { useUserStore } from '../../store/user'
import { get, post, put, del } from '../../utils/request'
import { useCache } from '../../utils/cache'

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

// 运动类型选项
const exerciseTypes = ['跑步', '健身', '瑜伽', '游泳']
const selectedExerciseType = ref('')
const customExerciseType = ref('')
const isCustomExerciseDialogOpen = ref(false)

// 统计数据
const stats = reactive({
  earlyRate: 0,
  sleepRate: 0,
  mealCount: 0,
  exerciseRate: 0
})

// 加载状态
const loading = ref(false)

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
const checkin = async (type) => {
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }

  const now = new Date()
  const time = now.getHours().toString().padStart(2, '0') + ':' + now.getMinutes().toString().padStart(2, '0')

  const checkinData = {
    type: type,
    checkinTime: now.toISOString()
  }

  if (type === 'meal') {
    checkinData.mealType = selectedMealType.value
  } else if (type === 'exercise') {
    checkinData.exerciseType = selectedExerciseType.value
  }

  try {
    loading.value = true

    const response = await post('/checkin', checkinData, {
      'Authorization': `Bearer ${userStore.getToken}`
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
        checkinList.push({ type: 'early', checkinTime: new Date().toISOString() })
      }
      if (checkins.sleep) {
        checkinList.push({ type: 'sleep', checkinTime: new Date().toISOString() })
      }
      for (const meal of checkins.meals) {
        checkinList.push({ type: 'meal', checkinTime: new Date().toISOString(), mealType: meal.type })
      }
      for (const exercise of checkins.exercises) {
        checkinList.push({ type: 'exercise', checkinTime: new Date().toISOString(), exerciseType: exercise.type })
      }
      setCache(cacheKey, {
        data: checkinList,
        date: date
      })
      removeCache('checkinStats_' + date)

      await fetchCheckinStats()

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
    uni.showToast({ title: '网络错误', icon: 'none' })
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
      'Authorization': `Bearer ${userStore.getToken}`
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
        checkinList.push({ type: 'early', checkinTime: new Date().toISOString() })
      }
      if (checkins.sleep) {
        checkinList.push({ type: 'sleep', checkinTime: new Date().toISOString() })
      }
      for (const meal of checkins.meals) {
        checkinList.push({ type: 'meal', checkinTime: new Date().toISOString(), mealType: meal.type })
      }
      for (const exercise of checkins.exercises) {
        checkinList.push({ type: 'exercise', checkinTime: new Date().toISOString(), exerciseType: exercise.type })
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
    uni.showToast({ title: '网络错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 重置用餐打卡
 */
const resetMealCheckin = async () => {
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
    const response = await del(`/checkin?type=meal&date=${date}`, {}, {
      'Authorization': `Bearer ${userStore.getToken}`
    })

    if (response.code === 200) {
      checkins.meals = []
      selectedMealType.value = ''

      const date = getCurrentDate()
      const cacheKey = getTodayCacheKey()
      const checkinList = []
      if (checkins.early) {
        checkinList.push({ type: 'early', checkinTime: new Date().toISOString() })
      }
      if (checkins.sleep) {
        checkinList.push({ type: 'sleep', checkinTime: new Date().toISOString() })
      }
      for (const meal of checkins.meals) {
        checkinList.push({ type: 'meal', checkinTime: new Date().toISOString(), mealType: meal.type })
      }
      for (const exercise of checkins.exercises) {
        checkinList.push({ type: 'exercise', checkinTime: new Date().toISOString(), exerciseType: exercise.type })
      }
      setCache(cacheKey, {
        data: checkinList,
        date: date
      })

      await fetchCheckinStats()

      uni.showToast({ title: '已重置，请重新打卡', icon: 'success' })
    } else if (response.code === 403) {
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
    } else {
      uni.showToast({ title: '重置失败', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: '网络错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 重置运动打卡
const resetExerciseCheckin = async () => {
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
    // 调用 API 删除所有运动打卡记录
    const response = await uni.request({
      url: `/api/checkin?type=exercise&date=${date}`,
      method: 'DELETE',
      header: {
        'Authorization': `Bearer ${userStore.getToken}`
      }
    })
    
    if (response.statusCode === 200 && response.data.code === 200) {
      // 清空本地状态
      checkins.exercises = []
      selectedExerciseType.value = ''

      // 更新缓存
      const date = getCurrentDate()
      const cacheKey = getTodayCacheKey()
      const checkinList = []
      if (checkins.early) {
        checkinList.push({ type: 'early', checkinTime: new Date().toISOString() })
      }
      if (checkins.sleep) {
        checkinList.push({ type: 'sleep', checkinTime: new Date().toISOString() })
      }
      for (const meal of checkins.meals) {
        checkinList.push({ type: 'meal', checkinTime: new Date().toISOString(), mealType: meal.type })
      }
      for (const exercise of checkins.exercises) {
        checkinList.push({ type: 'exercise', checkinTime: new Date().toISOString(), exerciseType: exercise.type })
      }
      setCache(cacheKey, {
        data: checkinList,
        date: date
      })

      // 重新获取统计数据
      await fetchCheckinStats()

      uni.showToast({ title: '已重置，请重新打卡', icon: 'success' })
    } else if (response.statusCode === 403) {
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
    } else {
      uni.showToast({ title: '重置失败', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: '网络错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 获取子类型列表
const getSubTypes = (type) => {
  if (type === 'meal') {
    return mealTypes
  } else if (type === 'exercise') {
    return exerciseTypes
  }
  return []
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
    // 调用API删除打卡记录
    const response = await del(`/checkin?type=meal&date=${date}`, {}, {
      'Authorization': `Bearer ${userStore.getToken}`
    })
    
    if (response.code === 200) {
      // 更新本地状态
      checkins.meals.splice(index, 1)

      // 更新缓存
      const date = getCurrentDate()
      const cacheKey = getTodayCacheKey()
      const checkinList = []
      if (checkins.early) {
        checkinList.push({ type: 'early', checkinTime: new Date().toISOString() })
      }
      if (checkins.sleep) {
        checkinList.push({ type: 'sleep', checkinTime: new Date().toISOString() })
      }
      for (const meal of checkins.meals) {
        checkinList.push({ type: 'meal', checkinTime: new Date().toISOString(), mealType: meal.type })
      }
      for (const exercise of checkins.exercises) {
        checkinList.push({ type: 'exercise', checkinTime: new Date().toISOString(), exerciseType: exercise.type })
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
    uni.showToast({ title: '网络错误', icon: 'none' })
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
    // 调用API删除打卡记录
    const response = await del(`/checkin?type=exercise&date=${date}`, {}, {
      'Authorization': `Bearer ${userStore.getToken}`
    })
    
    if (response.code === 200) {
      // 更新本地状态
      checkins.exercises.splice(index, 1)

      // 更新缓存
      const date = getCurrentDate()
      const cacheKey = getTodayCacheKey()
      const checkinList = []
      if (checkins.early) {
        checkinList.push({ type: 'early', checkinTime: new Date().toISOString() })
      }
      if (checkins.sleep) {
        checkinList.push({ type: 'sleep', checkinTime: new Date().toISOString() })
      }
      for (const meal of checkins.meals) {
        checkinList.push({ type: 'meal', checkinTime: new Date().toISOString(), mealType: meal.type })
      }
      for (const exercise of checkins.exercises) {
        checkinList.push({ type: 'exercise', checkinTime: new Date().toISOString(), exerciseType: exercise.type })
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
    uni.showToast({ title: '网络错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 获取打卡记录
const fetchCheckins = async () => {
  const date = getCurrentDate()
  const cacheKey = getTodayCacheKey()

  const fetchFn = async () => {
    if (!userStore.getToken) {
      return []
    }

    const response = await get(`/checkin/by-date?date=${date}`, {}, {
      'Authorization': `Bearer ${userStore.getToken}`
    })

    if (response.code === 200) {
      return response.data || []
    }
    return []
  }

  try {
    const data = await fetchWithCache(cacheKey, fetchFn)
    updateCheckinsFromList(data)
  } catch (error) {
    console.error('获取打卡数据失败:', error)
  }
}

const updateCheckinsFromList = (checkinList) => {
  checkins.early = false
  checkins.earlyTime = ''
  checkins.sleep = false
  checkins.sleepTime = ''
  checkins.meals = []
  checkins.exercises = []

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

  const fetchFn = async () => {
    const response = await get('/checkin/stats', {}, {
      'Authorization': `Bearer ${userStore.getToken}`
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
onMounted(() => {
  // 检查是否登录
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }

  // 初始化数据（异步加载，不阻塞页面渲染）
  fetchCheckins()
  fetchCheckinStats()
})
</script>

<style scoped>
.checkin-container {
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
  box-sizing: border-box;
}

/* 打卡卡片 */
.checkin-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 30px;
}

.checkin-card {
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.checkin-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.checkin-card.checked {
  background-color: rgba(194, 151, 127, 0.1);
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
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.checkin-icon {
  font-size: 24px;
  margin-right: 12px;
  color: #666666;
  transition: color 0.3s ease;
}

.checkin-icon.checked {
  color: #C2977F;
}

/* 图标样式 */
.icon-sunny::before {
  content: "☀️";
}

.icon-moon::before {
  content: "🌙";
}

.icon-restaurant::before {
  content: "🍽️";
}

.icon-run::before {
  content: "🏃";
}

.checkin-title {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
}

.checkin-time {
  margin-bottom: 16px;
  font-size: 14px;
  color: #666666;
}

.meal-type-text, .exercise-type-text {
  display: block;
  font-size: 12px;
  color: #999999;
  margin-top: 4px;
}

.checkin-btn {
  width: 100%;
  padding: 10px;
  background-color: #94A7C8;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
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
  gap: 10px;
  margin-top: 16px;
}

.cancel-btn {
  background-color: #f0f0f0;
  color: #666666;
}

.cancel-btn:hover {
  background-color: #e0e0e0;
  color: #333333;
}

.checkin-records {
  margin-bottom: 16px;
}

.checkin-record {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background-color: rgba(194, 151, 127, 0.05);
  border-radius: 8px;
  margin-bottom: 8px;
}

.checkin-record-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.checkin-record .checkin-time {
  font-size: 14px;
  color: #333333;
  min-width: 50px;
}

.checkin-record .meal-type-text,
.checkin-record .exercise-type-text {
  font-size: 12px;
  color: #666666;
  margin-top: 0;
}

.checkin-record-cancel {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid #e0e0e0;
  background-color: white;
  color: #999999;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.checkin-record-cancel:hover {
  background-color: #ff4d4f;
  border-color: #ff4d4f;
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
  background-color: white;
  border: 1px solid var(--sidebar-border, #D8C8BE);
  border-radius: 6px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #666666;
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
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 16px;
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
  background-color: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.stat-title {
  font-size: 14px;
  color: #666666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--primary-color, #C2977F);
  margin-bottom: 8px;
}

.stat-chart {
  height: 8px;
  background-color: #F0F0F0;
  border-radius: 4px;
  overflow: hidden;
}

.chart-bar {
  height: 100%;
  background-color: var(--primary-color, #C2977F);
  border-radius: 4px;
  transition: width 0.5s ease;
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
  width: 90%;
  max-width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h3 {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
}

.close-icon {
  font-size: 20px;
  cursor: pointer;
  color: #999999;
}

.modal-body {
  padding: 20px;
}

.modal-body input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #D8C8BE;
  border-radius: 8px;
  font-size: 14px;
  color: #333333;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  gap: 10px;
}

.modal-footer button {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn {
  background-color: white;
  color: #333333;
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
</style>