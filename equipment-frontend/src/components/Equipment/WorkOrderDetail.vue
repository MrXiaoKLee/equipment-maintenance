<template>
  <el-dialog v-model="visible" title="工单详情与动作记录" width="900px" class="workorder-detail" append-to-body>
    <div v-loading="loading">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单编号" :span="2">{{ detail.workOrderNo }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="设备" :span="2">{{ detail.equipmentCode }} · {{ detail.equipmentName }}</el-descriptions-item>
          <el-descriptions-item label="状态"><dict-tag :options="eq_work_order_status" :value="detail.status" /></el-descriptions-item>
          <el-descriptions-item label="优先级">{{ label(eq_priority, detail.priority) }}</el-descriptions-item>
          <el-descriptions-item label="故障类别">{{ label(eq_fault_category, detail.faultCategory) }}</el-descriptions-item>
          <el-descriptions-item label="发生时间">{{ detail.occurredTime }}</el-descriptions-item>
          <el-descriptions-item label="报修人">{{ detail.reporterName }}</el-descriptions-item>
          <el-descriptions-item label="维护人员">{{ detail.handlerName || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="报修提交时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="分配时间">{{ detail.assignedTime || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.cancelledTime" label="取消时间">{{ detail.cancelledTime }}</el-descriptions-item>
          <el-descriptions-item label="故障现象" :span="2"><span class="plain-text">{{ detail.symptom }}</span></el-descriptions-item>
        </el-descriptions>
        <h3>动作记录</h3>
        <el-empty v-if="records.length === 0" description="暂无动作记录" />
        <el-timeline v-else>
          <el-timeline-item v-for="record in records" :key="record.recordId" :timestamp="record.createTime" placement="top">
            <div><strong>{{ actionLabels[record.actionType] || record.actionType }}</strong> · {{ record.actorName }}</div>
            <div class="transition">{{ record.fromStatus ? label(eq_work_order_status, record.fromStatus) : '创建' }} → {{ label(eq_work_order_status, record.toStatus) }}</div>
            <div class="plain-text">{{ record.description }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </div>
  </el-dialog>
</template>

<script setup>
import { getWorkOrder, getWorkOrderRecords } from '@/api/equipment/workorder'

const props = defineProps({ modelValue: Boolean, workOrderId: [String, Number] })
const emit = defineEmits(['update:modelValue'])
const visible = computed({ get: () => props.modelValue, set: value => emit('update:modelValue', value) })
const { eq_work_order_status, eq_fault_category, eq_priority } = useDict('eq_work_order_status', 'eq_fault_category', 'eq_priority')
const loading = ref(false)
const detail = ref(null)
const records = ref([])
const actionLabels = { CREATE: '提交报修', ASSIGN: '分配维护人员', CANCEL: '取消报修' }
let requestSequence = 0

function label(options, value) {
  return options.find(item => item.value === value)?.label || value || '—'
}

watch(() => [props.modelValue, props.workOrderId], async ([open, id]) => {
  const sequence = ++requestSequence
  if (!open || !id) return
  loading.value = true
  detail.value = null
  records.value = []
  try {
    const [order, history] = await Promise.all([getWorkOrder(id), getWorkOrderRecords(id)])
    if (sequence !== requestSequence) return
    detail.value = order.data
    records.value = history.data
  } catch {
    if (sequence === requestSequence) visible.value = false
  } finally {
    if (sequence === requestSequence) loading.value = false
  }
})
</script>

<style scoped>
.plain-text { white-space: pre-wrap; overflow-wrap: anywhere; }
.transition { color: #606266; margin: 8px 0; }
</style>
<style>
.workorder-detail { max-width: calc(100vw - 32px); }
</style>
