<template>
  <view class="register-container">
    <view class="register-card">
      <!-- LOGO 区域（已修复） -->
      <view class="logo-container">
        <image src="../../static/logo.png" class="logo" mode="aspectFit"></image>
        <view class="title-wrapper">
          <text class="app-name">朝暮记</text>
          <text class="app-slogan">朝有目标，暮有记录</text>
        </view>
      </view>

      <!-- 用户名 -->
      <view class="form-item">
        <text class="label">用户名</text>
        <input
          v-model="formData.username"
          type="text"
          placeholder="请输入用户名（3-20位）"
          class="input-box"
        />
      </view>

      <!-- 密码 -->
      <view class="form-item">
        <text class="label">密码</text>
        <input
          v-model="formData.password"
          type="text"
          password
          placeholder="请输入密码（至少6位）"
          class="input-box"
        />
      </view>

      <!-- 昵称 -->
      <view class="form-item">
        <text class="label">昵称</text>
        <input
          v-model="formData.nickname"
          type="text"
          placeholder="请输入昵称"
          class="input-box"
        />
      </view>

      <!-- 手机号 -->
      <view class="form-item">
        <text class="label">手机号</text>
        <input
          v-model="formData.phone"
          type="tel"
          placeholder="请输入手机号"
          class="input-box"
        />
      </view>

      <!-- 邮箱（选填） -->
      <view class="form-item">
        <text class="label">邮箱（选填）</text>
        <input
          v-model="formData.email"
          type="text"
          placeholder="请输入邮箱"
          class="input-box"
        />
      </view>

      <!-- 协议 -->
      <view class="checkbox-item">
        <checkbox-group @change="handleAgreementChange">
          <checkbox :checked="formData.agreement" value="agreement" />
        </checkbox-group>
        <text class="checkbox-text">我已阅读并同意</text>
        <text class="link" @click="showUserAgreement">用户协议</text>
        <text class="checkbox-text">和</text>
        <text class="link" @click="showPrivacyPolicy">隐私政策</text>
      </view>

      <!-- 注册按钮 -->
      <button class="register-btn" @click="handleRegister" :disabled="loading">
        {{ loading ? '注册中...' : '注册' }}
      </button>

      <!-- 去登录 -->
      <view class="login-row">
        <text>已有账号？</text>
        <text class="link" @click="goToLogin">立即登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * MomentKeep 朝暮记 - 注册页面
 * @description 处理新用户注册，创建账户并自动登录
 * @author MomentKeep Team
 * @since 2026-04-18
 */
import { ref } from 'vue'
import { post } from '../../utils/request'

// 表单数据模型
const formData = ref({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  agreement: false
})

// 加载状态
const loading = ref(false)

/**
 * 处理用户注册
 * @description 验证表单数据，调用注册API，成功后跳转到登录页面
 */
const handleRegister = async () => {
  // 表单验证
  if (!formData.value.username) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return
  }
  if (!formData.value.password || formData.value.password.length < 6) {
    uni.showToast({ title: '密码至少6位', icon: 'none' })
    return
  }
  if (!formData.value.nickname) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return
  }
  if (!formData.value.phone) {
    uni.showToast({ title: '请输入手机号', icon: 'none' })
    return
  }
  if (!formData.value.agreement) {
    uni.showToast({ title: '请同意用户协议', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const res = await post('/user/register', formData.value)

    if (res.code === 200) {
      uni.showToast({ title: '注册成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateTo({ url: '/pages/login/login' })
        }, 1000)
    } else {
      uni.showToast({ title: res.message || '注册失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '网络异常，请重试', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 跳转到登录页面
 */
const goToLogin = () => {
  uni.navigateTo({ url: '/pages/login/login' })
}

/**
 * 显示用户协议弹窗
 */
const showUserAgreement = () => {
  uni.showModal({
    title: '用户协议',
    content: '用户协议\n\n1. 协议接受\n   您在使用朝暮记服务前，必须阅读并同意本协议。\n\n2. 账户注册\n   您需要提供真实、准确的个人信息，用于账户创建和验证。\n\n3. 用户行为规范\n   您不得使用本服务从事违法违规活动，不得干扰服务正常运行。\n\n4. 服务内容\n   我们提供日常打卡、待办管理、倒计时等功能，服务可能会根据用户需求进行调整。\n\n5. 知识产权\n   本服务的所有内容和功能均受知识产权保护，未经授权不得复制或使用。\n\n6. 免责声明\n   我们不对因网络故障、设备问题等非人为因素导致的服务中断负责。\n\n7. 协议修改\n   我们有权根据业务需要修改本协议，修改后将在平台公告。\n\n8. 终止服务\n   如您违反本协议，我们有权终止为您提供服务。',
    showCancel: false,
    confirmText: '我知道了'
  })
}

/**
 * 显示隐私政策弹窗
 */
const showPrivacyPolicy = () => {
  uni.showModal({
    title: '隐私政策',
    content: '隐私政策\n\n1. 隐私保护\n   我们重视您的隐私，承诺保护您的个人信息安全。\n\n2. 信息收集\n   我们会收集您的账户信息、使用数据等，用于提供和优化服务。\n\n3. 信息使用\n   您的信息将用于账户管理、服务提供、安全验证等必要用途。\n\n4. 信息共享\n   我们不会向第三方共享您的个人信息，除非获得您的明确授权。\n\n5. 数据安全\n   我们采取加密、访问控制等措施保护您的数据安全。\n\n6. 隐私设置\n   您可以通过设置管理您的隐私偏好和个人信息。\n\n7. 未成年人保护\n   我们特别保护未成年人的隐私，未满18岁用户需要监护人同意。\n\n8. 隐私政策更新\n   我们可能会更新隐私政策，更新后将在平台公告。',
    showCancel: false,
    confirmText: '我知道了'
  })
}

/**
 * 处理用户协议复选框变化
 */
const handleAgreementChange = (e) => {
  formData.value.agreement = e.detail.value.includes('agreement')
}
</script>

<style scoped>
/* 页面容器 */
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #F8F6F2;
  padding: 20px;
}

/* 注册卡片 */
.register-card {
  background-color: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 40px 30px;
  width: 100%;
  max-width: 400px;
}

/* LOGO 区域 */
.logo-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40px;
}

.logo {
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
}

.title-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.app-name {
  font-size: 28px;
  font-weight: 600;
  color: #C2977F;
  margin-bottom: 6px;
}

.app-slogan {
  font-size: 14px;
  color: #999;
}

/* 表单项 */
.form-item {
  margin-bottom: 20px;
}

.label {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  display: block;
}

.input-box {
  width: 100%;
  height: 50px;
  border: 2px solid #E8E1D6;
  border-radius: 8px;
  padding: 0 16px;
  font-size: 16px;
  box-sizing: border-box;
}

.input-box:focus {
  border-color: #C2977F;
}

/* 复选框 */
.checkbox-item {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.checkbox-text {
  font-size: 12px;
  color: #666;
  margin-left: 6px;
}

/* 注册按钮 */
.register-btn {
  width: 100%;
  height: 50px;
  background-color: #C2977F;
  color: #fff;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  margin-top: 24px;
  display: block;
  margin-left: auto;
  margin-right: auto;
  text-align: center;
  line-height: 50px;
}

.register-btn:disabled {
  background-color: #D8C8BE;
}

/* 登录链接 */
.login-row {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.link {
  color: #C2977F;
  margin-left: 6px;
  font-size: 12px;
}
</style>