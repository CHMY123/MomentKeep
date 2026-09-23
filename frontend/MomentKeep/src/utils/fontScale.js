/**
 * 字号偏好（文字大小）
 *
 * @description
 * 设置页的「字体大小」通过修改根节点上的 CSS 变量 --font-scale 实现：
 * 全项目的 font-size 都写成 calc(Npx * var(--font-scale, 1))，
 * 因此只要改这一个变量，所有文字同步放大/缩小。
 *
 * 为什么不是只改 body 的 font-size：
 * 组件内大量字号是硬编码的 px，父级 font-size 对它们没有任何影响
 * （这正是此前"点了没反应"的原因）。所以这里采用"统一变量 + 每处 calc"的方案。
 *
 * 为什么只缩放字号、不整体缩放页面：
 * 整体缩放（zoom/transform）会连带改变布局尺寸，还会与 100vh、fixed 定位、
 * canvas 图表测量相互干扰；只动字号的影响面最小、行为最可预期。
 *
 * 平台限制：小程序与 App 端没有 document，无法在运行时改写 CSS 变量，
 * 故本能力仅在 H5 生效；调用方需据此给用户如实提示。
 */

/** 本地存储键（与设置页共用） */
export const FONT_SCALE_STORAGE_KEY = 'fontSizeIndex'

/**
 * 档位 -> 缩放系数
 *
 * 基准是 16px：偏大约 +15%，偏小约 -15%，差异肉眼可辨。
 */
const SCALES = {
  0: 1,
  1: 1.15,
  2: 0.85
}

/** 当前端是否支持运行时改字号（即是否存在 document） */
export const isFontScaleSupported = () =>
  typeof document !== 'undefined' && !!document.documentElement

/**
 * 应用字号系数
 *
 * @param {number|string} index 档位：0 标准 / 1 偏大 / 2 偏小
 * @returns {boolean} 是否真正生效
 */
export const applyFontScale = (index) => {
  if (!isFontScaleSupported()) return false

  const parsed = Number(index)
  const scale = SCALES[parsed] === undefined ? SCALES[0] : SCALES[parsed]

  // 写多个目标是有意为之：
  // uni-app H5 会在 body 内再插入一个页面根容器（uni-page-body），
  // 它处在 <html> 与组件之间；只要它自己声明过 --font-scale，
  // 就会遮蔽 documentElement 上的值。这里逐层覆盖，任何一层都不会漏。
  // 样式表里已不再声明该变量，故正常情况下仅第一项即生效。
  const targets = [document.documentElement, document.body]
  const pageBody = document.querySelector('uni-page-body')
  if (pageBody) targets.push(pageBody)

  targets.forEach((el) => el && el.style && el.style.setProperty('--font-scale', String(scale)))
  return true
}

/**
 * 启动时恢复已保存的字号偏好
 *
 * @description 在应用根部调用一次即可让所有页面（含登录页等不使用 Layout 的页面）生效。
 * @returns {boolean} 是否真正生效
 */
export const applySavedFontScale = () => {
  try {
    const saved = uni.getStorageSync(FONT_SCALE_STORAGE_KEY)
    if (saved === null || saved === undefined || saved === '') return false
    return applyFontScale(parseInt(saved, 10))
  } catch (error) {
    console.error('恢复字号偏好失败:', error)
    return false
  }
}
