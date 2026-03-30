# 量化交易系统 v1.0

一个完整的量化交易监控系统，支持多用户、策略引擎、实时行情推送和交易管理。

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端展示层 (Vue 3)                     │
│  Dashboard │ 策略管理 │ 持仓管理 │ 订单管理 │ 账户管理   │
└─────────────────────────────────────────────────────────┘
                           │
┌─────────────────────────────────────────────────────────┐
│                后端服务层 (Spring Boot)                   │
│  用户认证 │ 策略引擎 │ 订单引擎 │ 行情推送 │ 风控引擎   │
└─────────────────────────────────────────────────────────┘
                           │
┌─────────────────────────────────────────────────────────┐
│                  数据持久层 (MySQL)                       │
│  用户数据 │ 策略数据 │ 交易数据 │ 行情数据 │ 日志数据   │
└─────────────────────────────────────────────────────────┘
```

## ✨ 核心功能

### 1. 用户系统
- ✅ 用户注册/登录 (Sa-Token认证)
- ✅ 多用户数据隔离
- ✅ 个人账户管理

### 2. 策略引擎
- ✅ 均线交叉策略 (MA_CROSSOVER)
- ✅ MACD策略
- ✅ RSI策略
- ✅ 布林带策略 (BOLLINGER)
- ✅ 动量策略 (MOMENTUM)
- ✅ VWAP策略
- ✅ 复合策略 (COMPOSITE)

### 3. 技术指标
- ✅ MA/EMA - 移动平均线
- ✅ MACD - 指数平滑异同移动平均线
- ✅ RSI - 相对强弱指标
- ✅ BOLL - 布林带
- ✅ KDJ - 随机指标
- ✅ VWAP - 成交量加权平均价

### 4. 交易管理
- ✅ 实时行情推送 (WebSocket)
- ✅ A股/美股双市场支持
- ✅ 订单管理
- ✅ 持仓管理
- ✅ 成交记录
- ✅ 账户资金管理

### 5. 数据持久化
- ✅ MySQL数据库存储
- ✅ 资金快照
- ✅ 策略执行日志
- ✅ K线历史数据

## 🌍 市场支持

### A股市场
- **交易时间**: 周一至周五 09:30-11:30, 13:00-15:00
- **数据源**: 新浪财经API
- **股票代码**: 6位数字（如：600547, 000001）
- **涨跌幅限制**: ±10%（科创板±20%）

### 美股市场
- **交易时间**: 周一至周五 21:30-04:00（夏令时）/ 22:30-05:00（冬令时）
- **数据源**: 新浪财经API
- **股票代码**: 字母代码（如：AAPL, TSLA, GOOGL）
- **涨跌幅限制**: 无限制

### 自动识别
系统会自动识别股票代码类型：
- 纯数字代码 → A股
- 字母代码 → 美股

## 🚀 快速开始

### 环境要求
- JDK 17+
- Node.js 16+
- MySQL 8.0+
- Maven 3.6+

### 1. 初始化数据库

```bash
# 创建数据库并导入表结构
mysql -u root -p < java-backend/src/main/resources/db/init.sql
```

### 2. 启动后端服务

```bash
cd java-backend

# 编译项目
mvn clean install

# 启动服务
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

### 3. 启动前端服务

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务将在 `http://localhost:3000` 启动

### 4. 访问系统

打开浏览器访问 `http://localhost:3000`

- 默认用户: `test` / `123456`
- 或注册新用户

## 📁 项目结构

```
Quantitative_Trading/
├── java-backend/                 # Java后端服务
│   ├── src/main/java/
│   │   └── com/quant/trading/
│   │       ├── controller/       # REST API控制器
│   │       ├── service/          # 业务逻辑层
│   │       ├── mapper/           # 数据访问层
│   │       ├── entity/           # 实体类
│   │       ├── strategy/         # 策略实现
│   │       ├── fetcher/          # 数据获取
│   │       ├── websocket/        # WebSocket服务
│   │       └── scheduler/        # 定时任务
│   └── src/main/resources/
│       ├── application.yml       # 配置文件
│       └── db/init.sql           # 数据库初始化
│
├── frontend/                     # Vue前端
│   ├── src/
│   │   ├── components/           # Vue组件
│   │   ├── api/                  # API接口
│   │   ├── utils/                # 工具函数
│   │   └── App.vue               # 主应用
│   └── vite.config.js            # Vite配置
│
└── README.md                     # 项目说明
```

## 🔧 配置说明

### 后端配置 (application.yml)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/quant_trading
    username: root
    password: your_password
```

### 前端配置 (vite.config.js)

```javascript
server: {
  port: 3000,
  proxy: {
    '/ws': {
      target: 'ws://localhost:8080',
      ws: true
    },
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 📊 API文档

### 认证接口
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/logout` - 退出登录

### 策略接口
- `GET /api/strategy/list` - 获取策略列表
- `POST /api/strategy/create` - 创建策略
- `POST /api/strategy/{id}/execute` - 执行策略

### 交易接口
- `GET /api/order/recent` - 获取最近订单
- `GET /api/position/active` - 获取活跃持仓
- `GET /api/account/latest` - 获取账户信息

### 行情接口
- `GET /api/market/search` - 搜索股票（支持A股和美股）
- `GET /api/market/tick/{stockCode}` - 获取实时行情
- `POST /api/market/tick/batch` - 批量获取行情
- `GET /api/market/status` - 获取市场交易状态

## 🛡️ 安全说明

- 使用 Sa-Token 进行用户认证
- 密码使用 BCrypt 加密存储
- 所有用户数据严格隔离
- WebSocket 连接需要用户认证

## 📝 开发计划

- [ ] 回测框架
- [ ] 风险管理器
- [ ] 订单流策略
- [ ] 策略参数优化
- [ ] 绩效分析报表
- [ ] 消息通知系统

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 👨‍💻 作者

houen_zhu

---

**注意**: 本系统仅供学习和研究使用，不构成任何投资建议。使用本系统进行实盘交易需自行承担风险。
