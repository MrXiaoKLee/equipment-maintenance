<template>
  <div class="app-container statistics-page">
    <h1>统计分析</h1>
    <p class="scope-note">设备与工单管理记录汇总，用于查看报修分布和流程情况。</p>
    <el-form :inline="true" label-width="84px" class="statistics-filters" @submit.prevent="applyFilters">
      <el-form-item label="提交日期"><el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" /></el-form-item>
      <el-form-item label="设备"><el-select v-model="equipmentId" filterable remote clearable :remote-method="searchEquipment" placeholder="全部设备，可搜索编号或名称" style="width: 280px"><el-option v-for="device in equipmentOptions" :key="device.equipmentId" :value="device.equipmentId" :label="`${device.equipmentCode} · ${device.equipmentName}`" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" :loading="loading" @click="applyFilters">查询</el-button><el-button icon="Refresh" :disabled="loading" @click="resetFilters">重置</el-button></el-form-item>
    </el-form>
    <el-alert v-if="failed" title="本次查询失败。下方保留的是上次成功查询的数据，请重试。" type="error" :closable="false" class="mb8" />
    <el-alert title="类别与设备报修次数排除已取消工单。平均闭环时长包含等待、处理及复核，不能视为实际维修时间。" type="info" :closable="false" class="mb8" />
    <div v-loading="loading" :aria-busy="loading">
      <template v-if="overview">
        <p class="scope-note" aria-live="polite">工单范围：{{ scopeLabel }}；更新时间：{{ overview.generatedTime }}（北京时间，手动查询刷新）</p>
        <div class="summary-grid">
          <article class="summary-item"><h2>设备总数</h2><strong>{{ overview.equipmentTotal }}</strong><p>全部设备，不受日期及设备筛选影响</p></article>
          <article class="summary-item"><h2>工单总数</h2><strong>{{ overview.orderTotal }}</strong><p>当前工单范围，含已取消</p></article>
          <article class="summary-item"><h2>未结束工单</h2><strong>{{ overview.openOrderCount }}</strong><p>待受理、处理中、待复核</p></article>
          <article class="summary-item"><h2>平均闭环时长</h2><strong>{{ overview.averageClosedHours === null ? '无数据' : `${overview.averageClosedHours} 小时` }}</strong><p>当前范围 {{ overview.closedOrderCount }} 条已关闭工单</p></article>
        </div>
        <div class="charts-grid">
          <statistics-chart title="工单状态分布" note="五种状态数量之和等于工单总数" :rows="overview.orderStatuses" />
          <statistics-chart title="设备使用状态" note="全部设备，不受工单筛选影响" unit="台" :rows="overview.equipmentStatuses" />
          <statistics-chart title="故障类别分布" note="当前工单范围，排除已取消" :rows="overview.faultCategories" />
          <statistics-chart title="设备报修次数前10名" note="排除已取消；相同次数按设备编号排序" :rows="rankingRows" />
        </div>
      </template>
      <el-empty v-else-if="!loading" description="暂无统计结果，请查询" />
    </div>
  </div>
</template>

<script setup name="EquipmentStatistics">
import { useRoute, useRouter } from 'vue-router'
import { getStatisticsOverview } from '@/api/equipment/statistics'
import { listEquipment, getEquipment } from '@/api/equipment/asset'
import StatisticsChart from '@/components/Equipment/StatisticsChart.vue'

const route = useRoute()
const router = useRouter()
const dateRange = ref([])
const equipmentId = ref()
const equipmentOptions = ref([])
const overview = ref(null)
const loading = ref(false)
const failed = ref(false)
const loadedFilter = ref({})
const rankingRows = computed(() => overview.value?.topEquipment.map(row => ({ code: row.equipmentId, label: `${row.equipmentCode} · ${row.equipmentName}`, count: row.count })) || [])
const scopeLabel = computed(() => `${loadedFilter.value.beginDate || '最早'} 至 ${loadedFilter.value.endDate || '最新'}，${loadedFilter.value.equipmentId ? `设备 ${loadedFilter.value.equipmentId}` : '全部设备'}`)
let sequence = 0

function urlFilter() {
  return Object.fromEntries(['beginDate', 'endDate', 'equipmentId'].filter(key => typeof route.query[key] === 'string' && route.query[key]).map(key => [key, route.query[key]]))
}
async function load() {
  const current = ++sequence
  const filter = urlFilter()
  dateRange.value = filter.beginDate || filter.endDate ? [filter.beginDate || '', filter.endDate || ''] : []
  equipmentId.value = filter.equipmentId
  loading.value = true
  failed.value = false
  try {
    const response = await getStatisticsOverview(filter)
    if (current !== sequence) return
    overview.value = response.data
    loadedFilter.value = filter
  } catch { if (current === sequence) failed.value = true }
  finally { if (current === sequence) loading.value = false }
}
async function applyFilters() {
  const filter = Object.fromEntries(Object.entries({ beginDate: dateRange.value?.[0], endDate: dateRange.value?.[1], equipmentId: equipmentId.value }).filter(([, value]) => value))
  if (JSON.stringify(filter) === JSON.stringify(urlFilter())) await load()
  else await router.replace({ query: filter })
}
async function resetFilters() { dateRange.value = []; equipmentId.value = undefined; await applyFilters() }
async function searchEquipment(keyword) {
  try {
    equipmentOptions.value = (await listEquipment({ keyword, pageSize: 100 })).rows
    if (equipmentId.value && !equipmentOptions.value.some(row => row.equipmentId === equipmentId.value)) {
      equipmentOptions.value.unshift((await getEquipment(equipmentId.value)).data)
    }
  } catch { /* 请求层统一展示错误。 */ }
}
watch(() => route.query, load, { immediate: true })
searchEquipment('')
</script>

<style scoped>
h1 { margin: 0 0 8px; font-size: 22px; }
.scope-note { color: var(--el-text-color-regular); font-size: 13px; line-height: 1.7; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin: 16px 0; }
.summary-item { padding: 16px; background: var(--el-fill-color-light); border-radius: 4px; }
.summary-item h2 { margin: 0 0 12px; font-size: 14px; }
.summary-item strong { font-size: 24px; overflow-wrap: anywhere; }
.summary-item p { color: var(--el-text-color-regular); font-size: 12px; margin: 10px 0 0; }
.charts-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
@media (max-width: 900px) { .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) {
  .charts-grid { grid-template-columns: minmax(0, 1fr); }
  .statistics-filters :deep(.el-form-item) { display: flex; margin-right: 0; }
  .statistics-filters :deep(.el-form-item__content) { min-width: 0; }
  .statistics-filters :deep(.el-date-editor), .statistics-filters :deep(.el-select) { width: 100% !important; }
}
</style>
