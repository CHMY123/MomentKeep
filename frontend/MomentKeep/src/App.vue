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
import { applySavedFontScale } from './utils/fontScale'
import { applySavedTheme } from './utils/theme'

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

  // 在应用根部恢复字号与主题偏好：放在这里而非设置页，
  // 才能让未使用 Layout 的页面也生效，并且刷新后不会退回默认值。
  applySavedFontScale()
  applySavedTheme()
})
</script>

<style>
/* 自绘图标（内联 base64，零外部依赖）。放在最前面，确保各页面 scoped 样式能按需覆盖它。 */
@import './styles/icons.css';

/*
 * 全局 CSS 变量说明（这里刻意"不"声明任何主题 / 字号变量）
 *
 * 【为什么不能在这里声明】
 * 该选择器在 H5 会被编译为 `:root, uni-page-body`，而 uni-page-body 是
 * uni-app 的页面根容器，位于 <html> 与各组件之间。一旦在此声明变量，
 * 它的值就会遮蔽运行时写进 documentElement 的值——
 *   · 字号设置失效（实测：inline 已是 1.15，元素计算值仍为 16px）；
 *   · 换肤只对"未被声明的变量"生效，表现为侧栏变黑、内容区不变的半深半浅状态。
 *
 * 【正确做法】
 * 所有变量只在"使用处"提供兜底值，例如 var(--text-color, #333333)。
 * 小程序 / App 端没有 document、无法运行时修改变量，取兜底值即正常渲染；
 * H5 端既能取兜底值，又允许运行时覆盖。
 *
 * 变量清单与三套主题取值见 pages/settings/settings.vue 的 applyTheme。
 */

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
  font-size: calc(16px * var(--font-scale, 1));
  background-color: var(--bg-color, #F8F6F2);
  color: var(--text-color, #333333);
}
</style>
