<template>
  <Layout>
    <div class="todo-container">
      <!-- 添加待办 -->
      <div class="add-todo">
        <input 
          type="text" 
          v-model="newTodo.title" 
          placeholder="输入待办事项..." 
          @keyup.enter="addTodo"
        />
        <button class="add-btn" @click="addTodo">添加</button>
      </div>
      
      <!-- 待办列表 -->
      <div class="todo-list">
        <div 
          v-for="todo in todos" 
          :key="todo.id"
          class="todo-item"
          :class="{ 'completed': todo.completed }"
        >
          <div class="todo-content">
            <div class="todo-icon" :class="{ 'completed': todo.completed }" @click="toggleTodo(todo.id)"></div>
            <div class="todo-text">
              <span :class="{ 'completed-text': todo.completed }">{{ todo.title }}</span>
              <span v-if="todo.description" class="todo-description">{{ todo.description }}</span>
            </div>
          </div>
          <div class="todo-actions">
            <div class="action-icon edit-icon" @click="editTodo(todo)"></div>
            <div class="action-icon delete-icon" @click="deleteTodo(todo.id)"></div>
          </div>
        </div>
        <div v-if="todos.length === 0" class="empty-todo">
          <span>暂无待办事项</span>
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
import { ref, computed, onMounted, watch, reactive } from 'vue'
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

    const response = await get('/todo', {}, {
      'Authorization': `Bearer ${userStore.getToken}`
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
    const data = await fetchWithCache('todos', fetchFn)
    todos.value = data
  } catch (error) {
    console.error('Error:', error)
    uni.showToast({ title: '网络错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 添加新待办
 */
const addTodo = async () => {
  if (newTodo.title.trim()) {
    if (!userStore.getToken) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1000)
      return
    }

    try {
      const response = await post('/todo', {
        title: newTodo.title,
        description: newTodo.description,
        todoDate: new Date().toISOString().split('T')[0],
        userId: 1
      }, {
        'Authorization': `Bearer ${userStore.getToken}`
      })

      if (response.code === 200) {
        todos.value.push(response.data)
        newTodo.title = ''
        newTodo.description = ''
        uni.showToast({ title: '待办添加成功', icon: 'success' })
      } else if (response.code === 403) {
        uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
        setTimeout(() => {
          uni.navigateTo({ url: '/pages/login/login' })
        }, 1000)
      } else {
        uni.showToast({ title: '添加失败', icon: 'none' })
      }
    } catch (error) {
      uni.showToast({ title: '网络错误', icon: 'none' })
    }
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
      const response = await uni.request({
        url: `/api/todo/${completingTodoId.value}/complete`,
        method: 'POST',
        header: {
          'Authorization': `Bearer ${userStore.getToken}`
        },
        data: {
          completionNote: completionNote.value
        }
      })

      if (response.statusCode === 200 && response.data.code === 200) {
        const index = todos.value.findIndex(t => t.id === completingTodoId.value)
        if (index !== -1) {
          todos.value[index] = response.data.data
        }
        closeCompleteDialog()
        uni.showToast({ title: '待办完成', icon: 'success' })
      } else if (response.statusCode === 403) {
        uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
        setTimeout(() => {
          uni.navigateTo({ url: '/pages/login/login' })
        }, 1000)
      } else {
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    } catch (error) {
      uni.showToast({ title: '网络错误', icon: 'none' })
    }
  }
}

/**
 * 更新待办状态
 * @param {number} id - 待办ID
 * @param {boolean} completed - 是否完成
 */
const updateTodoStatus = async (id, completed) => {
  if (!userStore.getToken) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1000)
    return
  }

  try {
    const response = await put('/todo', {
      id: id,
      completed: completed
    }, {
      'Authorization': `Bearer ${userStore.getToken}`
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
      uni.showToast({ title: '更新失败', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: '网络错误', icon: 'none' })
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
      const response = await put('/todo', {
        id: editTodoForm.id,
        title: editTodoForm.title,
        description: editTodoForm.description
      }, {
        'Authorization': `Bearer ${userStore.getToken}`
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
        uni.showToast({ title: '更新失败', icon: 'none' })
      }
    } catch (error) {
      uni.showToast({ title: '网络错误', icon: 'none' })
    }
  }
}

const deleteTodo = (id) => {
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
          'Authorization': `Bearer ${userStore.getToken}`
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
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      } catch (error) {
        uni.showToast({ title: '网络错误', icon: 'none' })
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

// 生命周期
onMounted(() => {
  // 初始化用户信息
  userStore.initUserInfo()
  
  // 初始化数据
  fetchTodos()
  
  // 检查是否是今天第一次进入
  const today = new Date().toDateString()
  const lastVisit = uni.getStorageSync('lastVisit')
  if (lastVisit !== today) {
    // 弹出提示是否保留昨天的待办
    uni.showModal({
      title: '提示',
      content: '是否保留昨天的待办到今天？',
      success: async (res) => {
        if (res.confirm && userStore.getToken) {
          // 保留昨天的待办
          try {
            const response = await post('/todo/copy-yesterday', {}, {
              'Authorization': `Bearer ${userStore.getToken}`
            })
            
            if (response.code === 200) {
              uni.showToast({ title: '已保留昨天的待办', icon: 'success' })
              // 重新加载待办列表
              await fetchTodos()
            } else {
              uni.showToast({ title: '保留待办失败', icon: 'none' })
            }
          } catch (error) {
            console.error('复制昨天待办失败:', error)
            uni.showToast({ title: '网络错误', icon: 'none' })
          }
        }
      }
    })
    uni.setStorageSync('lastVisit', today)
  }
})
</script>

<style scoped>
.todo-container {
  padding: 20px;
}

/* 添加待办 */
.add-todo {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  align-items: center;
}

.add-todo input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #D8C8BE;
  border-radius: 8px;
  background-color: white;
  font-size: calc(var(--base-font-size) * 0.875); /* 14px */
  color: #333333;
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
  border-radius: 8px;
  font-size: calc(var(--base-font-size) * 0.875); /* 14px */
  font-weight: 500;
  transition: all 0.3s ease;
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
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.todo-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px 16px;
  background-color: white;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.todo-item:hover {
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.todo-item.completed {
  background-color: rgba(194, 151, 127, 0.05);
}

.todo-content {
  display: flex;
  align-items: flex-start;
  flex: 1;
}

.todo-icon {
  font-size: calc(var(--base-font-size) * 1.5); /* 24px */
  margin-right: 12px;
  color: #666666;
  cursor: pointer;
  transition: color 0.3s ease;
}

.todo-icon::before {
  content: "☐";
}

.todo-icon.completed {
  color: #C2977F;
}

.todo-icon.completed::before {
  content: "☑";
}

/* 操作图标 */
.todo-actions {
  display: flex;
  gap: 12px;
}

.action-icon {
  font-size: calc(var(--base-font-size) * 1.25); /* 20px */
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

.todo-text {
  flex: 1;
}

.todo-text text {
  display: block;
}

.completed-text {
  text-decoration: line-through;
  color: #999999;
}

.todo-description {
  font-size: calc(var(--base-font-size) * 0.75); /* 12px */
  color: #999999;
  margin-top: 4px;
}

.todo-actions {
  display: flex;
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
  color: #999999;
  font-size: calc(var(--base-font-size) * 0.875); /* 14px */
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
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modal-header h3 {
  font-size: var(--base-font-size); /* 16px */
  font-weight: 500;
  color: #333333;
  margin: 0;
}

.close-icon {
  font-size: calc(var(--base-font-size) * 1.5); /* 24px */
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
  font-size: calc(var(--base-font-size) * 0.875); /* 14px */
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
  border-radius: 8px;
  font-size: calc(var(--base-font-size) * 0.875); /* 14px */
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

/* 响应式设计 */
@media (max-width: 768px) {
  .add-todo {
    /* 保持水平排列 */
  }
  
  .add-btn {
    padding: 0 16px;
    min-width: 70px;
  }
  
  .add-todo input {
    padding: 10px 14px;
  }
}
</style>