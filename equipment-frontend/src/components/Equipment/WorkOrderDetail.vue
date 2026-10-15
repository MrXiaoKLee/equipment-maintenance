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
          <el-descriptions-item label="最近提交复核">{{ detail.lastSubmitTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="关闭时间">{{ detail.closedTime || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.cancelledTime" label="取消时间">{{ detail.cancelledTime }}</el-descriptions-item>
          <el-descriptions-item label="故障现象" :span="2"><span class="plain-text">{{ detail.symptom }}</span></el-descriptions-item>
        </el-descriptions>
        <div class="workflow-actions">
          <el-button v-if="canProcess" v-hasPermi="['equipment:workorder:process']" type="primary" plain @click="openAction('note')">追加处理记录</el-button>
          <el-button v-if="canProcess" v-hasPermi="['equipment:workorder:submit']" type="primary" @click="openAction('submit')">提交处理结果</el-button>
          <el-button v-if="detail.status === 'PENDING_REVIEW' && isManager" v-hasPermi="['equipment:workorder:review']" type="primary" @click="openAction('review')">复核处理结果</el-button>
          <el-button :loading="loading" @click="loadDetail">刷新详情</el-button>
        </div>
        <template v-if="latestSubmission">
          <h3>最近一轮处理结果</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="提交人及时间">{{ latestSubmission.actorName }} · {{ latestSubmission.createTime }}</el-descriptions-item>
            <el-descriptions-item label="原因判断"><span class="plain-text">{{ latestSubmission.causeAnalysis }}</span></el-descriptions-item>
            <el-descriptions-item label="处理措施"><span class="plain-text">{{ latestSubmission.measures }}</span></el-descriptions-item>
            <el-descriptions-item label="处理结果"><span class="plain-text">{{ latestSubmission.resultDescription }}</span></el-descriptions-item>
          </el-descriptions>
        </template>
        <h3>动作记录</h3>
        <el-empty v-if="records.length === 0" description="暂无动作记录" />
        <el-timeline v-else>
          <el-timeline-item v-for="record in records" :key="record.recordId" :timestamp="record.createTime" placement="top">
            <div><strong>{{ actionLabels[record.actionType] || record.actionType }}</strong> · {{ record.actorName }}</div>
            <div class="transition">{{ record.fromStatus ? label(eq_work_order_status, record.fromStatus) : '创建' }} → {{ label(eq_work_order_status, record.toStatus) }}</div>
            <div class="plain-text">{{ record.description }}</div>
            <template v-if="record.actionType === 'SUBMIT_REVIEW'">
              <div class="plain-text">原因判断：{{ record.causeAnalysis }}</div>
              <div class="plain-text">处理措施：{{ record.measures }}</div>
              <div class="plain-text">处理结果：{{ record.resultDescription }}</div>
            </template>
          </el-timeline-item>
        </el-timeline>
      </template>
    </div>
    <work-order-action-dialog v-if="detail" v-model="actionOpen" :work-order="detail" :mode="actionMode" @saved="onSaved" />
  </el-dialog>
</template>

<script setup>
import { getWorkOrder, getWorkOrderRecords } from '@/api/equipment/workorder'
import useUserStore from '@/store/modules/user'
import WorkOrderActionDialog from './WorkOrderActionDialog.vue'

const props = defineProps({ modelValue: Boolean, workOrderId: [String, Number] })
const emit = defineEmits(['update:modelValue', 'changed'])
const userStore = useUserStore()
const isManager = computed(() => userStore.roles.includes('admin') || userStore.roles.includes('eq_manager'))
const canProcess = computed(() => detail.value?.status === 'PROCESSING' && String(detail.value.handlerId) === String(userStore.id)
  && (userStore.roles.includes('admin') || userStore.roles.includes('eq_maintainer')))
const visible = computed({ get: () => props.modelValue, set: value => emit('update:modelValue', value) })
const { eq_work_order_status, eq_fault_category, eq_priority } = useDict('eq_work_order_status', 'eq_fault_category', 'eq_priority')
const loading = ref(false)
const detail = ref(null)
const records = ref([])
const actionLabels = { CREATE: '提交报修', ASSIGN: '分配维护人员', CANCEL: '取消报修', PROCESS_NOTE: '追加处理记录', SUBMIT_REVIEW: '提交处理结果', REVIEW_REJECT: '复核退回', REVIEW_APPROVE: '复核通过并关闭' }
const latestSubmission = computed(() => [...records.value].reverse().find(record => record.actionType === 'SUBMIT_REVIEW'))
const actionOpen = ref(false)
const actionMode = ref('note')
let requestSequence = 0

function label(options, value) {
  return options.find(item => item.value === value)?.label || value || '—'
}

function openAction(mode) { actionMode.value = mode; actionOpen.value = true }
async function onSaved() { await loadDetail(); emit('changed') }

async function loadDetail() {
  const sequence = ++requestSequence
  const id = props.workOrderId
  if (!props.modelValue || !id) return
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
}

watch(() => [props.modelValue, props.workOrderId], () => { actionOpen.value = false; loadDetail() })
</script>

<style scoped>
.plain-text { white-space: pre-wrap; overflow-wrap: anywhere; }
.transition { color: #606266; margin: 8px 0; }
.workflow-actions { display: flex; flex-wrap: wrap; gap: 8px; margin: 16px 0; }
</style>
<style>
.workorder-detail { max-width: calc(100vw - 32px); }
</style>
