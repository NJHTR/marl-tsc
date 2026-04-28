<template>
  <div ref="containerRef" class="intersection-3d-container" @click="onClick">
    <div class="controls-hint">
      拖拽旋转 · 滚轮缩放 · 点击信号灯调整
    </div>
    <div class="phase-indicator" v-if="currentPhase">
      <span class="phase-dot" :style="{ background: phaseColor }"></span>
      当前相位: {{ currentDirection || '-' }}
    </div>
    <div class="data-source-badge" v-if="dataSource">
      {{ dataSource }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'

const props = defineProps({
  intersectionId: { type: String, default: 'INT-001' },
  phases: { type: Array, default: () => [] },
  currentPhaseId: { type: [String, Number], default: null },
  flow: { type: Number, default: 0 },
  speed: { type: Number, default: 0 },
  occupancy: { type: Number, default: 0 },
  // Data-driven geometry from backend
  approaches: { type: Array, default: null },
  buildingDensity: { type: Number, default: 1.0 }
})

const emit = defineEmits(['adjustPhase'])

const containerRef = ref(null)
const dataSource = computed(() => {
  if (props.approaches?.length) return '数据驱动'
  return '3D场景'
})

let scene, camera, renderer, controls
let trafficLights = {}
let cars = []
let animFrameId = null
let clock = new THREE.Clock()
let buildings = []

// --- Derived road geometry from backend data ---
const roadGeometry = computed(() => {
  if (props.approaches && props.approaches.length > 0) {
    // Data-driven: use backend approach data
    const nsLanes = Math.max(
      props.approaches.find(a => a.direction === 'N')?.lanes || 0,
      props.approaches.find(a => a.direction === 'S')?.lanes || 0
    )
    const ewLanes = Math.max(
      props.approaches.find(a => a.direction === 'E')?.lanes || 0,
      props.approaches.find(a => a.direction === 'W')?.lanes || 0
    )
    const nsWidth = props.approaches.find(a => a.direction === 'N')?.roadWidth || 3.5
    const ewWidth = props.approaches.find(a => a.direction === 'E')?.roadWidth || 3.5
    const roadLen = props.approaches.find(a => a.direction === 'N')?.roadLength || 180

    return {
      roadWidthNS: Math.max(nsLanes * nsWidth, 4),
      roadWidthEW: Math.max(ewLanes * ewWidth, 4),
      roadLength: Math.min(roadLen / 10, 20),
      lanesNS: nsLanes,
      lanesEW: ewLanes
    }
  }
  // Fallback defaults
  return { roadWidthNS: 8, roadWidthEW: 8, roadLength: 18, lanesNS: 3, lanesEW: 3 }
})

const currentPhase = computed(() => {
  if (!props.currentPhaseId || !props.phases.length) return null
  return props.phases.find(p => String(p.phaseId) === String(props.currentPhaseId))
})

const currentDirection = computed(() => currentPhase.value?.direction || '')

const phaseColor = computed(() => {
  const dir = currentDirection.value
  if (dir === '东西') return '#67c23a'
  if (dir === '南北') return '#409eff'
  return '#909399'
})

// --- Scene creation ---
function createScene() {
  const container = containerRef.value
  if (!container) return
  const w = container.clientWidth
  const h = container.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x1a1a2e)
  scene.fog = new THREE.Fog(0x1a1a2e, 40, 80)

  camera = new THREE.PerspectiveCamera(40, w / h, 0.1, 100)
  camera.position.set(22, 18, 22)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  container.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.target.set(0, 0, 0)
  controls.enableDamping = true
  controls.dampingFactor = 0.08
  controls.maxPolarAngle = Math.PI / 2.4
  controls.minDistance = 8
  controls.maxDistance = 50
  controls.update()

  // Lights
  scene.add(new THREE.AmbientLight(0x404060, 0.6))
  const sun = new THREE.DirectionalLight(0xffeedd, 1.2)
  sun.position.set(20, 30, 10)
  sun.castShadow = true
  sun.shadow.mapSize.width = 1024
  sun.shadow.mapSize.height = 1024
  const d = 25
  sun.shadow.camera.left = -d
  sun.shadow.camera.right = d
  sun.shadow.camera.top = d
  sun.shadow.camera.bottom = -d
  scene.add(sun)
  scene.add(new THREE.HemisphereLight(0x445566, 0x221133, 0.4))

  // Ground
  const ground = new THREE.Mesh(
    new THREE.PlaneGeometry(60, 60),
    new THREE.MeshStandardMaterial({ color: 0x2a2a3e, roughness: 0.9 })
  )
  ground.rotation.x = -Math.PI / 2
  ground.receiveShadow = true
  scene.add(ground)

  const grid = new THREE.GridHelper(50, 20, 0x444466, 0x333355)
  grid.position.y = 0.02
  scene.add(grid)
}

// --- Data-driven roads ---
function createRoads() {
  const g = roadGeometry.value
  const roadMat = new THREE.MeshStandardMaterial({ color: 0x333344, roughness: 0.8 })
  const laneMat = new THREE.MeshStandardMaterial({ color: 0x555577, roughness: 0.6, transparent: true, opacity: 0.3 })
  const sideMat = new THREE.MeshStandardMaterial({ color: 0x444455, roughness: 0.9 })
  const dashMat = new THREE.MeshStandardMaterial({ color: 0x8888aa, emissive: 0x555577, emissiveIntensity: 0.3 })
  const cwMat = new THREE.MeshStandardMaterial({ color: 0x666688, transparent: true, opacity: 0.4 })

  const roadL = g.roadLength
  const nsW = g.roadWidthNS
  const ewW = g.roadWidthEW
  const sideW = 0.3

  for (const dir of ['ns', 'ew']) {
    const isX = dir === 'ew'
    const rw = isX ? ewW : nsW
    const rl = isX ? nsW : ewW

    // Road surface
    const road = new THREE.Mesh(new THREE.BoxGeometry(isX ? roadL : rw, 0.1, isX ? rw : roadL), roadMat)
    road.position.y = 0.05
    road.receiveShadow = true
    scene.add(road)

    // Sidewalks
    for (const side of [-1, 1]) {
      const sidewalk = new THREE.Mesh(
        new THREE.BoxGeometry(isX ? roadL : sideW, 0.15, isX ? sideW : roadL),
        sideMat
      )
      sidewalk.position.set(isX ? 0 : side * (rw / 2 + sideW / 2), 0.08, isX ? side * (rw / 2 + sideW / 2) : 0)
      scene.add(sidewalk)
    }

    // Center lane markings
    const dashCount = Math.floor(roadL / 1.5)
    for (let i = 0; i < dashCount; i++) {
      const t = -roadL / 2 + (i + 0.5) * (roadL / dashCount)
      if (Math.abs(t) < 0.5) continue
      const dash = new THREE.Mesh(new THREE.BoxGeometry(isX ? 0.1 : 0.8, 0.02, isX ? 0.8 : 0.1), dashMat)
      dash.position.set(isX ? t : 0, 0.12, isX ? 0 : t)
      scene.add(dash)
    }

    // Crosswalks
    for (const s of [-1, 1]) {
      const cw = new THREE.Mesh(
        new THREE.BoxGeometry(isX ? rw : 1.0, 0.02, isX ? 1.0 : rw),
        cwMat
      )
      cw.position.set(isX ? s * Math.min(2.0, rw / 3) : 0, 0.13, isX ? 0 : s * Math.min(2.0, rw / 3))
      scene.add(cw)
    }
  }

  // Intersection center
  const center = new THREE.Mesh(
    new THREE.BoxGeometry(ewW, 0.1, nsW),
    new THREE.MeshStandardMaterial({ color: 0x2a2a3e, roughness: 0.9 })
  )
  center.position.y = 0.05
  scene.add(center)
}

// --- Data-driven traffic lights ---
function createTrafficLights() {
  const g = roadGeometry.value
  const poleMat = new THREE.MeshStandardMaterial({ color: 0x555566, metalness: 0.5, roughness: 0.3 })
  const armMat = new THREE.MeshStandardMaterial({ color: 0x444455, metalness: 0.3, roughness: 0.4 })

  // Position lights at edge of road
  const offset = Math.max(g.roadWidthNS, g.roadWidthEW) / 2 + 0.5
  const stopBar = Math.max(g.roadLength / 2 - 0.5, 7)

  const directions = [
    { name: 'north', angle: 0, x: 0, z: -stopBar },
    { name: 'south', angle: Math.PI, x: 0, z: stopBar },
    { name: 'east', angle: Math.PI / 2, x: stopBar, z: 0 },
    { name: 'west', angle: -Math.PI / 2, x: -stopBar, z: 0 }
  ]

  for (const dir of directions) {
    const group = new THREE.Group()
    group.position.set(dir.x, 0, dir.z)

    const pole = new THREE.Mesh(new THREE.CylinderGeometry(0.12, 0.15, 3.5, 8), poleMat)
    pole.position.y = 1.75
    pole.castShadow = true
    group.add(pole)

    const base = new THREE.Mesh(new THREE.CylinderGeometry(0.3, 0.4, 0.15, 8), poleMat)
    base.position.y = 0.08
    group.add(base)

    const arm = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.06, 1.2, 6), armMat)
    arm.rotation.z = Math.PI / 2
    arm.position.set(0.6, 2.8, 0)
    group.add(arm)

    const housing = new THREE.Mesh(
      new THREE.BoxGeometry(0.35, 0.9, 0.35),
      new THREE.MeshStandardMaterial({ color: 0x333344, metalness: 0.3, roughness: 0.5 })
    )
    housing.position.set(1.2, 2.8, 0)
    group.add(housing)

    const colors = [
      { type: 'red', y: 2.4, color: 0xff0000 },
      { type: 'yellow', y: 2.0, color: 0xffff00 },
      { type: 'green', y: 1.6, color: 0x00ff00 }
    ]

    const lights = {}
    for (const c of colors) {
      const bulbMat = new THREE.MeshStandardMaterial({ color: c.color, emissive: 0x000000, emissiveIntensity: 0 })
      const bulb = new THREE.Mesh(new THREE.SphereGeometry(0.12, 12, 12), bulbMat)
      bulb.position.set(1.2, c.y, 0)
      group.add(bulb)
      lights[c.type] = bulb
    }

    group.rotation.y = dir.angle
    scene.add(group)
    trafficLights[dir.name] = lights
  }
}

// --- Data-driven buildings ---
function createBuilding(x, z, w, d, h, color) {
  const mat = new THREE.MeshStandardMaterial({ color: color || 0x445566, roughness: 0.7, metalness: 0.1 })
  const building = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), mat)
  building.position.set(x, h / 2, z)
  building.castShadow = true
  building.receiveShadow = true
  scene.add(building)
  buildings.push(building)

  // Windows
  const winMat = new THREE.MeshStandardMaterial({
    color: 0x88aacc, emissive: 0x446688, emissiveIntensity: 0.3,
    transparent: true, opacity: 0.5
  })
  for (let i = 0; i < Math.floor(h / 1.5); i++) {
    for (const j of [-1, 1]) {
      const win = new THREE.Mesh(new THREE.PlaneGeometry(0.4, 0.6), winMat)
      const wi = Math.random() > 0.5 ? 1 : -1
      win.position.set(x + j * (w / 2 + 0.01), 0.8 + i * 1.2, z + wi * (d / 2 + 0.01))
      win.lookAt(x + j * 10, 0, z + wi * 10)
      scene.add(win)
    }
  }
}

function createBuildings() {
  const g = roadGeometry.value
  const roadEnd = g.roadLength / 2 + 0.5
  const density = props.buildingDensity

  // Compute building positions dynamically based on road layout
  const placements = []
  const buildingColors = [0x4a5568, 0x2d3748, 0x5a6778, 0x3d4a5a, 0x445566, 0x334455]

  // Buildings placed around the perimeter, avoiding road space
  for (let ring = 0; ring < Math.ceil(density * 2); ring++) {
    const baseDist = roadEnd + 2 + ring * 3
    for (let side = 0; side < 4; side++) {
      const cx = side === 0 ? baseDist : side === 1 ? -baseDist : 0
      const cz = side === 2 ? baseDist : side === 3 ? -baseDist : 0
      const isXDir = side < 2
      for (let offset = -1; offset <= 1; offset += 0.5) {
        if (Math.random() > density * 0.7) continue
        const px = isXDir ? cx : (offset * (roadEnd + 1 + Math.random() * 2))
        const pz = isXDir ? (offset * (roadEnd + 1 + Math.random() * 2)) : cz
        if (Math.abs(px) < 0.5 && Math.abs(pz) < 0.5) continue
        const w = 1.5 + Math.random() * 1.5
        const d = 1.5 + Math.random() * 1.5
        const h = 2 + Math.random() * 3.5
        const c = buildingColors[Math.floor(Math.random() * buildingColors.length)]
        placements.push({ x: px, z: pz, w, d, h, c })
      }
    }
  }

  for (const b of placements) {
    createBuilding(b.x, b.z, b.w, b.d, b.h, b.c)
  }
}

function createStreetLights() {
  const g = roadGeometry.value
  const roadEnd = g.roadLength / 2 + 2
  const offset = Math.max(g.roadWidthNS, g.roadWidthEW) / 2 + 0.8
  const poleMat = new THREE.MeshStandardMaterial({ color: 0x555566, metalness: 0.3, roughness: 0.5 })
  const lampMat = new THREE.MeshStandardMaterial({ color: 0xffee88, emissive: 0xffdd66, emissiveIntensity: 0.2 })

  const positions = [
    [-roadEnd, -offset], [-roadEnd, offset],
    [roadEnd, -offset], [roadEnd, offset],
    [-offset, -roadEnd], [offset, -roadEnd],
    [-offset, roadEnd], [offset, roadEnd]
  ]
  for (const pos of positions) {
    const pole = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.08, 2.5, 6), poleMat)
    pole.position.set(pos[0], 1.25, pos[1])
    scene.add(pole)
    const lamp = new THREE.Mesh(new THREE.SphereGeometry(0.15, 8, 8), lampMat)
    lamp.position.set(pos[0], 2.6, pos[1])
    scene.add(lamp)
  }
}

// --- Data-driven car spawning ---
function createCar(color, startX, startZ, dirX, dirZ, speed) {
  const carMat = new THREE.MeshStandardMaterial({ color, roughness: 0.3, metalness: 0.4 })
  const carMatDark = new THREE.MeshStandardMaterial({ color: 0x222233, roughness: 0.6 })

  const group = new THREE.Group()
  const body = new THREE.Mesh(new THREE.BoxGeometry(0.6, 0.2, 0.35), carMat)
  body.position.y = 0.2
  body.castShadow = true
  group.add(body)

  const cabin = new THREE.Mesh(new THREE.BoxGeometry(0.35, 0.15, 0.3), carMatDark)
  cabin.position.set(-0.05, 0.35, 0)
  group.add(cabin)

  const wheelMat = new THREE.MeshStandardMaterial({ color: 0x111111, roughness: 0.9 })
  for (const wx of [-0.25, 0.25]) {
    for (const wz of [-0.2, 0.2]) {
      const wheel = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.06, 0.04, 8), wheelMat)
      wheel.rotation.x = Math.PI / 2
      wheel.position.set(wx, 0.06, wz)
      group.add(wheel)
    }
  }

  const hlMat = new THREE.MeshStandardMaterial({ color: 0xffffcc, emissive: 0xffffcc, emissiveIntensity: 0.3 })
  for (const wz of [-0.1, 0.1]) {
    const hl = new THREE.Mesh(new THREE.SphereGeometry(0.03, 6, 6), hlMat)
    hl.position.set(0.32, 0.15, wz)
    group.add(hl)
  }

  group.position.set(startX, 0, startZ)
  if (dirX > 0) group.rotation.y = -Math.PI / 2
  else if (dirX < 0) group.rotation.y = Math.PI / 2
  else if (dirZ > 0) group.rotation.y = Math.PI
  else group.rotation.y = 0

  scene.add(group)
  return { group, startX, startZ, dirX, dirZ, speed, dist: Math.random() * 16 }
}

function spawnCars() {
  const g = roadGeometry.value
  for (const car of cars) { scene.remove(car.group) }
  cars = []

  const colors = [0xff4444, 0x44aaff, 0x44ff88, 0xffaa44, 0xff66aa, 0xffffff, 0x88ff44, 0x44ddff]
  const roadEnd = g.roadLength / 2 + 1
  const laneOffset = Math.min(1.5, g.roadWidthNS / 4)

  // Generate routes based on actual road lanes
  const routes = []
  for (let lane = -1; lane <= 1; lane++) {
    if (Math.abs(lane) > g.lanesNS / 2) continue
    routes.push({ sx: -roadEnd, sz: lane * laneOffset, dx: 1, dz: 0 })
    routes.push({ sx: roadEnd, sz: lane * laneOffset, dx: -1, dz: 0 })
  }
  for (let lane = -1; lane <= 1; lane++) {
    if (Math.abs(lane) > g.lanesEW / 2) continue
    routes.push({ sx: lane * laneOffset, sz: -roadEnd, dx: 0, dz: 1 })
    routes.push({ sx: lane * laneOffset, sz: roadEnd, dx: 0, dz: -1 })
  }

  const count = Math.min(Math.floor(8 + props.flow / 150), 30)
  for (let i = 0; i < count; i++) {
    const route = routes[i % routes.length]
    const color = colors[Math.floor(Math.random() * colors.length)]
    const speed = 0.015 + Math.random() * 0.015 + (props.speed / 100) * 0.01
    const car = createCar(color, route.sx, route.sz, route.dx, route.dz, Math.min(speed, 0.05))
    car.dist = Math.random() * roadEnd * 2
    cars.push(car)
  }
}

// --- Traffic light control ---
function setTrafficLights(phaseId, phases) {
  let direction = null
  if (phases && phaseId) {
    const phase = phases.find(p => String(p.phaseId) === String(phaseId))
    if (phase) direction = phase.direction
  }

  const ewGreen = direction === '东西'
  const nsGreen = direction === '南北'

  const lightStates = {
    north: nsGreen ? 'green' : 'red',
    south: nsGreen ? 'green' : 'red',
    east: ewGreen ? 'green' : 'red',
    west: ewGreen ? 'green' : 'red'
  }

  for (const [name, lights] of Object.entries(trafficLights)) {
    const state = lightStates[name]
    for (const [type, bulb] of Object.entries(lights)) {
      if ((type === state) || (state === 'green' && type === 'green') || (state === 'red' && type === 'red')) {
        const intensity = (type === state) ? 2.0 : 0.3
        const c = type === 'red' ? 0xff0000 : type === 'yellow' ? 0xffff00 : 0x00ff00
        bulb.material.color.setHex(c)
        bulb.material.emissive.setHex(c)
        bulb.material.emissiveIntensity = intensity
      } else {
        bulb.material.emissiveIntensity = 0.05
        bulb.material.emissive.setHex(0x000000)
        bulb.material.color.setHex(0x222222)
      }
    }
  }
}

// --- Animation loop ---
function animate() {
  animFrameId = requestAnimationFrame(animate)
  const delta = clock.getDelta()
  const g = roadGeometry.value
  const roadEnd = g.roadLength / 2 + 1

  for (const car of cars) {
    car.dist += car.speed * delta * 40
    if (car.dist > roadEnd * 2) car.dist -= roadEnd * 2
    const pos = -roadEnd + car.dist

    if (car.dirX !== 0) {
      car.group.position.x = pos
      car.group.position.z = car.startZ + Math.sin(car.dist * 0.3) * 0.15
    } else {
      car.group.position.z = pos
      car.group.position.x = car.startX + Math.sin(car.dist * 0.3) * 0.15
    }
  }

  controls.update()
  renderer.render(scene, camera)
}

// --- Click handler ---
function onClick(event) {
  const rect = containerRef.value.getBoundingClientRect()
  const mouse = new THREE.Vector2(
    ((event.clientX - rect.left) / rect.width) * 2 - 1,
    -((event.clientY - rect.top) / rect.height) * 2 + 1
  )

  const raycaster = new THREE.Raycaster()
  raycaster.setFromCamera(mouse, camera)

  const bulbs = []
  for (const [dir, lights] of Object.entries(trafficLights)) {
    for (const [type, bulb] of Object.entries(lights)) {
      bulbs.push({ mesh: bulb, dir, type })
    }
  }

  const intersects = raycaster.intersectObjects(bulbs.map(b => b.mesh))
  if (intersects.length > 0) {
    const hit = intersects[0].object
    const bulbInfo = bulbs.find(b => b.mesh === hit)
    if (bulbInfo) {
      emit('adjustPhase', { direction: bulbInfo.dir, type: bulbInfo.type })
    }
  }
}

function handleResize() {
  if (!containerRef.value || !renderer) return
  const w = containerRef.value.clientWidth
  const h = containerRef.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

// --- Watchers ---
watch(() => [props.currentPhaseId, props.phases], () => {
  setTrafficLights(props.currentPhaseId, props.phases)
})

watch(() => props.flow, () => { spawnCars() })

watch(() => props.approaches, () => {
  // Rebuild scene if approaches change (new intersection)
  if (!scene) return
  // Clear existing buildings and cars
  for (const b of buildings) { scene.remove(b) }
  buildings = []
  for (const car of cars) { scene.remove(car.group) }
  cars = []
  // Recreate with new geometry
  createRoads()
  createTrafficLights()
  createBuildings()
  createStreetLights()
  spawnCars()
  setTrafficLights(props.currentPhaseId, props.phases)
}, { deep: true })

onMounted(() => {
  createScene()
  createRoads()
  createTrafficLights()
  createBuildings()
  createStreetLights()
  spawnCars()
  setTrafficLights(props.currentPhaseId, props.phases)
  animate()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animFrameId)
  window.removeEventListener('resize', handleResize)
  if (renderer) {
    renderer.dispose()
    if (containerRef.value && renderer.domElement.parentElement === containerRef.value) {
      containerRef.value.removeChild(renderer.domElement)
    }
  }
  if (scene) {
    scene.traverse(obj => {
      if (obj.geometry) obj.geometry.dispose()
      if (obj.material) {
        if (Array.isArray(obj.material)) obj.material.forEach(m => m.dispose())
        else obj.material.dispose()
      }
    })
  }
})
</script>

<style scoped>
.intersection-3d-container {
  width: 100%;
  height: 500px;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  cursor: grab;
}
.intersection-3d-container:active { cursor: grabbing; }
.controls-hint {
  position: absolute; bottom: 12px; left: 50%;
  transform: translateX(-50%);
  color: rgba(255,255,255,0.5); font-size: 12px;
  pointer-events: none;
  background: rgba(0,0,0,0.3); padding: 4px 12px;
  border-radius: 12px; white-space: nowrap;
}
.phase-indicator {
  position: absolute; top: 12px; left: 12px;
  display: flex; align-items: center; gap: 6px;
  color: #fff; font-size: 14px;
  background: rgba(0,0,0,0.5); padding: 6px 12px; border-radius: 6px;
}
.phase-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.data-source-badge {
  position: absolute; top: 12px; right: 12px;
  background: rgba(64,158,255,0.7); color: #fff;
  font-size: 11px; padding: 3px 8px; border-radius: 10px;
}
</style>
