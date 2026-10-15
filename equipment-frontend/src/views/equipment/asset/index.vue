<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch">
      <el-form-item label="设备编号" prop="equipmentCode">
        <el-input v-model="queryParams.equipmentCode" placeholder="请输入设备编号" clearable maxlength="32" style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="设备名称" prop="equipmentName">
        <el-input v-model="queryParams.equipmentName" placeholder="请输入设备名称" clearable maxlength="100" style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="设备类型" prop="equipmentType">
        <el-select v-model="queryParams.equipmentType" placeholder="全部类型" clearable style="width: 150px">
          <el-option v-for="item in eq_equipment_type" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="使用状态" prop="equipmentStatus">
        <el-select v-model="queryParams.equipmentStatus" placeholder="全部状态" clearable style="width: 150px">
          <el-option v-for="item in eq_equipment_status" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" v-hasPermi="['equipment:asset:add']" @click="handleAdd">新增设备</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" v-hasPermi="['equipment:asset:export']" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="equipmentList" row-key="equipmentId" empty-text="暂无设备记录">
      <el-table-column label="设备编号" prop="equipmentCode" min-width="140" show-overflow-tooltip />
      <el-table-column label="设备名称" prop="equipmentName" min-width="160" show-overflow-tooltip />
      <el-table-column label="设备类型" width="110">
        <template #default="scope">{{ typeLabel(scope.row.equipmentType) }}</template>
      </el-table-column>
      <el-table-column label="型号" prop="model" min-width="120" show-overflow-tooltip />
      <el-table-column label="位置" prop="location" min-width="150" show-overflow-tooltip />
      <el-table-column label="使用状态" width="110">
        <template #default="scope"><dict-tag :options="eq_equipment_status" :value="scope.row.equipmentStatus" /></template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="175" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="scope">
          <el-button link type="primary" v-hasPermi="['equipment:asset:query']" @click="handleDetail(scope.row)">详情</el-button>
          <el-button link type="primary" v-hasPermi="['equipment:asset:edit']" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button v-if="scope.row.equipmentStatus !== 'RETIRED'" link type="primary" v-hasPermi="['equipment:asset:status']" @click="handleStatus(scope.row)">状态</el-button>
          <el-button link type="primary" v-hasPermi="['equipment:workorder:list']" @click="handleHistory(scope.row)">工单历史</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="editOpen" :title="editingId ? '编辑设备' : '新增设备'" width="600px" append-to-body>
      <el-form ref="equipmentRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="设备编号" prop="equipmentCode">
          <el-input v-model="form.equipmentCode" :disabled="!!editingId" maxlength="32" placeholder="例如 EQ-0001，创建后不可修改" />
        </el-form-item>
        <el-form-item label="设备名称" prop="equipmentName"><el-input v-model="form.equipmentName" maxlength="100" /></el-form-item>
        <el-form-item label="设备类型" prop="equipmentType">
          <el-select v-model="form.equipmentType" style="width: 100%" placeholder="请选择设备类型">
            <el-option v-for="item in eq_equipment_type" :key="item.value" :label="item.label" :value="item.value" />
            <el-option v-if="legacyType" :label="`${legacyType}（原类型已停用）`" :value="legacyType" />
          </el-select>
        </el-form-item>
        <el-form-item label="型号" prop="model"><el-input v-model="form.model" maxlength="100" /></el-form-item>
        <el-form-item label="位置" prop="location"><el-input v-model="form.location" maxlength="200" placeholder="例如 实验室A / 试验台区" /></el-form-item>
        <el-form-item label="说明" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="2000" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editOpen = false" :disabled="saving">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusOpen" title="调整设备使用状态" width="480px" append-to-body>
      <p>{{ statusEquipment.equipmentCode }} · {{ statusEquipment.equipmentName }}</p>
      <el-form label-width="90px">
        <el-form-item label="目标状态">
          <el-select v-model="targetStatus" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <p class="status-hint">当前有 {{ statusEquipment.openWorkOrderCount }} 条未结束工单。停用后不能新增报修，已有工单仍可处理。报废不可恢复，存在未结束工单时不能报废。</p>
      <template #footer>
        <el-button @click="statusOpen = false" :disabled="saving">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitStatus">确认调整</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailOpen" title="设备详情" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="设备编号">{{ detail.equipmentCode }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detail.equipmentName }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ typeLabel(detail.equipmentType) }}</el-descriptions-item>
        <el-descriptions-item label="使用状态"><dict-tag :options="eq_equipment_status" :value="detail.equipmentStatus" /></el-descriptions-item>
        <el-descriptions-item label="型号">{{ detail.model || '—' }}</el-descriptions-item>
        <el-descriptions-item label="位置">{{ detail.location }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="说明" :span="2"><span class="record-text">{{ detail.remark || '—' }}</span></el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="historyOpen" :title="`${historyEquipment.equipmentCode || ''} 工单历史`" width="900px" append-to-body>
      <el-table v-loading="historyLoading" :data="historyList" empty-text="当前可查看范围内暂无工单">
        <el-table-column label="工单编号" prop="workOrderNo" min-width="200" show-overflow-tooltip />
        <el-table-column label="标题" prop="title" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="110"><template #default="scope"><dict-tag :options="eq_work_order_status" :value="scope.row.status" /></template></el-table-column>
        <el-table-column label="报修人" prop="reporterName" width="100" />
        <el-table-column label="处理人" prop="handlerName" width="100" />
        <el-table-column label="提交时间" prop="createTime" width="175" />
        <el-table-column label="操作" width="70"><template #default="scope"><el-button link type="primary" v-hasPermi="['equipment:workorder:query']" @click="historyDetailId = scope.row.workOrderId; historyDetailOpen = true">查看</el-button></template></el-table-column>
      </el-table>
      <pagination v-show="historyTotal > 0" :total="historyTotal" v-model:page="historyParams.pageNum" v-model:limit="historyParams.pageSize" @pagination="getHistory" />
    </el-dialog>
    <work-order-detail v-model="historyDetailOpen" :work-order-id="historyDetailId" />
  </div>
</template>

<script setup name="EquipmentAsset">
import WorkOrderDetail from '@/components/Equipment/WorkOrderDetail.vue'
import { listEquipment, getEquipment, addEquipment, updateEquipment, changeEquipmentStatus, listEquipmentHistory } from '@/api/equipment/asset'

const { proxy } = getCurrentInstance()
const { eq_equipment_type, eq_equipment_status, eq_work_order_status } = useDict('eq_equipment_type', 'eq_equipment_status', 'eq_work_order_status')
const equipmentList = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const showSearch = ref(true)
const queryParams = reactive({ pageNum: 1, pageSize: 10, equipmentCode: undefined, equipmentName: undefined, equipmentType: undefined, equipmentStatus: undefined })
const form = ref({})
const editingId = ref(null)
const editOpen = ref(false)
const legacyType = ref(null)
const rules = {
  equipmentCode: [{ validator: (_rule, value, callback) => /^[a-zA-Z0-9_-]{1,32}$/.test((value || '').trim()) ? callback() : callback(new Error('请输入1至32位字母、数字、连字符或下划线')), trigger: 'blur' }],
  equipmentName: [{ required: true, whitespace: true, message: '请输入设备名称', trigger: 'blur' }],
  equipmentType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  location: [{ required: true, whitespace: true, message: '请输入位置', trigger: 'blur' }]
}
const statusOpen = ref(false)
const statusEquipment = ref({})
const targetStatus = ref(null)
const statusOptions = computed(() => eq_equipment_status.value.filter(item => item.value !== statusEquipment.value.equipmentStatus))
const detailOpen = ref(false)
const detail = ref({})
const historyOpen = ref(false)
const historyEquipment = ref({})
const historyList = ref([])
const historyTotal = ref(0)
const historyLoading = ref(false)
const historyParams = reactive({ pageNum: 1, pageSize: 10 })
const historyDetailOpen = ref(false)
const historyDetailId = ref(null)

function typeLabel(value) {
  return eq_equipment_type.value.find(item => item.value === value)?.label || value
}

async function getList() {
  loading.value = true
  try {
    const response = await listEquipment(queryParams)
    equipmentList.value = response.rows
    total.value = response.total
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleAdd() {
  editingId.value = null
  legacyType.value = null
  form.value = { equipmentCode: '', equipmentName: '', equipmentType: undefined, model: '', location: '', remark: '' }
  editOpen.value = true
  nextTick(() => proxy.$refs.equipmentRef?.clearValidate())
}

async function handleEdit(row) {
  const response = await getEquipment(row.equipmentId)
  editingId.value = row.equipmentId
  form.value = { ...response.data }
  legacyType.value = eq_equipment_type.value.some(item => item.value === form.value.equipmentType) ? null : form.value.equipmentType
  editOpen.value = true
  nextTick(() => proxy.$refs.equipmentRef?.clearValidate())
}

async function submitForm() {
  const valid = await proxy.$refs.equipmentRef.validate().catch(() => false)
  if (!valid || saving.value) return
  saving.value = true
  const data = { equipmentName: form.value.equipmentName, equipmentType: form.value.equipmentType, model: form.value.model, location: form.value.location, remark: form.value.remark }
  try {
    if (editingId.value) {
      await updateEquipment(editingId.value, { ...data, version: form.value.version })
    } else {
      await addEquipment({ ...data, equipmentCode: form.value.equipmentCode.trim() })
    }
    proxy.$modal.msgSuccess('保存成功')
    editOpen.value = false
    await getList()
  } finally {
    saving.value = false
  }
}

async function handleStatus(row) {
  const response = await getEquipment(row.equipmentId)
  statusEquipment.value = response.data
  targetStatus.value = null
  statusOpen.value = true
}

async function submitStatus() {
  if (!targetStatus.value) return proxy.$modal.msgWarning('请选择目标状态')
  if (saving.value) return
  const label = eq_equipment_status.value.find(item => item.value === targetStatus.value)?.label
  try {
    await proxy.$modal.confirm(`确认将设备 ${statusEquipment.value.equipmentCode} 调整为“${label}”？`)
  } catch {
    return
  }
  saving.value = true
  try {
    await changeEquipmentStatus(statusEquipment.value.equipmentId, { equipmentStatus: targetStatus.value, version: statusEquipment.value.version })
    proxy.$modal.msgSuccess('状态已调整')
    statusOpen.value = false
    await getList()
  } finally {
    saving.value = false
  }
}

async function handleDetail(row) {
  const response = await getEquipment(row.equipmentId)
  detail.value = response.data
  detailOpen.value = true
}

function handleHistory(row) {
  historyEquipment.value = row
  historyParams.pageNum = 1
  historyList.value = []
  historyTotal.value = 0
  historyOpen.value = true
  getHistory()
}

async function getHistory() {
  historyLoading.value = true
  try {
    const response = await listEquipmentHistory(historyEquipment.value.equipmentId, historyParams)
    historyList.value = response.rows
    historyTotal.value = response.total
  } finally {
    historyLoading.value = false
  }
}

function handleExport() {
  proxy.download('equipment/asset/export', { ...queryParams }, `equipment_${Date.now()}.xlsx`)
}

getList()
</script>

<style scoped>
.status-hint { color: #606266; line-height: 1.7; }
.record-text { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
