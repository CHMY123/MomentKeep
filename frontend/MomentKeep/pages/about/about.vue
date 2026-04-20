<template>
  <Layout>
    <div class="about-container">
      <!-- 项目信息 -->
      <div class="about-section">
        <div class="logo-section">
          <img src="https://momentkeep.s3.bitiful.net/avatars/logo.png" alt="朝暮记" class="logo" />
          <span class="app-name">朝暮记</span>
          <span class="app-slogan">朝有目标，暮有记录</span>
        </div>
        
        <div class="info-section">
          <span class="info-title">项目介绍</span>
          <p class="info-content">
            朝暮记是一款专注于日常打卡、待办管理和倒计时的个人效率工具，
            旨在帮助用户养成良好的生活习惯，提高生活质量。
            通过简洁优雅的界面设计和实用的功能，
            让用户能够轻松记录每一天的成长和进步。
          </p>
        </div>
        
        <div class="feature-section">
          <span class="feature-title">核心功能</span>
          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-icon clock-icon"></div>
              <span class="feature-text">每日打卡</span>
            </div>
            <div class="feature-item">
              <div class="feature-icon todo-icon"></div>
              <span class="feature-text">今日待办</span>
            </div>
            <div class="feature-item">
              <div class="feature-icon time-icon"></div>
              <span class="feature-text">未来倒计时</span>
            </div>
            <div class="feature-item">
              <div class="feature-icon chat-icon"></div>
              <span class="feature-text">AI助手</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 开发者信息 -->
      <div class="developer-section">
        <span class="section-title">贡献者</span>
        <div class="developer-list">
          <div class="developer-item">
            <div class="developer-avatar">
              <img src="https://momentkeep.s3.bitiful.net/avatars/developer.jpg" alt="赖文韬" @error="handleImageError" />
            </div>
            <div class="developer-info">
              <span class="developer-name">赖文韬</span>
              <span class="developer-role">全栈开发工程师</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 技术栈 -->
      <div class="tech-section">
        <span class="section-title">技术栈</span>
        <div class="tech-list">
          <div class="tech-item">
            <span class="tech-name">前端</span>
            <span class="tech-value">Vue 3 + uniapp</span>
          </div>
          <div class="tech-item">
            <span class="tech-name">后端</span>
            <span class="tech-value">Spring Boot 3.5.13</span>
          </div>
          <div class="tech-item">
            <span class="tech-name">数据库</span>
            <span class="tech-value">TiDB Cloud</span>
          </div>
          <div class="tech-item">
            <span class="tech-name">存储</span>
            <span class="tech-value">缤纷云</span>
          </div>
          <div class="tech-item">
            <span class="tech-name">AI</span>
            <span class="tech-value">DeepSeek API</span>
          </div>
          <div class="tech-item">
            <span class="tech-name">部署</span>
            <span class="tech-value">Docker + Nginx</span>
          </div>
        </div>
      </div>
      
      <!-- 版权信息 -->
      <div class="copyright-section">
        <span class="copyright">© 2026 朝暮记 MomentKeep</span>
        <span class="version">版本 1.0.0</span>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Layout from '../../components/Layout.vue'

// 处理图片加载错误
const handleImageError = (event) => {
  // 如果图片加载失败，显示默认头像
  const target = event.target
  target.style.display = 'none'
  const parent = target.parentElement
  const defaultAvatar = document.createElement('div')
  defaultAvatar.className = 'default-avatar'
  defaultAvatar.textContent = '赖'
  parent.appendChild(defaultAvatar)
}

// 生命周期
onMounted(() => {
  // 初始化数据
})
</script>

<style scoped>
.about-container {
  padding: 20px;
}

/* 项目信息 */
.about-section {
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  text-align: center;
}

.logo-section {
  margin-bottom: 24px;
}

.logo {
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
}

.app-name {
  font-size: 24px;
  font-weight: 600;
  color: #C2977F;
  display: block;
  margin-bottom: 8px;
}

.app-slogan {
  font-size: 14px;
  color: #666666;
  display: block;
}

.info-section {
  margin-bottom: 24px;
  text-align: left;
}

.info-title {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  margin-bottom: 12px;
  border-bottom: 1px solid #94A7C8;
  padding-bottom: 8px;
}

.info-content {
  font-size: 14px;
  color: #666666;
  line-height: 1.6;
}

.feature-section {
  text-align: left;
}

.feature-title {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  margin-bottom: 16px;
  border-bottom: 1px solid #94A7C8;
  padding-bottom: 8px;
}

.feature-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.feature-icon {
  margin-right: 12px;
  font-size: 24px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clock-icon::before {
  content: "";
  width: 24px;
  height: 24px;
  display: inline-block;
  background-image: url(https://img.icons8.com/color/48/000000/clock.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.todo-icon::before {
  content: "";
  width: 24px;
  height: 24px;
  display: inline-block;
  background-image: url(https://img.icons8.com/color/48/000000/todo-list.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.time-icon::before {
  content: "";
  width: 24px;
  height: 24px;
  display: inline-block;
  background-image: url(https://img.icons8.com/color/48/000000/hourglass.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.chat-icon::before {
  content: "";
  width: 24px;
  height: 24px;
  display: block;
  background-image: url(https://img.icons8.com/color/48/000000/chat.png);
  background-size: contain;
  background-repeat: no-repeat;
}

.feature-text {
  font-size: 14px;
  color: #333333;
}

/* 开发者信息 */
.developer-section {
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  margin-bottom: 16px;
  border-bottom: 1px solid #94A7C8;
  padding-bottom: 8px;
}

.developer-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.developer-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.developer-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  background-color: #F8F6F2;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.developer-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.developer-avatar .default-avatar {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #C2977F 0%, #94A7C8 100%);
  color: white;
  font-size: 24px;
  font-weight: 600;
}

.developer-info {
  flex: 1;
}

.developer-name {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  display: block;
  margin-bottom: 4px;
}

.developer-role {
  font-size: 14px;
  color: #666666;
}

/* 技术栈 */
.tech-section {
  background-color: #F2EEE8;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.tech-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.tech-item {
  padding: 12px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.tech-name {
  font-size: 14px;
  color: #666666;
  display: block;
  margin-bottom: 4px;
}

.tech-value {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
}

/* 版权信息 */
.copyright-section {
  text-align: center;
  padding: 20px;
  background-color: #F2EEE8;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.copyright {
  font-size: 14px;
  color: #666666;
  display: block;
  margin-bottom: 8px;
}

.version {
  font-size: 12px;
  color: #999999;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .feature-list {
    grid-template-columns: 1fr;
  }
  
  .tech-list {
    grid-template-columns: 1fr;
  }
  
  .developer-item {
    flex-direction: column;
    text-align: center;
    gap: 12px;
  }
  
  .developer-avatar {
    margin-right: 0;
  }
}
</style>