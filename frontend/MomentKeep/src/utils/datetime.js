/**
 * 本地时间工具
 *
 * 【为什么需要这个文件】
 * 后端统一使用 `LocalDateTime`（无时区）语义，因此前端**必须提交本地墙钟时间**。
 * 若使用原生 `Date.prototype.toISOString()`，会先转成 UTC，在东八区会少 8 小时，
 * 由此引发过三个真实缺陷：
 *   1. 倒计时选「20 号零点」，后端存成 19 号 16 点；
 *   2. 专注记录的 startTime 早 8 小时，凌晨 0–8 点的记录被算到前一天，
 *      既不出现在「今日专注记录」，也不计入「今日专注时长」；
 *   3. 首页用 `toISOString().split('T')[0]` 取「今天」，每天 0–8 点实际查的是昨天。
 *
 * 统一改用本文件的方法后，写入与读取都以本地时间为准，与后端口径一致。
 *
 * @since 2026-09-19
 */

/** 补零到两位 */
const pad = value => String(value).padStart(2, '0')

/** 统一转成 Date（兼容 Date 实例、时间戳、时间字符串） */
const toDate = value => {
  if (value instanceof Date) return value
  if (value === null || value === undefined || value === '') return new Date()
  return new Date(value)
}

/**
 * 转成本地时间的 ISO 风格字符串（不带 Z 时区后缀），如 `2026-09-20T00:00:00`
 * @description 提交给后端 LocalDateTime 字段时应使用本方法，而不是 toISOString()
 * @param {Date|string|number} [value] 时间值，缺省为当前时间
 * @returns {string} `YYYY-MM-DDTHH:mm:ss`
 */
export const toLocalDateTime = (value) => {
  const date = toDate(value)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    + `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

/**
 * 转成本地日期字符串，如 `2026-09-20`
 * @description 用于「今天」这类日期比较与查询参数，避免 UTC 偏移
 * @param {Date|string|number} [value] 时间值，缺省为当前时间
 * @returns {string} `YYYY-MM-DD`
 */
export const toLocalDate = (value) => toLocalDateTime(value).slice(0, 10)

/**
 * 解析后端返回的时间字符串为 Date（按本地时区解释）
 * @description 兼容 `2026-09-20T00:00:00`、`2026-09-20 00:00:00` 与带 Z 的标准 ISO 串。
 *              iOS 低版本 Safari 无法解析用空格分隔的日期时间，这里统一归一化为 `T`。
 * @param {string|Date} value 后端返回的时间值
 * @returns {Date|null} 解析失败或空值返回 null
 */
export const parseDateTime = (value) => {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  const normalized = String(value).trim().replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

/**
 * 格式化时间为 `HH:mm`
 * @param {string|Date} value 时间值
 * @returns {string} 如 `09:05`；无法解析时返回空串
 */
export const formatTime = (value) => {
  const date = parseDateTime(value)
  if (!date) return ''
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/**
 * 格式化日期为 `YYYY-MM-DD`
 * @param {string|Date} value 时间值
 * @returns {string} 无法解析时返回空串
 */
export const formatDate = (value) => {
  const date = parseDateTime(value)
  if (!date) return ''
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** 星期文案 */
const WEEK_NAMES = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

/**
 * 格式化为「月-日 周几」，用于历史记录分组标题
 * @param {string|Date} value 时间值
 * @returns {string} 如 `09-18 周四`
 */
export const formatDateWithWeek = (value) => {
  const date = parseDateTime(value)
  if (!date) return ''
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${WEEK_NAMES[date.getDay()]}`
}

/**
 * 相对当前日期偏移若干天的本地日期串
 * @param {number} days 偏移天数（负数表示过去）
 * @returns {string} `YYYY-MM-DD`
 */
export const localDateOffset = (days) => {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return toLocalDate(date)
}
