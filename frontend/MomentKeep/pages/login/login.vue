<template>
  <div class="login-container">
    <div class="login-card">
      <div class="logo-container">
        <img src="../../static/logo.png" alt="朝暮记" class="logo" />
        <h1 class="app-name">朝暮记</h1>
        <p class="app-slogan">朝有目标，暮有记录</p>
      </div>
      
      <form @submit.prevent="handleLogin">
        <div class="form-item">
          <label for="username">用户名</label>
          <input 
            type="text" 
            id="username" 
            v-model="formData.username" 
            placeholder="请输入用户名" 
            required
          />
        </div>
        
        <div class="form-item">
          <label for="password">密码</label>
          <input 
            type="password" 
            id="password" 
            v-model="formData.password" 
            placeholder="请输入密码" 
            required
          />
        </div>
        
        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        
        <div class="register-link">
          <span>还没有账号？</span>
          <a @click="goToRegister">立即注册</a>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()

const formData = ref({
  username: '',
  password: ''
})

const loading = ref(false)

const handleLogin = async () => {
  if (!formData.value.username || !formData.value.password) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  
  loading.value = true
  
  try {
    const response = await uni.request({
      url: 'http://localhost:8080/api/user/login',
      method: 'POST',
      data: formData.value
    })
    
    if (response.data.code === 200) {
      const { token, user } = response.data.data
      userStore.setUser(user)
      userStore.setToken(token)
      uni.showToast({ title: '登录成功', icon: 'success' })
      router.push('/pages/index/index')
    } else {
      uni.showToast({ title: response.data.message, icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: '登录失败，请稍后重试', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  router.push('/pages/register/register')
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #F8F6F2;
  padding: 20px;
}

.login-card {
  background-color: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 40px 30px;
  width: 100%;
  max-width: 400px;
}

.logo-container {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
}

.app-name {
  font-size: 28px;
  font-weight: 600;
  color: #C2977F;
  margin-bottom: 10px;
}

.app-slogan {
  font-size: 14px;
  color: #999999;
}

.form-item {
  margin-bottom: 24px;
}

.form-item label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333333;
  margin-bottom: 8px;
}

.form-item input {
  width: 100%;
  padding: 14px 16px;
  border: 2px solid #E8E1D6;
  border-radius: 8px;
  font-size: 16px;
  color: #333333;
  transition: all 0.3s ease;
}

.form-item input:focus {
  outline: none;
  border-color: #C2977F;
  box-shadow: 0 0 0 3px rgba(194, 151, 127, 0.1);
}

.login-btn {
  width: 100%;
  padding: 16px;
  background-color: #C2977F;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
}

.login-btn:hover {
  background-color: #A8846B;
}

.login-btn:disabled {
  background-color: #D8C8BE;
  cursor: not-allowed;
}

.register-link {
  text-align: center;
  font-size: 14px;
  color: #666666;
}

.register-link a {
  color: #C2977F;
  text-decoration: none;
  font-weight: 500;
  margin-left: 8px;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>
