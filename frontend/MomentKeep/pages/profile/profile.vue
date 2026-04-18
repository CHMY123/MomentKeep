<template>
  <Layout>
    <view class="profile-container">
      <!-- 用户信息卡片 -->
      <view class="user-card">
        <view class="user-info-header">
          <view class="avatar-section">
            <view class="avatar" @click="uploadAvatar">
              <image :src="userInfo.avatar || 'https://img.icons8.com/ios-filled/50/000000/user.png'" alt="avatar" />
              <view class="avatar-edit">
                <view class="icon-camera"></view>
              </view>
            </view>
          </view>
          <view class="edit-btn-container">
            <button class="edit-profile-btn" @click="toggleEditMode">
              {{ isEditMode ? '取消' : '变更资料' }}
            </button>
          </view>
          <view class="user-info-grid">
            <view class="info-item">
              <view class="info-label">用户名</view>
              <view class="info-value">{{ userInfo.username }}</view>
            </view>
            <view class="info-item">
              <view class="info-label">昵称</view>
              <view v-if="!isEditMode" class="info-value">{{ userInfo.nickname }}</view>
              <input v-else type="text" :value="formData.nickname" @input="formData.nickname = $event.target.value" placeholder="请输入昵称" class="input-box" />
            </view>
            <view class="info-item">
              <view class="info-label">手机号</view>
              <view v-if="!isEditMode" class="info-value">{{ userInfo.phone }}</view>
              <input v-else type="tel" :value="formData.phone" @input="formData.phone = $event.target.value" placeholder="请输入手机号" class="input-box" />
            </view>
            <view class="info-item">
              <view class="info-label">邮箱</view>
              <view v-if="!isEditMode" class="info-value">{{ userInfo.email }}</view>
              <input v-else type="email" :value="formData.email" @input="formData.email = $event.target.value" placeholder="请输入邮箱" class="input-box" />
            </view>
          </view>
          <button v-if="isEditMode" class="submit-btn" @click="updateProfile">保存修改</button>
        </view>
      </view>
      
      <!-- 账户操作 -->
      <view class="action-card">
        <view class="form-title">账户操作</view>
        
        <view class="action-item" @click="showChangePasswordDialog">
          <view class="icon-lock"></view>
          <view class="action-text">修改密码</view>
        </view>
        
        <view class="action-item" @click="logout">
          <view class="icon-logout"></view>
          <view class="action-text">退出登录</view>
        </view>
        
        <view class="action-item" @click="showDeleteDialog">
          <view class="icon-close-circle"></view>
          <view class="action-text">注销账户</view>
        </view>
      </view>
      
      <!-- 注销确认对话框 -->
      <view v-if="isDeleteDialogVisible" class="dialog-overlay" @click="closeDeleteDialog">
        <view class="dialog-content" @click.stop>
          <view class="dialog-header">
            <view class="dialog-title">注销账户</view>
            <view class="dialog-close" @click="closeDeleteDialog">×</view>
          </view>
          <view class="dialog-body">
            <text>您确定要注销账户吗？注销后所有数据将被删除，且无法恢复。</text>
          </view>
          <view class="dialog-footer">
            <button class="dialog-btn cancel-btn" @click="closeDeleteDialog">取消</button>
            <button class="dialog-btn confirm-btn" @click="confirmDeleteAccount">确定注销</button>
          </view>
        </view>
      </view>
      
      <!-- 修改密码对话框 -->
      <view v-if="isChangePasswordDialogVisible" class="dialog-overlay" @click="closeChangePasswordDialog">
        <view class="dialog-content" @click.stop>
          <view class="dialog-header">
            <view class="dialog-title">修改密码</view>
            <view class="dialog-close" @click="closeChangePasswordDialog">×</view>
          </view>
          <view class="dialog-body">
            <view class="form-item">
              <view class="form-label">当前密码</view>
              <input type="password" v-model="passwordData.oldPassword" placeholder="请输入当前密码" class="input-box" />
            </view>
            <view class="form-item">
              <view class="form-label">新密码</view>
              <input type="password" v-model="passwordData.newPassword" placeholder="请输入新密码" class="input-box" />
            </view>
            <view class="form-item">
              <view class="form-label">确认新密码</view>
              <input type="password" v-model="passwordData.confirmPassword" placeholder="请确认新密码" class="input-box" />
            </view>
          </view>
          <view class="dialog-footer">
            <button class="dialog-btn cancel-btn" @click="closeChangePasswordDialog">取消</button>
            <button class="dialog-btn confirm-btn" @click="changePassword">确定修改</button>
          </view>
        </view>
      </view>
    </view>
  </Layout>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import Layout from '../../components/Layout.vue'
import { useUserStore } from '../../store/user'

const isDeleteDialogVisible = ref(false)
const isChangePasswordDialogVisible = ref(false)
const isEditMode = ref(false)

// 用户状态管理
const userStore = useUserStore()
const userInfo = computed(() => userStore.getUserInfo)

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

// 方法
const toggleEditMode = () => {
  isEditMode.value = !isEditMode.value
  if (isEditMode.value) {
    // 进入编辑模式时，初始化表单数据
    formData.nickname = userInfo.value.nickname || ''
    formData.email = userInfo.value.email || ''
    formData.phone = userInfo.value.phone || ''
  }
}

const uploadAvatar = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      // 上传图片到服务器
      const tempFilePaths = res.tempFilePaths
      uni.uploadFile({
        url: '/api/user/avatar',
        filePath: tempFilePaths[0],
        name: 'file',
        header: {
          'Authorization': `Bearer ${userStore.getToken}`
        },
        success: (uploadRes) => {
          try {
            const data = JSON.parse(uploadRes.data)
            if (data.code === 200) {
              // 更新用户信息
              const updatedUserInfo = { ...userInfo.value, avatar: data.data }
              userStore.setUserInfo(updatedUserInfo)
              uni.showToast({ title: '头像上传成功', icon: 'success' })
            } else {
              uni.showToast({ title: data.message || '头像上传失败', icon: 'none' })
            }
          } catch (error) {
            uni.showToast({ title: '头像上传失败', icon: 'none' })
          }
        },
        fail: () => {
          uni.showToast({ title: '网络错误，头像上传失败', icon: 'none' })
        }
      })
    }
  })
}

const updateProfile = () => {
  // 调用API更新用户资料
  uni.request({
    url: '/api/user/update',
    method: 'POST',
    header: {
      'Authorization': `Bearer ${userStore.getToken}`
    },
    data: {
      nickname: formData.nickname,
      email: formData.email,
      phone: formData.phone
    },
    success: (res) => {
      if (res.data.code === 200) {
        // 更新本地存储的用户信息
        const updatedUserInfo = { ...userInfo.value, ...formData }
        userStore.setUserInfo(updatedUserInfo)
        uni.showToast({ title: '资料更新成功', icon: 'success' })
        isEditMode.value = false
      } else {
        uni.showToast({ title: res.data.message || '资料更新失败', icon: 'none' })
      }
    },
    fail: () => {
      uni.showToast({ title: '网络错误，资料更新失败', icon: 'none' })
    }
  })
}

const showDeleteDialog = () => {
  isDeleteDialogVisible.value = true
}

const closeDeleteDialog = () => {
  isDeleteDialogVisible.value = false
}

const confirmDeleteAccount = () => {
  // 调用API注销账户
  uni.request({
    url: '/api/user/delete',
    method: 'POST',
    header: {
      'Authorization': `Bearer ${userStore.getToken}`
    },
    success: (res) => {
      if (res.data.code === 200) {
        // 清除用户信息
        userStore.logout()
        uni.showToast({ title: '账户注销成功', icon: 'success' })
        // 跳转到登录页面
        setTimeout(() => {
          uni.navigateTo({
            url: '/pages/login/login'
          })
        }, 1500)
      } else {
        uni.showToast({ title: res.data.message || '账户注销失败', icon: 'none' })
      }
    },
    fail: () => {
      uni.showToast({ title: '网络错误，账户注销失败', icon: 'none' })
    }
  })
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
  // 调用API修改密码
  uni.request({
    url: '/api/user/change-password',
    method: 'POST',
    header: {
      'Authorization': `Bearer ${userStore.getToken}`
    },
    data: {
      oldPassword: passwordData.oldPassword,
      newPassword: passwordData.newPassword
    },
    success: (res) => {
      if (res.data.code === 200) {
        uni.showToast({ title: '密码修改成功', icon: 'success' })
        closeChangePasswordDialog()
      } else {
        uni.showToast({ title: res.data.message || '密码修改失败', icon: 'none' })
      }
    },
    fail: () => {
      uni.showToast({ title: '网络错误，密码修改失败', icon: 'none' })
    }
  })
}

const logout = () => {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        // 调用用户存储的退出方法
        userStore.logout()
        uni.showToast({ title: '退出登录成功', icon: 'success' })
        // 跳转到登录页面
        setTimeout(() => {
          uni.navigateTo({
            url: '/pages/login/login'
          })
        }, 1000)
      }
    }
  })
}

// 生命周期
onMounted(() => {
  // 初始化用户信息
  userStore.initUserInfo()
  // 初始化表单数据
  formData.nickname = userInfo.value.nickname || ''
  formData.email = userInfo.value.email || ''
  formData.phone = userInfo.value.phone || ''
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

.avatar image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: all 0.3s ease;
}

.avatar:hover image {
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
  content: "";
  width: 14px;
  height: 14px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/ffffff/camera.png);
  background-size: contain;
  background-repeat: no-repeat;
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
  min-height: 100px;
  justify-content: flex-start;
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

.info-item .input-box {
  width: 100%;
  padding: 16px;
  border: 2px solid var(--primary-color);
  border-radius: 8px;
  background-color: white;
  font-size: 16px;
  color: var(--text-color);
  font-weight: 500;
  margin-top: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  pointer-events: auto;
  cursor: text;
  box-sizing: border-box;
  line-height: 1.6;
  min-height: 60px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.info-item .input-box:focus {
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
  box-sizing: border-box;
  line-height: 1.6;
  min-height: 60px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

textarea {
  resize: none;
  min-height: 100px;
}

/* uni-app 输入框样式 */
.input-box {
  width: 100%;
  padding: 14px 16px;
  border: 2px solid #E8E1D6;
  border-radius: 8px;
  background-color: white;
  font-size: 18px;
  color: var(--text-color);
  box-sizing: border-box;
}

.input-box:focus {
  border-color: var(--primary-color);
}

.textarea-box {
  width: 100%;
  padding: 14px 16px;
  border: 2px solid #E8E1D6;
  border-radius: 8px;
  background-color: white;
  font-size: 16px;
  color: var(--text-color);
  box-sizing: border-box;
  min-height: 120px;
  resize: none;
}

.textarea-box:focus {
  border-color: var(--primary-color);
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
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  min-height: 20px;
}

.icon-lock::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/000000/lock--v1.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-close-circle {
  margin-right: 12px;
  font-size: 20px;
  color: #ff4d4f;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  min-height: 20px;
}

.icon-close-circle::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/ff4d4f/close-window.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-logout {
  margin-right: 12px;
  font-size: 20px;
  color: #ff4d4f;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  min-height: 20px;
}

.icon-logout::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url(https://img.icons8.com/ios-filled/50/ff4d4f/logout-rounded-left.png);
  background-size: contain;
  background-repeat: no-repeat;
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
