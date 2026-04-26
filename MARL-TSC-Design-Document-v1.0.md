# MARL-TSC 系统设计文档

> **项目名称**：基于多源异构数据融合与深度强化学习的交通信号-路径协同智能动态优化系统  
> **英文代号**：MARL-TSC (Multi-Agent Reinforcement Learning for Traffic Signal Control)  
> **版本**：v1.0  
> **日期**：2026-04-25  
> **技术栈**：Java 17 + Spring Boot 3.2 + Spring Cloud 2023 + Flink 1.18 + Kafka + Neo4j + Redis + TDengine  
> **作者**：NJHTR
---

## 目录

1. [项目概述](#1-项目概述)
2. [系统架构设计](#2-系统架构设计)
3. [模块划分与职责](#3-模块划分与职责)
4. [文件夹与包结构规范](#4-文件夹与包结构规范)
5. [代码编写规范](#5-代码编写规范)
6. [数据流与交互设计](#6-数据流与交互设计)
7. [部署与运维规范](#7-部署与运维规范)

---

## 1. 项目概述

### 1.1 项目背景
城市交通拥堵问题日益严重，传统固定配时信号控制无法适应动态交通流变化。本项目通过融合多源异构交通数据（线圈检测器、视频检测器、雷达、浮动车GPS、气象数据等），构建基于深度强化学习（DRL）的多智能体协同决策系统，实现交通信号控制与车辆路径规划的动态协同优化。

### 1.2 核心目标
- **实时性**：秒级交通状态感知与信号决策响应
- **协同性**：信号控制与路径规划双向耦合优化
- **智能化**：基于MADDPG/SAC等算法实现自适应控制
- **可扩展**：微服务架构支持水平扩展与模块独立演进

### 1.3 设计原则
| 原则 | 说明 |
|:---|:---|
| **高内聚低耦合** | 每个微服务只负责单一业务领域，通过消息总线解耦 |
| **领域驱动设计(DDD)** | 核心业务逻辑沉淀在Domain层，与基础设施解耦 |
| **配置外部化** | 所有环境相关配置抽离到配置文件，支持多环境切换 |
| **防御式编程** | 所有外部输入校验、空指针防护、异常兜底 |
| **可观测性** | 全链路日志追踪、指标监控、健康检查 |

---

## 2. 系统架构设计

### 2.1 总体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           应用服务层 (Application Layer)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│  │  信号控制服务  │  │  路径规划服务  │  │  协同优化服务  │ │   可视化监控平台   │ │
│  │  (8081)      │  │  (8082)      │  │  (8083)      │ │   (8080)         │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────────┘ │
├─────────────────────────────────────────────────────────────────────────────┤
│                           智能决策层 (AI Decision Layer)                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                    深度强化学习引擎 (DRL Engine)                          ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────────┐  ││
│  │  │  Actor网络  │  │ Critic网络  │  │  经验回放池  │  │  多智能体协调  │  ││
│  │  │ (PPO/SAC)   │  │  (TD3/A3C)  │  │  (PER)      │  │  (MADDPG)     │  ││
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └───────────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────────────────┤
│                         数据融合层 (Data Fusion Layer)                        │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌───────────┐ │
│  │  交通流数据  │ │  信号机数据  │ │  浮动车数据  │ │  气象/事件  │ │  地图/POI  │ │
│  │ (线圈/雷达)  │ │ (SCATS/UTC) │ │ (GPS/北斗)  │ │  数据      │ │  数据     │ │
│  └─────┬──────┘ └─────┬──────┘ └─────┬──────┘ └─────┬──────┘ └─────┬─────┘ │
│        └────────────────┴────────────────┴────────────────┴──────────────┘  │
│                              多源异构数据融合引擎                              │
│                    (时空对齐 · 质量评估 · 特征工程 · 知识图谱)                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                         基础设施层 (Infrastructure Layer)                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │  Kafka   │ │ Flink    │ │  Redis   │ │  Neo4j   │ │  TDengine│           │
│  │ (消息总线) │ │(实时计算) │ │(状态缓存) │ │(路网图谱) │ │(时序数据库)│          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 技术架构分层

| 层级 | 职责 | 技术组件 |
|:---|:---|:---|
| **接入层** | 数据接入、协议适配、设备通信 | Netty、MQTT、Modbus TCP |
| **计算层** | 实时流处理、特征计算、窗口聚合 | Apache Flink |
| **存储层** | 时序数据、图数据、缓存、消息 | TDengine、Neo4j、Redis、Kafka |
| **服务层** | 业务编排、领域计算、接口暴露 | Spring Boot、Spring Cloud |
| **决策层** | 强化学习训练、推理、模型管理 | DL4J / ONNX Runtime |
| **展示层** | 可视化、监控、告警 | Vue3 + WebSocket |

### 2.3 技术栈选型

| 类别 | 选型 | 版本 | 用途 |
|:---|:---|:---|:---|
| JDK | OpenJDK | 17 LTS | 运行时 |
| 基础框架 | Spring Boot | 3.2.5 | 微服务基础 |
| 服务治理 | Spring Cloud | 2023.0.1 | 注册发现、配置中心、熔断 |
| 服务调用 | OpenFeign | 4.x | 同步HTTP调用 |
| 流处理 | Apache Flink | 1.18.0 | 实时特征计算 |
| 消息队列 | Apache Kafka | 3.6.0 | 异步事件总线 |
| 图数据库 | Neo4j | 5.15.0 | 路网拓扑存储 |
| 时序数据库 | TDengine | 3.2.7 | 交通流时序数据 |
| 缓存 | Redis | 7.x | 实时状态缓存 |
| AI框架 | DL4J | 1.0.0-M2.1 | 神经网络训练推理 |
| GIS计算 | JTS | 1.19.0 | 空间几何计算 |
| 构建工具 | Maven | 3.9.x | 依赖管理 |

---

## 3. 模块划分与职责

### 3.1 模块总览

```
marl-tsc (Parent POM)
├── marl-tsc-common              # 公共依赖模块（无启动类）
├── marl-tsc-data-fusion         # 数据融合服务 (8085)
├── marl-tsc-stream              # 实时流处理服务 (8086)
├── marl-tsc-drl-engine          # DRL引擎服务 (8084)
├── marl-tsc-signal-control      # 信号控制服务 (8081)
├── marl-tsc-route-planning      # 路径规划服务 (8082)
├── marl-tsc-co-optimization     # 协同优化服务 (8083)
└── marl-tsc-web                 # 前端网关/可视化 (8080)
```

### 3.2 各模块详细职责

#### 3.2.1 marl-tsc-common
- **定位**：纯依赖库，无Spring Boot启动类，`<packaging>jar</packaging>`
- **职责**：
  - 统一异常基类与错误码定义
  - 统一API响应包装（ApiResult）
  - 通用工具类（JSON、时间、地理坐标）
  - 跨服务共享的极简DTO（分页、基础枚举）
- **注意**：禁止包含任何业务实体、数据库访问、Spring上下文依赖

#### 3.2.2 marl-tsc-data-fusion
- **定位**：数据接入与预处理中心
- **职责**：
  - 多源数据接入（线圈、视频、雷达、浮动车、气象）
  - 时空对齐与数据质量评估
  - 特征工程（状态向量构建）
  - 统一数据模型输出
- **对外**：通过Kafka向stream模块推送原始数据，向co-optimization推送融合特征

#### 3.2.3 marl-tsc-stream
- **定位**：Flink实时计算引擎
- **职责**：
  - 滑动窗口交通流统计（流量、速度、占有率）
  - 实时拥堵检测（3-sigma异常检测）
  - 特征聚合与窗口计算
  - 结果写入Redis/Kafka
- **注意**：该模块以Flink Job为主，Spring Boot仅用于配置管理

#### 3.2.4 marl-tsc-drl-engine
- **定位**：强化学习训练与推理中心
- **职责**：
  - 神经网络定义（Actor/Critic）
  - 经验回放与优先采样（PER）
  - 多智能体训练（MADDPG/SAC）
  - 模型版本管理与热更新
  - 推理服务（状态输入→动作输出）
- **对外**：提供REST/gRPC推理接口，供signal-control调用

#### 3.2.5 marl-tsc-signal-control
- **定位**：信号配时控制执行服务
- **职责**：
  - 路口信号状态管理
  - 信号方案下发（通过MQTT/Modbus到信号机）
  - 实时信号状态反馈采集
  - 与DRL引擎交互获取动作决策
- **对外**：REST接口供co-optimization调用，MQTT下发到硬件

#### 3.2.6 marl-tsc-route-planning
- **定位**：动态路径规划服务
- **职责**：
  - 时变路网图构建（基于Neo4j）
  - 时变A*路径搜索
  - 预测行程时间计算
  - 路径重分配策略
- **对外**：REST接口供co-optimization调用，向车辆终端推送路径

#### 3.2.7 marl-tsc-co-optimization
- **定位**：协同优化编排中心（核心调度）
- **职责**：
  - 滚动时域优化调度（每5秒触发）
  - 信号优化与路径优化的联合决策
  - 全局奖励函数计算
  - 预测路网状态推演
  - 迭代均衡求解
- **对外**：暴露全局优化触发接口，供web层调用

#### 3.2.8 marl-tsc-web
- **定位**：前端BFF层与可视化
- **职责**：
  - 前端静态资源托管
  - 后端接口聚合（聚合多个微服务数据）
  - WebSocket实时推送
  - 用户权限与登录（如需要）

---

## 4. 文件夹与包结构规范

### 4.1 包名总规范

```
com.njhtr.marltsc.{module}
```

| 模块 | 根包名 |
|:---|:---|
| common | `com.njhtr.marltsc.common` |
| data-fusion | `com.njhtr.marltsc.fusion` |
| stream | `com.njhtr.marltsc.stream` |
| drl-engine | `com.njhtr.marltsc.drl` |
| signal-control | `com.njhtr.marltsc.signal` |
| route-planning | `com.njhtr.marltsc.route` |
| co-optimization | `com.njhtr.marltsc.coopt` |
| web | `com.njhtr.marltsc.web` |

### 4.2 通用包结构（以co-optimization为例）

```
src/main/java/com/njhtr/marltsc/coopt/
│
├── CoOptimizationApplication.java          # 启动类，必须放在根包
│
├── api/                                    # 【接口层】对外暴露
│   ├── controller/                         # REST Controller
│   │   └── CoOptimizationController.java
│   ├── dto/                                # 数据传输对象
│   │   ├── request/                        # 入参DTO
│   │   │   └── OptimizationTriggerRequest.java
│   │   └── response/                       # 出参DTO
│   │       └── OptimizationResultResponse.java
│   └── qo/                                 # 查询对象（复杂查询条件封装）
│       └── TrafficStateQuery.java
│
├── domain/                                 # 【领域层】核心业务
│   ├── entity/                             # 数据库实体（PO）
│   │   └── OptimizationLog.java
│   ├── bo/                                 # 业务对象（多实体组合）
│   │   └── JointOptimizationBO.java
│   ├── vo/                                 # 视图对象（只读，给前端）
│   │   └── IntersectionDashboardVO.java
│   └── service/                            # 领域服务（核心业务算法）
│       ├── RewardCalculationService.java
│       └── NetworkPredictionService.java
│
├── service/                                # 【应用服务层】技术胶水
│   ├── api/                                # 应用服务接口
│   │   └── CoOptimizationAppService.java
│   └── impl/                               # 应用服务实现
│       └── CoOptimizationAppServiceImpl.java
│
├── event/                                  # 【事件层】异步消息
│   ├── publisher/
│   │   └── OptimizationEventPublisher.java
│   └── listener/
│       └── TrafficFlowEventListener.java
│
├── infrastructure/                         # 【基础设施层】技术细节
│   ├── client/                             # Feign客户端（调用其他服务）
│   │   ├── SignalControlClient.java
│   │   └── RoutePlanningClient.java
│   ├── config/                             # 配置类
│   │   └── KafkaConfig.java
│   ├── mapper/                             # MyBatis Mapper（如使用）
│   │   └── OptimizationLogMapper.java
│   └── repository/                         # Spring Data Repository（如使用JPA）
│       └── OptimizationLogRepository.java
│
└── task/                                   # 【任务层】定时/调度
    └── PeriodicOptimizationTask.java
```

### 4.3 各模块差异化结构

#### 4.3.1 marl-tsc-common（无启动类）

```
com.njhtr.marltsc.common/
├── constant/               # 系统级常量
│   └── SystemConstant.java
├── dto/                    # 跨服务共享极简DTO
│   └── PageDTO.java
├── enums/                  # 基础枚举
│   └── DataStatus.java
├── exception/              # 全局异常
│   ├── BaseException.java
│   └── ErrorCode.java
├── result/                 # 统一响应
│   └── ApiResult.java
└── util/                   # 通用工具
    ├── JsonUtils.java
    └── GeoUtils.java
```

**禁止**：`entity`、`mapper`、`service`、`controller`

#### 4.3.2 marl-tsc-stream（Flink为主）

```
com.njhtr.marltsc.stream/
├── StreamApplication.java
├── job/                    # Flink Job主类
│   ├── TrafficStreamJob.java
│   └── FeatureAggregateJob.java
├── function/               # Flink算子
│   ├── TrafficFlowAggregateFunction.java
│   └── CongestionDetectionFunction.java
├── sink/                   # 数据输出映射
│   ├── RedisSinkMapper.java
│   └── KafkaRecordSerializer.java
├── source/                 # 自定义数据源（如需要）
│   └── MqttSourceFunction.java
└── watermark/              # 水位线策略
    └── TrafficWatermarkStrategy.java
```

**注意**：该模块不遵循标准Controller-Service分层，以Flink算子为核心。

#### 4.3.3 marl-tsc-drl-engine

```
com.njhtr.marltsc.drl/
├── DrlEngineApplication.java
├── agent/                  # 智能体定义
│   ├── RLAgent.java        # 接口
│   ├── SACAgent.java
│   └── MADDPGAgent.java
├── network/                # 神经网络
│   ├── ActorNetwork.java
│   ├── CriticNetwork.java
│   └── NetworkFactory.java
├── buffer/                 # 经验回放
│   ├── ReplayBuffer.java
│   └── PrioritizedReplayBuffer.java
├── env/                    # 环境建模
│   ├── TrafficEnvironment.java
│   └── StateSpace.java
├── train/                  # 训练管理
│   ├── Trainer.java
│   └── CheckpointManager.java
├── api/                    # 对外REST接口
│   └── controller/
│       └── ModelController.java
└── infrastructure/
    └── config/
        └── Nd4jConfig.java
```

#### 4.3.4 marl-tsc-signal-control

```
com.njhtr.marltsc.signal/
├── SignalControlApplication.java
├── api/
│   ├── controller/
│   │   ├── SignalPlanController.java
│   │   └── IntersectionController.java
│   └── dto/
│       ├── request/
│       └── response/
├── domain/
│   ├── entity/
│   │   ├── SignalPlan.java
│   │   ├── SignalPhase.java
│   │   └── Intersection.java
│   ├── bo/
│   └── service/
│       └── PhaseOptimizationDomainService.java
├── service/
│   ├── api/
│   │   └── SignalControlAppService.java
│   └── impl/
│       └── SignalControlAppServiceImpl.java
├── infrastructure/
│   ├── client/
│   │   └── DrlEngineClient.java
│   ├── config/
│   ├── mapper/
│   └── protocol/           # 信号机通信协议
│       ├── SignalProtocolAdapter.java
│       └── ModbusTcpClient.java
└── task/
    └── SignalStateSyncTask.java
```

### 4.4 资源文件结构

```
src/main/resources/
├── application.yml              # 主配置（仅放spring.profiles.active）
├── application-dev.yml          # 开发环境
├── application-test.yml         # 测试环境
├── application-prod.yml         # 生产环境
├── logback-spring.xml           # 日志配置
├── db/                          # 数据库脚本（可选）
│   ├── schema.sql
│   └── data.sql
└── static/                      # 静态资源（仅web模块需要）
```

---

## 5. 代码编写规范

### 5.1 命名规范

#### 5.1.1 包名
- 全小写，点分命名
- 禁止下划线、驼峰
- 模块缩写控制在4-6个字符

```java
// ✅ 正确
package com.njhtr.marltsc.coopt.domain.entity;

// ❌ 错误
package com.njhtr.marl_tsc.co_optimization.domainEntity;
```

#### 5.1.2 类名
| 类型 | 命名规则 | 示例 |
|:---|:---|:---|
| 启动类 | `XxxApplication` | `CoOptimizationApplication` |
| Controller | `XxxController` | `SignalPlanController` |
| Service接口 | `XxxService` | `SignalControlAppService` |
| Service实现 | `XxxServiceImpl` | `SignalControlAppServiceImpl` |
| DomainService | `XxxDomainService` | `PhaseOptimizationDomainService` |
| Repository | `XxxRepository` | `SignalPlanRepository` |
| Mapper | `XxxMapper` | `SignalPlanMapper` |
| Entity | 名词，无后缀 | `SignalPlan`、`Intersection` |
| DTO Request | `XxxRequest` | `PhaseAdjustRequest` |
| DTO Response | `XxxResponse` | `SignalStateResponse` |
| VO | `XxxVO` | `RealtimeSignalVO` |
| QO | `XxxQuery` | `TrafficFlowQuery` |
| BO | `XxxBO` | `SignalOptimizationBO` |
| Config | `XxxConfig` | `KafkaConfig` |
| Exception | `XxxException` | `BusinessException` |
| Util | `XxxUtils` | `TimeUtils` |
| Task | `XxxTask` | `SignalStateSyncTask` |
| Client | `XxxClient` | `RoutePlanningClient` |

#### 5.1.3 方法名
| 操作 | 前缀 | 示例 |
|:---|:---|:---|
| 查询单个 | `get` / `find` | `getById`、`findByIntersectionId` |
| 查询列表 | `list` / `query` | `listActivePlans`、`queryTrafficFlow` |
| 分页查询 | `page` | `pageSignalLogs` |
| 创建 | `create` / `save` | `createPlan`、`saveEntity` |
| 更新 | `update` / `modify` | `updatePhase` |
| 删除 | `delete` / `remove` | `deleteById` |
| 校验 | `check` / `validate` | `checkParam`、`validateStatus` |
| 转换 | `convert` / `to` / `from` | `convertToBO`、`toResponse` |
| 计算 | `calculate` / `compute` | `calculateDelay`、`computeGreenTime` |
| 判断 | `is` / `has` / `can` | `isCongested`、`hasActivePlan` |
| 处理 | `handle` / `process` | `handleEvent`、`processMessage` |

#### 5.1.4 变量与常量
```java
// 局部变量：驼峰，首字母小写
int currentPhaseId;
List<TrafficFlow> flowList;

// 类成员变量：驼峰，禁止下划线（除static final外）
private SignalPlan currentPlan;

// 常量：全大写，下划线分隔
private static final int DEFAULT_CYCLE_TIME = 120;
private static final String CACHE_KEY_PREFIX = "signal:plan:";
```

### 5.2 分层规范

#### 5.2.1 Controller 层
- **职责**：接收请求、参数校验、调用Service、返回响应
- **禁止**：直接操作数据库、写业务逻辑、调用Mapper

```java
@RestController
@RequestMapping("/api/v1/signal")
@RequiredArgsConstructor
public class SignalPlanController {

    private final SignalControlAppService signalService;

    @GetMapping("/plans/{intersectionId}")
    public ApiResult<SignalPlanResponse> getPlan(@PathVariable String intersectionId) {
        // 参数校验（简单校验，复杂校验放Service）
        Assert.hasText(intersectionId, "路口ID不能为空");

        // 调用应用服务
        SignalPlanResponse response = signalService.getCurrentPlan(intersectionId);

        return ApiResult.ok(response);
    }

    @PostMapping("/plans/adjust")
    public ApiResult<Void> adjustPhase(@RequestBody @Valid PhaseAdjustRequest request) {
        signalService.adjustPhase(request);
        return ApiResult.ok();
    }
}
```

#### 5.2.2 AppService 层（应用服务）
- **职责**：事务控制、数据组装、调用DomainService、调用基础设施
- **特点**：很薄，只写"取数据-调业务-存数据-转DTO"

```java
@Service
@RequiredArgsConstructor
public class SignalControlAppServiceImpl implements SignalControlAppService {

    private final SignalPlanMapper planMapper;
    private final PhaseOptimizationDomainService domainService;
    private final SignalProtocolAdapter protocolAdapter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPhase(PhaseAdjustRequest request) {
        // 1. 取数据
        SignalPlan plan = planMapper.selectById(request.getPlanId());
        if (plan == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
        }

        // 2. 转BO
        SignalOptimizationBO bo = convertToBO(plan, request);

        // 3. 核心业务交给领域层
        SignalOptimizationBO result = domainService.optimize(bo);

        // 4. 存结果
        planMapper.updatePhase(result.getPlan());

        // 5. 下发到信号机
        protocolAdapter.sendPhaseCommand(result.getPhaseCommand());
    }
}
```

#### 5.2.3 DomainService 层（领域服务）
- **职责**：核心业务规则、算法逻辑、纯计算
- **特点**：不依赖Spring上下文，不操作数据库，可单元测试

```java
@Component
public class PhaseOptimizationDomainService {

    public SignalOptimizationBO optimize(SignalOptimizationBO bo) {
        // 纯业务计算，和Mapper、Http无关
        double criticalFlow = calculateCriticalFlow(bo.getFlows());
        int optimalGreen = computeWebsterGreenTime(criticalFlow, bo.getCycleTime());

        if (bo.getCurrentDelay() > 60 && bo.getQueueLength() > 20) {
            bo.suggestPhaseChange();
        }

        return bo;
    }

    private double calculateCriticalFlow(List<TrafficFlow> flows) {
        return flows.stream()
            .mapToDouble(TrafficFlow::getFlowRate)
            .max()
            .orElse(0.0);
    }
}
```

#### 5.2.4 Repository/Mapper 层
- **职责**：数据持久化，只写SQL或ORM操作
- **禁止**：业务逻辑、参数校验、事务控制（除批量操作外）

### 5.3 POJO 规范

| 类型 | 定义 | 典型字段 | 使用场景 |
|:---|:---|:---|:---|
| **Entity/PO** | 数据库表映射 | `@Id`, `@Column`, `createTime` | DAO层与数据库交互 |
| **DTO Request** | 接口入参 | `@NotBlank`, `@Min` 校验注解 | Controller接收参数 |
| **DTO Response** | 接口出参 | 格式化字符串、脱敏字段 | Controller返回前端 |
| **VO** | 视图对象 | 嵌套结构、枚举中文 | 前端页面渲染 |
| **QO** | 查询对象 | `pageNum`, `pageSize`, 条件字段 | 复杂查询参数封装 |
| **BO** | 业务对象 | 多Entity组合、业务方法 | DomainService内部流转 |

**转换原则**：
- Entity 与 DTO 必须显式转换，禁止直接返回 Entity
- 转换方法命名：`convertToXxx` / `toXxx` / `fromXxx`
- 简单转换用手动setter，复杂转换用 MapStruct

### 5.4 异常处理规范

#### 5.4.1 异常体系
```
Throwable
├── Error（系统级错误，不捕获）
└── Exception
    ├── RuntimeException
    │   ├── BaseException（自定义业务异常基类）
    │   │   ├── BusinessException（业务规则违反）
    │   │   ├── ParamException（参数非法）
    │   │   └── ServiceException（外部服务调用失败）
    │   └── IllegalArgumentException（JDK，参数校验）
    └── CheckedException（IO、SQL等，必须处理）
```

#### 5.4.2 全局异常处理器
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail(ErrorCode.SYSTEM_ERROR);
    }
}
```

#### 5.4.3 异常使用规范
```java
// ✅ 业务异常：已知业务规则违反
if (plan == null) {
    throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
}

// ✅ 参数异常：入参不合法
if (cycleTime < 30 || cycleTime > 300) {
    throw new ParamException("周期时长必须在30-300秒之间");
}

// ❌ 禁止：用异常控制正常流程
try {
    plan = planMapper.selectById(id);
} catch (Exception e) {
    plan = null;  // 错误！
}

// ✅ 正确：先判断再处理
plan = planMapper.selectById(id);
if (plan == null) {
    // 处理空值
}
```

### 5.5 日志规范

#### 5.5.1 日志级别使用
| 级别 | 使用场景 |
|:---|:---|
| **ERROR** | 系统异常、数据丢失、无法恢复的错误 |
| **WARN** | 业务异常、可恢复的异常、潜在风险 |
| **INFO** | 核心业务节点、状态变更、外部调用出入参 |
| **DEBUG** | 详细流程、调试信息、SQL打印 |
| **TRACE** | 最详细跟踪，一般不用 |

#### 5.5.2 日志内容规范
```java
// ✅ 正确：有上下文、有结果
log.info("信号优化完成, intersectionId={}, planId={}, delay={}", 
         intersectionId, planId, delay);

// ❌ 错误：无意义日志
log.info("进入方法");
log.info("处理中");
log.info("结束");

// ✅ 异常日志必须打印堆栈
log.error("下发信号方案失败, intersectionId={}", intersectionId, e);

// ❌ 禁止：log4j/slf4j字符串拼接
log.info("结果=" + result);  // 错误！即使INFO不输出也会拼接字符串
```

#### 5.5.3 日志配置要点
- 生产环境日志级别：`INFO` 及以上
- 开发环境日志级别：`DEBUG`
- 日志文件按天滚动，保留30天
- 敏感字段（车牌号、手机号）必须脱敏

### 5.6 注释规范

#### 5.6.1 类注释
```java
/**
 * 信号配时优化领域服务
 * 
 * <p>基于Webster公式和实时流量数据计算最优绿信比分配</p>
 * 
 * @author 张三
 * @since 1.0.0
 */
@Component
public class PhaseOptimizationDomainService {
```

#### 5.6.2 方法注释
```java
/**
 * 计算最优绿灯时长
 * 
 * @param criticalFlow 关键进口道流量 (veh/h)
 * @param cycleTime 信号周期时长 (s)
 * @return 最优绿灯时长 (s)
 * @throws IllegalArgumentException 当流量或周期为负数时
 */
public int computeOptimalGreenTime(double criticalFlow, int cycleTime) {
```

#### 5.6.3 代码块注释
```java
// 基于Webster延误最小化模型计算绿信比
// 公式: g = (y / Y) * (C - L)
// 其中 y为进口道流量比, Y为总流量比, C为周期, L为总损失时间
double greenRatio = calculateGreenRatio(flowRatios, cycleTime, lostTime);
```

**禁止**：
- 无意义的注释（`// 定义变量`、`// 循环`）
- 注释与代码不符
- 用注释删除代码（用Git管理历史）

### 5.7 接口设计规范（RESTful）

#### 5.7.1 URL设计
```
GET    /api/v1/signal/plans              # 查询列表
GET    /api/v1/signal/plans/{id}         # 查询单个
POST   /api/v1/signal/plans              # 创建
PUT    /api/v1/signal/plans/{id}         # 全量更新
PATCH  /api/v1/signal/plans/{id}/status  # 局部更新
DELETE /api/v1/signal/plans/{id}         # 删除
POST   /api/v1/signal/plans/{id}/execute # 执行动作（非CRUD）
```

#### 5.7.2 响应格式
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "planId": "PLAN_001",
        "phases": [
            {"phaseId": 1, "greenTime": 45}
        ]
    },
    "timestamp": 1714032000000
}
```

错误响应：
```json
{
    "code": 4001,
    "message": "信号方案不存在",
    "data": null,
    "timestamp": 1714032000000
}
```

### 5.8 数据库访问规范

#### 5.8.1 通用原则
- 禁止在代码中写死表名、字段名字符串（用常量或Entity）
- 禁止 `SELECT *`，必须显式指定字段
- 批量操作必须分批（每批不超过1000条）
- 大数据量查询必须分页

#### 5.8.2 MyBatis Mapper示例
```xml
<select id="selectByIntersectionId" resultType="SignalPlan">
    SELECT 
        plan_id,
        intersection_id,
        plan_name,
        cycle_time,
        create_time
    FROM signal_plan
    WHERE intersection_id = #{intersectionId}
      AND status = #{status}
    ORDER BY create_time DESC
    LIMIT #{limit}
</select>
```

### 5.9 并发与线程规范

- 禁止手动创建线程，使用线程池（`ThreadPoolTaskExecutor`）
- 异步方法加 `@Async` 并指定线程池名称
- 共享变量必须考虑线程安全（`ConcurrentHashMap`、`AtomicInteger`）
- Flink算子内禁止使用阻塞IO

### 5.10 测试规范

```
src/test/java/com/njhtr/marltsc/xxx/
├── unit/                       # 单元测试
│   ├── domain/
│   │   └── PhaseOptimizationDomainServiceTest.java
│   └── util/
│       └── GeoUtilsTest.java
└── integration/                # 集成测试
    └── controller/
        └── SignalPlanControllerIT.java
```

- 单元测试：不依赖Spring，直接 `new` 对象测试
- 集成测试：使用 `@SpringBootTest`，测试完整流程
- 测试方法命名：`shouldXxxWhenYxx`、`testXxx`
- 必须断言结果，禁止无断言的测试

---

## 6. 数据流与交互设计

### 6.1 主数据流

```
传感器/设备
    ↓ (MQTT/Modbus)
data-fusion (数据融合)
    ↓ (Kafka: topic=traffic-raw)
stream (Flink实时计算)
    ↓ (Kafka: topic=traffic-features + Redis)
co-optimization (协同优化)
    ├─→ signal-control (信号控制) → 信号机
    └─→ route-planning (路径规划) → 车载终端
    ↓
drl-engine (模型训练/推理，反馈优化)
```

### 6.2 服务间调用方式

| 调用方向 | 方式 | 场景 |
|:---|:---|:---|
| co-optimization → signal-control | OpenFeign + 降级 | 获取当前信号状态 |
| co-optimization → route-planning | OpenFeign + 降级 | 获取路网状态 |
| signal-control → drl-engine | OpenFeign / gRPC | 请求动作决策 |
| data-fusion → stream | Kafka | 原始数据推送 |
| stream → co-optimization | Kafka | 特征数据推送 |
| 任何服务 → 其他服务 | Kafka Event | 异步事件（状态变更、告警） |

---

## 7. 部署与运维规范

### 7.1 端口分配

| 服务 | 端口 | 说明 |
|:---|:---|:---|
| marl-tsc-web | 8080 | 前端入口 |
| marl-tsc-signal-control | 8081 | 信号控制 |
| marl-tsc-route-planning | 8082 | 路径规划 |
| marl-tsc-co-optimization | 8083 | 协同优化 |
| marl-tsc-drl-engine | 8084 | DRL引擎 |
| marl-tsc-data-fusion | 8085 | 数据融合 |
| marl-tsc-stream | 8086 | 流处理 |

### 7.2 健康检查

每个服务必须暴露：
```
GET /actuator/health
GET /actuator/info
```

### 7.3 配置管理

- 开发/测试/生产配置分离（`application-{profile}.yml`）
- 敏感配置（密码、密钥）走配置中心或环境变量
- 禁止在代码库中提交生产密码

### 7.4 容器化（后续）

```dockerfile
# 每个服务独立Dockerfile
FROM eclipse-temurin:17-jre-alpine
COPY target/marl-tsc-xxx-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## 附录：快速检查清单

### 新建模块时检查
- [ ] `pom.xml` 有 `<parent>` 指向 `marl-tsc`
- [ ] `artifactId` 以 `marl-tsc-` 开头
- [ ] 根包名为 `com.njhtr.marltsc.xxx`
- [ ] 启动类在根包下，名称为 `XxxApplication`
- [ ] `application.yml` 配置了独立端口
- [ ] `pom.xml` 引入了 `spring-boot-starter-web`

### 编写代码时检查
- [ ] Controller 只接收参数、调用Service、返回结果
- [ ] ServiceImpl 只取数据、调Domain、存数据、转DTO
- [ ] DomainService 只写纯业务逻辑，不碰数据库
- [ ] Entity 不直接返回给前端
- [ ] 异常有全局处理，不吞异常
- [ ] 日志有上下文参数，不打印无意义信息
- [ ] 接口有注释，复杂算法有公式说明
