<template>
  <div class="data-fusion">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never" class="section-card">
          <template #header><span style="font-weight:600">数据注入</span></template>
          <el-form :model="ingestForm" label-width="100px">
            <el-form-item label="路口ID">
              <el-input v-model="ingestForm.intersectionId" placeholder="如: INT-001" />
            </el-form-item>
            <el-form-item label="数据来源">
              <el-select v-model="ingestForm.sourceType" style="width:100%">
                <el-option label="线圈检测器" value="loop" />
                <el-option label="视频检测" value="video" />
                <el-option label="雷达检测" value="radar" />
                <el-option label="网联车" value="connected" />
              </el-select>
            </el-form-item>
            <el-form-item label="车流量">
              <el-input-number v-model="ingestForm.flow" :min="0" :max="10000" style="width:100%" />
            </el-form-item>
            <el-form-item label="平均速度">
              <el-input-number v-model="ingestForm.speed" :min="0" :max="120" style="width:100%" />
            </el-form-item>
            <el-form-item label="占用率">
              <el-slider v-model="ingestForm.occupancy" :min="0" :max="1" :step="0.01" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="ingestData" :loading="ingesting" size="large" style="width:100%">
                注入数据
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="never" class="section-card">
              <template #header><span style="font-weight:600">路口特征</span></template>
              <el-select v-model="featureId" filterable placeholder="选择路口" style="width:100%;margin-bottom:16px"
                @change="loadFeatures">
                <el-option v-for="item in intersections" :key="item.intersectionId"
                  :label="item.name || item.intersectionId" :value="item.intersectionId" />
              </el-select>
              <div v-if="features">
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="车流量">{{ features.flow }}</el-descriptions-item>
                  <el-descriptions-item label="平均速度">{{ features.speed }}</el-descriptions-item>
                  <el-descriptions-item label="占用率">
                    <el-progress :percentage="Math.round((features.occupancy || 0) * 100)"
                      :color="features.occupancy > 0.8 ? '#f56c6c' : features.occupancy > 0.5 ? '#e6a23c' : '#67c23a'"
                      :stroke-width="12" />
                  </el-descriptions-item>
                  <el-descriptions-item label="排队长度(m)">{{ features.queueLength }}</el-descriptions-item>
                  <el-descriptions-item label="延误(s)">{{ features.delay }}</el-descriptions-item>
                </el-descriptions>
              </div>
              <el-empty v-else description="选择路口查看特征" />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" class="section-card">
              <template #header><span style="font-weight:600">状态向量</span></template>
              <el-select v-model="stateId" filterable placeholder="选择路口" style="width:100%;margin-bottom:16px"
                @change="loadState">
                <el-option v-for="item in intersections" :key="item.intersectionId"
                  :label="item.name || item.intersectionId" :value="item.intersectionId" />
              </el-select>
              <div v-if="stateVector">
                <div ref="stateChartRef" style="height:200px"></div>
                <el-descriptions :column="1" border size="small" style="margin-top:12px">
                  <el-descriptions-item label="路口ID">{{ stateVector.intersectionId }}</el-descriptions-item>
                  <el-descriptions-item label="向量维度">{{ stateVector.values?.length || 0 }}</el-descriptions-item>
                  <el-descriptions-item label="时间戳">{{ formatTime(stateVector.timestamp) }}</el-descriptions-item>
                </el-descriptions>
              </div>
              <el-empty v-else description="选择路口查看状态" />
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="never" class="section-card" style="margin-top:16px">
          <template #header><span style="font-weight:600">融合参数</span></template>
          <el-descriptions :column="4" border size="small">
            <el-descriptions-item label="质量阈值">0.8</el-descriptions-item>
            <el-descriptions-item label="时间窗(秒)">30</el-descriptions-item>
            <el-descriptions-item label="空间半径(m)">200</el-descriptions-item>
            <el-descriptions-item label="融合策略">加权平均</el-descriptions-item>
            <el-descriptions-item label="卡尔曼滤波">启用</el-descriptions-item>
            <el-descriptions-item label="异常检测">3-Sigma</el-descriptions-item>
            <el-descriptions-item label="缺失值处理">线性插值</el-descriptions-item>
            <el-descriptions-item label="输出频率">5s</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { fusionApi, webApi } from '@/api'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const intersections = ref([])
const featureId = ref('')
const stateId = ref('')
const features = ref(null)
const stateVector = ref(null)
const ingesting = ref(false)
const stateChartRef = ref(null)
let stateChart = null

const ingestForm = ref({
  intersectionId: '',
  sourceType: 'loop',
  flow: 500,
  speed: 40,
  occupancy: 0.3
})

function formatTime(ts) {
  if (!ts) return ''
  return new Date(ts).toLocaleString('zh-CN')
}

async function loadIntersections() {
  try {
    intersections.value = await webApi.listIntersections() || []
  } catch (e) { console.error(e) }
}

async function loadFeatures() {
  if (!featureId.value) return
  try {
    features.value = await fusionApi.getFeatures(featureId.value)
  } catch (e) {
    ElMessage.warning('特征服务暂不可用')
  }
}

async function loadState() {
  if (!stateId.value) return
  try {
    stateVector.value = await fusionApi.getState(stateId.value)
    nextTick(renderStateChart)
  } catch (e) {
    ElMessage.warning('状态服务暂不可用')
  }
}

function renderStateChart() {
  if (!stateChartRef.value || !stateVector.value?.values) return
  if (!stateChart) stateChart = echarts.init(stateChartRef.value)
  stateChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', containLabel: true },
    xAxis: { type: 'category', data: stateVector.value.values.map((_, i) => `d${i}`) },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar', data: stateVector.value.values,
      itemStyle: { color: '#409eff' }
    }]
  })
}

async function ingestData() {
  ingesting.value = true
  try {
    await fusionApi.ingest({
      intersectionId: ingestForm.value.intersectionId,
      sourceType: ingestForm.value.sourceType,
      timestamp: Date.now(),
      features: {
        flow: ingestForm.value.flow,
        speed: ingestForm.value.speed,
        occupancy: ingestForm.value.occupancy
      }
    })
    ElMessage.success('数据注入成功')
  } catch (e) {
    ElMessage.error('注入失败: ' + e.message)
  } finally {
    ingesting.value = false
  }
}

onMounted(loadIntersections)
onBeforeUnmount(() => stateChart?.dispose())
</script>

<style scoped>
.data-fusion { padding: 4px; }
.section-card { border-radius: 6px; }
</style>
