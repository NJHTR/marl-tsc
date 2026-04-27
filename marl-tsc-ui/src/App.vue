<template>
  <el-container class="app-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="app-aside">
      <div class="logo-area">
        <span v-if="!isCollapse" class="logo-text">MARL-TSC</span>
        <span v-else class="logo-text-small">M</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        :router="true"
        background-color="#001529"
        text-color="#fff"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/signal">
          <el-icon><Switch /></el-icon>
          <span>信号控制</span>
        </el-menu-item>
        <el-menu-item index="/route">
          <el-icon><MapLocation /></el-icon>
          <span>路径规划</span>
        </el-menu-item>
        <el-menu-item index="/drl">
          <el-icon><Cpu /></el-icon>
          <span>DRL引擎</span>
        </el-menu-item>
        <el-menu-item index="/monitor">
          <el-icon><VideoCamera /></el-icon>
          <span>实时监控</span>
        </el-menu-item>
        <el-menu-item index="/fusion">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据融合</span>
        </el-menu-item>
      </el-menu>
      <div class="collapse-btn" @click="isCollapse = !isCollapse">
        <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
      </div>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <h2>{{ currentTitle }}</h2>
        </div>
        <div class="header-right">
          <el-tag type="success" effect="dark" size="small">系统运行中</el-tag>
          <el-tag type="info" effect="plain" size="small" style="margin-left: 8px;">
            {{ currentTime }}
          </el-tag>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Fold, Expand } from '@element-plus/icons-vue'

const route = useRoute()
const isCollapse = ref(false)

const currentTime = ref(new Date().toLocaleString('zh-CN'))
setInterval(() => {
  currentTime.value = new Date().toLocaleString('zh-CN')
}, 1000)

const currentTitle = computed(() => route.meta?.title || 'MARL-TSC')
</script>

<style>
html, body, #app { margin: 0; padding: 0; height: 100%; }
.app-container { height: 100vh; }
.app-aside {
  background-color: #001529;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  overflow: hidden;
}
.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  font-size: 18px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.logo-text-small { font-size: 24px; }
.el-menu { border-right: none; }
.collapse-btn {
  margin-top: auto;
  padding: 12px;
  text-align: center;
  color: #fff;
  cursor: pointer;
  border-top: 1px solid rgba(255,255,255,0.1);
}
.collapse-btn:hover { background-color: rgba(255,255,255,0.05); }
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.header-left h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }
.app-main {
  background-color: #f0f2f5;
  overflow-y: auto;
}
</style>
