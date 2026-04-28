<template>
  <div class="route-container">
    <!-- Left: Route form panel -->
    <div class="route-sidebar">
      <div class="panel-section">
        <div class="panel-title">路径查询</div>
        <div class="form-group">
          <label class="form-label">起点路口</label>
          <el-select v-model="form.originId" filterable placeholder="选择或点击地图" style="width:100%"
            @change="onOriginChange">
            <el-option-group v-if="intersections.length" label="预设路口">
              <el-option v-for="item in intersections" :key="item.intersectionId"
                :label="`${item.intersectionId} (${item.latitude?.toFixed(4)}, ${item.longitude?.toFixed(4)})`"
                :value="item.intersectionId" />
            </el-option-group>
            <el-option-group v-if="customPoints.length" label="自定义点">
              <el-option v-for="item in customPoints" :key="item.intersectionId"
                :label="item.name" :value="item.intersectionId" />
            </el-option-group>
          </el-select>
        </div>
        <div class="form-group">
          <label class="form-label">终点路口</label>
          <el-select v-model="form.destinationId" filterable placeholder="选择或点击地图" style="width:100%"
            @change="onDestinationChange">
            <el-option-group v-if="intersections.length" label="预设路口">
              <el-option v-for="item in intersections" :key="item.intersectionId"
                :label="`${item.intersectionId} (${item.latitude?.toFixed(4)}, ${item.longitude?.toFixed(4)})`"
                :value="item.intersectionId" />
            </el-option-group>
            <el-option-group v-if="customPoints.length" label="自定义点">
              <el-option v-for="item in customPoints" :key="item.intersectionId"
                :label="item.name" :value="item.intersectionId" />
            </el-option-group>
          </el-select>
        </div>
        <div class="form-group">
          <label class="form-label">车辆类型</label>
          <el-radio-group v-model="form.vehicleType" class="vehicle-group">
            <el-radio value="car" size="small">小汽车</el-radio>
            <el-radio value="bus" size="small">公交车</el-radio>
            <el-radio value="emergency" size="small">应急车</el-radio>
          </el-radio-group>
        </div>
        <div class="form-group">
          <label class="form-label">优先级 ({{ form.priority }})</label>
          <el-slider v-model="form.priority" :min="1" :max="5" :step="1" show-stops size="small" />
        </div>
        <el-button type="primary" @click="planRoute" :loading="loading" style="width:100%;margin-top:8px">
          计算路径
        </el-button>
      </div>

      <!-- Route result -->
      <div v-if="routeResult" class="panel-section">
        <div class="panel-title">路径详情</div>
        <div class="route-stat">
          <span class="route-stat-label">距离</span>
          <span class="route-stat-value">{{ (routeResult.totalDistance / 1000).toFixed(2) }} km</span>
        </div>
        <div class="route-stat">
          <span class="route-stat-label">预计时间</span>
          <span class="route-stat-value">{{ (routeResult.estimatedTime / 60).toFixed(1) }} min</span>
        </div>
        <div class="route-stat">
          <span class="route-stat-label">途径路口</span>
          <span class="route-stat-value">{{ routeResult.path?.length || 0 }} 个</span>
        </div>
        <div class="node-list">
          <div v-for="(node, i) in routeResult.path" :key="i" class="node-item" :class="{ active: i === hoverNodeIndex }"
            @mouseenter="hoverNodeIndex = i" @mouseleave="hoverNodeIndex = -1">
            <span class="node-index">{{ i + 1 }}</span>
            <span class="node-id">{{ node.intersectionId }}</span>
            <span v-if="i < routeResult.path.length - 1" class="node-arrow"></span>
          </div>
        </div>
      </div>

      <!-- Alternatives -->
      <div v-if="alternatives.length > 1" class="panel-section">
        <div class="panel-title">备选路径 ({{ alternatives.length - 1 }})</div>
        <div v-for="(alt, i) in alternatives.slice(1)" :key="i" class="alt-item"
          :class="{ selected: selectedAltIndex === i + 1 }"
          @click="selectAlt(i + 1)">
          <div class="alt-header">
            <span class="alt-label">方案 {{ i + 2 }}</span>
            <span class="alt-distance">{{ (alt.totalDistance / 1000).toFixed(2) }} km</span>
            <span class="alt-time">{{ (alt.estimatedTime / 60).toFixed(1) }} min</span>
          </div>
          <div class="alt-path">{{ alt.path?.map(n => n.intersectionId).join(' → ') }}</div>
        </div>
      </div>
    </div>

    <!-- Right: Map -->
    <div class="route-map">
      <div class="map-click-hint-banner">点击地图任意位置自由选点</div>
      <div ref="mapRef" class="leaflet-map"></div>
      <div class="map-legend">
        <div class="legend-item"><span class="legend-dot primary"></span> 推荐路径</div>
        <div class="legend-item"><span class="legend-dot alt"></span> 备选路径</div>
        <div class="legend-item"><span class="legend-dot origin"></span> 起点</div>
        <div class="legend-item"><span class="legend-dot dest"></span> 终点</div>
        <div class="legend-item"><span class="legend-dot custom"></span> 自定义点</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { routeApi, fusionApi } from '@/api'
import { ElMessage } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const intersections = ref([])
const customPoints = ref([]) // 用户自由点击产生的自定义点
const loading = ref(false)
const routeResult = ref(null)
const alternatives = ref([])
const selectedAltIndex = ref(0)
const hoverNodeIndex = ref(-1)

const mapRef = ref(null)
let map = null
let markers = []
let routeLines = []
let roadLines = []

const form = ref({
  originId: '',
  destinationId: '',
  vehicleType: 'car',
  priority: 3
})

// Default center: Shanghai
const MAP_CENTER = [31.2304, 121.4737]

function initMap() {
  if (!mapRef.value || map) return

  map = L.map(mapRef.value, {
    center: MAP_CENTER,
    zoom: 15,
    zoomControl: true,
    attributionControl: false
  })

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19
  }).addTo(map)

  // Click on map to place a point at the exact clicked location
  map.on('click', (e) => {
    const point = addCustomPoint(e.latlng.lat, e.latlng.lng)
    selectPoint(point.intersectionId)
  })

  setTimeout(() => map.invalidateSize(), 200)
}

function selectPoint(id) {
  if (!form.value.originId || (form.value.originId && form.value.destinationId)) {
    form.value.originId = id
    onOriginChange()
  } else {
    form.value.destinationId = id
    onDestinationChange()
  }
}

function addCustomPoint(lat, lng) {
  // Check if we already have a custom point at roughly this location
  const existing = customPoints.value.find(p =>
    Math.abs(p.latitude - lat) < 0.0001 && Math.abs(p.longitude - lng) < 0.0001
  )
  if (existing) return existing

  const id = `CUSTOM-${customPoints.value.length + 1}-${Date.now()}`
  const point = {
    intersectionId: id,
    latitude: lat,
    longitude: lng,
    name: `自定义点 (${lat.toFixed(4)}, ${lng.toFixed(4)})`
  }
  customPoints.value.push(point)
  return point
}

function findNearestIntersection(lat, lng, threshold = 0.01) {
  let nearest = null
  let minDist = Infinity
  for (const node of intersections.value) {
    if (node.latitude == null || node.longitude == null) continue
    const d = Math.sqrt(Math.pow(lat - node.latitude, 2) + Math.pow(lng - node.longitude, 2))
    if (d < minDist) {
      minDist = d
      nearest = node
    }
  }
  return minDist < threshold ? nearest : null
}

function renderRoadNetwork() {
  if (!map) return
  // Clear existing road lines
  for (const l of roadLines) map.removeLayer(l)
  roadLines = []

  const nodes = intersections.value.filter(n => n.latitude != null && n.longitude != null)
  // Build grid edges (adjacent intersections by ID)
  for (const node of nodes) {
    const num = parseInt(node.intersectionId.replace('INT-', ''))
    const row = Math.floor((num - 1) / 3)
    const col = (num - 1) % 3

    // Right neighbor
    if (col < 2) {
      const neighborId = `INT-00${num + 1}`
      const neighbor = nodes.find(n => n.intersectionId === neighborId)
      if (neighbor) {
        const line = L.polyline(
          [[node.latitude, node.longitude], [neighbor.latitude, neighbor.longitude]],
          { color: '#909399', weight: 1.5, opacity: 0.4, dashArray: '4,4' }
        ).addTo(map)
        roadLines.push(line)
      }
    }
    // Bottom neighbor
    if (row < 2) {
      const neighborId = `INT-00${num + 3}`
      const neighbor = nodes.find(n => n.intersectionId === neighborId)
      if (neighbor) {
        const line = L.polyline(
          [[node.latitude, node.longitude], [neighbor.latitude, neighbor.longitude]],
          { color: '#909399', weight: 1.5, opacity: 0.4, dashArray: '4,4' }
        ).addTo(map)
        roadLines.push(line)
      }
    }
  }
}

function renderMarkers() {
  if (!map) return
  for (const m of markers) map.removeLayer(m)
  markers = []

  // Render predefined intersections
  for (const node of intersections.value) {
    if (node.latitude == null || node.longitude == null) continue
    const isOrigin = node.intersectionId === form.value.originId
    const isDest = node.intersectionId === form.value.destinationId

    const color = isOrigin ? '#409eff' : isDest ? '#f56c6c' : '#606266'
    const size = isOrigin || isDest ? 12 : 8

    const icon = L.divIcon({
      html: `<div style="
        width:${size}px;height:${size}px;
        background:${color};
        border:2px solid #fff;
        border-radius:50%;
        box-shadow:0 1px 3px rgba(0,0,0,0.3);
        cursor:pointer;
      "></div>`,
      className: '',
      iconSize: [size, size],
      iconAnchor: [size / 2, size / 2]
    })

    const marker = L.marker([node.latitude, node.longitude], { icon })
      .addTo(map)
      .bindTooltip(node.intersectionId.startsWith('CUSTOM-') ? node.name : `${node.intersectionId}`, { direction: 'top', offset: [0, -8] })

    marker.on('click', () => {
      if (!form.value.originId || (form.value.originId && form.value.destinationId)) {
        form.value.originId = node.intersectionId
        onOriginChange()
      } else {
        form.value.destinationId = node.intersectionId
        onDestinationChange()
      }
    })

    markers.push(marker)
  }

  // Render user-created custom points
  for (const node of customPoints.value) {
    if (node.latitude == null || node.longitude == null) continue
    if (intersections.value.find(i => i.intersectionId === node.intersectionId)) continue // already rendered above
    const isOrigin = node.intersectionId === form.value.originId
    const isDest = node.intersectionId === form.value.destinationId
    const color = isOrigin ? '#409eff' : isDest ? '#f56c6c' : '#e6a23c'
    const size = isOrigin || isDest ? 14 : 10

    const icon = L.divIcon({
      html: `<div style="
        width:${size}px;height:${size}px;
        background:${color};
        border:2px solid #fff;
        border-radius:50%;
        box-shadow:0 0 0 2px ${color}40, 0 2px 6px rgba(0,0,0,0.3);
        cursor:pointer;
      "></div>`,
      className: '',
      iconSize: [size, size],
      iconAnchor: [size / 2, size / 2]
    })

    const marker = L.marker([node.latitude, node.longitude], { icon })
      .addTo(map)
      .bindTooltip(node.name || `自定义点`, { direction: 'top', offset: [0, -8] })

    marker.on('click', () => {
      if (!form.value.originId || (form.value.originId && form.value.destinationId)) {
        form.value.originId = node.intersectionId
        onOriginChange()
      } else {
        form.value.destinationId = node.intersectionId
        onDestinationChange()
      }
    })

    markers.push(marker)
  }
}

function renderRoutes() {
  if (!map) return
  for (const l of routeLines) map.removeLayer(l)
  routeLines = []

  if (!routeResult.value?.path?.length) return

  // Primary route
  const primaryPath = routeResult.value.path
    .filter(n => n.latitude != null && n.longitude != null)
    .map(n => [n.latitude, n.longitude])

  if (primaryPath.length >= 2) {
    const line = L.polyline(primaryPath, {
      color: '#409eff', weight: 4, opacity: 0.9
    }).addTo(map)
    routeLines.push(line)

    // Add direction arrows
    for (let i = 0; i < primaryPath.length - 1; i++) {
      const mid = [
        (primaryPath[i][0] + primaryPath[i + 1][0]) / 2,
        (primaryPath[i][1] + primaryPath[i + 1][1]) / 2
      ]
      const angle = Math.atan2(
        primaryPath[i + 1][1] - primaryPath[i][1],
        primaryPath[i + 1][0] - primaryPath[i][0]
      ) * 180 / Math.PI

      const arrow = L.marker(mid, {
        icon: L.divIcon({
          html: `<div style="
            width:0;height:0;
            border-left:5px solid transparent;
            border-right:5px solid transparent;
            border-bottom:8px solid #409eff;
            transform:rotate(${angle - 90}deg);
          "></div>`,
          className: '',
          iconSize: [10, 8],
          iconAnchor: [5, 4]
        }),
        interactive: false
      }).addTo(map)
      routeLines.push(arrow)
    }
  }

  // Alternative routes (in order of index)
  for (let ai = 1; ai < alternatives.length; ai++) {
    const alt = alternatives[ai]
    if (!alt.path?.length) continue
    const altCoords = alt.path
      .filter(n => n.latitude != null && n.longitude != null)
      .map(n => [n.latitude, n.longitude])

    if (altCoords.length >= 2) {
      const isSelected = selectedAltIndex.value === ai
      const color = ['#e6a23c', '#67c23a', '#909399'][(ai - 1) % 3]
      const line = L.polyline(altCoords, {
        color, weight: isSelected ? 4 : 2.5,
        opacity: isSelected ? 0.9 : 0.5,
        dashArray: isSelected ? '' : '8,6'
      }).addTo(map)
      routeLines.push(line)
    }
  }

  // Fit bounds to show all routes
  const allCoords = []
  allCoords.push(...primaryPath)
  for (const alt of alternatives) {
    if (alt.path) {
      allCoords.push(...alt.path.filter(n => n.latitude != null).map(n => [n.latitude, n.longitude]))
    }
  }
  if (allCoords.length > 0) {
    map.fitBounds(allCoords, { padding: [50, 50] })
  }
}

function onOriginChange() {
  if (form.value.destinationId && form.value.originId === form.value.destinationId) {
    form.value.destinationId = ''
  }
  renderMarkers()
  if (form.value.originId && form.value.destinationId) {
    planRoute()
  }
}

function onDestinationChange() {
  if (form.value.originId && form.value.originId === form.value.destinationId) {
    form.value.destinationId = ''
    return
  }
  renderMarkers()
  if (form.value.originId && form.value.destinationId) {
    planRoute()
  }
}

async function loadIntersections() {
  try {
    const list = await fusionApi.listIntersectionInfo()
    if (list && list.length) {
      intersections.value = list
      return
    }
  } catch (e) { /* fallback */ }
  // Fallback
  intersections.value = ['INT-001', 'INT-002', 'INT-003', 'INT-004',
    'INT-005', 'INT-006', 'INT-007', 'INT-008', 'INT-009'
  ].map((id, i) => {
    const row = Math.floor(i / 3)
    const col = i % 3
    return {
      intersectionId: id,
      latitude: 31.2304 + row * 0.005,
      longitude: 121.4737 + col * 0.005
    }
  })
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
  selectedAltIndex.value = 0
  try {
    const [primary, alts] = await Promise.all([
      routeApi.planRoute({
        originId: form.value.originId,
        destinationId: form.value.destinationId,
        vehicleType: form.value.vehicleType,
        priority: form.value.priority
      }),
      routeApi.getAlternatives({
        originId: form.value.originId,
        destinationId: form.value.destinationId,
        vehicleType: form.value.vehicleType,
        priority: form.value.priority
      })
    ])
    routeResult.value = primary
    alternatives.value = alts || []
    nextTick(renderRoutes)
  } catch (e) {
    // Demo mode: generate route locally
    generateDemoRoute()
  } finally {
    loading.value = false
  }
}

function isCustomId(id) {
  return id && id.startsWith('CUSTOM-')
}

function getIntersection(id) {
  return intersections.value.find(i => i.intersectionId === id)
    || customPoints.value.find(i => i.intersectionId === id)
}

function generateDemoRoute() {
  const origin = getIntersection(form.value.originId)
  const dest = getIntersection(form.value.destinationId)
  if (!origin || !dest) {
    ElMessage.error('路口数据不可用')
    return
  }

  const isOriginCustom = isCustomId(form.value.originId)
  const isDestCustom = isCustomId(form.value.destinationId)

  // If both are predefined intersections, use grid routing
  if (!isOriginCustom && !isDestCustom) {
    const allIds = intersections.value.map(i => i.intersectionId)
    const originIdx = allIds.indexOf(form.value.originId)
    const destIdx = allIds.indexOf(form.value.destinationId)

    if (originIdx >= 0 && destIdx >= 0) {
      const originRow = Math.floor(originIdx / 3)
      const originCol = originIdx % 3
      const destRow = Math.floor(destIdx / 3)
      const destCol = destIdx % 3

      const path = []
      const stepRow = originRow <= destRow ? 1 : -1
      for (let r = originRow; r !== destRow; r += stepRow) {
        const idx = r * 3 + originCol
        path.push(intersections.value[idx])
      }
      const stepCol = originCol <= destCol ? 1 : -1
      for (let c = originCol; c !== destCol; c += stepCol) {
        const idx = destRow * 3 + c
        path.push(intersections.value[idx])
      }
      path.push(intersections.value[destIdx])

      const totalDist = ((Math.abs(destRow - originRow) * 0.6) + (Math.abs(destCol - originCol) * 0.5)) * 1000
      const totalTime = totalDist / 8.3

      routeResult.value = {
        routeId: 'demo-' + Date.now(),
        totalDistance: totalDist,
        estimatedTime: totalTime,
        path: path.map(n => ({
          intersectionId: n.intersectionId,
          latitude: n.latitude,
          longitude: n.longitude,
          intersectionName: n.name || n.intersectionId
        }))
      }
      alternatives.value = [routeResult.value]
      selectedAltIndex.value = 0
      ElMessage.success('路径规划完成 (演示模式)')
      nextTick(renderRoutes)
      return
    }
  }

  // Custom point involved — route via the nearest predefined intersection
  const nearestToOrigin = findNearestIntersection(origin.latitude, origin.longitude, 999) || origin
  const nearestToDest = findNearestIntersection(dest.latitude, dest.longitude, 999) || dest

  // Build path: origin → nearest origin intersection → ... → nearest dest intersection → dest
  const path = [origin]

  if (nearestToOrigin.intersectionId !== origin.intersectionId) {
    path.push(nearestToOrigin)
  }

  if (nearestToDest.intersectionId !== nearestToOrigin.intersectionId) {
    // Try grid routing between the two nearest intersections
    const allIds = intersections.value.map(i => i.intersectionId)
    const oIdx = allIds.indexOf(nearestToOrigin.intersectionId)
    const dIdx = allIds.indexOf(nearestToDest.intersectionId)
    if (oIdx >= 0 && dIdx >= 0) {
      const oRow = Math.floor(oIdx / 3), oCol = oIdx % 3
      const dRow = Math.floor(dIdx / 3), dCol = dIdx % 3
      const stepRow = oRow <= dRow ? 1 : -1
      for (let r = oRow + stepRow; r !== dRow; r += stepRow) {
        const idx = r * 3 + oCol
        const n = intersections.value[idx]
        if (n && n.intersectionId !== nearestToOrigin.intersectionId) path.push(n)
      }
      const stepCol = oCol <= dCol ? 1 : -1
      for (let c = oCol + stepCol; c !== dCol; c += stepCol) {
        const idx = dRow * 3 + c
        const n = intersections.value[idx]
        if (n && n.intersectionId !== nearestToOrigin.intersectionId) path.push(n)
      }
      if (intersections.value[dIdx]?.intersectionId !== nearestToOrigin.intersectionId) {
        path.push(intersections.value[dIdx])
      }
    }
  }

  if (nearestToDest.intersectionId !== dest.intersectionId) {
    path.push(dest)
  }

  const totalDist = origin && dest
    ? Math.sqrt(Math.pow(dest.latitude - origin.latitude, 2) + Math.pow(dest.longitude - origin.longitude, 2)) * 111000
    : 1000
  const totalTime = totalDist / 8.3

  routeResult.value = {
    routeId: 'demo-' + Date.now(),
    totalDistance: totalDist,
    estimatedTime: totalTime,
    path: path.map(n => ({
      intersectionId: n.intersectionId,
      latitude: n.latitude,
      longitude: n.longitude,
      intersectionName: n.name || n.intersectionId
    }))
  }
  alternatives.value = [routeResult.value]
  selectedAltIndex.value = 0
  ElMessage.success('路径规划完成 (演示模式)')
  nextTick(renderRoutes)
}

function selectAlt(index) {
  selectedAltIndex.value = index
  if (alternatives.value[index]) {
    routeResult.value = alternatives.value[index]
    nextTick(renderRoutes)
  }
}

onMounted(async () => {
  await loadIntersections()
  await nextTick()
  initMap()
  if (map) {
    setTimeout(() => {
      renderRoadNetwork()
      renderMarkers()
    }, 100)
  }
})

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<style scoped>
.route-container {
  display: flex;
  height: calc(100vh - 100px);
  gap: 1px;
  background: #e4e7ed;
}
.route-sidebar {
  width: 320px;
  min-width: 320px;
  background: #fff;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.panel-section {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.form-group {
  margin-bottom: 12px;
}
.form-label {
  display: block;
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}
.vehicle-group {
  display: flex;
  gap: 8px;
}
.route-stat {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
  border-bottom: 1px solid #f5f5f5;
}
.route-stat-label { color: #909399; }
.route-stat-value { font-weight: 600; color: #303133; }
.node-list {
  margin-top: 8px;
  max-height: 200px;
  overflow-y: auto;
}
.node-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 3px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.node-item:hover, .node-item.active {
  background: #ecf5ff;
}
.node-index {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #409eff;
  color: #fff;
  border-radius: 50%;
  font-size: 10px;
  flex-shrink: 0;
}
.node-id { color: #303133; font-family: monospace; }
.node-arrow {
  margin-left: auto;
  color: #909399;
}
.node-arrow::after { content: '→'; }

.alt-item {
  padding: 8px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.15s;
}
.alt-item:hover { border-color: #c0c4cc; }
.alt-item.selected { border-color: #409eff; background: #ecf5ff; }
.alt-header { display: flex; gap: 8px; font-size: 12px; margin-bottom: 4px; }
.alt-label { font-weight: 600; color: #303133; }
.alt-distance { color: #606266; }
.alt-time { color: #909399; margin-left: auto; }
.alt-path { font-size: 11px; color: #909399; }

.route-map {
  flex: 1;
  position: relative;
  background: #f5f5f5;
}
.leaflet-map {
  width: 100%;
  height: 100%;
}
.map-legend {
  position: absolute;
  bottom: 20px;
  left: 20px;
  background: rgba(255,255,255,0.95);
  padding: 8px 12px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  font-size: 11px;
  z-index: 1000;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 3px;
  color: #606266;
}
.legend-item:last-child { margin-bottom: 0; }
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.legend-dot.primary { background: #409eff; }
.legend-dot.alt { background: #e6a23c; }
.legend-dot.origin { background: #409eff; border: 2px solid #fff; box-shadow: 0 0 0 1px #409eff; }
.legend-dot.dest { background: #f56c6c; border: 2px solid #fff; box-shadow: 0 0 0 1px #f56c6c; }
.legend-dot.custom { background: #e6a23c; border: 2px solid #fff; box-shadow: 0 0 0 2px #e6a23c40; }

.map-click-hint-banner {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  background: rgba(0,0,0,0.55);
  color: #fff;
  font-size: 12px;
  padding: 4px 14px;
  border-radius: 14px;
  pointer-events: none;
}
</style>
