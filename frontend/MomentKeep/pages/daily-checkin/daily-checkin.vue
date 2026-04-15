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
          </div>
          <div class="checkin-time" v-if="checkins.early">
            <div>{{ checkins.earlyTime }}</div>
          </div>
          <button 
            class="checkin-btn" 
            :class="{ 'checked-btn': checkins.early }"
            @click="checkin('early')"
            :disabled="checkins.early"
          >
            {{ checkins.early ? '已打卡' : '打卡' }}
          </button>
        </div>
        
        <!-- 睡眠打卡 -->
        <div class="checkin-card" :class="{ 'checked': checkins.sleep }">
          <div class="checkin-header">
            <div class="checkin-icon icon-moon" :class="{ 'checked': checkins.sleep }"></div>
            <div class="checkin-title">睡眠打卡</div>
          </div>
          <div class="checkin-time" v-if="checkins.sleep">
            <div>{{ checkins.sleepTime }}</div>
          </div>
          <button 
            class="checkin-btn" 
            :class="{ 'checked-btn': checkins.sleep }"
            @click="checkin('sleep')"
            :disabled="checkins.sleep"
          >
            {{ checkins.sleep ? '已打卡' : '打卡' }}
          </button>
        </div>
        
        <!-- 用餐打卡 -->
        <div class="checkin-card" :class="{ 'checked': checkins.meal }">
          <div class="checkin-header">
            <div class="checkin-icon icon-restaurant" :class="{ 'checked': checkins.meal }"></div>
            <div class="checkin-title">用餐打卡</div>
          </div>
          <div class="meal-types" v-if="!checkins.meal">
            <div 
              v-for="type in mealTypes" 
              :key="type"
              class="meal-type"
              @click="selectMealType(type)"
            >
              <div>{{ type }}</div>
            </div>
            <div class="meal-type custom" @click="showCustomMeal">
              <div>自定义</div>
            </div>
          </div>
          <div class="checkin-time" v-else>
            <div>{{ checkins.mealTime }}</div>
            <div class="meal-type-text">{{ checkins.selectedMealType }}</div>
          </div>
          <button 
            class="checkin-btn" 
            :class="{ 'checked-btn': checkins.meal }"
            @click="checkin('meal')"
            :disabled="checkins.meal || !selectedMealType"
          >
            {{ checkins.meal ? '已打卡' : '打卡' }}
          </button>
        </div>
        
        <!-- 运动打卡 -->
        <div class="checkin-card" :class="{ 'checked': checkins.exercise }">
          <div class="checkin-header">
            <div class="checkin-icon icon-run" :class="{ 'checked': checkins.exercise }"></div>
            <div class="checkin-title">运动打卡</div>
          </div>
          <div class="exercise-types" v-if="!checkins.exercise">
            <div 
              v-for="type in exerciseTypes" 
              :key="type"
              class="exercise-type"
              @click="selectExerciseType(type)"
            >
              <div>{{ type }}</div>
            </div>
            <div class="exercise-type custom" @click="showCustomExercise">
              <div>自定义</div>
            </div>
          </div>
          <div class="checkin-time" v-else>
            <div>{{ checkins.exerciseTime }}</div>
            <div class="exercise-type-text">{{ checkins.selectedExerciseType }}</div>
          </div>
          <button 
            class="checkin-btn" 
            :class="{ 'checked-btn': checkins.exercise }"
            @click="checkin('exercise')"
            :disabled="checkins.exercise || !selectedExerciseType"
          >
            {{ checkins.exercise ? '已打卡' : '打卡' }}
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
import { ref, reactive, onMounted } from 'vue'
import Layout from '../../components/Layout.vue'

// 打卡状态
const checkins = reactive({
  early: true,
  earlyTime: '07:30',
  sleep: false,
  sleepTime: '',
  meal: false,
  mealTime: '',
  selectedMealType: '',
  exercise: false,
  exerciseTime: '',
  selectedExerciseType: ''
})

// 用餐类型
const mealTypes = ['早餐', '午餐', '晚餐', '宵夜']
const selectedMealType = ref('')
const customMealType = ref('')
const isCustomMealDialogOpen = ref(false)

// 运动类型
const exerciseTypes = ['跑步', '健身', '瑜伽', '游泳']
const selectedExerciseType = ref('')
const customExerciseType = ref('')
const isCustomExerciseDialogOpen = ref(false)

// 统计数据
const stats = reactive({
  earlyRate: 85,
  sleepRate: 70,
  mealCount: 3,
  exerciseRate: 60
})

// 方法
const checkin = (type) => {
  const now = new Date()
  const time = now.getHours().toString().padStart(2, '0') + ':' + now.getMinutes().toString().padStart(2, '0')
  
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
      checkins.meal = true
      checkins.mealTime = time
      checkins.selectedMealType = selectedMealType.value
      break
    case 'exercise':
      checkins.exercise = true
      checkins.exerciseTime = time
      checkins.selectedExerciseType = selectedExerciseType.value
      break
  }
  
  // 这里可以调用API保存打卡记录
  uni.showToast({ title: '打卡成功', icon: 'success' })
}

const selectMealType = (type) => {
  selectedMealType.value = type
}

const showCustomMeal = () => {
  isCustomMealDialogOpen.value = true
}

const closeCustomMealDialog = () => {
  isCustomMealDialogOpen.value = false
  customMealType.value = ''
}

const confirmCustomMeal = () => {
  if (customMealType.value.trim()) {
    selectedMealType.value = customMealType.value
    closeCustomMealDialog()
  } else {
    uni.showToast({ title: '请输入用餐类型', icon: 'none' })
  }
}

const selectExerciseType = (type) => {
  selectedExerciseType.value = type
}

const showCustomExercise = () => {
  isCustomExerciseDialogOpen.value = true
}

const closeCustomExerciseDialog = () => {
  isCustomExerciseDialogOpen.value = false
  customExerciseType.value = ''
}

const confirmCustomExercise = () => {
  if (customExerciseType.value.trim()) {
    selectedExerciseType.value = customExerciseType.value
    closeCustomExerciseDialog()
  } else {
    uni.showToast({ title: '请输入运动类型', icon: 'none' })
  }
}

// 生命周期
onMounted(() => {
  // 初始化数据
  // 这里可以从API获取实际打卡数据
})
</script>

<style scoped>
.checkin-container {
  width: 100%;
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

.checked-btn {
  background-color: #C2977F;
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
  border: 1px solid #D8C8BE;
  border-radius: 6px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #666666;
}

.meal-type:hover, .exercise-type:hover {
  border-color: #C2977F;
  color: #C2977F;
}

.meal-type.custom, .exercise-type.custom {
  border-style: dashed;
}

/* 数据统计 */
.stats-section {
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  margin-bottom: 16px;
  border-bottom: 1px solid #94A7C8;
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
  color: #C2977F;
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
  background-color: #C2977F;
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