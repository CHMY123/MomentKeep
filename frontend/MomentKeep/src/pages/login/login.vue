<template>
  <view class="login-container">
    <view class="login-card">
      <!-- LOGO 区域（已修复） -->
      <view class="logo-container">
        <image src="../../static/logo.png" class="logo" mode="aspectFit"></image>
        <view class="title-wrapper">
          <text class="app-name">朝暮记</text>
          <text class="app-slogan">打卡 · 待办 · 专注 · 倒计时</text>
        </view>
      </view>

      <!-- 用户名 -->
      <view class="form-item">
        <text class="label">用户名</text>
        <input
          v-model="formData.username"
          type="text"
          placeholder="请输入用户名"
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
          placeholder="请输入密码"
          class="input-box"
        />
      </view>

      <!-- 记住我 -->
      <view class="checkbox-item">
        <checkbox-group @change="handleRememberChange">
          <checkbox :checked="formData.remember" value="remember" />
        </checkbox-group>
        <text class="checkbox-text">记住我</text>
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

      <!-- 登录按钮 -->
      <button class="login-btn" @click="handleLogin" :disabled="loading">
        {{ loading ? '登录中…' : '登录' }}
      </button>

      <!-- 注册链接 -->
      <view class="register-row">
        <text>还没有账号？</text>
        <text class="link" @click="goToRegister">立即注册</text>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * MomentKeep 朝暮记 - 登录页面
 * @description 处理用户登录认证，包括表单验证、API调用和错误处理
 * @author MomentKeep Team
 * @since 2026-04-18
 */
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '../../store/user'
import { post } from '../../utils/request'

const userStore = useUserStore()

/** 「记住我」保存的账号名（只存账号，不存密码） */
const REMEMBER_USERNAME_KEY = 'rememberedUsername'

// 表单数据模型
const formData = ref({
  username: '',
  password: '',
  remember: false,
  agreement: false
})

/*
 * 进入页面时回填上次记住的账号。
 *
 * 原实现只"写"不"读"——勾选后把 token/userInfo 存了一遍（而 store 本来就会存），
 * 却没有任何地方读取，用户下次打开面对的仍是空白输入框，
 * 所以这个选项从观感上就是"设了等于没设"。
 * 现在：记住的是**账号**，并在进入页面时回填 + 保持复选框勾选状态，
 * 与用户对"记住我"的预期一致。密码不做本地保存（明文留存的风险高于便利）。
 */
onMounted(() => {
  /*
   * 已登录则直接进首页。
   *
   * 为什么这一步必不可少：登录页是 app.json 里的**首个页面**，应用启动一定先到它这里，
   * 而此前这里没有任何"已登录就跳走"的判断 —— 即使 token 完好无损，
   * 用户每次打开看到的仍是登录表单，于是"记住我"在观感上依旧是失效的。
   * （严格说这也是原实现"形同虚设"的另一半原因：写进去了，却没人在启动时认这个状态。）
   * 放在回填之前：已登录时无需再回填账号。
   */
  try {
    if (userStore.token || userStore.isLoggedIn) {
      uni.reLaunch({ url: '/pages/index/index' })
      return
    }
  } catch (error) {
    console.error('检查登录态失败:', error)
  }

  try {
    const savedUsername = uni.getStorageSync(REMEMBER_USERNAME_KEY)
    if (savedUsername) {
      formData.value.username = savedUsername
      formData.value.remember = true
    }
  } catch (error) {
    console.error('回填记住的账号失败:', error)
  }
})

// 加载状态
const loading = ref(false)

/**
 * 处理用户登录
 * @description 验证表单数据，调用登录API，成功后存储用户信息和Token
 */
const handleLogin = async () => {
  // 表单验证
  if (!formData.value.username || !formData.value.password) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  if (!formData.value.agreement) {
    uni.showToast({ title: '请同意用户协议', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const res = await post('/user/login', formData.value)

    if (res.code === 200) {
      const { token, user } = res.data
      userStore.setUser(user)
      userStore.setToken(token)
      
      /*
       * 「记住我」的语义修正
       *
       * 原实现的漏洞：store 的 setToken 会**无条件**把 token 写进本地存储，
       * 这里随后又在勾选时重复写一遍 userInfo/token —— 也就是说勾与不勾，
       * 登录态都被持久化了，这个复选框等于没有作用。
       * 而且它保存的是 token 而非账号，用户下次打开仍要重新输用户名。
       *
       * 现在的语义：
       *   勾选   → 持久化登录态 + 记住账号（下次进入自动回填并保持勾选）
       *   不勾选 → 清掉持久化的登录态，只保留本次运行的内存态，
       *            关闭应用后需重新登录（这才是"不记住"应有的行为）
       * 密码不做本地保存：明文留存的风险高于便利收益。
       */
      if (formData.value.remember) {
        uni.setStorageSync('userInfo', user)
        uni.setStorageSync('token', token)
        uni.setStorageSync(REMEMBER_USERNAME_KEY, formData.value.username)
        // 标记"允许自动登录"。键名 'rememberMe' 与 App.vue 的启动检查是同一约定，
        // 两处必须一致（App.vue 里无法 import 本文件的常量，故用字面量并在两侧注明）。
        uni.setStorageSync('rememberMe', '1')
      } else {
        /*
         * 这里**不能**顺手删掉 token：
         * utils/request.js 的 getToken() 是"每次请求都从本地存储读取"，
         * 一旦删除，本次会话后续所有请求都会丢掉 Authorization 头、直接 401。
         * 「不记住」的正确落点是"下次启动、任何请求之前"清掉持久化登录态
         * （由 App.vue 完成），这样本次会话不受影响、下次打开才需要重新登录。
         */
        uni.removeStorageSync(REMEMBER_USERNAME_KEY)
        uni.setStorageSync('rememberMe', '0')
      }
      
      uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          // 用 reLaunch 重置页面栈：否则返回键会回到登录页
          uni.reLaunch({ url: '/pages/index/index' })
        }, 1000)
    } else {
      uni.showToast({ title: res.message || '登录失败', icon: 'none' })
    }
  } catch (error) {
    console.error('登录失败:', error)
    // 业务错误（如"用户名或密码错误"）在 request.js 中是以 Error(message) 抛出的，
    // 必须展示 error.message，否则用户只会看到"网络错误"而无法判断原因
    uni.showToast({ title: error.message || '网络错误，请稍后重试', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 跳转到注册页面
 */
const goToRegister = () => {
  uni.navigateTo({ url: '/pages/register/register' })
}

/**
 * 查看用户协议 / 隐私政策
 *
 * @description 改为跳转独立页面。原先用 showModal 弹一段骨架文案，
 * 既不满足微信"隐私政策须可在应用内独立查看"的审核要求，内容也不具体。
 * 现由 pages/agreement 与 pages/privacy 两个完整页面承载。
 */
const showUserAgreement = () => {
  uni.navigateTo({ url: '/pages/agreement/agreement' })
}

const showPrivacyPolicy = () => {
  uni.navigateTo({ url: '/pages/privacy/privacy' })
}

/**
 * 处理"记住我"复选框变化
 */
const handleRememberChange = (e) => {
  formData.value.remember = e.detail.value.includes('remember')
}

/**
 * 处理用户协议复选框变化
 */
const handleAgreementChange = (e) => {
  formData.value.agreement = e.detail.value.includes('agreement')
}
</script>

<style scoped>
/* 整体页面 */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #F8F6F2;
  padding: 20px;
}

/* 登录卡片 */
.login-card {
  background-color: var(--surface-strong, #FFFFFF);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: 40px 32px;
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
  font-size: var(--fs-3xl);
  font-weight: 600;
  color: #C2977F;
  margin-bottom: 8px;
}

.app-slogan {
  font-size: var(--fs-body);
  color: #999;
}

/* 表单项 */
.form-item {
  margin-bottom: 24px;
}

.label {
  font-size: var(--fs-body);
  color: #333;
  margin-bottom: 8px;
  display: block;
}

.input-box {
  width: 100%;
  height: 50px;
  border: 2px solid #E8E1D6;
  border-radius: var(--radius-sm);
  padding: 0 16px;
  font-size: var(--fs-md);
  box-sizing: border-box;
}

.input-box:focus {
  border-color: #C2977F;
}

/* 复选框 */
.checkbox-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.checkbox-text {
  font-size: var(--fs-xs);
  color: #666;
  margin-left: 8px;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 50px;
  background-color: #C2977F;
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: var(--fs-md);
  font-weight: 500;
  margin-top: 24px;
  display: block;
  margin-left: auto;
  margin-right: auto;
  text-align: center;
  line-height: 50px;
}

.login-btn:disabled {
  background-color: #D8C8BE;
}

/* 注册行 */
.register-row {
  text-align: center;
  margin-top: 20px;
  font-size: var(--fs-body);
  color: #666;
}

.link {
  color: #C2977F;
  margin-left: 8px;
  font-size: var(--fs-xs);
}
</style>