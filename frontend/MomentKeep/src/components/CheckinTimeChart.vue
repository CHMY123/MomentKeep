<template>
  <view class="chart-card">
    <!-- 图例用 view 绘制（不放进 canvas）：窄屏可自动换行，且不占用绘图区 -->
    <view v-if="total > 0" class="chart-legend">
      <view class="legend-item">
        <view class="legend-dot legend-dot-bar"></view>
        <text class="legend-text">{{ scopeLabel }}次数（左轴）</text>
      </view>
      <view v-if="showRate" class="legend-item">
        <view class="legend-dot legend-dot-line"></view>
        <text class="legend-text">打卡率（右轴）</text>
      </view>
    </view>

    <!-- 容器始终存在，便于测量宽度与绑定 ResizeObserver -->
    <view class="chart-wrap" :style="{ height: canvasStyleHeight }">
      <!--
        canvas 只在有数据时挂载：无数据时把它从 DOM 上移除，而不是"擦除画布"。
        uni 的非 2d 画布由指令队列驱动，clearRect 与后续帧之间存在时序问题——
        实测从有数据口径切到无数据口径时，旧图仍会留在画布上。
        移除元素可以从结构上排除这一类残留，不必依赖清空是否生效。
      -->
      <canvas
        v-if="total > 0"
        id="checkinTimeChart"
        canvas-id="checkinTimeChart"
        class="chart-canvas"
        :style="{ width: canvasStyleWidth, height: canvasStyleHeight }"
        @touchstart="onPointerDown"
        @touchmove="onPointerMove"
        @touchend="onPointerOut"
      ></canvas>
      <view v-else class="chart-empty">
        <view class="empty-graphic-chart"></view>
        <text>这段时间还没有打卡记录</text>
      </view>
    </view>

    <!--
      数值提示条：固定在图表下方，而不是跟随手指的气泡。
      气泡方案在窄屏容易贴边被裁切（正是此前"图表溢出屏幕"的同一类问题），
      固定提示条既能完整显示，又能同时给出「次数」和「打卡率」两个不同维度的值。
    -->
    <view class="chart-tip">
      <template v-if="activeIndex >= 0">
        <text class="tip-range">{{ bucketLabels[activeIndex] }}</text>
        <text class="tip-value">该时段 {{ counts[activeIndex] }} 次</text>
        <text v-if="showRate" class="tip-value">打卡率 {{ rateSeries[activeIndex] }}%</text>
      </template>
      <text v-else class="tip-hint">
        {{ total > 0 ? (showRate ? '点击柱状图或折线节点，可查看该时段的次数与打卡率' : '点击柱状图，可查看该时段的具体次数') : '暂无打卡数据' }}
      </text>
    </view>
  </view>
</template>

<script setup>
/**
 * 历史打卡时间分布图表（柱状 + 折线 双 Y 轴组合图）
 *
 * 设计要点：
 * 1. 尺寸：进入页面后测量容器真实 px 宽度，canvas 宽度与之严格相等 → 结构上不可能溢出屏幕；
 * 2. 双轴：左轴 = 各时段次数（柱），右轴 = 打卡率（折线，0~100%），两轴单位不同、各自取量程；
 * 3. 窄屏：按"每个标签所需宽度"动态抽稀横轴标签（置空而非缩放），避免文字重叠；
 * 4. 交互：点击/长按/悬停时通过 chart.getCurrentDataIndex 定位数据点，在下方固定提示条展示数值；
 * 5. 重绘：uCharts 会就地修改配置对象，因此每次重绘都重建 opts；容器尺寸变化时防抖重绘。
 */
import { ref, computed, onMounted, onUnmounted, nextTick, getCurrentInstance, watch } from 'vue'
import uCharts from '@qiun/ucharts'

const props = defineProps({
  /** 12 个 2 小时时段：[{ label: '00-02', count: 3 }] */
  buckets: {
    type: Array,
    default: () => []
  },
  /**
   * 打卡率（%）：与 buckets 等长的 12 个值，供折线使用
   *
   * @description 打卡率 = 该时段有打卡的天数 ÷ 区间内活跃天数。
   * 它与柱状的「次数」是两个互相独立的维度：次数说明"打得多不多"，
   * 打卡率说明"坚持得久不久"（一天只在 7 点打一次，次数低但坚持度可以是 100%）。
   * 空数组表示不提供，此时图表退化为纯柱状图，不会画一条全 0 的线。
   */
  rate: {
    type: Array,
    default: () => []
  },
  /** 当前统计口径名称（全部 / 早起 / 睡眠 / 用餐 / 运动），用于图例与提示文案 */
  scopeLabel: {
    type: String,
    default: '全部'
  }
})

const instance = getCurrentInstance()

const canvasStyleWidth = ref('100%')
const canvasStyleHeight = ref('200px')
/** 当前选中的数据点下标，-1 表示未选中 */
const activeIndex = ref(-1)

let chart = null
let resizeTimer = null
let hideTipTimer = null

const CANVAS_ID = 'checkinTimeChart'

/** 桌面端断点：>=768px 视为桌面 */
const BREAKPOINT = 768

/** 每个纵轴分几格（左右轴共用，保证网格线对齐） */
const SPLIT_NUMBER = 4

const total = computed(() =>
  props.buckets.reduce((sum, item) => sum + (Number(item.count) || 0), 0)
)

/** 完整时段标签（横轴可能因抽稀而留空，提示条里始终用完整标签） */
const bucketLabels = computed(() => props.buckets.map(item => item.label || ''))

/** 柱状：各时段次数 */
const counts = computed(() => props.buckets.map(item => Number(item.count) || 0))

/** 取"好看"的每格步长：1 / 2 / 2.5 / 5 / 10 的 10^n 倍 */
const niceStep = raw => {
  const magnitude = Math.pow(10, Math.floor(Math.log10(raw)))
  for (const multiple of [1, 2, 2.5, 5, 10]) {
    const candidate = multiple * magnitude
    if (candidate >= raw) return candidate
  }
  return 10 * magnitude
}

/**
 * 计算纵轴上限。
 *
 * 关键：先定"每格步长"再乘格数，保证刻度恒为整数。
 * 若反过来先定上限（如 5）再除以 4 格，会得到 1.25 / 2.5 / 3.75 这种小数刻度，
 * 对"次数"这类离散数据是错误表达。
 *
 * @param rawMax  数据的实际最大值
 * @param minTop  最小上限（避免只有 1 次打卡时柱子顶满全屏）
 */
const niceMax = (rawMax, minTop = SPLIT_NUMBER) => {
  if (!rawMax || rawMax <= 0) return minTop
  const step = niceStep(rawMax / SPLIT_NUMBER)
  return Math.max(Math.ceil(step) * SPLIT_NUMBER, minTop)
}

/** 柱与折线各自成轴，故分别给量程：次数取 niceMax，打卡率固定 0~100 */
const axisMax = computed(() => niceMax(Math.max(...counts.value, 0)))

/** 折线数据：打卡率（%） */
const rateSeries = computed(() => props.rate.map(value => Number(value) || 0))

/**
 * 是否绘制打卡率折线
 *
 * @description 三个条件缺一不可：有数据、长度与柱状对齐、至少有一个非零值。
 * 否则（例如某类型在区间内一次都没打卡）画一条贴底的零线只会造成误解。
 */
const showRate = computed(() =>
  counts.value.length > 0 &&
  rateSeries.value.length === counts.value.length &&
  rateSeries.value.some(value => value > 0)
)

/** 当前窗口宽度（用于桌面/移动分支与标签抽稀） */
const getWindowWidth = () => {
  try {
    const info = uni.getSystemInfoSync()
    return info.windowWidth || 375
  } catch (e) {
    return 375
  }
}

/**
 * 按可用宽度动态抽稀横轴标签。
 * uCharts 的 xAxis.labelCount 是"滚动窗口长度"，不是标签抽稀，
 * 因此这里直接把不需要显示的标签置为空字符串，效果确定且不会重叠。
 */
const buildCategories = (width, narrow) => {
  const labels = bucketLabels.value
  const perLabelWidth = narrow ? 30 : 34
  const step = Math.max(1, Math.ceil((labels.length * perLabelWidth) / Math.max(width, 1)))
  return labels.map((label, index) => (index % step === 0 ? label : ''))
}

/** 测量图表容器真实宽度（px） */
const measureWidth = () =>
  new Promise(resolve => {
    uni
      .createSelectorQuery()
      .in(instance.proxy)
      .select('.chart-wrap')
      .boundingClientRect(rect => resolve(rect && rect.width ? Math.floor(rect.width) : 0))
      .exec()
  })

/** 构建 uCharts 配置（每次重绘都重建，避免库内部改写配置带来的脏状态） */
const buildOptions = (width, height, narrow) => ({
  type: 'mix',
  context: null, // 由调用处注入
  width,
  height,
  pixelRatio: 1, // 非 2d 模式固定为 1
  background: 'transparent',
  animation: true,
  rotate: false,
  categories: buildCategories(width, narrow),
  series: [
    {
      name: '次数',
      type: 'column',
      data: counts.value,
      color: '#C2977F',
      // 【必须显式声明】uCharts 的 calYAxisData 是按 series.index 把数据分到各根 Y 轴的，
      // 不是按 yAxisIndex（见 u-charts.js：if (series[j].index == i)）。
      // 声明后柱状归左轴、折线归右轴，各自按自己的量程绘制。
      index: 0
    },
    // 折线 = 打卡率（%）——与柱状的「次数」是两个互相独立的维度：
    // 次数看"打得多不多"，打卡率看"坚持得久不久"（一天只在 7 点打一次，次数低但坚持度可到 100%）。
    // 没有可用的打卡率数据时整条折线不注册，图表自然退化为纯柱状图，而不是画一条贴底的零线。
    ...(showRate.value
      ? [{
          name: '打卡率',
          type: 'line',
          data: rateSeries.value,
          color: '#94A7C8',
          // 挂到第 2 根 Y 轴（百分比），量程 0~100 由下面 yAxis.data[1] 指定
          index: 1,
          width: 2,
          // 【关键】mix 组合图的曲线开关在这里，不在 extra.line.type。
          // uCharts 源码中两条绘制路径取值方式不同：
          //   drawLineDataPoints（type:'line'）  -> 读 lineOption.type（即 extra.line.type）
          //   drawMixDataPoints（type:'mix'）    -> 只读 eachSeries.style
          // 因此组合图必须把 style 写在折线 series 自身，否则画出来始终是折线。
          style: 'curve'
        }]
      : [])
  ],
  xAxis: {
    disableGrid: true,
    fontSize: 10,
    fontColor: '#999999',
    axisLineColor: '#E8E4DE',
    itemCount: props.buckets.length,
    labelCount: props.buckets.length,
    scrollShow: false,
    rotateLabel: false
  },
  // 注意：双 Y 轴必须是 { data: [...] } 结构，写成数组会静默失效。
  // 这里两根轴的单位不同（次 / %），属于双轴的正确用法；
  // 与早期"柱=次数、线=累计次数"那种同量纲却硬拆两轴的写法不同。
  yAxis: {
    disabled: false,
    splitNumber: SPLIT_NUMBER,
    gridType: 'dash',
    dashLength: 4,
    gridColor: '#EFEBE6',
    fontSize: 10,
    // 轴标题默认不绘制（uCharts 仅在 showTitle 为 true 时画）。
    // 竖排轴标题在窄屏会挤占绘图区宽度，语义改由图例（左轴/右轴）+ 刻度染色表达。
    showTitle: false,
    data: [
      {
        min: 0,
        max: axisMax.value,
        fontColor: '#C2977F',
        axisLineColor: '#E3D3C7',
        // 兜底：即使量程异常，刻度也只显示整数（次数不接受小数）
        format: value => String(Math.round(value))
      },
      ...(showRate.value
        ? [{
            min: 0,
            max: 100,
            // 不声明 position 时 uCharts 会把两根轴的刻度都画在左侧
            position: 'right',
            fontColor: '#94A7C8',
            axisLineColor: '#C7D2E2',
            format: value => Math.round(value) + '%'
          }]
        : [])
    ]
  },
  // 图例交给 view 渲染，canvas 内不再画图例
  legend: { show: false },
  dataLabel: false,
  dataPointShape: true,
  padding: [12, 4, 0, 4],
  fontSize: 10,
  extra: {
    // 兜底：若日后把图表类型换成纯 line/area，这两个键才会生效；
    // 当前 mix 类型的曲线由上面 series[1].style === 'curve' 决定。
    line: { type: 'curve' },
    mix: {
      // uCharts 用 column.width 对柱宽取上限（源码：item.width = Math.min(item.width, width)），
      // 不设时柱宽会撑满整个分类槽位，相邻柱就会黏在一起。
      // 窄屏槽位本就窄，给 14px；宽屏给 22px，留出明显间隔。
      column: { seriesGap: 2, width: narrow ? 14 : 22 },
      line: { type: 'curve' }
    },
    tooltip: {
      showBox: true,
      showArrow: false,
      fontSize: 10,
      bgColor: '#000000',
      bgOpacity: 0.7,
      fontColor: '#FFFFFF'
    }
  }
})

const renderChart = async () => {
  // 等待 DOM/布局稳定后再测量，避免拿到 0 或旧宽度
  await nextTick()

  const width = await measureWidth()
  if (!width) return

  const narrow = getWindowWidth() < BREAKPOINT
  const height = narrow ? 200 : 300

  // 先定尺寸：即使当前没有数据、canvas 不挂载，容器也要保持高度，空态文案才能垂直居中
  canvasStyleWidth.value = width + 'px'
  canvasStyleHeight.value = height + 'px'

  activeIndex.value = -1

  // 无数据：不绘制任何东西。
  // 模板里 canvas 是 v-if="total > 0"，此处 return 后画布随之下线，
  // 因此不需要（也不应该）在这里 clearRect —— 那时元素已经不存在了。
  // 之前正是"先 clearRect 再 return"，实测切到无数据口径后旧图仍留在画布上；
  // 现在改为从 DOM 上移除元素，结构上排除残留。
  if (total.value === 0) {
    chart = null
    return
  }

  // 先无条件清空画布：
  // uCharts 的 background 设为 transparent，它不会自行擦除上一帧，
  // 若不清空，数据口径之间切换（柱高、折线形状都不同）时会叠着上一帧。
  const context = uni.createCanvasContext(CANVAS_ID, instance.proxy)
  context.clearRect(0, 0, width, height)

  const options = buildOptions(width, height, narrow)
  options.context = context

  // 每次都新建实例：uCharts 会复用并改写传入的 opts，复用实例在重绘时容易出现脏状态
  chart = new uCharts(options)

  // 图表就绪后确保事件已绑定（此时容器必然已存在）
  bindPointer()
}

const scheduleRender = () => {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    resizeTimer = null
    renderChart()
  }, 160)
}

/* ------------------------------ 数值提示交互 ------------------------------ */

/** 无操作后自动隐藏提示条的时间 */
const TIP_AUTO_HIDE_MS = 4000

/** 事件绑定目标（图表容器，而非内层 canvas） */
let pointerEl = null
/** 是否已绑定，避免重复注册监听 */
let pointerBound = false

/**
 * 解析指针事件绑定目标。
 *
 * @description 绑在 .chart-wrap（uni-view，稳定存在）而不是内层 canvas：
 * uni-h5 的内层 canvas 在尺寸变化时可能被重建，而 canvas 上的鼠标事件会冒泡到容器，
 * 因此绑容器既能收到事件，又不会因为节点重建而丢失监听。
 */
const resolvePointerEl = () => {
  if (pointerEl) return pointerEl
  // #ifdef H5
  if (typeof document !== 'undefined') {
    pointerEl = document.querySelector('.chart-wrap')
  }
  // #endif
  return pointerEl
}

const scheduleHideTip = () => {
  if (hideTipTimer) clearTimeout(hideTipTimer)
  hideTipTimer = setTimeout(() => {
    activeIndex.value = -1
    hideTipTimer = null
  }, TIP_AUTO_HIDE_MS)
}

/**
 * 构造 uCharts 需要的事件对象（关键：必须给出"相对画布"的 x/y）
 *
 * 【为什么不能直接把鼠标事件丢给 uCharts】
 * uCharts 内部的 getTouches 有两个分支：
 *   1) 事件上带 clientX 时：x = clientX * pix（没有减去画布左偏移），
 *      y = (pageY - currentTarget.offsetTop) * pix —— offsetTop 取决于 offsetParent，
 *      在 uni-h5 里常常算出一个超出画布高度的 y；
 *   2) 只有 x/y 时：直接按"相对画布坐标"使用。
 * 而 findCurrentIndex 的第一步是 isInExactChartArea(currentPoints, opts, config)，
 * 它要求 x 与 y **同时**落在绘图区内，不满足就直接返回 index = -1
 * （外部表现就是"悬停、长按都没有任何反应"）。
 * 所以这里统一构造第 2 种形态，用 DOM 矩形自己换算，绕开 offsetTop 这个坑。
 */
const buildChartEvent = (clientX, clientY) => {
  const el = resolvePointerEl()
  if (!el || typeof el.getBoundingClientRect !== 'function') return null
  const rect = el.getBoundingClientRect()
  return {
    changedTouches: [{ x: clientX - rect.left, y: clientY - rect.top }],
    currentTarget: el
  }
}

/**
 * 把各来源的指针事件归一化成 uCharts 可识别的事件
 * @param {Object} event uni 触摸事件或原生鼠标事件
 * @returns {Object|null} 归一化事件，无法解析时返回 null
 */
const normalizePointerEvent = event => {
  if (!event) return null
  const touch = (event.changedTouches && event.changedTouches[0])
    || (event.touches && event.touches[0])
    || event

  // #ifdef H5
  // H5 一律按 DOM 矩形换算：坐标必然相对画布，鼠标与触摸两种来源都能覆盖
  const clientX = typeof touch.clientX === 'number' ? touch.clientX : touch.pageX
  const clientY = typeof touch.clientY === 'number' ? touch.clientY : touch.pageY
  if (typeof clientX !== 'number' || typeof clientY !== 'number') return null
  return buildChartEvent(clientX, clientY)
  // #endif

  // #ifndef H5
  /*
   * 小程序 / App：统一走与 H5 相同的"事件里只给相对画布坐标"分支。
   *
   * 原因：uni 的 canvas 触摸事件同时带 clientX/clientY（视口坐标）和 x/y（画布坐标），
   * 而 uCharts 的 getTouches 各版本对二者的取舍并不一致（有的还会乘一次 pixelRatio），
   * 于是同一份事件在不同端算出的坐标不同——小程序端因此算出越界坐标、
   * isInExactChartArea 判定失败，表现为"点节点完全没有反应"。
   * 这里先用缓存的容器矩形把视口坐标换算成相对画布坐标，
   * 且事件里只保留 x/y，让两端走同一段逻辑（H5 已验证可用）。
   */
  // 变量名带 mp 前缀：条件编译只是注释，lint 看不到平台差异，
  // 与上面 H5 分支同名会报 "Cannot redeclare block-scoped variable"。
  const mpClientX = typeof touch.clientX === 'number' ? touch.clientX : touch.pageX
  const mpClientY = typeof touch.clientY === 'number' ? touch.clientY : touch.pageY
  const rect = pointerRect.value
  if (rect && typeof mpClientX === 'number' && typeof mpClientY === 'number') {
    return {
      changedTouches: [{ x: mpClientX - rect.left, y: mpClientY - rect.top }],
      currentTarget: event.currentTarget || resolvePointerEl()
    }
  }
  // 矩形还没测到（首帧极短窗口内）：退回 uni 给出的画布坐标，维持原有行为
  if (typeof touch.x === 'number' && typeof touch.y === 'number') {
    return { changedTouches: [touch], currentTarget: event.currentTarget || resolvePointerEl() }
  }
  return null
  // #endif
}

/**
 * 定位指针所在的数据点下标
 * @param {Object} event 指针事件
 * @returns {number} 数据下标，未命中返回 -1
 */
const locateIndex = event => {
  if (!chart) return -1
  const normalized = normalizePointerEvent(event)
  if (!normalized) return -1
  try {
    const result = chart.getCurrentDataIndex(normalized)
    const index = result && typeof result.index === 'number' ? result.index : -1
    return index >= 0 && index < props.buckets.length ? index : -1
  } catch (e) {
    return -1
  }
}

/** 按下 / 点击：定位最近的数据点并展示数值 */
const onPointerDown = event => {
  const index = locateIndex(event)
  if (index < 0) return
  activeIndex.value = index
  scheduleHideTip()
}

/** 移动（鼠标悬停或手指滑动）：实时跟随数据点 */
const onPointerMove = event => {
  const index = locateIndex(event)
  if (index >= 0) {
    activeIndex.value = index
    scheduleHideTip()
  }
}

/** 离开图表：延迟隐藏，避免数值瞬间消失来不及看 */
const onPointerOut = () => {
  scheduleHideTip()
}

/* --------------------------- 事件绑定与尺寸重绘 --------------------------- */

let resizeObserver = null
let windowResizeBound = false
/** 非 H5 端的窗口尺寸监听回调，卸载时必须注销（uni.offWindowResize 要求同一函数引用） */
let windowResizeHandler = null

/**
 * 绑定指针事件（仅 H5 需要，移动端由模板上的 touchstart/touchmove 覆盖）
 * @description 绑在图表容器上：既不会因内层 canvas 重建而丢失，又能收到冒泡上来的鼠标事件
 */
/**
 * 非 H5 端：图表容器在视口中的矩形，用于把触摸的视口坐标换算成"相对画布"坐标。
 *
 * @description 在这里缓存而不是每次触摸时现查——uni 的 boundingClientRect 是异步的，
 * 而"定位数据点"必须同步返回，所以改为每次渲染后测量一次
 * （bindPointer 由 renderChart 调用，尺寸变化 / 横竖屏切换都会重新走到这里）。
 */
/**
 * 【必须用 ref，不能用普通的 let 变量】
 *
 * 这里最初写的是 `let pointerRect = null`，编译产物里也确有 `let ...=null`，
 * 但开发版（dist/dev）运行时却抛 "pointerRect is not defined" ——
 * 顶层可变绑定在这个位置没能稳定落地。改用 ref 后与同文件里其它状态
 * （canvasStyleWidth 等）走完全相同的编译路径，不再有"声明丢失"的可能。
 * 矩形本身是普通对象，放进 ref 不会有响应式开销问题（只在替换时触发）。
 */
const pointerRect = ref(null)

const measurePointerRect = () => {
  // #ifndef H5
  if (typeof uni.createSelectorQuery !== 'function') return
  uni
    .createSelectorQuery()
    .in(instance.proxy)
    .select('.chart-wrap')
    .boundingClientRect(rect => {
      if (rect) pointerRect.value = rect
    })
    .exec()
  // #endif
}

const bindPointer = () => {
  // 先刷新矩形再判断是否需要绑定事件：即使事件已绑定，尺寸变化后矩形也必须是新的
  measurePointerRect()
  if (pointerBound) return
  // #ifdef H5
  const el = resolvePointerEl()
  if (!el) return
  el.addEventListener('mousemove', onPointerMove)
  el.addEventListener('click', onPointerDown)
  el.addEventListener('mouseleave', onPointerOut)
  // #endif
  pointerBound = true
}

const unbindPointer = () => {
  // #ifdef H5
  if (pointerEl) {
    pointerEl.removeEventListener('mousemove', onPointerMove)
    pointerEl.removeEventListener('click', onPointerDown)
    pointerEl.removeEventListener('mouseleave', onPointerOut)
  }
  // #endif
  pointerEl = null
  pointerBound = false
}

const bindResize = () => {
  // #ifdef H5
  // 容器宽度变化（例如展开/收起左侧边栏）不会触发 window.resize，必须用 ResizeObserver
  const el = resolvePointerEl()
  if (el && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => scheduleRender())
    resizeObserver.observe(el)
  } else if (typeof window !== 'undefined') {
    window.addEventListener('resize', scheduleRender)
    windowResizeBound = true
  }
  // #endif

  // #ifndef H5
  // 保存回调引用：uni.offWindowResize 需要同一个函数实例才能注销，
  // 否则组件卸载后监听仍留在全局，反复进出页面会不断累积。
  if (typeof uni.onWindowResize === 'function' && !windowResizeHandler) {
    windowResizeHandler = () => scheduleRender()
    uni.onWindowResize(windowResizeHandler)
  }
  // #endif
}

onMounted(async () => {
  await renderChart()
  bindResize()
  bindPointer()
})

onUnmounted(() => {
  if (resizeTimer) {
    clearTimeout(resizeTimer)
    resizeTimer = null
  }
  if (hideTipTimer) {
    clearTimeout(hideTipTimer)
    hideTipTimer = null
  }
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (windowResizeBound && typeof window !== 'undefined') {
    window.removeEventListener('resize', scheduleRender)
    windowResizeBound = false
  }
  unbindPointer()

  // #ifndef H5
  if (windowResizeHandler && typeof uni.offWindowResize === 'function') {
    uni.offWindowResize(windowResizeHandler)
  }
  windowResizeHandler = null
  // #endif

  chart = null
})

// 数据到达后（或切换时间范围后）重绘
watch(
  () => props.buckets,
  () => {
    renderChart()
  }
)

/** 供父组件在必要时手动触发重绘 */
defineExpose({
  refresh: renderChart
})
</script>

<style scoped>
.chart-card {
  width: 100%;
  box-sizing: border-box;
}

.chart-legend {
  display: flex;
  flex-wrap: wrap; /* 窄屏自动换行，不撑宽容器 */
  gap: 12px;
  margin-bottom: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-xs);
  margin-right: 4px;
}

.legend-dot-bar {
  background-color: #c2977f;
}

.legend-dot-line {
  height: 2px;
  border-radius: var(--radius-xs);
  background-color: #94a7c8;
}

.legend-text {
  font-size: calc(11px * var(--font-scale, 1));
  color: #888888;
}

/* 关键：宽度只跟随父容器，overflow hidden 兜底，杜绝横向溢出 */
.chart-wrap {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  line-height: 0;
}

.chart-canvas {
  display: block;
}

.chart-empty {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1.4;
  font-size: var(--fs-sm);
  color: #aaaaaa;
}

/* 数值提示条：固定高度，不会因内容变化撑动布局 */
.chart-tip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  min-height: 20px;
  margin-top: 8px;
  padding: 8px 12px;
  background-color: var(--surface-strong, #FFFFFF)ff;
  border-radius: var(--radius-sm);
  box-sizing: border-box;
}

.tip-range {
  font-size: var(--fs-xs);
  font-weight: var(--fw-normal); /* 正文应为 400；原 500 是把"标签的重量"用在了描述文字上 */
  color: #c2977f;
}

.tip-value {
  font-size: var(--fs-xs);
  color: #555555;
}

.tip-hint {
  font-size: var(--fs-xs);
  color: var(--text-muted, #999999);
}
</style>
