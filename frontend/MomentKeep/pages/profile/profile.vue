<template>
  <Layout>
    <div class="profile-container">
      <!-- 用户信息卡片 -->
      <div class="user-card">
        <div class="user-info-header">
          <div class="avatar-section">
            <div class="avatar" @click="uploadAvatar">
              <img :src="userInfo.avatar" alt="avatar" />
              <div class="avatar-edit">
                <div class="icon-camera"></div>
              </div>
            </div>
          </div>
          <div class="edit-btn-container">
            <button class="edit-profile-btn" @click="toggleEditMode">
              {{ isEditMode ? '取消' : '变更资料' }}
            </button>
          </div>
          <div class="user-info-grid">
            <div class="info-item">
              <div class="info-label">用户名</div>
              <div class="info-value">{{ userInfo.username }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">昵称</div>
              <div v-if="!isEditMode" class="info-value">{{ userInfo.nickname }}</div>
              <input v-else type="text" v-model="formData.nickname" placeholder="请输入昵称" />
            </div>
            <div class="info-item">
              <div class="info-label">手机号</div>
              <div v-if="!isEditMode" class="info-value">{{ userInfo.phone }}</div>
              <input v-else type="tel" v-model="formData.phone" placeholder="请输入手机号" />
            </div>
            <div class="info-item">
              <div class="info-label">邮箱</div>
              <div v-if="!isEditMode" class="info-value">{{ userInfo.email }}</div>
              <input v-else type="email" v-model="formData.email" placeholder="请输入邮箱" />
            </div>
          </div>
          <button v-if="isEditMode" class="submit-btn" @click="updateProfile">保存修改</button>
        </div>
      </div>
      
      <!-- 反馈提交 -->
      <div class="form-card">
        <div class="form-title">意见反馈</div>
        
        <div class="form-item">
          <textarea v-model="feedbackContent" placeholder="请输入您的反馈..." rows="4" />
        </div>
        
        <div class="form-item">
          <div class="form-label">联系方式</div>
          <div class="contact-info">{{ userInfo.phone }}</div>
        </div>
        
        <button class="submit-btn" @click="submitFeedback">提交反馈</button>
      </div>
      
      <!-- 账户操作 -->
      <div class="action-card">
        <div class="form-title">账户操作</div>
        
        <div class="action-item" @click="showChangePasswordDialog">
          <div class="icon-lock"></div>
          <div class="action-text">修改密码</div>
        </div>
        
        <div class="action-item" @click="showDeleteDialog">
          <div class="icon-close-circle"></div>
          <div class="action-text">注销账户</div>
        </div>
      </div>
      
      <!-- 注销确认对话框 -->
      <div v-if="isDeleteDialogVisible" class="dialog-overlay" @click="closeDeleteDialog">
        <div class="dialog-content" @click.stop>
          <div class="dialog-header">
            <div class="dialog-title">注销账户</div>
            <div class="dialog-close" @click="closeDeleteDialog">×</div>
          </div>
          <div class="dialog-body">
            <p>您确定要注销账户吗？注销后所有数据将被删除，且无法恢复。</p>
          </div>
          <div class="dialog-footer">
            <button class="dialog-btn cancel-btn" @click="closeDeleteDialog">取消</button>
            <button class="dialog-btn confirm-btn" @click="confirmDeleteAccount">确定注销</button>
          </div>
        </div>
      </div>
      
      <!-- 修改密码对话框 -->
      <div v-if="isChangePasswordDialogVisible" class="dialog-overlay" @click="closeChangePasswordDialog">
        <div class="dialog-content" @click.stop>
          <div class="dialog-header">
            <div class="dialog-title">修改密码</div>
            <div class="dialog-close" @click="closeChangePasswordDialog">×</div>
          </div>
          <div class="dialog-body">
            <div class="form-item">
              <div class="form-label">当前密码</div>
              <input type="password" v-model="passwordData.oldPassword" placeholder="请输入当前密码" />
            </div>
            <div class="form-item">
              <div class="form-label">新密码</div>
              <input type="password" v-model="passwordData.newPassword" placeholder="请输入新密码" />
            </div>
            <div class="form-item">
              <div class="form-label">确认新密码</div>
              <input type="password" v-model="passwordData.confirmPassword" placeholder="请确认新密码" />
            </div>
          </div>
          <div class="dialog-footer">
            <button class="dialog-btn cancel-btn" @click="closeChangePasswordDialog">取消</button>
            <button class="dialog-btn confirm-btn" @click="changePassword">确定修改</button>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Layout from '../../components/Layout.vue'

const isDeleteDialogVisible = ref(false)
const isChangePasswordDialogVisible = ref(false)
const isEditMode = ref(false)

// 用户信息
const userInfo = reactive({
  username: 'test',
  nickname: '测试用户',
  email: 'test@example.com',
  phone: '13800138000',
  avatar: 'https://img.icons8.com/ios-filled/50/000000/user.png'
})

// 表单数据
const formData = reactive({
  nickname: '',
  email: '',
  phone: ''
})

// 密码数据
const passwordData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 反馈数据
const feedbackContent = ref('')

// 方法
const toggleEditMode = () => {
  isEditMode.value = !isEditMode.value
  if (isEditMode.value) {
    // 进入编辑模式时，初始化表单数据
    Object.assign(formData, userInfo)
  }
}

const uploadAvatar = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      // 这里可以上传图片到服务器
      userInfo.avatar = res.tempFilePaths[0]
      uni.showToast({ title: '头像上传成功', icon: 'success' })
    }
  })
}

const updateProfile = () => {
  // 这里可以调用API更新用户资料
  Object.assign(userInfo, formData)
  uni.showToast({ title: '资料更新成功', icon: 'success' })
  isEditMode.value = false
}

const submitFeedback = () => {
  if (!feedbackContent.value.trim()) {
    uni.showToast({ title: '请输入反馈内容', icon: 'none' })
    return
  }
  // 这里可以调用API提交反馈
  uni.showToast({ title: '反馈提交成功', icon: 'success' })
  feedbackContent.value = ''
}

const showDeleteDialog = () => {
  isDeleteDialogVisible.value = true
}

const closeDeleteDialog = () => {
  isDeleteDialogVisible.value = false
}

const confirmDeleteAccount = () => {
  // 这里可以调用API注销账户
  uni.showToast({ title: '账户注销成功', icon: 'success' })
  // 跳转到登录页面
  setTimeout(() => {
    uni.navigateTo({
      url: '/pages/login/login'
    })
  }, 1500)
}

const showChangePasswordDialog = () => {
  isChangePasswordDialogVisible.value = true
}

const closeChangePasswordDialog = () => {
  isChangePasswordDialogVisible.value = false
  // 重置密码表单
  passwordData.oldPassword = ''
  passwordData.newPassword = ''
  passwordData.confirmPassword = ''
}

const changePassword = () => {
  if (!passwordData.oldPassword) {
    uni.showToast({ title: '请输入当前密码', icon: 'none' })
    return
  }
  if (!passwordData.newPassword) {
    uni.showToast({ title: '请输入新密码', icon: 'none' })
    return
  }
  if (passwordData.newPassword !== passwordData.confirmPassword) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return
  }
  // 这里可以调用API修改密码
  uni.showToast({ title: '密码修改成功', icon: 'success' })
  closeChangePasswordDialog()
}

// 生命周期
onMounted(() => {
  // 初始化表单数据
  Object.assign(formData, userInfo)
})
</script>

<style scoped>
.profile-container {
  width: 100%;
}

/* 用户信息卡片 */
.user-card {
  background-color: var(--bg-color);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--sidebar-border, rgba(0, 0, 0, 0.05));
}

.user-info-header {
  width: 100%;
  position: relative;
}

.avatar-section {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.edit-btn-container {
  position: absolute;
  top: 20px;
  right: 20px;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: all 0.3s ease;
}

.avatar:hover img {
  transform: scale(1.1);
}

.avatar-edit {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 32px;
  height: 32px;
  background-color: var(--primary-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.avatar:hover .avatar-edit {
  background-color: var(--secondary-color);
  transform: scale(1.1);
}

.icon-camera::before {
  content: "📷";
  font-size: 14px;
  color: white;
}

.edit-profile-btn {
  padding: 8px 16px;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.edit-profile-btn:hover {
  background-color: var(--secondary-color);
  transform: translateY(-1px);
}

/* 田字形布局 */
.user-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-gap: 16px;
  margin-bottom: 20px;
}

.info-item {
  background-color: rgba(0, 0, 0, 0.02);
  padding: 20px;
  border-radius: 12px;
  border: 1px solid var(--sidebar-border, rgba(0, 0, 0, 0.05));
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
}

.info-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.info-label {
  font-size: 14px;
  color: #999999;
  margin-bottom: 8px;
  display: block;
  font-weight: 500;
}

.info-value {
  font-size: 18px;
  color: var(--text-color);
  font-weight: 600;
  line-height: 1.5;
}

.info-item input {
  width: 100%;
  padding: 14px 16px;
  border: 2px solid var(--primary-color);
  border-radius: 8px;
  background-color: white;
  font-size: 18px;
  color: var(--text-color);
  font-weight: 500;
  margin-top: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.info-item input:focus {
  outline: none;
  border-color: var(--secondary-color);
  box-shadow: 0 0 0 3px rgba(148, 167, 200, 0.1);
}

/* 通用卡片样式 */
.form-card {
  background-color: var(--bg-color);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--sidebar-border, rgba(0, 0, 0, 0.05));
}

.form-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-color);
  margin-bottom: 16px;
  border-bottom: 1px solid var(--secondary-color);
  padding-bottom: 8px;
}

/* 表单样式 */
.form-item {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  color: #666666;
  margin-bottom: 8px;
}

input, textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #D8C8BE;
  border-radius: 8px;
  background-color: white;
  font-size: 14px;
  color: var(--text-color);
}

textarea {
  resize: none;
  min-height: 100px;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  margin-top: 8px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.submit-btn:hover {
  background-color: var(--secondary-color);
}

/* 联系方式显示 */
.contact-info {
  padding: 10px 14px;
  background-color: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-color);
  border: 1px solid var(--sidebar-border, rgba(0, 0, 0, 0.05));
}

/* 账户操作 */
.action-card {
  background-color: var(--bg-color);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--sidebar-border, rgba(0, 0, 0, 0.05));
}

.action-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  margin-bottom: 8px;
}

.action-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
  padding-left: 12px;
  transform: translateY(-1px);
}

.icon-lock {
  margin-right: 12px;
  font-size: 20px;
  color: var(--primary-color);
}

.icon-lock::before {
  content: "🔒";
}

.icon-close-circle {
  margin-right: 12px;
  font-size: 20px;
  color: #ff4d4f;
}

.icon-close-circle::before {
  content: "❌";
}

.action-text {
  font-size: 14px;
  font-weight: 500;
}

.action-item:nth-child(2) .action-text {
  color: #ff4d4f;
}

.action-item:nth-child(1) .action-text {
  color: var(--primary-color);
}

/* 对话框样式 */
.dialog-overlay {
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

.dialog-content {
  background-color: var(--bg-color);
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid var(--sidebar-border, rgba(0, 0, 0, 0.05));
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--sidebar-border, rgba(0, 0, 0, 0.05));
  background-color: var(--sidebar-bg, var(--bg-color));
}

.dialog-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-color);
}

.dialog-close {
  font-size: 20px;
  cursor: pointer;
  color: #999999;
  transition: color 0.3s ease;
}

.dialog-close:hover {
  color: var(--text-color);
}

.dialog-body {
  padding: 20px;
  font-size: 14px;
  color: var(--text-color);
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--sidebar-border, rgba(0, 0, 0, 0.05));
  gap: 10px;
  background-color: var(--sidebar-bg, var(--bg-color));
}

.dialog-btn {
  padding: 8px 16px;
  border: 1px solid var(--sidebar-border, rgba(0, 0, 0, 0.05));
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn {
  background-color: rgba(0, 0, 0, 0.05);
  color: var(--text-color);
}

.cancel-btn:hover {
  background-color: rgba(0, 0, 0, 0.1);
}

.confirm-btn {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

.confirm-btn:hover {
  background-color: var(--secondary-color);
  border-color: var(--secondary-color);
}
</style>