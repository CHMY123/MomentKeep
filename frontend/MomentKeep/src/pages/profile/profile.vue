<template>
  <Layout>
    <view class="profile-container">
      <!-- 用户信息卡片 -->
      <view class="user-card">
        <view class="user-info-header">
          <view class="avatar-section">
            <view class="avatar" @click="uploadAvatar">
              <image :src="userInfo.avatar || 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAADu0lEQVR4AexYS0hUYRQ+5zcXFWREr03vqKgoKrKIolU5Cq0Cm2lROQZGtC2hgqQHVIs20cLKaxQ51xa1EJ0ZFawgLFDoQQ+zVesiWvRQ857+MyWKzP+4c68vmMs9zPF833l8/z/3Xu8ImOJHXsBEb2B+B/I7EHAF8l+hgAsYOD30HUi7daUp13FSCedZyq37mjH2E05dWmKBJx5VIDQBXbW1hUm37ioBNsseFYCwAwDnZIx9hDhjUthl5kJIRygCWtw7K74UFXYi4Ek5F0pTnSiFVTOXc1QkP/FQBAjyLsmmW6TZnlsQPBZry1fyAgtIurejclUPKDsoAASoCuOaCCwASBxTzGgMe4TVRpKBEEgA1dQIufrFhh5qGKE4U0PNMCLCyNAQ2tYu2SC/CtM1FC3Eua2rlqzSkgxgIAHeoLfdUN8IexisRjABKF4aJzQQCMUHA0ULCy1qAAdn9r8mIs9AU8JE5M0WM18pCRZAIAH79lX9lBdxr0Wf7BSE3h3l5b+yg3bRQAK4BZI4z5+5mCA8l0veyBwfAkamDfuRWEUDEFwZjlh6MqckFm+0ZCtpgQVw5ZKez6cB6Cn7dkbJfzl2bB0rFAFYU+NFopW7BdFWAnijakhEXQUCN0huGeeoeH7ioQgYarg3VtkVOVCx0ROwTj6kqoDoHlvG98T6SDRevKe8QilwqI6fz1AFcGNEpLLy+LuSaPxmJFZ5iC3jHzzyljHmhGmhCwhzOJtaeQHZVqnjUf1sfuNKJ5xtbOxzLBs3aCyUHUi5zuZUor5avsC3JBPO974++ibA+0QIz9nY5xhjSddpTjU6pzgn6PCcn7OA9oZbC5KNztlUwnkvC3UD0mUALEWEWaA4GJN3pLL/D75uzk27zpn0w7vzFSnGsG8B/AIiV+/EHyF6kOCC/F9ojbGLioCwRj43LlL/wMekW3+ca6uoqrgvAR0NtXNTqxc9kcWuA2ARhHZgEQLdSK9e/LjlgTPPT1lrAekHt5b9xsIXiLjTTwNfXIRd6EFn+31nuW2elYCmptoZnlfQhgjWhW0HGM2T18iKgQJo5Z6jsWx/WwmY9qPwGhfOVmAsYtyLe9rUNgpoSdzeJAtW2RQLk8M9ubepplGAQHHYVGSscAHC+IOZUYC8Z8fGakBjXaT9Jo5WQDJRv1Te53N+yJiam3FcmZlBQ9QKkHlLpU30qZ3BIMDTJo+LMhxcqOujFVAaq7wj36LQt0XjoeWURo+6OQvQJU4WTLsDk2VI3Rx5AbrVGQ9syu/AXwAAAP//8SRC4gAAAAZJREFUAwAgDj1wJiPQ2AAAAABJRU5ErkJggg=='" alt="avatar" />
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
              <!-- 小程序 / App 端的 input 事件对象是 {detail:{value}}，没有 event.target.value，
                   必须用 v-model（同文件的密码框就是这么写的）；
                   原写法在小程序端会把昵称写成 undefined，保存后资料被清空 -->
              <input v-else type="text" v-model="formData.nickname" placeholder="请输入昵称" class="input-box" />
            </view>
            <view class="info-item">
              <view class="info-label">手机号</view>
              <view v-if="!isEditMode" class="info-value">{{ userInfo.phone }}</view>
              <input v-else type="tel" v-model="formData.phone" placeholder="请输入手机号" class="input-box" />
            </view>
            <view class="info-item">
              <view class="info-label">邮箱</view>
              <view v-if="!isEditMode" class="info-value">{{ userInfo.email }}</view>
              <input v-else type="text" v-model="formData.email" placeholder="请输入邮箱" class="input-box" />
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
            <text>注销后所有数据会被删除且无法恢复。</text>
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
import { post, put, del, buildUrl } from '../../utils/request'

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
      // 使用统一的 API 地址拼接，避免硬编码服务器地址
      const apiUrl = buildUrl('/user/avatar')
      uni.uploadFile({
        url: apiUrl,
        filePath: tempFilePaths[0],
        name: 'file',
        header: {
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
          uni.showToast({ title: '上传失败，请重试' || '网络错误，头像上传失败', icon: 'none' })
        }
      })
    }
  })
}

const updateProfile = async () => {
  try {
    const response = await put('/user/profile', {
      nickname: formData.nickname,
      email: formData.email,
      phone: formData.phone
    }, {
    })

    if (response.code === 200) {
      // 更新本地存储的用户信息
      const updatedUserInfo = { ...userInfo.value, ...formData }
      userStore.setUserInfo(updatedUserInfo)
      uni.showToast({ title: '资料更新成功', icon: 'success' })
      isEditMode.value = false
    } else {
      uni.showToast({ title: response.message || '资料更新失败', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: error.message || '网络错误，资料更新失败', icon: 'none' })
  }
}

const showDeleteDialog = () => {
  isDeleteDialogVisible.value = true
}

const closeDeleteDialog = () => {
  isDeleteDialogVisible.value = false
}

const confirmDeleteAccount = async () => {
  try {
    // 后端 DeleteAccountDTO 的 confirmation 是必填（@NotNull）用于防误触，
    // 此前前端发了空对象，导致注销接口一律返回 400
    const response = await post('/user/delete', { confirmation: true }, {
    })

    if (response.code === 200) {
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
      uni.showToast({ title: response.message || '账户注销失败', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: error.message || '网络错误，账户注销失败', icon: 'none' })
  } finally {
    isDeleteDialogVisible.value = false
  }
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

const changePassword = async () => {
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
  try {
    // 后端 ChangePasswordDTO 的 confirmPassword 为必填（@NotBlank），
    // 此前请求体漏传该字段，改密接口一律返回 400
    const response = await post('/user/change-password', {
      oldPassword: passwordData.oldPassword,
      newPassword: passwordData.newPassword,
      confirmPassword: passwordData.confirmPassword
    }, {
    })

    if (response.code === 200) {
      uni.showToast({ title: '密码修改成功', icon: 'success' })
      closeChangePasswordDialog()
    } else {
      uni.showToast({ title: response.message || '密码修改失败', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: error.message || '网络错误，密码修改失败', icon: 'none' })
  }
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
  border-radius: var(--radius-md);
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--sidebar-border, var(--border-subtle));
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
  border-radius: var(--radius-circle);
  overflow: hidden;
  position: relative;
  cursor: pointer;
  box-shadow: var(--shadow-lg);
  transition: var(--transition-interactive);
}

.avatar:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-xl);
}

.avatar image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: var(--transition-interactive);
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
  border-radius: var(--radius-circle);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-xl);
  transition: var(--transition-interactive);
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
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAADzUlEQVR4AexX20sUYRT/nXHcjDKol6yHcrvbhXrrodvGWup2RSqoKCoqMyGIoof+ha5UCFGRSJldoJfcMrK2FKKHwC5QDxV0g1JaoTIydb6+s4Is+s1tZ9xF2GGOc+b3/c75nd98M8JqGOZH1kCmNzC7A9kd8PgEsq+QxwfoudzXHYiEF5dGipcJy5Acz1MnNfDNQFnZtBEg7WRSb3UqOQmuetU16psB0T3xIEBFsD2oqI9rS3RE8MVAcfGiSZrAUUeKksRcrpGp59OVgZWh0KzS4sWhgaGLnCOC8F5AtAonIblcM7AP37OGG1e2BsLhhePlR1kXCS/9qevijYacR4OCqIpAC8hFaLJmUB/ZmzVYizVZ286MpYFIeNmKEch7IZtsBlG+vKbn7NPazNo8g5WoqYFIKFQAEtdBGG/VYEjXpLYgUV+yZMkEMx1TA0I3agAaiwwf8rUcpwW0S2ZjKA2UhkKFsrDErCjdOBHKeCaVrtKAltMzS0V2gxERJhcGE0FEbkqVXLOZlAYM0hYouzgAp02fgeOnz+F2QyOqL1xOBOfHTp3FjJmpPxdBpCzWVDNpAh0q3A5bX74RJ85Uo2jOXOTmBvrpnM+eOw9sYs368n7cTSIE/VHxlQZURMAaXVe+AXsqq6DruilRz83FvqoDKFu91pTjdsEXA5ODQezaXeFYe3dFJQomTHTMtyL6YqCkdBX46VoJJa/l5Y3E2hRfpeQ+nPtiYN589998KjU88MDwbGDUqNEITpk6sK/tfWFwCvLzx9jy7AieDQQCARC5/z+vaRqsPng4PDwb6OiIg8OhXj+Nazj6gRQTzwZY98P7d3xxFanUqAR8MXCzvg6GYaj6KzHm3qi7olxzC/pi4NWLVtRfrXWsXXv5Il6/eumYb0X0xQALXK2twZmTx9DZ+ZtvlcFrZ08dB++YkpAC6JsB1m6824CKXdtx+9YNfP3ymaFEcM4Yr92L3klgfv0xMWB8TFWgIx7HxfPV2LtzG3Zs3ZQIzhnjtVT7ajA+qWrVBnr1VhXZLdbe1gYOt3WD+cIQ/+j5YBxQGojGYt+EkL+HVRUZwITAzbvNze0qaaUBJhp6935Z+IPzTIaAiGtdRqXZDKYGGhufxkkYh80K04WTIQ41tLSY/sAyNcADRh8213SJvwUyvwYhfslres4+rWuszTNYiVoa4MKmpmffow8eb4k2PRnT00NFBnqXD2WwBmuxJmvzDFZhayC5+H4s9vbeg5bYUAZrJGva5a4M2DXLxPrQGEijk6yBND5spVR2B5SPJY3gsN+B/wAAAP//FvMKTgAAAAZJREFUAwDwJMZwwuWSAQAAAABJRU5ErkJggg==);
  background-size: contain;
  background-repeat: no-repeat;
}

.edit-profile-btn {
  padding: 8px 16px;
  background-color: #C2977F;
  color: white;
  border: none;
  border-radius: var(--radius-lg);
  font-size: var(--fs-body);
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition-interactive);
}

.edit-profile-btn:hover {
  background-color: #94A7C8;
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
  border-radius: var(--radius-md);
  border: 1px solid var(--sidebar-border, var(--border-subtle));
  transition: var(--transition-interactive);
  display: flex;
  flex-direction: column;
  min-height: 100px;
  justify-content: flex-start;
}

.info-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.info-label {
  font-size: var(--fs-body);
  color: var(--text-muted, #999999);
  margin-bottom: 8px;
  display: block;
  font-weight: 500;
}

.info-value {
  font-size: var(--fs-lg);
  color: var(--text-color);
  font-weight: 600;
  line-height: 1.5;
}

.info-item .input-box {
  width: 100%;
  padding: 16px;
  border: 2px solid var(--primary-color);
  border-radius: var(--radius-sm);
  background-color: var(--surface-strong, #FFFFFF);
  font-size: var(--fs-md);
  color: var(--text-color);
  font-weight: 500;
  margin-top: 8px;
  box-shadow: var(--shadow-sm);
  transition: var(--transition-interactive);
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
  border-radius: var(--radius-md);
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--sidebar-border, var(--border-subtle));
}

.form-title {
  font-size: var(--fs-md);
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
  font-size: var(--fs-body);
  color: var(--text-secondary, #666666);
  margin-bottom: 8px;
}

input, textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color, #D8C8BE);
  border-radius: var(--radius-sm);
  background-color: var(--surface-strong, #FFFFFF);
  font-size: var(--fs-body);
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
  padding: 16px 16px;
  border: 2px solid #E8E1D6;
  border-radius: var(--radius-sm);
  background-color: var(--surface-strong, #FFFFFF);
  font-size: var(--fs-lg);
  color: var(--text-color);
  box-sizing: border-box;
}

.input-box:focus {
  border-color: var(--primary-color);
}

.textarea-box {
  width: 100%;
  padding: 16px 16px;
  border: 2px solid #E8E1D6;
  border-radius: var(--radius-sm);
  background-color: var(--surface-strong, #FFFFFF);
  font-size: var(--fs-md);
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
  background-color: #C2977F;
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--fs-md);
  font-weight: 500;
  margin-top: 8px;
  transition: var(--transition-interactive);
  cursor: pointer;
}

.submit-btn:hover {
  background-color: #94A7C8;
}

/* 联系方式显示 */
.contact-info {
  padding: 12px 16px;
  background-color: rgba(0, 0, 0, 0.02);
  border-radius: var(--radius-sm);
  font-size: var(--fs-body);
  color: var(--text-color);
  border: 1px solid var(--sidebar-border, var(--border-subtle));
}

/* 账户操作 */
.action-card {
  background-color: var(--bg-color);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--sidebar-border, var(--border-subtle));
}

.action-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  cursor: pointer;
  transition: var(--transition-interactive);
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
}

.action-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
  padding-left: 12px;
  transform: translateY(-1px);
}

.icon-lock {
  margin-right: 12px;
  font-size: var(--fs-xl);
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
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAEjUlEQVR4AexYXWgcVRT+zmyyaWzxr9WGgj+IDxoEsbWiqdldshuaXVvtS0ERxBdFEWurYLGtaasGFVT64IMVkWDx702U7KrJhs02CSIGBRV9MA/+lfRFkDalTXbn9NxpH2ZmJzt3Zme3P+Tmnsw959x7vvPNnbl75xq4xMsygQs9gcszcFnNQDp9z+qBdOLJbDr5aS6T/DmbSfyvxGqLLZtJPrF5833XRkk6kkdo+3bEsunEC3HqnDWIDhPhYUnyDgJdqcRqi42A92OV+GwunXhObJFgNxwk29t73fx/iTIRvSUJXiWJ1a+Eq0F0KJtJjKuxaLA0RGDrhg1XUIdRloR6guZBoCTFjdFUKtUWdKy9f0MEqtes+kCC3Sbiqmwy849gPmwJ8BPAJtyFcGdnjAfd5iB6aALZ/t6kAD0i4qgM/maRqjcUiuX1+WL5KUvGJu6CWblFSIw4OotCxHsH0smN0gxVQxOASe/UIDL2FMbKA6OjU8fcvvz49J/5sfIWIfGy00fy3vNOp01fC0Wgv3/TOnlp1ztgmKfzxYk3HDYPRUgMySxNuVwpl66thiLQXjVytQjmq2JjEb/KxKaDqLzQ63J9PTf5DfTyhyIAg1xgbJ6qtk16AXjZ2k7zhDxKjpfapFivV18/WygCcpvX2AOL/mupVDppt9Vrfzk1dYKZfrP3kVm4267rtkMRACgGWyHGgk3VbPIZV8dOl66lhiSgFbslnZYJtOQ21wEJMwME5g5nTF6TyyQOBBFZhZwLAVNcYpJIoBqAAJDtS/TJtvkXQXhMxFYNWVZpPxBEjBthK7KleFzFfqAvEWhjqEUgl0p1yd39XH70i0TUfe42ye8pywIqFbD+2dLRacp4GSebPplQ1Z9kU0vdJmEym0l+qDCV1U+0CJix6vUSaJvIuUokV1KAAAGwdAQqRATrT10JkAtUIVXAj1apslbpfqJFgEDPA9YzitYU+VIwjF06WLoEQm+2dJLw6iM3TQvTl8BAKnUzCPKSesE00SaYFrYPhC8Box2O5c4nnuXuWLECWx7chj2DBy3JbX0I8bhaJS239r8YVVb7dfYl4BfAy//iS/vw9LM7sUk+2pQ8s2MXdu8d9OrasC1yAitXrsK9PffXJKZsylfjaNAQOYG1XV1LplTPt+QgH0fkBI7PzS0JWc+35CAfR+QE5udPYuaH72tgv5uehPLVOBo0RE5A5fPa/n1499Db+OTIsCWq/ebQK8oVuTSFwMLCAgojX+Hjj4YtUW1lizx7CdgUAhK3ZdWfwCL+aVk2LiDmtn9dphrVl0C+VJoD43jNyGYbBNPC9sHxJaDGy859WF1bKsTv6eBpEWg/XR2Sj5a/dAJG0Udu2N/cfux1nVhaBNRBlJyX79YJGEkf5h2Fwh/ucyPP0FoE1Mivi0c/Y3PxVvn+m1F6U4R5RmEUike/0I2vTUAFLIxPz8p5/8ZKhW6HCfmw54PyaNUKAtolFoG7VWyFobB0JRCB80H521Lp9/z4xBE5Kj8QiUiskbGyOisNfDoQhsB5HhfHZZnAhZ6HS34GzgIAAP//bUazJAAAAAZJREFUAwCX06hwL+F+xwAAAABJRU5ErkJggg==);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-close-circle {
  margin-right: 12px;
  font-size: var(--fs-xl);
  color: var(--danger-color, #B5544A);
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
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAGZklEQVR4AexZa2wUVRQ+Z9qtLU9LkQgoLc/yKGBQQIW2amgQiCQmSgGVmKiBVqJRATVCmRZFg4DRpF3FGAxoseIPQBAIEGxLoPzwEWpbEaFUhRLlISDy6O5cz5nulN2de3dmt4sGw82cufee5/fN3Ll7Cxpc5+0Ggf/6Bd54A/+rN1D9waKx1d6ixZXexaVV3qJykm0BKWcd26q8+ph4km73EqpepQ8ikIuqyop+FIZWIwB1BCgEwBkkEwMyg3VsAxD7iUxDZZm+sPLDhX2hnS1mAtVlr6QSkHeFX9QCYAkgZoLLRmQGI4ol2KLVEfGlnMtlqM0tagJ7V76QUllW9LKApMME5DnKmEQS24WYQsRf5VxEZD7njjZRVAR2l+qdfCld1iDiW1Q4NdpiSn/EVMq3zJfcefXu1Xqy0k9icE1gb6k+QNOMbynHIyTX5kLMT7hk1PB35baAKwJ7PlrQ2YdiCwIOdJs4dj8cKXzGppr39C5ucjgSELR1GFeS1wPCIDcJ4+KDmHk50fiMazvlcyRQ9X7ROwBI2yH8qw0RJ9H2/KZT0YgEqr36fbRsno+UJMFzE3RIvSWSi9TGMVpi5A0MERZUlS3KlSYIKLVAL+2EMHSpIaDs3m8I3PPkfLgz/1kYNmkGOAHiMPYZNmmmGcOxaX0Hs1ohRAGwWGE01UoC/PQBUc0eNeg/bjJgQqKZqFt6JoyYOotIeMy57Mbg2adbeuvnpCV6YMD4yeSKJIqLMJhYFGYlAQOM6YoYU+1J7gCelI7m2Lp17nEbZE15XEqCwWZNeQzYx/Ln3pPSifJ04KFSImFREqBnMlWZkQwtF/+CY7U1NAq9uvZMt5Fg8MOnPAFsC/UGOHZgH7RcvBCuDpmjwIdCFEETKYE9ZfrdANgTHFpjzQ44dfSgzYuBMmBeXhb4Lj372Pw4tnH/TpvepkDo1YrJZgEpAQOACNidbRraqOu3V8DJIw02EwPOmjzT/Lh5HO5w8kg9cCxQjnCbbE6Y7pDppQQEGLfanRUaYUDDjvXSN3Fz737AEh7JT75hxxd0siZY4UbFXKAck5QArX/3BLggkeCneebXn3kWUdiHfYFiIjqGGVFABkialAD5SZ1Jr74IUN3WdXC66Selz+mmg8A+0YIPJJRiUhEIxETfIdL7U4T5rlwG4XLNK1LY1CoCJ2yeDgrUEswPNrWP+sDaY+AIGDoxH4B+BCH6JsUkJSAQpM7KmgSIjxKptw9QuliGtIzMmEioMEkJAKB7AgR+6MRpIAN/trkJzp34BcJbGpEYkkd/F6F6uUFYQ6EdDVOZU828h900xO/DVPKpCT4f0jLsB7I/jzVC7ea1ppxrtpPo3m8oDMl7FOi8BW4acZVikhJIweO7aJNudkqcMeYBAm//x4izx4/SblMOwu8Dw9cCtVvWgopE+uj7ncrQhw8nsgv0r2WOUgJ3zV7VIgA2yQIsXYInCXpljbambT0D/eGrT03gltIicf733yxVW997+FjgXG0K2QDFRpmadVICpgG1DdyrhLdDYYT+kjJ4ftoMODyOdQc2rYFwEpyDc4X7B8+1CFi0YMfgcfYcfRsto+3BuuAxAzpUtbntSfPxQAXeijN8V4BJWD92Bi2vQ5VftuWw/EJ7sbEVS6jWmikJmA5CW2D2itvJw3Ww7+O34ZuKUqjfts4BSGsSg0jUbS03YziWD3WtFvtd0Fek+cVrdstVTUQCOYX6AfqCKq6620cM6O8zf9gNDhqO4dhIbnT++WT83CV1kXwiEuDAxCStkEhETMJ+cRch6qj2i055HQnc+7R+GhJ8eSDgiFOyuNm5FtU0azskdSTA8TmzlzYLj38C7RaneH4thWtwLa7ppo4rApwo95nXGxNuutSX3oSLvwE5IibZzDW4ltto1wQ44finlp3P7l7/II2XkcT3ErA8e07xVK4RTeKoCHBinLben1NQTP8/YIyij7uCtzrWxyIcS7/4nwswRuUUFs+n8w5No8sUNQErfW7Bku9yCkumJ/r9/VHgS9EtLbFLgJjHsbkFxfmcy8obbR8zAavQuLlvNGUX6ivpCeZ17XihE5HJAxCFBHAFkdrAYo5Jxzb2ySkomZBbULKCY608sfbtJhBceOSs5ReIzE4C6CWA84jUwyzmuKDEyzb2CY5p7ziuBNoLJpb4GwRieWrxjLnu38A/AAAA///4xh67AAAABklEQVQDAOgdT38GDA39AAAAAElFTkSuQmCC);
  background-size: contain;
  background-repeat: no-repeat;
}

.icon-logout {
  margin-right: 12px;
  font-size: var(--fs-xl);
  color: var(--danger-color, #B5544A);
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
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAD4klEQVR4AeyWX2wMXRTAz7lbJV+/fJ/I9wlvHjRBoiEl8dDdnaZVVS+80AQvSLQlIuJf/Ouo+FPCg9ASDxIkEg8kHlqlm253JUU0/jwg4YEnfyNFKa29x5mKmN2dO53pdNeQvZkzc++555x7fvfembkCfvOSA/jVC5hbgdwKeJwBV1tonqZNriwv0VQyPxj832M+rt2HBCguLh5VVR7aUVUWfh7Io8cCAh0qwdHiVVV5+GFlWbDadSbDdLAFWFAemjp+bMFNANwDCBPAWZkiUJyfXxY+r2naWGcuw7eyBZAE9Yg4czjhEaF6TJ6sGY6vGx8lQGVZeDYiLnETLNWWg++uqCgqSNWPZJvHsA4nSBZb97jRYn5AjpvuxsOtrRIAEGe4DWZljzIxInGsYhs6JQABIIxAkYhyBMIoQygBlB4+6/ANQMdx/e94k74h1lR/KdakLyNytgN8AyBQHiCkw5z2QkA6G2+uP+JksX0DwC/c4qSEEdbHmnfVJuksGr4BAMTTafkRHrt+YtfcNL1J4RuA/AHcS0CPTbkBIIiEBOOdKAJF8Q3AnHX6+3wQFQTwxpwrnwYKGKyNX3LLs5gLAHNY9/XbJ7f8G2vaGY4365pKvgJMAsRGSCnIB8kAUtu9MxvTjiVZAeAvyv5PiTFvAUWUZ7PDTpDoUEr+35sIRT29BRe/N37eMw6QJ4QgotWA4HksRKiIndCngal4DmqKZVlF4EM5Im9ty27PyowDDEjgBaBTQOD5TMR/58uhGv2BmTrjAMZg4bqGrX8FPo8DkhoCltoJ229hSb8I7kvCpakdWQEwBp21uvFdqG5PZ7BWj6okD+Ap77VNhr1ZeOZfJAjnla7Re816o541AGMwO7lxVP+nH+RVBPjPbMf77yPCYPIvzPofdd8A9I+i7Zxo4Y/EBp/83gQELArV6fcH2xY33wBwbqtYki+ktSU1DdeSlckt/wAQXEhKjeBIqLahOUln0fANgBjdtxmITrN08r5vDNbu3miRb5rKNwAlKw9+CNU1rGDRwvzZReQ/R1q66QrfAKSn5kzz5wLwqfCOszmwt0IUyk+gvaezXuUKkAjcdRbCxoqor+fjl3s2Fp67lACt7dFb/FuPehmB/Y92dXX1eYkxlK8SgB1lP32u5m/BM667v4hirZHYNveO7jzsACASufnyZU9vIZHcxyCWZxGL4R5JkstbIrEw90mWjF62AMbI3d3dA62R+PaWSOfExFcslJAoVQl9keNb2junXonEzxm+2ZAhAcxJtEWjT660X4+qpDUefz1on8WbK4As5uV4qByA46nKkGFuBTI0sY7DfgMAAP//RUw+hwAAAAZJREFUAwDPwHBwdtkLtAAAAABJRU5ErkJggg==);
  background-size: contain;
  background-repeat: no-repeat;
}

.action-text {
  font-size: var(--fs-body);
  font-weight: 500;
}

.action-item:nth-child(2) .action-text {
  color: var(--danger-color, #B5544A);
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
  border-radius: var(--radius-md);
  width: 90%;
  max-width: 400px;
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--sidebar-border, var(--border-subtle));
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--sidebar-border, var(--border-subtle));
  background-color: var(--sidebar-bg, var(--bg-color));
}

.dialog-title {
  font-size: var(--fs-md);
  font-weight: 500;
  color: var(--text-color);
}

.dialog-close {
  font-size: var(--fs-xl);
  cursor: pointer;
  color: var(--text-muted, #999999);
  transition: color 0.3s ease;
}

.dialog-close:hover {
  color: var(--text-color);
}

.dialog-body {
  padding: 20px;
  font-size: var(--fs-body);
  color: var(--text-color);
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--sidebar-border, var(--border-subtle));
  gap: 12px;
  background-color: var(--sidebar-bg, var(--bg-color));
}

.dialog-btn {
  padding: 8px 16px;
  border: 1px solid var(--sidebar-border, var(--border-subtle));
  border-radius: var(--radius-xs);
  font-size: var(--fs-body);
  cursor: pointer;
  transition: var(--transition-interactive);
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
