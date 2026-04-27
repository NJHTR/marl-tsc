<template>
  <div class="route-planning">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never" class="section-card">
          <template #header><span style="font-weight:600">路径查询</span></template>
          <el-form :model="form" label-width="100px">
            <el-form-item label="起点路口">
              <el-select v-model="form.originId" filterable style="width:100%">
                <el-option v-for="item in intersections" :key="item.intersectionId"
                  :label="item.name || item.intersectionId" :value="item.intersectionId" />
              </el-select>
            </el-form-item>
            <el-form-item label="终点路口">
              <el-select v-model="form.destinationId" filterable style="width:100%">
                <el-option v-for="item in intersections" :key="item.intersectionId"
                  :label="item.name || item.intersectionId" :value="item.intersectionId" />
              </el-select>
            </el-form-item>
            <el-form-item label="车辆类型">
              <el-radio-group v-model="form.vehicleType">
                <el-radio value="car">小汽车</el-radio>
                <el-radio value="bus">公交车</el-radio>
                <el-radio value="emergency">应急车</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="优先级">
              <el-slider v-model="form.priority" :min="1" :max="5" :step="1" show-stops />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="planRoute" :loading="loading" size="large" style="width:100%">
                规划路径
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never" class="section-card" style="margin-bottom:16px">
          <template #header><span style="font-weight:600">推荐路径</span></template>
          <div v-if="routeResult">
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="路径ID">{{ routeResult.routeId }}</el-descriptions-item>
              <el-descriptions-item label="距离(km)">{{ routeResult.totalDistance }}</el-descriptions-item>
              <el-descriptions-item label="预计时间(min)">{{ routeResult.estimatedTime }}</el-descriptions-item>
            </el-descriptions>
            <el-divider />
            <el-steps :active="pathNodes.length" align-center>
              <el-step v-for="(node, i) in pathNodes" :key="i" :title="'路口 ' + (i + 1)" :description="node.intersectionId" />
            </el-steps>
          </div>
          <el-empty v-else description="请输入起点和终点进行路径规划" />
        </el-card>
        <el-card shadow="never" class="section-card">
          <template #header><span style="font-weight:600">备选路径</span></template>
          <el-table :data="alternatives" border stripe size="small" v-if="alternatives.length">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="routeId" label="路径ID" />
            <el-table-column prop="totalDistance" label="距离(km)" />
            <el-table-column prop="estimatedTime" label="预计时间(min)" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="selectAlt(row)">选择</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无备选路径" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { routeApi, webApi } from '@/api'
import { ElMessage } from 'element-plus'

const intersections = ref([])
const loading = ref(false)
const routeResult = ref(null)
const alternatives = ref([])
const pathNodes = ref([])

const form = ref({
  originId: '',
  destinationId: '',
  vehicleType: 'car',
  priority: 3
})

async function loadIntersections() {
  try {
    intersections.value = await webApi.listIntersections() || []
  } catch (e) { console.error(e) }
}

async function planRoute() {
  if (!form.value.originId || !form.value.destinationId) {
    ElMessage.warning('请选择起点和终点')
    return
  }
  if (form.value.originId === form.value.destinationId) {
    ElMessage.warning('起点和终点不能相同')
    return
  }
  loading.value = true
  try {
    const res = await routeApi.planRoute({
      originId: form.value.originId,
      destinationId: form.value.destinationId,
      vehicleType: form.value.vehicleType,
      priority: form.value.priority
    })
    routeResult.value = res
    pathNodes.value = res.path || []

    // Load alternatives
    const alts = await routeApi.getAlternatives({
      originId: form.value.originId,
      destinationId: form.value.destinationId,
      vehicleType: form.value.vehicleType,
      priority: form.value.priority
    })
    alternatives.value = alts || []
  } catch (e) {
    ElMessage.error('路径规划失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

function selectAlt(row) {
  routeResult.value = row
  pathNodes.value = row.path || []
  ElMessage.success('已切换路径')
}

loadIntersections()
</script>

<style scoped>
.route-planning { padding: 4px; }
.section-card { border-radius: 6px; }
</style>
