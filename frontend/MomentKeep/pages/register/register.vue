<template>
  <div class="register-container">
    <div class="register-card">
      <div class="logo-container">
        <img src="../../static/logo.png" alt="朝暮记" class="logo" />
        <h1 class="app-name">朝暮记</h1>
        <p class="app-slogan">朝有目标，暮有记录</p>
      </div>
      
      <form @submit.prevent="handleRegister">
        <div class="form-item">
          <label for="username">用户名</label>
          <input 
            type="text" 
            id="username" 
            v-model="formData.username" 
            placeholder="请输入用户名（3-20位）" 
            required
          />
        </div>
        
        <div class="form-item">
          <label for="password">密码</label>
          <input 
            type="password" 
            id="password" 
            v-model="formData.password" 
            placeholder="请输入密码（至少6位）" 
            required
          />
        </div>
        
        <div class="form-item">
          <label for="nickname">昵称</label>
          <input 
            type="text" 
            id="nickname" 
            v-model="formData.nickname" 
            placeholder="请输入昵称" 
            required
          />
        </div>
        
        <div class="form-item">
          <label for="phone">手机号</label>
          <input 
            type="tel" 
            id="phone" 
            v-model="formData.phone" 
            placeholder="请输入手机号" 
            required
          />
        </div>
        
        <div class="form-item">
          <label for="email">邮箱（选填）</label>
          <input 
            type="email" 
            id="email" 
            v-model="formData.email" 
            placeholder="请输入邮箱"
          />
        </div>
        
        <button type="submit" class="register-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        
        <div class="login-link">
          <span>已有账号？</span>
          <a @click="goToLogin">立即登录</a>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const formData = ref({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: ''
})

const loading = ref(false)

const handleRegister = async () => {
  if (!formData.value.username || !formData.value.password || !formData.value.nickname || !formData.value.phone) {
    uni.showToast({ title: '请填写必填项', icon: 'none' })
    return
  }
  
  loading.value = true
  
  try {
    const response = await uni.request({
      url: 'http://localhost:8080/api/user/register',
      method: 'POST',
      data: formData.value
    })
    
    if (response.data.code === 200) {
      uni.showToast({ title: '注册成功', icon: 'success' })
      router.push('/pages/login/login')
    } else {
      uni.showToast({ title: response.data.message, icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: '注册失败，请稍后重试', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/pages/login/login')
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #F8F6F2;
  padding: 20px;
}

.register-card {
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
  margin-bottom: 20px;
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

.register-btn {
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
  margin-top: 20px;
}

.register-btn:hover {
  background-color: #A8846B;
}

.register-btn:disabled {
  background-color: #D8C8BE;
  cursor: not-allowed;
}

.login-link {
  text-align: center;
  font-size: 14px;
  color: #666666;
}

.login-link a {
  color: #C2977F;
  text-decoration: none;
  font-weight: 500;
  margin-left: 8px;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
