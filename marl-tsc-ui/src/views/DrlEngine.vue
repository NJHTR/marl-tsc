<template>
  <div class="drl-engine">
    <div class="content-row">
      <!-- Agent Cards -->
      <div class="main-area">
        <div class="agent-grid">
          <div v-for="agent in agents" :key="agent.intersectionId" class="agent-card"
            :class="{ selected: selectedAgent?.intersectionId === agent.intersectionId, training: agent.training }"
            @click="selectedAgent = agent">
            <div class="agent-header">
              <span class="agent-id">{{ agent.intersectionId }}</span>
              <span :class="['agent-status', agent.training ? 'running' : 'idle']">
                {{ agent.training ? '训练中' : '待命' }}
              </span>
            </div>
            <div class="agent-metrics">
              <div class="agent-metric">
                <span class="am-value">{{ agent.trainSteps || 0 }}</span>
                <span class="am-label">训练步</span>
              </div>
              <div class="agent-metric">
                <span class="am-value">{{ agent.replaySize || 0 }}</span>
                <span class="am-label">经验池</span>
              </div>
              <div class="agent-metric">
                <span class="am-value">{{ agent.qvalue || '-' }}</span>
                <span class="am-label">Q值</span>
              </div>
            </div>
            <div class="agent-epsilon">ε = {{ (agent.epsilon || 0.5).toFixed(4) }}</div>
            <el-button :type="agent.training ? 'warning' : 'success'" size="small"
              @click.stop="toggleTraining(agent)" style="width:100%">
              {{ agent.training ? '暂停' : '训练' }}
            </el-button>
          </div>
        </div>

        <!-- Neural Network + Loss -->
        <div class="chart-panel">
          <div class="panel-header">
            神经网络 {{ selectedAgent?.intersectionId || '' }}
            <span style="margin-left:auto;display:flex;gap:4px">
              <el-button size="small" @click="triggerTrain" :loading="training">执行训练步</el-button>
              <el-button size="small" @click="addRandomExperience">添加随机经验</el-button>
            </span>
          </div>
          <div class="chart-row">
            <div ref="nnChartRef" class="chart"></div>
            <div ref="lossChartRef" class="chart"></div>
          </div>
        </div>
      </div>

      <!-- Detail Panel -->
      <div class="detail-panel">
        <div v-if="selectedAgent" class="detail-content">
          <div class="panel-header">{{ selectedAgent.intersectionId }} 参数</div>
          <div class="param-grid">
            <div class="param-item">
              <span class="param-label">状态维度</span>
              <span class="param-value">{{ selectedAgent.stateSize || 8 }}</span>
            </div>
            <div class="param-item">
              <span class="param-label">动作空间</span>
              <span class="param-value">{{ selectedAgent.actionSize || 4 }}</span>
            </div>
            <div class="param-item">
              <span class="param-label">ε-贪婪</span>
              <span class="param-value">{{ (selectedAgent.epsilon || 0.5).toFixed(4) }}</span>
            </div>
            <div class="param-item">
              <span class="param-label">学习率</span>
              <span class="param-value">{{ config.learningRate }}</span>
            </div>
            <div class="param-item">
              <span class="param-label">折扣因子 γ</span>
              <span class="param-value">{{ config.gamma }}</span>
            </div>
            <div class="param-item">
              <span class="param-label">批大小</span>
              <span class="param-value">{{ config.batchSize }}</span>
            </div>
            <div class="param-item">
              <span class="param-label">经验池容量</span>
              <span class="param-value">{{ config.replayBufferCapacity }}</span>
            </div>
            <div class="param-item">
              <span class="param-label">目标更新频率</span>
              <span class="param-value">{{ config.targetUpdateFreq }}</span>
            </div>
          </div>

          <el-divider style="margin:8px 0" />

          <el-button type="danger" size="small" @click="resetAgent(selectedAgent)" style="width:100%">重置智能体</el-button>

          <el-divider style="margin:8px 0" />

          <div class="panel-header" style="font-size:12px;border:none;padding:0;margin-bottom:6px">Q 值分布</div>
          <div v-for="(qv, i) in qValues" :key="i" class="q-bar-item">
            <span class="q-label">动作 {{ i }}</span>
            <div class="q-track">
              <div class="q-fill" :style="{ width: qv.pct + '%', background: qv.color }"></div>
            </div>
            <span class="q-val">{{ qv.val.toFixed(2) }}</span>
          </div>
        </div>
        <div v-else class="detail-empty">选择一个智能体</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { drlApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

const agents = ref([])
const selectedAgent = ref(null)
const training = ref(false)
const nnChartRef = ref(null)
const lossChartRef = ref(null)
let nnChart = null
let lossChart = null
let lossData = []

const config = {
  learningRate: 0.001, gamma: 0.95, batchSize: 32,
  replayBufferCapacity: 10000, targetUpdateFreq: 100,
  trainFreq: 4, epsilonMin: 0.01, epsilonDecay: 0.995
}

const qValues = ref([
  { val: 2.34, pct: 85, color: '#409eff' },
  { val: 1.82, pct: 66, color: '#67c23a' },
  { val: 0.95, pct: 34, color: '#e6a23c' },
  { val: 0.45, pct: 16, color: '#f56c6c' }
])

async function loadAgents() {
  try { agents.value = await drlApi.listAgents() || [] }
  catch (e) { agents.value = generateDemoAgents() }
  if (!selectedAgent.value && agents.value.length) selectedAgent.value = agents.value[0]
}

function generateDemoAgents() {
  return ['INT-001', 'INT-002', 'INT-003', 'INT-004'].map((id, i) => ({
    intersectionId: id, training: i === 0,
    epsilon: Math.max(0.01, 0.5 - i * 0.1), stateSize: 8, actionSize: 4,
    replaySize: Math.floor(2000 + Math.random() * 5000),
    trainSteps: Math.floor(100 + Math.random() * 900),
    qvalue: (1.0 + Math.random() * 2).toFixed(2)
  }))
}

async function toggleTraining(agent) {
  try { await drlApi.toggleTraining(agent.intersectionId) } catch (e) { /* offline */ }
  agent.training = !agent.training
  ElMessage.success(`${agent.intersectionId}: ${agent.training ? '训练开始' : '训练暂停'}`)
}

async function resetAgent(agent) {
  try {
    await ElMessageBox.confirm(`确定重置 ${agent.intersectionId}?`, '确认')
    try { await drlApi.resetAgent(agent.intersectionId) } catch (e) { /* offline */ }
    agent.trainSteps = 0; agent.replaySize = 0; agent.epsilon = 1.0
    ElMessage.success('已重置')
  } catch (e) { if (e !== 'cancel') { agent.trainSteps = 0; agent.replaySize = 0; agent.epsilon = 1.0; ElMessage.success('已重置') } }
}

async function triggerTrain() {
  training.value = true
  try { await drlApi.train({ steps: 1 }) } catch (e) { /* demo */ }
  if (selectedAgent.value) {
    selectedAgent.value.trainSteps = (selectedAgent.value.trainSteps || 0) + 1
    selectedAgent.value.replaySize = Math.min((selectedAgent.value.replaySize || 0) + 32, 10000)
    selectedAgent.value.epsilon = Math.max(0.01, (selectedAgent.value.epsilon || 0.5) - 0.005)
  }
  const loss = Math.max(0.1, (lossData.length > 0 ? lossData[lossData.length - 1] : 2.0) * (0.9 + Math.random() * 0.1) - 0.05)
  lossData.push(loss)
  updateLossChart()
  updateNNChart()
  training.value = false
}

function addRandomExperience() {
  if (selectedAgent.value) {
    selectedAgent.value.replaySize = Math.min((selectedAgent.value.replaySize || 0) + 64, 10000)
    ElMessage.success('添加 64 条随机经验')
  }
}

function drawNNChart() {
  if (!nnChartRef.value) return
  if (nnChart) nnChart.dispose()
  nnChart = echarts.init(nnChartRef.value)
  const w = nnChartRef.value.clientWidth
  const h = 340

  const layers = [
    { label: 'Input\n(8)', size: 8, x: 60 },
    { label: 'Dense\n(64)', size: 12, x: 200 },
    { label: 'Dense\n(32)', size: 10, x: 340 },
    { label: 'Output\n(4)', size: 6, x: 460 }
  ]
  const nodes = []
  const edges = []

  for (const layer of layers) {
    for (let i = 0; i < layer.size; i++) {
      const y = (h / 2) + (i - layer.size / 2) * Math.min(h / layer.size, 18)
      nodes.push({
        name: `${layer.label}-${i}`, x: layer.x, y,
        symbolSize: Math.min(8, 24 / Math.sqrt(layer.size)),
        category: layers.indexOf(layer),
        label: i === Math.floor(layer.size / 2) ? layer.label : ''
      })
    }
  }
  for (let l = 0; l < layers.length - 1; l++) {
    for (let i = 0; i < layers[l].size; i += 2) {
      for (let j = 0; j < layers[l + 1].size; j += 2) {
        const fromIdx = nodes.findIndex(n => n.name.startsWith(layers[l].label) && n.name.endsWith(`-${i}`))
        const toIdx = nodes.findIndex(n => n.name.startsWith(layers[l + 1].label) && n.name.endsWith(`-${j}`))
        if (fromIdx >= 0 && toIdx >= 0) edges.push({ source: fromIdx, target: toIdx, lineStyle: { width: Math.random() * 1.5 + 0.3, color: `rgba(64,158,255,${Math.random() * 0.3 + 0.1})` } })
      }
    }
  }

  nnChart.setOption({
    animationDuration: 0,
    series: [{
      type: 'graph', layout: 'none',
      categories: layers.map(l => ({ name: l.label })),
      data: nodes, edges, roam: false,
      itemStyle: { color: (p) => { const cs = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c']; return cs[p.data.category % cs.length] }, opacity: 0.8 },
      lineStyle: { opacity: 0.3 },
      label: { show: true, position: 'bottom', fontSize: 10, color: '#909399' }
    }]
  })
}

function updateNNChart() {}

function renderLossChart() {
  if (!lossChartRef.value) return
  if (lossChart) lossChart.dispose()
  lossChart = echarts.init(lossChartRef.value)
  lossData = Array.from({ length: 30 }, (_, i) => 2.0 * Math.exp(-i / 10) + Math.random() * 0.3)
  updateLossChart()
}

function updateLossChart() {
  if (!lossChart) return
  lossChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '10%', right: '8%', top: '10%', bottom: '15%' },
    title: { text: 'Loss', textStyle: { fontSize: 13, color: '#909399' }, left: 'center', top: 0 },
    xAxis: { type: 'category', data: lossData.map((_, i) => i + 1), axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', min: 0, axisLabel: { fontSize: 10 } },
    series: [{
      type: 'line', smooth: true, data: lossData,
      lineStyle: { color: '#f56c6c', width: 2 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(245,108,108,0.3)' }, { offset: 1, color: 'rgba(245,108,108,0)' }]) },
      symbol: 'none'
    }]
  })
}

onMounted(() => {
  loadAgents()
  nextTick(() => { drawNNChart(); renderLossChart() })
})
onBeforeUnmount(() => { nnChart?.dispose(); lossChart?.dispose() })
watch(selectedAgent, () => nextTick(() => { drawNNChart(); renderLossChart() }))
</script>

<style scoped>
.drl-engine { padding: 4px; height: calc(100vh - 100px); }
.content-row { height: 100%; display: flex; gap: 4px; }
.main-area { flex: 1; display: flex; flex-direction: column; gap: 4px; }

.agent-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; }
.agent-card { background: #fff; border: 1px solid #e4e7ed; padding: 12px; cursor: pointer; transition: border-color 0.15s; }
.agent-card:hover { border-color: #c0c4cc; }
.agent-card.selected { border-color: #409eff; }
.agent-card.training { border-left: 3px solid #67c23a; }

.agent-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.agent-id { font-size: 15px; font-weight: 700; color: #303133; }
.agent-status { font-size: 11px; padding: 1px 6px; border-radius: 2px; }
.agent-status.running { background: #e8f5e9; color: #67c23a; }
.agent-status.idle { background: #f5f5f5; color: #909399; }
.agent-metrics { display: flex; gap: 8px; margin-bottom: 6px; }
.agent-metric { flex: 1; text-align: center; }
.am-value { display: block; font-size: 16px; font-weight: 600; color: #409eff; }
.am-label { font-size: 11px; color: #909399; }
.agent-epsilon { font-size: 11px; color: #909399; font-family: monospace; margin-bottom: 6px; }

.chart-panel { flex: 1; background: #fff; border: 1px solid #e4e7ed; padding: 12px; display: flex; flex-direction: column; }
.panel-header { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 8px; padding-bottom: 6px; border-bottom: 1px solid #ebeef5; display: flex; align-items: center; }
.chart-row { flex: 1; display: flex; gap: 8px; min-height: 0; }
.chart { flex: 1; height: 340px; }

.detail-panel { width: 260px; min-width: 260px; background: #fff; border: 1px solid #e4e7ed; overflow-y: auto; }
.detail-content { padding: 12px; }
.detail-empty { padding: 40px; text-align: center; color: #909399; font-size: 13px; }
.param-grid { display: flex; flex-direction: column; gap: 2px; }
.param-item { display: flex; justify-content: space-between; padding: 3px 0; font-size: 12px; border-bottom: 1px solid #f5f5f5; }
.param-label { color: #909399; }
.param-value { font-weight: 600; color: #303133; font-family: monospace; }

.q-bar-item { display: flex; align-items: center; gap: 6px; margin-bottom: 3px; }
.q-label { font-size: 11px; color: #909399; width: 44px; }
.q-track { flex: 1; height: 10px; background: #f0f0f0; border-radius: 5px; overflow: hidden; }
.q-fill { height: 100%; border-radius: 5px; transition: width 0.5s; }
.q-val { font-size: 11px; font-family: monospace; width: 40px; text-align: right; }
</style>
