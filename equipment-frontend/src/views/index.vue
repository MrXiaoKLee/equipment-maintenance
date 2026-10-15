<template>
  <div class="app-container project-home">
    <header>
      <p class="version">v0.1.0 · 本地演示</p>
      <h1>设备故障与维护记录管理系统</h1>
      <p>登记设备，跟踪故障处理，保留每轮结果与复核记录。</p>
    </header>
    <el-alert title="设备、工单及处理结果均为模拟演示数据，不代表真实设备运行或维修成效。" type="info" :closable="false" />
    <section aria-label="功能入口" class="entry-grid">
      <el-card v-hasPermi="['equipment:asset:query']" shadow="never">
        <h2>设备台账</h2>
        <p>查看设备基本信息、使用状态和关联工单历史。</p>
        <el-button type="primary" plain @click="router.push('/equipment/asset')">进入设备台账</el-button>
      </el-card>
      <el-card v-hasPermi="['equipment:workorder:list']" shadow="never">
        <h2>故障工单</h2>
        <p>按角色完成报修、分配、处理、复核和记录导出。</p>
        <el-button type="primary" plain @click="router.push('/equipment/workorder')">进入故障工单</el-button>
      </el-card>
      <el-card v-hasPermi="['equipment:statistics:query']" shadow="never">
        <h2>统计分析</h2>
        <p>管理人员按提交日期和设备查看报修分布与闭环时长。</p>
        <el-button type="primary" plain @click="router.push('/equipment/statistics')">进入统计分析</el-button>
      </el-card>
    </section>
    <section class="workflow" aria-labelledby="workflow-title">
      <h2 id="workflow-title">工单处理流程</h2>
      <ol>
        <li>报修人员登记异常</li>
        <li>管理人员分配任务</li>
        <li>维护人员记录处理并提交</li>
        <li>管理人员通过关闭，或退回补充</li>
      </ol>
      <p>列表与导出只展示本人角色范围内的记录。已提交的每轮结果保留在时间线中。</p>
    </section>
    <footer>
      基于 <a href="https://github.com/yangzongzhuan/RuoYi-Vue" target="_blank" rel="noopener noreferrer">RuoYi 3.9.2</a> 开发，复用登录、用户、角色、菜单及通用管理能力。
    </footer>
  </div>
</template>

<script setup name="Index">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.project-home { max-width: 1400px; }
header { margin-bottom: 20px; }
.version { color: var(--el-color-primary); font-size: 13px; }
h1 { font-size: 24px; line-height: 1.5; margin: 8px 0; }
h2 { font-size: 17px; margin-top: 0; }
p, li, footer { color: var(--el-text-color-regular); line-height: 1.8; }
.entry-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 260px), 1fr)); gap: 16px; margin: 20px 0; }
.entry-grid p { min-height: 52px; font-size: 14px; }
.workflow { border: 1px solid var(--el-border-color-light); border-radius: 4px; padding: 20px; }
ol { margin: 0; padding-left: 24px; }
footer { margin-top: 20px; font-size: 13px; }
footer a { color: var(--el-color-primary); }
</style>
