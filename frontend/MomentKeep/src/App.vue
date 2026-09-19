<script setup>
/**
 * MomentKeep 朝暮记 - 根组件
 * @description 应用根组件，负责用户状态初始化。
 *              uni-app 的 App.vue 不承载页面结构（无需 template），只做全局初始化与全局样式。
 * @author MomentKeep Team
 * @since 2026-04-18
 */
import { onMounted } from 'vue'
import { useUserStore } from './store/user'

const userStore = useUserStore()

/**
 * 应用启动时从本地存储恢复登录态。
 * 注意：这里不引入 @dcloudio/uni-app 的 onLaunch，避免与
 * 当前锁定的 @dcloudio 版本产生额外的运行时依赖。
 */
onMounted(() => {
  try {
    userStore.initUserInfo()
  } catch (error) {
    console.error('初始化用户信息失败:', error)
  }
})
</script>

<style>
/*
 * 全局 CSS 变量。
 * H5 下 :root 生效；小程序 / App 下没有 :root，必须同时声明在 page 选择器上，
 * 否则 todo.vue 等页面里的 var(--base-font-size) 会取不到值、字号失效。
 * 同时给所有变量提供字面量兜底值，做到两端都能正常渲染。
 */
:root,
page {
  --bg-color: #F8F6F2;
  --text-color: #333333;
  --primary-color: #C2977F;
  --secondary-color: #94A7C8;
  --base-font-size: 16px;
}

page,
view,
text,
image,
navigator,
button,
input,
textarea,
picker,
picker-view {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* H5 用 body，小程序 / App 用 page 承载页面根节点样式 */
page,
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: var(--base-font-size, 16px);
  background-color: var(--bg-color, #F8F6F2);
  color: var(--text-color, #333333);
}
</style>
