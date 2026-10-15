<template>
  <el-dialog v-model="visible" :title="title" width="680px" class="workorder-action" append-to-body
    :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
    <p>{{ workOrder.workOrderNo }} · {{ workOrder.title }}</p>
    <p>如提示工单已更新，请返回并刷新详情，核对最新状态后重试。</p>
    <el-form ref="formRef" :model="form" :disabled="saving" label-width="100px">
      <template v-if="mode === 'submit'">
        <el-alert title="提交后本轮结果不可修改；复核退回后可以补充并重新提交，历史将保留。" type="info" :closable="false" class="mb8" />
        <el-form-item v-for="field in resultFields" :key="field.key" :label="field.label" :prop="field.key" :rules="required(field.label)">
          <el-input v-model="form[field.key]" type="textarea" :rows="3" maxlength="2000" show-word-limit :placeholder="field.placeholder" />
        </el-form-item>
      </template>
      <template v-else>
        <el-form-item v-if="mode === 'review'" label="复核决定" prop="decision" :rules="[{ required: true, message: '请选择复核决定', trigger: 'change' }]">
          <el-radio-group v-model="form.decision"><el-radio value="APPROVE">通过并关闭</el-radio><el-radio value="REJECT">退回处理</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item :label="descriptionLabel" prop="description" :rules="required(descriptionLabel)">
          <el-input v-model="form.description" type="textarea" :rows="5" maxlength="2000" show-word-limit :placeholder="`请输入${descriptionLabel}`" />
        </el-form-item>
        <el-alert v-if="mode === 'review' && form.decision === 'APPROVE'" title="通过将结束本次工单，设备使用状态保持原值。" type="info" :closable="false" />
      </template>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="visible = false">返回</el-button>
      <el-button type="primary" :loading="saving" @click="save">{{ mode === 'review' ? '确认复核' : title }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { addProcessRecord, submitWorkOrderReview, reviewWorkOrder } from '@/api/equipment/workorder'

const props = defineProps({ modelValue: Boolean, workOrder: { type: Object, required: true }, mode: String })
const emit = defineEmits(['update:modelValue', 'saved'])
const { proxy } = getCurrentInstance()
const visible = computed({ get: () => props.modelValue, set: value => emit('update:modelValue', value) })
const title = computed(() => ({ note: '追加处理记录', submit: '提交处理结果', review: '复核处理结果' })[props.mode])
const descriptionLabel = computed(() => props.mode === 'note' ? '处理记录' : form.value.decision === 'REJECT' ? '退回原因' : '复核结论')
const form = ref({})
const saving = ref(false)
const formRef = ref()
const resultFields = [
  { key: 'causeAnalysis', label: '原因判断', placeholder: '记录排查判断，尚未确认可如实填写待确认' },
  { key: 'measures', label: '处理措施', placeholder: '说明采取的处理措施' },
  { key: 'resultDescription', label: '处理结果', placeholder: '记录处理后的复查情况与结果' }
]
const required = label => [{ required: true, whitespace: true, message: `请输入${label}`, trigger: 'blur' }]

watch(() => props.modelValue, open => {
  if (!open) return
  form.value = props.mode === 'submit' ? { causeAnalysis: '', measures: '', resultDescription: '' } : { description: '', decision: undefined }
  nextTick(() => formRef.value?.clearValidate())
})

async function save() {
  if (saving.value || !await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    if (props.mode === 'review') {
      await proxy.$modal.confirm(`确认对工单 ${props.workOrder.workOrderNo} ${form.value.decision === 'APPROVE' ? '复核通过并关闭' : '退回处理'}？`)
    }
    const action = { note: addProcessRecord, submit: submitWorkOrderReview, review: reviewWorkOrder }[props.mode]
    await action(props.workOrder.workOrderId, { ...form.value, version: props.workOrder.version })
    visible.value = false
    proxy.$modal.msgSuccess(props.mode === 'note' ? '处理记录已追加' : props.mode === 'submit' ? '处理结果已提交复核' : '复核结果已保存')
    emit('saved')
  } catch {
    // 请求层统一提示错误，保留未提交内容。
  } finally { saving.value = false }
}
</script>

<style>
.workorder-action { max-width: calc(100vw - 32px); }
</style>
