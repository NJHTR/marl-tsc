<template>
  <div class="data-fusion">
    <div class="content-row">
      <!-- Left: Data ingestion -->
      <div class="panel" style="width:320px">
        <div class="panel-header">数据注入</div>
        <el-form :model="ingestForm" label-width="80px" size="small">
          <el-form-item label="路口ID">
            <el-input v-model="ingestForm.intersectionId" placeholder="INT-001" />
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
            <el-button type="primary" @click="ingestData" :loading="ingesting" style="width:100%">注入</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- Right: Features + State vector -->
      <div class="panel" style="flex:1">
        <div class="panel-header">路口特征</div>
        <div class="inline-selects">
          <el-select v-model="featureId" filterable placeholder="选择路口" style="width:180px" @change="loadFeatures">
            <el-option v-for="item in intersections" :key="item.intersectionId"
              :label="item.name || item.intersectionId" :value="item.intersectionId" />
          </el-select>
          <el-select v-model="stateId" filterable placeholder="选择路口" style="width:180px" @change="loadState">
            <el-option v-for="item in intersections" :key="item.intersectionId"
              :label="item.name || item.intersectionId" :value="item.intersectionId" />
          </el-select>
        </div>
        <div class="feature-grid">
          <div v-if="features" class="feature-cards">
            <div class="feature-card">
              <span class="fc-value">{{ features.flow }}</span>
              <span class="fc-label">车流量</span>
            </div>
            <div class="feature-card">
              <span class="fc-value">{{ features.speed }}</span>
              <span class="fc-label">平均速度</span>
            </div>
            <div class="feature-card">
              <span class="fc-value">{{ (features.occupancy * 100).toFixed(0) }}%</span>
              <span class="fc-label">占用率</span>
            </div>
            <div class="feature-card">
              <span class="fc-value">{{ features.queueLength }}</span>
              <span class="fc-label">排队长度</span>
            </div>
            <div class="feature-card">
              <span class="fc-value">{{ features.delay }}</span>
              <span class="fc-label">延误 (s)</span>
            </div>
          </div>
          <el-empty v-else description="选择路口查看特征" :image-size="40" />
        </div>

        <el-divider style="margin:12px 0" />

        <div class="panel-header">状态向量</div>
        <div v-if="stateVector" class="state-content">
          <div ref="stateChartRef" style="height:160px"></div>
          <div class="state-meta">
            <span>路口: {{ stateVector.intersectionId }}</span>
            <span>维度: {{ stateVector.values?.length || 0 }}</span>
            <span>时间: {{ formatTime(stateVector.timestamp) }}</span>
          </div>
        </div>
        <el-empty v-else description="选择路口查看状态" :image-size="40" />
      </div>
    </div>

    <!-- Fusion params at bottom -->
    <div class="panel" style="margin-top:4px">
      <div class="panel-header">融合参数</div>
      <div class="param-bar">
        <span class="param-chip">质量阈值: 0.8</span>
        <span class="param-chip">时间窗: 30s</span>
        <span class="param-chip">空间半径: 200m</span>
        <span class="param-chip">融合策略: 加权平均</span>
        <span class="param-chip">卡尔曼滤波: 启用</span>
        <span class="param-chip">异常检测: 3-Sigma</span>
        <span class="param-chip">缺失值: 线性插值</span>
        <span class="param-chip">输出频率: 5s</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { fusionApi } from '@/api'
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

const ingestForm = ref({ intersectionId: '', sourceType: 'loop', flow: 500, speed: 40, occupancy: 0.3 })

function formatTime(ts) { return ts ? new Date(ts).toLocaleString('zh-CN') : '' }

async function loadIntersections() {
  try {
    const list = await fusionApi.listIntersectionInfo()
    intersections.value = list?.length ? list : []
  } catch (e) { /* fallback */ }
  if (!intersections.value.length) {
    intersections.value = ['INT-001', 'INT-002', 'INT-003'].map(id => ({ intersectionId: id, name: id }))
  }
}

async function loadFeatures() {
  if (!featureId.value) return
  try { features.value = await fusionApi.getFeatures(featureId.value) }
  catch (e) { ElMessage.warning('特征服务暂不可用') }
}

async function loadState() {
  if (!stateId.value) return
  try {
    stateVector.value = await fusionApi.getState(stateId.value)
    nextTick(renderStateChart)
  } catch (e) { ElMessage.warning('状态服务暂不可用') }
}

function renderStateChart() {
  if (!stateChartRef.value || !stateVector.value?.values) return
  if (!stateChart) stateChart = echarts.init(stateChartRef.value)
  stateChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', containLabel: true },
    xAxis: { type: 'category', data: stateVector.value.values.map((_, i) => `d${i}`) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: stateVector.value.values, itemStyle: { color: '#409eff' } }]
  })
}

async function ingestData() {
  ingesting.value = true
  try {
    await fusionApi.ingest({ intersectionId: ingestForm.value.intersectionId, sourceType: ingestForm.value.sourceType, timestamp: Date.now(), features: { flow: ingestForm.value.flow, speed: ingestForm.value.speed, occupancy: ingestForm.value.occupancy } })
    ElMessage.success('数据注入成功')
  } catch (e) { ElMessage.error('注入失败: ' + e.message) }
  finally { ingesting.value = false }
}

onMounted(loadIntersections)
onBeforeUnmount(() => stateChart?.dispose())
</script>

<style scoped>
.data-fusion { padding: 4px; }
.content-row { display: flex; gap: 4px; }
.panel { background: #fff; border: 1px solid #e4e7ed; padding: 12px; }
.panel-header { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 10px; padding-bottom: 6px; border-bottom: 1px solid #ebeef5; }

.inline-selects { display: flex; gap: 8px; margin-bottom: 12px; }

.feature-grid { min-height: 60px; }
.feature-cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.feature-card { padding: 10px; background: #fafafa; border: 1px solid #f0f0f0; border-radius: 3px; text-align: center; }
.fc-value { display: block; font-size: 18px; font-weight: 600; color: #303133; }
.fc-label { display: block; font-size: 11px; color: #909399; margin-top: 2px; }

.state-content { }
.state-meta { display: flex; gap: 16px; font-size: 12px; color: #909399; margin-top: 8px; }

.param-bar { display: flex; flex-wrap: wrap; gap: 6px; }
.param-chip { font-size: 12px; color: #606266; background: #f5f7fa; padding: 2px 8px; border: 1px solid #e4e7ed; border-radius: 2px; }
</style>
