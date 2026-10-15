<template>
  <div class="app-container">
    <el-form ref="queryRef" v-show="showSearch" :model="query" inline label-width="84px">
      <el-form-item label="工单编号" prop="workOrderNo"><el-input v-model="query.workOrderNo" placeholder="请输入工单编号" maxlength="40" clearable @keyup.enter="search" /></el-form-item>
      <el-form-item label="设备" prop="equipmentId"><el-select v-model="query.equipmentId" filterable remote clearable :remote-method="searchFilterEquipment" placeholder="搜索设备编号或名称" style="width: 230px"><el-option v-for="e in filterEquipmentOptions" :key="e.equipmentId" :value="e.equipmentId" :label="`${e.equipmentCode} · ${e.equipmentName}`" /></el-select></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px"><el-option v-for="d in eq_work_order_status" :key="d.value" :label="d.label" :value="d.value" /></el-select></el-form-item>
      <el-form-item label="故障类别" prop="faultCategory"><el-select v-model="query.faultCategory" placeholder="全部类别" clearable style="width: 160px"><el-option v-for="d in eq_fault_category" :key="d.value" :label="d.label" :value="d.value" /></el-select></el-form-item>
      <el-form-item label="优先级" prop="priority"><el-select v-model="query.priority" placeholder="全部优先级" clearable style="width: 150px"><el-option v-for="d in eq_priority" :key="d.value" :label="d.label" :value="d.value" /></el-select></el-form-item>
      <el-form-item label="提交日期"><el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" /></el-form-item>
      <el-form-item v-if="isManager" label="报修人" prop="reporterName"><el-input v-model="query.reporterName" maxlength="64" placeholder="姓名或账号" clearable /></el-form-item>
      <el-form-item v-if="isManager" label="维护人员" prop="handlerName"><el-input v-model="query.handlerName" maxlength="64" placeholder="姓名或账号" clearable /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="search">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="3"><el-button v-hasPermi="['equipment:workorder:add']" type="primary" plain icon="Plus" @click="openCreate">提交报修</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>
    <el-alert title="列表、详情与设备历史均按当前账号的角色范围展示。" type="info" :closable="false" class="mb8" />
    <el-table v-loading="loading" :data="rows" row-key="workOrderId" empty-text="当前范围内暂无工单">
      <el-table-column label="工单编号" prop="workOrderNo" min-width="190" show-overflow-tooltip />
      <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column label="设备" min-width="170" show-overflow-tooltip><template #default="{ row }">{{ row.equipmentCode }} · {{ row.equipmentName }}</template></el-table-column>
      <el-table-column label="状态" width="110"><template #default="{ row }"><dict-tag :options="eq_work_order_status" :value="row.status" /></template></el-table-column>
      <el-table-column label="类别" width="120"><template #default="{ row }">{{ label(eq_fault_category, row.faultCategory) }}</template></el-table-column>
      <el-table-column label="优先级" width="90"><template #default="{ row }">{{ label(eq_priority, row.priority) }}</template></el-table-column>
      <el-table-column label="报修人" prop="reporterName" width="110" />
      <el-table-column label="维护人员" width="110"><template #default="{ row }">{{ row.handlerName || '未分配' }}</template></el-table-column>
      <el-table-column label="提交时间" prop="createTime" width="175" />
      <el-table-column label="操作" width="190" fixed="right"><template #default="{ row }">
        <el-button v-hasPermi="['equipment:workorder:query']" link type="primary" @click="showDetail(row)">详情</el-button>
        <el-button v-if="row.status === 'PROCESSING' && String(row.handlerId) === String(userStore.id)" v-hasPermi="['equipment:workorder:process']" link type="primary" @click="showDetail(row)">处理</el-button>
        <el-button v-if="row.status === 'PENDING_REVIEW' && isManager" v-hasPermi="['equipment:workorder:review']" link type="primary" @click="showDetail(row)">复核</el-button>
        <el-button v-if="row.status === 'WAITING'" v-hasPermi="['equipment:workorder:assign']" link type="primary" @click="openAssign(row)">分配</el-button>
        <el-button v-if="row.status === 'WAITING' && (isManager || String(row.reporterId) === String(userStore.id))" v-hasPermi="['equipment:workorder:cancel']" link type="danger" @click="openCancel(row)">取消报修</el-button>
      </template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="getList" />

    <el-dialog v-model="createOpen" title="提交故障报修" width="700px" class="workorder-form" append-to-body :close-on-click-modal="!saving" :show-close="!saving">
      <el-form ref="createRef" :model="form" :rules="rules" label-width="100px" :disabled="saving">
        <el-form-item label="在用设备" prop="equipmentId">
          <el-select v-model="form.equipmentId" filterable remote :remote-method="searchEquipment" :loading="equipmentLoading" placeholder="搜索设备编号或名称" style="width: 100%" @change="loadExisting">
            <el-option v-for="e in equipmentOptions" :key="e.equipmentId" :value="e.equipmentId" :label="`${e.equipmentCode} · ${e.equipmentName}`" />
          </el-select>
        </el-form-item>
        <el-alert v-if="existingTotal > 0" :title="`当前可见范围内有 ${existingTotal} 条未结束工单，提交前将再次确认。`" type="warning" :closable="false" class="mb8" />
        <div v-if="existingTotal > 0" class="existing-orders"><el-button v-for="w in existingOrders" :key="w.workOrderId" link type="primary" @click="showDetail(w)">{{ w.workOrderNo }} · {{ w.title }}</el-button></div>
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" maxlength="100" placeholder="简要描述设备问题" /></el-form-item>
        <el-form-item label="故障现象" prop="symptom"><el-input v-model="form.symptom" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="记录实际观察到的现象" /></el-form-item>
        <el-form-item label="发生时间" prop="occurredTime"><el-date-picker v-model="form.occurredTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="按北京时间填写" style="width: 100%" /></el-form-item>
        <el-form-item label="故障类别" prop="faultCategory"><el-select v-model="form.faultCategory" placeholder="请选择初步分类" style="width: 100%"><el-option v-for="d in eq_fault_category" :key="d.value" :label="d.label" :value="d.value" /></el-select></el-form-item>
        <el-form-item label="优先级" prop="priority"><el-select v-model="form.priority" style="width: 100%"><el-option v-for="d in eq_priority" :key="d.value" :label="d.label" :value="d.value" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="saving" @click="createOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitCreate">提交报修</el-button></template>
    </el-dialog>

    <el-dialog v-model="assignOpen" title="分配维护人员" width="560px" class="workorder-form" append-to-body :close-on-click-modal="!saving" :show-close="!saving">
      <p>{{ actionOrder.workOrderNo }} · {{ actionOrder.title }}</p>
      <el-form ref="assignRef" :model="assignment" :disabled="saving" label-width="90px">
        <el-form-item label="维护人员" prop="handlerId" :rules="[{ required: true, message: '请选择维护人员', trigger: 'change' }]">
          <el-select v-model="assignment.handlerId" filterable remote :remote-method="searchMaintainers" :loading="maintainerLoading" placeholder="搜索姓名或账号，仅显示有效维护人员" style="width: 100%"><el-option v-for="u in maintainerOptions" :key="u.userId" :value="u.userId" :label="`${u.nickName}（${u.userName}）`" /></el-select>
        </el-form-item>
        <el-form-item label="分配说明"><el-input v-model="assignment.description" type="textarea" :rows="3" maxlength="1800" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="saving" @click="assignOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitAssign">确认分配</el-button></template>
    </el-dialog>

    <el-dialog v-model="cancelOpen" title="取消待受理报修" width="560px" class="workorder-form" append-to-body :close-on-click-modal="!saving" :show-close="!saving">
      <p>{{ actionOrder.workOrderNo }} · {{ actionOrder.title }}</p>
      <el-form ref="cancelRef" :model="cancellation" :disabled="saving" label-width="90px">
        <el-form-item label="取消原因" prop="reason" :rules="[{ required: true, whitespace: true, message: '请输入取消原因', trigger: 'blur' }]"><el-input v-model="cancellation.reason" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="saving" @click="cancelOpen = false">返回</el-button><el-button type="danger" :loading="saving" @click="submitCancel">确认取消报修</el-button></template>
    </el-dialog>
    <work-order-detail v-model="detailOpen" :work-order-id="detailId" @changed="getList" />
  </div>
</template>

<script setup name="EquipmentWorkOrder">
import useUserStore from '@/store/modules/user'
import WorkOrderDetail from '@/components/Equipment/WorkOrderDetail.vue'
import { listEquipment, listEquipmentOptions } from '@/api/equipment/asset'
import { listWorkOrders, getWorkOrder, createWorkOrder, assignWorkOrder, cancelWorkOrder, listMaintainers } from '@/api/equipment/workorder'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const isManager = computed(() => userStore.roles.includes('admin') || userStore.roles.includes('eq_manager'))
const { eq_work_order_status, eq_fault_category, eq_priority } = useDict('eq_work_order_status', 'eq_fault_category', 'eq_priority')
const query = reactive({ pageNum: 1, pageSize: 10, workOrderNo: '', equipmentId: undefined, status: undefined, faultCategory: undefined, priority: undefined, reporterName: undefined, handlerName: undefined })
const dateRange = ref([])
const rows = ref([])
const total = ref(0)
const showSearch = ref(true)
const loading = ref(false)
const saving = ref(false)
const createOpen = ref(false)
const form = ref({})
const rules = Object.fromEntries(['equipmentId','title','symptom','occurredTime','faultCategory','priority'].map(key => [key, [{ required: true, ...(key === 'title' || key === 'symptom' ? { whitespace: true } : {}), message: '请完整填写该项', trigger: 'blur' }]]))
const equipmentOptions = ref([])
const filterEquipmentOptions = ref([])
const equipmentLoading = ref(false)
const existingOrders = ref([])
const existingTotal = ref(0)
const assignOpen = ref(false)
const cancelOpen = ref(false)
const actionOrder = ref({})
const assignment = ref({})
const cancellation = ref({})
const maintainerOptions = ref([])
const maintainerLoading = ref(false)
const detailOpen = ref(false)
const detailId = ref(null)
let existingSequence = 0

function label(options, value) { return options.find(d => d.value === value)?.label || value }

async function getList() {
  loading.value = true
  try {
    const result = await listWorkOrders({ ...query, beginDate: dateRange.value?.[0], endDate: dateRange.value?.[1] })
    rows.value = result.rows
    total.value = result.total
  } catch {
    // 请求层统一展示错误提示。
  } finally { loading.value = false }
}
function search() { query.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); dateRange.value = []; search() }
function showDetail(row) { detailId.value = row.workOrderId; detailOpen.value = true }

function openCreate() {
  form.value = { equipmentId: undefined, title: '', symptom: '', occurredTime: '', faultCategory: undefined, priority: 'NORMAL' }
  existingOrders.value = []; existingTotal.value = 0; ++existingSequence
  createOpen.value = true
  nextTick(() => proxy.$refs.createRef?.clearValidate())
  searchEquipment('')
}
async function searchFilterEquipment(keyword) {
  try { filterEquipmentOptions.value = (await listEquipment({ keyword, pageSize: 100 })).rows }
  catch {}
}
async function searchEquipment(keyword) {
  equipmentLoading.value = true
  try { equipmentOptions.value = (await listEquipmentOptions({ keyword, pageSize: 100 })).rows }
  catch {} finally { equipmentLoading.value = false }
}
async function loadExisting() {
  const sequence = ++existingSequence
  const id = form.value.equipmentId
  existingOrders.value = []; existingTotal.value = 0
  if (!id) return
  const result = await listWorkOrders({ equipmentId: id, openOnly: true, pageNum: 1, pageSize: 5 })
  if (sequence !== existingSequence || id !== form.value.equipmentId) return
  existingOrders.value = result.rows; existingTotal.value = result.total
}
async function submitCreate() {
  if (saving.value || !await proxy.$refs.createRef.validate().catch(() => false)) return
  saving.value = true
  try {
    await loadExisting()
    let confirmExisting = false
    if (existingTotal.value > 0) {
      await proxy.$modal.confirm(`当前可见范围内已有 ${existingTotal.value} 条未结束工单，是否仍要继续提交本次报修？`)
      confirmExisting = true
    }
    const response = await createWorkOrder({ ...form.value, confirmExisting })
    createOpen.value = false
    proxy.$modal.msgSuccess('报修已提交')
    await getList()
    showDetail(response.data)
  } catch {} finally { saving.value = false }
}
async function openAssign(row) {
  try {
    actionOrder.value = (await getWorkOrder(row.workOrderId)).data
    assignment.value = { handlerId: undefined, description: '' }
    assignOpen.value = true
    nextTick(() => proxy.$refs.assignRef?.clearValidate())
    searchMaintainers('')
  } catch {}
}
async function searchMaintainers(keyword) {
  maintainerLoading.value = true
  try { maintainerOptions.value = (await listMaintainers({ keyword, pageSize: 100 })).rows }
  catch {} finally { maintainerLoading.value = false }
}
async function submitAssign() {
  if (saving.value || !await proxy.$refs.assignRef.validate().catch(() => false)) return
  saving.value = true
  try {
    await assignWorkOrder(actionOrder.value.workOrderId, { ...assignment.value, version: actionOrder.value.version })
    assignOpen.value = false
    proxy.$modal.msgSuccess('维护人员已分配')
    await getList()
  } catch {} finally { saving.value = false }
}
async function openCancel(row) {
  try {
    actionOrder.value = (await getWorkOrder(row.workOrderId)).data
    cancellation.value = { reason: '' }
    cancelOpen.value = true
    nextTick(() => proxy.$refs.cancelRef?.clearValidate())
  } catch {}
}
async function submitCancel() {
  if (saving.value || !await proxy.$refs.cancelRef.validate().catch(() => false)) return
  saving.value = true
  try {
    await cancelWorkOrder(actionOrder.value.workOrderId, { ...cancellation.value, version: actionOrder.value.version })
    cancelOpen.value = false
    proxy.$modal.msgSuccess('报修已取消，原因已记录')
    await getList()
  } catch {} finally { saving.value = false }
}

getList()
searchFilterEquipment('')
</script>

<style scoped>
.existing-orders { display: flex; flex-direction: column; align-items: flex-start; gap: 6px; margin: 8px 0 16px; }
</style>
<style>
.workorder-form { max-width: calc(100vw - 32px); }
</style>
