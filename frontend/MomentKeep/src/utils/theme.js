/**
 * 主题（配色方案）
 *
 * @description
 * 设置页的「主题模式」通过改写根节点上的一组 CSS 变量实现换肤。
 * 变量表放在这里而不是设置页内，是为了让应用启动时（App.vue）也能恢复上次选择，
 * 否则刷新后主题会退回浅色。
 *
 * 变量覆盖范围：页面底色、表面色（卡片/面板）、三级文字色、边框色、主/辅色、侧栏。
 * 其中"表面色/次级文字/边框"是后补的——早期只有底色与主色，组件里那些硬编码的浅色
 * （卡片 #F2EEE8、正文 #333333）不随主题变化，于是深色主题会呈现
 * "深色页面 + 浅色卡片 + 深色文字"的半深半浅观感。
 *
 * 【重要】CSS 侧刻意不声明这些变量（见 App.vue 的说明）：
 * H5 下 `:root, page` 会被编译为 `:root, uni-page-body`，而 uni-page-body 位于
 * <html> 与组件之间，一旦声明就会遮蔽这里写入运行时值，换肤只在部分位置生效。
 * 因此各使用处只提供兜底值：var(--text-color, #333333)。
 *
 * 平台限制：小程序与 App 端没有 document，无法在运行时改写 CSS 变量，
 * 会一直使用各 var() 的兜底值（即浅色），故本能力仅在 H5 生效。
 */

/** 本地存储键（与设置页共用） */
export const THEME_STORAGE_KEY = 'themeIndex'

/**
 * 三套主题的变量表
 *
 * 浅色档的取值与各处 var() 的兜底值完全一致，因此切回浅色不会有任何偏差。
 */
export const THEMES = {
  // 浅色
  0: {
    '--bg-color': '#F8F6F2',
    '--surface-color': '#F2EEE8',
    '--surface-strong': '#FFFFFF',
    '--text-color': '#333333',
    '--text-secondary': '#666666',
    '--text-muted': '#999999',
    '--border-color': '#D8C8BE',
    '--border-color-light': '#E8E4DE',
    '--primary-color': '#C2977F',
    '--secondary-color': '#94A7C8',
    '--sidebar-bg': '#F2EEE8',
    '--sidebar-border': 'rgba(0, 0, 0, 0.05)'
  },
  // 深色：暖调深棕灰，避免纯黑生硬、也无刺眼纯白
  1: {
    '--bg-color': '#26211E',
    '--surface-color': '#322C29',
    '--surface-strong': '#3A332F',
    '--text-color': '#EDE6DF',
    '--text-secondary': '#BCB2AA',
    '--text-muted': '#8E857D',
    '--border-color': '#4A423D',
    '--border-color-light': '#3E3833',
    '--primary-color': '#D9B79B',
    '--secondary-color': '#8FA2BF',
    '--sidebar-bg': '#1E1A18',
    '--sidebar-border': 'rgba(255, 255, 255, 0.08)'
  },
  // 柔和
  2: {
    '--bg-color': '#F0EAE1',
    '--surface-color': '#E9E2D8',
    '--surface-strong': '#FBF7F1',
    '--text-color': '#4A4A4A',
    '--text-secondary': '#6E6862',
    '--text-muted': '#9A938B',
    '--border-color': '#D6C9B8',
    '--border-color-light': '#E3D9CB',
    '--primary-color': '#A8846B',
    '--secondary-color': '#8A9BB0',
    '--sidebar-bg': '#E8E1D6',
    '--sidebar-border': 'rgba(0, 0, 0, 0.05)'
  }
}

/** 当前端是否支持运行时换肤（即是否存在 document） */
export const isThemeSupported = () =>
  typeof document !== 'undefined' && !!document.documentElement

/**
 * 应用主题
 *
 * @param {number|string} index 主题序号：0 浅色 / 1 深色 / 2 柔和
 * @returns {boolean} 是否真正生效
 */
export const applyTheme = (index) => {
  if (!isThemeSupported()) return false

  const theme = THEMES[Number(index)]
  if (!theme) return false

  // 逐层写入：uni-app H5 会在 body 内插入页面根容器（uni-page-body），
  // 处在 <html> 与组件之间，逐层覆盖可避免任何一层残留旧值。
  const targets = [document.documentElement, document.body]
  const pageBody = document.querySelector('uni-page-body')
  if (pageBody) targets.push(pageBody)

  for (const [key, value] of Object.entries(theme)) {
    targets.forEach((el) => {
      if (el && el.style) el.style.setProperty(key, value)
    })
  }
  return true
}

/**
 * 启动时恢复已保存的主题
 *
 * @description 在应用根部调用一次，避免"设置过深色、刷新后变回浅色"。
 * @returns {boolean} 是否真正生效
 */
export const applySavedTheme = () => {
  try {
    const saved = uni.getStorageSync(THEME_STORAGE_KEY)
    if (saved === null || saved === undefined || saved === '') return false
    return applyTheme(parseInt(saved, 10))
  } catch (error) {
    console.error('恢复主题失败:', error)
    return false
  }
}
