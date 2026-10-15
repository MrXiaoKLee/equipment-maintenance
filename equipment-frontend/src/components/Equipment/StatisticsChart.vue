<template>
  <section class="statistics-panel" :aria-label="title">
    <div class="panel-heading">
      <h2>{{ title }}</h2>
      <el-radio-group v-model="mode" size="small" :aria-label="`${title}显示方式`">
        <el-radio-button value="chart">图表</el-radio-button><el-radio-button value="table">表格</el-radio-button>
      </el-radio-group>
    </div>
    <p class="chart-note">{{ note }}</p>
    <el-empty v-if="!rows.some(row => row.count > 0)" description="当前范围内无数据" :image-size="64" />
    <template v-else>
      <div v-show="mode === 'chart'" ref="chartRef" class="chart" :style="{ height: `${Math.max(220, rows.length * 38 + 50)}px` }" role="img" :aria-label="summary" />
      <el-table v-if="mode === 'table'" :data="rows" size="small" :aria-label="`${title}明细`">
        <el-table-column label="项目" prop="label" min-width="180" />
        <el-table-column :label="`数量（${unit}）`" prop="count" width="115" align="right" />
      </el-table>
    </template>
  </section>
</template>

<script setup>
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, AriaComponent } from 'echarts/components'
import { SVGRenderer } from 'echarts/renderers'

echarts.use([BarChart, GridComponent, TooltipComponent, AriaComponent, SVGRenderer])
const props = defineProps({ title: String, note: String, unit: { type: String, default: '条' }, rows: { type: Array, default: () => [] } })
const chartRef = ref()
const mode = ref('chart')
const summary = computed(() => `${props.title}：${props.rows.map(row => `${row.label} ${row.count}${props.unit}`).join('，')}`)
let chart
let observer

function render() {
  if (!chartRef.value || !props.rows.some(row => row.count > 0)) {
    chart?.dispose(); chart = undefined
    observer?.disconnect(); observer = undefined
    return
  }
  if (mode.value !== 'chart') return
  if (!chart) {
    chart = echarts.init(chartRef.value, null, { renderer: 'svg' })
    observer = new ResizeObserver(() => { if (chartRef.value?.clientWidth > 0) chart?.resize() })
    observer.observe(chartRef.value)
  }
  chart.setOption({
    animation: false,
    aria: { enabled: true, label: { description: summary.value } },
    grid: { left: 8, right: 40, top: 12, bottom: 24, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, renderMode: 'richText' },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#e5e7eb' } } },
    yAxis: { type: 'category', inverse: true, data: props.rows.map(row => row.label), axisLabel: { width: 150, overflow: 'truncate' }, axisTick: { show: false } },
    series: [{ type: 'bar', data: props.rows.map(row => row.count), barMaxWidth: 22, itemStyle: { color: '#337ecc' }, label: { show: true, position: 'right', color: '#303133' } }]
  }, true)
  chart.resize()
}
watch(() => [props.rows, mode.value], () => nextTick(render), { deep: true })
onMounted(render)
onActivated(() => nextTick(render))
onBeforeUnmount(() => { observer?.disconnect(); chart?.dispose() })
</script>

<style scoped>
.statistics-panel { border: 1px solid #e5e7eb; border-radius: 4px; padding: 16px; background: var(--el-bg-color); min-width: 0; }
.panel-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
h2 { font-size: 16px; margin: 0; }
.chart-note { font-size: 13px; color: var(--el-text-color-regular); margin: 10px 0; }
.chart { width: 100%; }
</style>
