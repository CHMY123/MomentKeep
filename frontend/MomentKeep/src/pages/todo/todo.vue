<template>
  <Layout>
    <div class="todo-container">
      <!-- 添加待办 -->
      <div class="add-todo">
        <input 
          type="text" 
          v-model="newTodo.title" 
          placeholder="输入待办事项…" 
          @keyup.enter="addTodo"
          @confirm="addTodo"
        />
        <button class="add-btn" @click="addTodo">添加</button>
      </div>
      
      <!-- 待办列表 -->
      <div class="todo-list">
        <!--
          加载态 / 空态。
          此前两者都没有：首次进入或接口失败时页面是一片空白，
          用户无法区分"确实没有待办"和"还没加载出来/加载失败了"。
        -->
        <div v-if="loading && todos.length === 0" class="list-state">
          <span class="state-text">正在加载待办…</span>
        </div>
        <div v-else-if="!loading && todos.length === 0" class="list-state">
          <div class="empty-graphic-list"></div>
          <span class="state-text">还没有待办 · 在上方输入框添加第一条</span>
        </div>

        <div 
          v-for="todo in todos" 
          :key="todo.id"
          class="todo-item stagger-item"
          :class="{ 'completed': todo.completed }"
        >
          <div class="todo-content">
            <div class="todo-icon" :class="{ 'completed': todo.completed }" @click="toggleTodo(todo.id)"></div>
            <div class="todo-text">
              <span :class="['todo-title-link', { 'completed-text': todo.completed }]" @click="showFocusStats(todo)">{{ todo.title }}</span>
              <span v-if="todo.description" class="todo-description">{{ todo.description }}</span>
            </div>
          </div>
          <div class="todo-actions">
            <div class="action-icon edit-icon" @click="editTodo(todo)"></div>
            <div class="action-icon delete-icon" @click="deleteTodo(todo.id)"></div>
          </div>
        </div>
      </div>
      
      <!-- 编辑待办弹窗 -->
      <div class="modal" v-if="isEditDialogOpen">
        <div class="modal-content">
          <div class="modal-header">
            <h3>编辑待办</h3>
            <div class="close-icon" @click="closeEditDialog">×</div>
          </div>
          <div class="modal-body">
          <input type="text" v-model="editTodoForm.title" placeholder="输入待办事项" />
          <textarea v-model="editTodoForm.description" placeholder="输入描述（可选）" rows="3" />
        </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="closeEditDialog">取消</button>
            <button class="confirm-btn" @click="saveTodo">保存</button>
          </div>
        </div>
      </div>
      
      <!-- 完成待办弹窗 -->
      <div class="modal" v-if="isCompleteDialogOpen">
        <div class="modal-content">
          <div class="modal-header">
            <h3>完成待办</h3>
            <div class="close-icon" @click="closeCompleteDialog">×</div>
          </div>
          <div class="modal-body">
            <textarea v-model="completionNote" placeholder="输入完成情况（可选）" rows="3" />
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="closeCompleteDialog">取消</button>
            <button class="confirm-btn" @click="confirmCompleteTodo">完成</button>
          </div>
        </div>
      </div>

      <!-- 专注记录统计弹窗 -->
      <div class="modal" v-if="isFocusStatsOpen">
        <div class="modal-content">
          <div class="modal-header">
            <h3>专注记录统计</h3>
            <div class="close-icon" @click="closeFocusStats">×</div>
          </div>
          <div class="modal-body" v-if="currentTodoFocusStats">
            <div class="focus-stats-todo-title">{{ currentTodoFocusStats.todoTitle }}</div>
            <div class="focus-stats-summary">
              <div class="stat-item">
                <span class="stat-value">{{ formatDuration(currentTodoFocusStats.totalDuration) }}</span>
                <span class="stat-label">总专注时长</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ currentTodoFocusStats.recordCount }}</span>
                <span class="stat-label">专注次数</span>
              </div>
            </div>
            <div v-if="currentTodoFocusStats.records && currentTodoFocusStats.records.length > 0" class="focus-records-list">
              <div class="records-title">专注记录</div>
              <div v-for="(record, index) in currentTodoFocusStats.records" :key="index" class="record-item stagger-item">
                <span class="record-mode">{{ record.mode === 'stopwatch' ? '计时' : record.mode === 'countdown' ? '倒计时' : '番茄钟' }}</span>
                <span class="record-duration">{{ formatDuration(record.duration) }}</span>
                <span class="record-time">{{ record.startTime ? new Date(record.startTime).toLocaleString('zh-CN') : '' }}</span>
              </div>
            </div>
            <div v-else class="empty-focus-records">
              这件待办还没有专注记录
            </div>
          </div>
          <div class="modal-footer">
            <button class="confirm-btn" @click="closeFocusStats">关闭</button>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
/**
 * MomentKeep 朝暮记 - 每日待办页面
 * @description 提供待办事项的添加、编辑、删除、标记完成等功能
 * @author MomentKeep Team
 * @since 2026-04-18
 */
import { ref, computed, onMounted, reactive } from 'vue'
import Layout from '../../components/Layout.vue'
import { useUserStore } from '../../store/user'
import { get, post, put, del } from '../../utils/request'
import { useCache } from '../../utils/cache'

const userStore = useUserStore()
const { getCache, setCache, removeCache, fetchWithCache } = useCache()

// 待办数据列表
const todos = ref([])

// 新待办表单数据
const newTodo = reactive({
  title: '',
  description: ''
})

// 编辑待办表单数据
const editTodoForm = reactive({
  id: null,
  title: '',
  description: ''
})

// 完成备注
const completionNote = ref('')

// 弹窗状态
const isEditDialogOpen = ref(false)
const isCompleteDialogOpen = ref(false)
const completingTodoId = ref(null)

// 专注记录弹窗
const isFocusStatsOpen = ref(false)
const currentTodoFocusStats = ref(null)
const currentTodoFocusRecords = ref([])

// 加载状态
const loading = ref(false)

/**
 * 获取待办列表
 * @description 从API获取待办数据，支持缓存优先策略
 */
const fetchTodos = async () => {
  const fetchFn = async () => {
    if (!userStore.getToken) {
      return []
    }

    /*
     * 用 /todo/today 而不是 /todo。
     * 本页标题与侧栏菜单都叫「今日待办」，但此前取的是全量列表，
     * 几天前已完成的待办会一直堆在这里；后端本就提供了按今日过滤的接口
     * （专注页选待办用的就是它），这里改用同一口径。
     */
    const response = await get('/todo/today', {}, {
    })

    if (response.code === 200) {
      return response.data || []
    }
    return []
  }

  try {
    loading.value = true
    const cachedTodos = getCache('todos')
    if (cachedTodos !== null) {
      todos.value = cachedTodos
    }
    /*
     * 先失效缓存再取数据。
     *
     * fetchWithCache 是"缓存优先"语义：命中缓存时它**只会在后台刷新缓存，
     * 从不把新数据交回调用方**（utils/cache.js）。而侧栏切换用的是 reLaunch，
     * 每次切页都会重建本页、必然命中缓存 —— 于是刚新增的待办消失、删除的复活、
     * 勾掉的又变回未完成，最新数据只被写进缓存却没人用。
     * 这里读取前主动失效，令其走"未命中 → 请求 → 回填"；上方那次 getCache 保留，
     * 用于首帧直接出内容。任何写操作后都不再需要单独清缓存，本函数即入口。
     */
    removeCache('todos')
    const data = await fetchWithCache('todos', fetchFn)
    todos.value = data
  } catch (error) {
    console.error('Error:', error)
    uni.showToast({ title: error.message || '网络错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 添加新待办
 */
/** 上次提交时间：连点「添加」时防止重复写入 */
let lastSubmitAt = 0

const addTodo = async () => {
  if (newTodo.title.trim()) {
    // 800ms 内的重复点击直接忽略
    if (Date.now() - lastSubmitAt < 800) return
    lastSubmitAt = Date.now()
    if (!userStore.getToken) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
      return
    }

    try {
      // 提交前 trim 并做长度校验：后端限制 100 字符，超长只会在请求后被拒
      // （或原样入库），不如在本地就拦住；顺带避免把首尾空格写进数据库。
      const trimmedTitle = (newTodo.title || '').trim()
      if (!trimmedTitle) {
        uni.showToast({ title: '请输入待办内容', icon: 'none' })
        return
      }
      if (trimmedTitle.length > 100) {
        uni.showToast({ title: '待办最多 100 字', icon: 'none' })
        return
      }

      const response = await post('/todo', {
        title: trimmedTitle,
        description: (newTodo.description || '').trim()
      }, {
      })

      if (response.code === 200) {
        // 插到**开头**：后端返回的是 create_time desc（最新在前），
        // 此前 push 到末尾会让新条目先出现在底部、刷新后又"跳"到顶部。
        todos.value.unshift(response.data)
        newTodo.title = ''
        newTodo.description = ''
        uni.showToast({ title: '待办添加成功', icon: 'success' })
      } else if (response.code === 403) {
        uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
        setTimeout(() => {
          uni.navigateTo({ url: '/pages/login/login' })
        }, 1000)
      } else {
        uni.showToast({ title: response.message || '添加失败', icon: 'none' })
      }
    } catch (error) {
      uni.showToast({ title: error.message || '网络错误', icon: 'none' })
    }
  } else {
    uni.showToast({ title: '请输入待办标题', icon: 'none' })
  }
}

/**
 * 切换待办完成状态
 * @param {number} id - 待办ID
 */
const toggleTodo = (id) => {
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }

  const todo = todos.value.find(t => t.id === id)
  if (!todo.completed) {
    completionNote.value = ''
    completingTodoId.value = id
    isCompleteDialogOpen.value = true
  } else {
    updateTodoStatus(id, false)
  }
}

/**
 * 确认完成待办
 */
const confirmCompleteTodo = async () => {
  if (completingTodoId.value) {
    if (!userStore.getToken) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
      return
    }

    try {
      // 统一使用 request 工具：自动拼接各端 baseURL、注入 token、处理 401/403。
      // 原先裸调 uni.request 并硬编码 /api 前缀，在小程序 / App 端会直接请求失败。
      // 后端该接口用 @RequestParam 接收参数，这里以查询串形式传递。
      const response = await post(
        `/todo/${completingTodoId.value}/complete?completionNote=${encodeURIComponent(completionNote.value || '')}`
      )

      if (response.code === 200) {
        const index = todos.value.findIndex(t => t.id === completingTodoId.value)
        if (index !== -1) {
          todos.value[index] = response.data
        }
        closeCompleteDialog()
        uni.showToast({ title: '待办完成', icon: 'success' })
      } else {
        uni.showToast({ title: response.message || '操作失败', icon: 'none' })
      }
    } catch (error) {
      // 401/403 已由 request 工具统一跳转登录页，这里只提示业务错误
      uni.showToast({ title: error.message || '网络错误', icon: 'none' })
    }
  }
}

/**
 * 更新待办状态
 * @param {number} id - 待办ID
 * @param {boolean} completed - 是否完成
 */
const updateTodoStatus = async (id, completed) => {
  // 防连点：同一条待办 500ms 内的重复触发直接忽略。
  // 锁挂在函数对象上，不新增模块级变量——顶层可变绑定在构建中曾出现丢失的情况。
  const now = Date.now()
  const lock = updateTodoStatus._lock || (updateTodoStatus._lock = {})
  if (lock[id] && now - lock[id] < 500) return
  lock[id] = now

  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }

  try {
    /*
     * 必须回传被覆盖的字段，不能只发 { id, completed }。
     *
     * 后端更新是"整对象覆盖"语义：请求里没带的字段会被写成 null。
     * 此前取消勾选只提交 id + completed，于是一条带描述与优先级的待办，
     * 只要被取消勾选一次，描述和优先级就被静默清空且无法恢复 —— 这是数据丢失。
     * 这里按列表里已有的值补齐这几个字段（不整体展开对象，
     * 以免把 userId / createTime 这类不应由客户端提交的字段一并送回）。
     */
    const current = todos.value.find(t => t.id === id)
    if (!current) {
      uni.showToast({ title: '待办不存在，请刷新后重试', icon: 'none' })
      return
    }
    const payload = {
      id: id,
      completed: completed,
      title: current.title,
      description: current.description
    }
    // 优先级缺省时不提交，避免用 null 覆盖掉原本"未设置"的状态
    if (current.priority !== undefined && current.priority !== null) {
      payload.priority = current.priority
    }

    const response = await put('/todo', payload, {
    })

    if (response.code === 200) {
      const index = todos.value.findIndex(t => t.id === id)
      if (index !== -1) {
        todos.value[index].completed = completed
      }
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

/**
 * 编辑待办
 * @param {Object} todo - 待办对象
 */
const editTodo = (todo) => {
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }

  editTodoForm.id = todo.id
  editTodoForm.title = todo.title
  editTodoForm.description = todo.description
  isEditDialogOpen.value = true
}

/**
 * 保存编辑后的待办
 */
const saveTodo = async () => {
  if (editTodoForm.title.trim()) {
    if (!userStore.getToken) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
      return
    }

    try {
      /*
       * 与勾选处同一类问题：后端是整对象覆盖，这里只发 id/title/description，
       * 于是"编辑一次"会把 completed（以及 priority）写成 null —— 已完成的待办
       * 被编辑后自己变回未完成，且没有任何提示。
       * 补齐这两个字段（priority 缺省时不提交，避免用 null 覆盖"未设置"）。
       */
      const currentEdit = todos.value.find(t => t.id === editTodoForm.id) || {}
      const editPayload = {
        id: editTodoForm.id,
        title: editTodoForm.title,
        description: editTodoForm.description,
        completed: currentEdit.completed
      }
      if (currentEdit.priority !== undefined && currentEdit.priority !== null) {
        editPayload.priority = currentEdit.priority
      }

      const response = await put('/todo', editPayload, {
      })

      if (response.code === 200) {
        const index = todos.value.findIndex(t => t.id === editTodoForm.id)
        if (index !== -1) {
          todos.value[index] = response.data
        }
        closeEditDialog()
        uni.showToast({ title: '待办更新成功', icon: 'success' })
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
}

const deleteTodo = (id) => {
  // 防连点：同一条待办 500ms 内重复触发直接忽略（重复删除会得到"待办不存在"的报错提示）。
  // 锁挂在函数对象上，与 updateTodoStatus 用的是同一种写法。
  const now = Date.now()
  const lock = deleteTodo._lock || (deleteTodo._lock = {})
  if (lock[id] && now - lock[id] < 500) return
  lock[id] = now

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
    content: '确定要删除这个待办吗？',
    success: async (res) => {
    if (res.confirm) {
      try {
        const response = await del(`/todo/${id}`, {}, {
        })

        if (response.code === 200) {
          const index = todos.value.findIndex(t => t.id === id)
          if (index !== -1) {
            todos.value.splice(index, 1)
          }
          uni.showToast({ title: '待办删除成功', icon: 'success' })
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

const closeEditDialog = () => {
  isEditDialogOpen.value = false
  editTodoForm.id = null
  editTodoForm.title = ''
  editTodoForm.description = ''
}

const closeCompleteDialog = () => {
  isCompleteDialogOpen.value = false
  completionNote.value = ''
  completingTodoId.value = null
}

/**
 * 显示待办的专注记录统计
 */
const showFocusStats = async (todo) => {
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }

  try {
    const response = await get(`/focus/record/todo/${todo.id}`, {}, {
    })

    if (response.code === 200) {
      currentTodoFocusStats.value = {
        todoTitle: todo.title,
        totalDuration: response.data.totalDuration || 0,
        recordCount: response.data.recordCount || 0,
        records: response.data.records || []
      }
      isFocusStatsOpen.value = true
    } else if (response.code === 403) {
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
    } else {
      uni.showToast({ title: '专注记录加载失败，请重试', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: error.message || '网络错误', icon: 'none' })
  }
}

/**
 * 关闭专注记录弹窗
 */
const closeFocusStats = () => {
  isFocusStatsOpen.value = false
  currentTodoFocusStats.value = null
}

/**
 * 格式化时长显示
 */
const formatDuration = (seconds) => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  }
  return `${minutes}分钟`
}

// 生命周期
onMounted(() => {
  // 初始化用户信息
  userStore.initUserInfo()
  
  // 初始化数据
  fetchTodos()
})
</script>

<style scoped>
.todo-container {
  padding: 20px;
}

/* 添加待办 */
.add-todo {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
}

.add-todo input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid var(--border-color, #D8C8BE);
  border-radius: var(--radius-sm);
  background-color: var(--surface-strong, #FFFFFF);
  font-size: var(--fs-body); /* 14px */
  color: var(--text-color, #333333);
  box-sizing: border-box;
  line-height: 1.6;
  height: 44px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.add-btn {
  padding: 0 20px;
  background-color: #C2977F;
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--fs-body); /* 14px */
  font-weight: 500;
  transition: var(--transition-interactive);
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  min-width: 80px;
  text-align: center;
}

.add-btn:hover {
  background-color: #A8846B;
}

/* 待办列表 */
.todo-list {
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-md);
  padding: 16px;
  box-shadow: var(--shadow-md);
}

.todo-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px 16px;
  background-color: var(--surface-strong, #FFFFFF);
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
  box-shadow: var(--shadow-sm);
  transition: var(--transition-interactive);
}

.todo-item:hover {
  box-shadow: var(--shadow-lg);
}

.todo-item.completed {
  background-color: rgba(194, 151, 127, 0.05);
}

.todo-content {
  display: flex;
  align-items: center;
  flex: 1;
}

.todo-icon {
  font-size: var(--fs-2xl); /* 24px */
  margin-right: 12px;
  color: var(--text-secondary, #666666);
  cursor: pointer;
  transition: color 0.3s ease;
  line-height: 1;
}

/* 复选框图标已迁到全局 src/styles/icons.css（自绘 base64 PNG，随 .completed 切换） */
.todo-icon.completed {
  color: #C2977F;
}

/* 操作图标 */
.todo-actions {
  display: flex;
  gap: 12px;
}

.action-icon {
  font-size: var(--fs-xl); /* 20px */
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

.todo-text {
  flex: 1;
}

.todo-text text {
  display: block;
}

.completed-text {
  text-decoration: line-through;
  color: var(--text-muted, #999999);
}

.todo-description {
  font-size: var(--fs-xs); /* 12px */
  color: var(--text-muted, #999999);
  margin-top: 8px;
  display: block;
}

.todo-title-link {
  cursor: pointer;
  transition: color 0.2s ease;
}

.todo-title-link:hover {
  color: var(--primary-color, #C2977F);
}

.todo-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 12px;
}

.todo-actions uni-icons {
  cursor: pointer;
  transition: color 0.3s ease;
}

.todo-actions uni-icons:hover {
  color: #C2977F;
}

.empty-todo {
  text-align: center;
  padding: 40px 0;
  color: var(--text-muted, #999999);
  font-size: var(--fs-body); /* 14px */
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
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modal-header h3 {
  font-size: var(--fs-md); /* 16px */
  font-weight: 500;
  color: var(--text-color, #333333);
  margin: 0;
}

.close-icon {
  font-size: var(--fs-2xl); /* 24px */
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
  font-size: var(--fs-body); /* 14px */
  margin-bottom: 12px;
  box-sizing: border-box;
  line-height: 1.6;
  min-height: 60px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
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
  font-size: var(--fs-body); /* 14px */
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

/* 专注记录统计弹窗样式 */
.focus-stats-todo-title {
  font-size: var(--fs-lg); /* 18px */
  font-weight: 600;
  color: var(--primary-color, #C2977F);
  text-align: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #E8D5C4;
}

.focus-stats-summary {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
}

.stat-item .stat-value {
  display: block;
  font-size: var(--fs-2xl); /* 24px */
  font-weight: 600;
  color: var(--text-color, #333333);
}

.stat-item .stat-label {
  display: block;
  font-size: var(--fs-xs); /* 12px */
  color: var(--text-muted, #999999);
  margin-top: 4px;
}

.focus-records-list {
  max-height: 200px;
  overflow-y: auto;
}

.records-title {
  font-size: var(--fs-body); /* 14px */
  font-weight: 500;
  color: var(--text-color, #333333);
  margin-bottom: 12px;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 12px;
  background-color: var(--surface-color, #F2EEE8);
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
}

.record-mode {
  font-size: var(--fs-xs); /* 12px */
  color: var(--primary-color, #C2977F);
}

.record-duration {
  font-size: var(--fs-body); /* 14px */
  font-weight: 500;
  color: var(--text-color, #333333);
}

.record-time {
  font-size: var(--fs-xs); /* 12px */
  color: var(--text-muted, #999999);
}

.empty-focus-records {
  text-align: center;
  padding: 32px 0;
  color: var(--text-muted, #999999);
  font-size: var(--fs-body); /* 14px */
}

/* 响应式设计 */
@media (max-width: 768px) {
  .add-btn {
    padding: 0 16px;
    min-width: 70px;
  }
  
  .add-todo input {
    padding: 12px 16px;
  }
}
/* 加载态 / 空态提示（列表为空时替代整片空白） */
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

/* ==== 设计修订（覆盖规则，勿手改上面旧值） ==== */
/*
 * 触控热区：编辑/删除图标实际只有约 20px，远小于 44px 的最小可点尺寸，
 * 误触会直接删错。用 padding 撑开热区、再用等量负 margin 抵消占位，
 * 视觉位置与排版完全不变，只有可点范围变大。
 */
.action-icon,
.todo-icon {
  padding: 10px;
  margin: -10px;
}

</style>