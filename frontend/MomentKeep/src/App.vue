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
  /*
   * 「记住我」未勾选时的启动处理。
   *
   * 【为什么必须放在启动、且在 initUserInfo 之前】
   * token 必须留在本地存储里——utils/request.js 的 getToken() 每次请求都从存储读取
   * 并注入 Authorization 头，所以"不记住"不能在登录成功后立刻删 token（那会让本次
   * 会话的后续请求全部 401）。正确落点是下次启动、任何请求发生之前：把持久化的
   * 登录态清掉，用户需要重新登录；登录成功后 token 会再次写入，本次会话不受影响。
   *
   * 键名 'rememberMe' 与 pages/login/login.vue 写入处是同一约定（值为 '1'/'0'）。
   */
  try {
    if (uni.getStorageSync('rememberMe') === '0') {
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
    }
  } catch (error) {
    console.error('处理「记住我」状态失败:', error)
  }

  try {
    userStore.initUserInfo()
  } catch (error) {
    console.error('初始化用户信息失败:', error)
  }

  // 在应用根部恢复字号与主题偏好：放在这里而非设置页，
  // 才能让未使用 Layout 的页面也生效，并且刷新后不会退回默认值。
  //
  // 【逐项 try/catch 的意义】
  // 这两项在 App.vue 的启动阶段执行，一旦抛错就会打断后续初始化；
  // 小程序的错误上报（如 appServiceSDKScriptError）会把它放大成整页不可用。
  // 恢复偏好属于"锦上添花"，任何一项失败都不该影响应用可用性，
  // 因此各自兜住并记录，不向上冒泡。
  try {
    applySavedFontScale()
  } catch (error) {
    console.error('恢复字号偏好失败:', error)
  }

  try {
    applySavedTheme()
  } catch (error) {
    console.error('恢复主题偏好失败:', error)
  }
})
</script>

<style>
/* 设计 token（圆角 / 阴影 / 动效 / 字号刻度）。必须最先引入，其余样式都引用它。 */
@import './styles/tokens.css';

/* 自绘图标（内联 base64，零外部依赖）。放在 token 之后，确保各页面 scoped 样式能按需覆盖它。 */
@import './styles/icons.css';
@import './styles/motion.css';

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
  font-size: var(--fs-md);
  /**
   * 行高基线。
   * 此前全局没有声明 line-height，各元素退回浏览器默认值（中英混排时约 1.4，
   * 且不同标签不一致），长段落显得拥挤、行距忽大忽小。
   * 这里给出统一基线 1.5；需要更紧凑（标题）或更疏朗（协议正文）的地方
   * 分别用 var(--lh-tight) / var(--lh-loose) 覆盖。
   */
  line-height: var(--lh-body);
  background-color: var(--bg-color, #F8F6F2);
  color: var(--text-color, #333333);
}

/*
 * 尊重系统的"减少动态效果"设置。
 * 项目里有大量 hover 位移与状态过渡，此前完全没有考虑该偏好，
 * 对前庭敏感的用户会造成不适。这里统一把动效降级为"瞬时完成"，
 * 只压缩时长，不改变任何布局与状态。
 */
/* #ifdef H5 */
/*
 * 【必须限定 H5】这段规则用了通配选择器 *、*::before、*::after，
 * 而 WXSS 不支持通配选择器，会以 "unexpected token `*`" 中断整个 app.wxss 的编译，
 * 连带 page 底色、token、动效全部失效，表现为小程序端整页白屏。
 * 小程序也不支持 prefers-reduced-motion 媒体特性（这里永远不匹配），故仅在 H5 生效。
 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
/* #endif */

/*
 * 小程序 / App 的 <button> 自带 ::after 边框，会与自定义圆角叠成"双框"。
 * 页面样式里并未设置该伪元素，因此放在全局清除不会与任何 scoped 规则冲突。
 */
button::after,
button::before {
  border: none;
  background: transparent;
}
</style>
