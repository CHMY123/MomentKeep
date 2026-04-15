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
          v-for="(todo, index) in todos" 
          :key="index"
          class="todo-item"
          :class="{ 'completed': todo.completed }"
        >
          <div class="todo-content">
            <div class="todo-icon" :class="{ 'completed': todo.completed }" @click="toggleTodo(index)"></div>
            <div class="todo-text">
              <span :class="{ 'completed-text': todo.completed }">{{ todo.title }}</span>
              <span v-if="todo.description" class="todo-description">{{ todo.description }}</span>
            </div>
          </div>
          <div class="todo-actions">
            <div class="action-icon edit-icon" @click="editTodo(index)"></div>
            <div class="action-icon delete-icon" @click="deleteTodo(index)"></div>
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
import { ref, reactive, onMounted } from 'vue'
import Layout from '../../components/Layout.vue'

// 待办数据
const todos = ref([
  {
    title: '完成前端开发',
    description: '实现所有页面的UI和交互',
    completed: false,
    completionNote: ''
  },
  {
    title: '测试API接口',
    description: '确保所有接口正常工作',
    completed: false,
    completionNote: ''
  },
  {
    title: '提交代码',
    description: '将代码提交到版本控制系统',
    completed: false,
    completionNote: ''
  }
])

// 新待办
const newTodo = reactive({
  title: '',
  description: ''
})

// 编辑待办
const editTodoForm = reactive({
  index: -1,
  title: '',
  description: ''
})

// 完成备注
const completionNote = ref('')

// 弹窗状态
const isEditDialogOpen = ref(false)
const isCompleteDialogOpen = ref(false)

// 方法
const addTodo = () => {
  if (newTodo.title.trim()) {
    todos.value.push({
      title: newTodo.title,
      description: newTodo.description,
      completed: false,
      completionNote: ''
    })
    newTodo.title = ''
    newTodo.description = ''
    // 这里可以调用API保存待办
    uni.showToast({ title: '待办添加成功', icon: 'success' })
  }
}

const toggleTodo = (index) => {
  const todo = todos.value[index]
  if (!todo.completed) {
    // 显示完成弹窗
    completionNote.value = ''
    editTodoForm.index = index
    isCompleteDialogOpen.value = true
  } else {
    // 取消完成
    todo.completed = false
    todo.completionNote = ''
    // 这里可以调用API更新待办
  }
}

const confirmCompleteTodo = () => {
  const todo = todos.value[editTodoForm.index]
  todo.completed = true
  todo.completionNote = completionNote.value
  closeCompleteDialog()
  // 这里可以调用API更新待办
  uni.showToast({ title: '待办完成', icon: 'success' })
}

const editTodo = (index) => {
  const todo = todos.value[index]
  editTodoForm.index = index
  editTodoForm.title = todo.title
  editTodoForm.description = todo.description
  isEditDialogOpen.value = true
}

const saveTodo = () => {
  if (editTodoForm.title.trim()) {
    const todo = todos.value[editTodoForm.index]
    todo.title = editTodoForm.title
    todo.description = editTodoForm.description
    closeEditDialog()
    // 这里可以调用API更新待办
    uni.showToast({ title: '待办更新成功', icon: 'success' })
  }
}

const deleteTodo = (index) => {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这个待办吗？',
    success: (res) => {
      if (res.confirm) {
        todos.value.splice(index, 1)
        // 这里可以调用API删除待办
        uni.showToast({ title: '待办删除成功', icon: 'success' })
      }
    }
  })
}

const closeEditDialog = () => {
  isEditDialogOpen.value = false
  editTodoForm.index = -1
  editTodoForm.title = ''
  editTodoForm.description = ''
}

const closeCompleteDialog = () => {
  isCompleteDialogOpen.value = false
  completionNote.value = ''
}

// 生命周期
onMounted(() => {
  // 初始化数据
  // 这里可以从API获取实际待办数据
  
  // 检查是否是今天第一次进入
  const today = new Date().toDateString()
  const lastVisit = uni.getStorageSync('lastVisit')
  if (lastVisit !== today) {
    // 弹出提示是否保留昨天的待办
    uni.showModal({
      title: '提示',
      content: '是否保留昨天的待办到今天？',
      success: (res) => {
        if (res.confirm) {
          // 保留昨天的待办
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
}

.add-todo input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #D8C8BE;
  border-radius: 8px;
  background-color: white;
  font-size: 14px;
  color: #333333;
}

.add-btn {
  padding: 0 20px;
  background-color: #C2977F;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
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
  font-size: 24px;
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
  font-size: 20px;
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
  font-size: 12px;
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
  font-size: 14px;
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
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  margin: 0;
}

.close-icon {
  font-size: 24px;
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
  font-size: 14px;
  margin-bottom: 12px;
  box-sizing: border-box;
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
  font-size: 14px;
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
    flex-direction: column;
  }
  
  .add-btn {
    padding: 12px;
  }
}
</style>