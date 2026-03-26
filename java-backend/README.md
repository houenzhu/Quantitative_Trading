# 量化交易系统 - Java Spring Boot 3 版本

## 技术栈

- Java 17
- Spring Boot 3.2.0
- MyBatis-Plus 3.5.5
- MySQL 8.0+
- WebSocket (STOMP)
- OkHttp (行情数据获取)

## 项目结构

```
java-backend/
├── src/main/java/com/quant/trading/
│   ├── QuantTradingApplication.java    # 启动类
│   ├── common/                         # 通用类
│   │   └── Result.java                 # 统一响应结果
│   ├── config/                         # 配置类
│   │   ├── CorsConfig.java             # CORS配置
│   │   ├── MyMetaObjectHandler.java    # MyBatis-Plus自动填充
│   │   └── WebSocketConfig.java        # WebSocket配置
│   ├── controller/                     # 控制器层
│   │   ├── AccountController.java
│   │   ├── MarketDataController.java
│   │   ├── OrderController.java
│   │   ├── PositionController.java
│   │   ├── StockPoolController.java
│   │   └── TradeController.java
│   ├── entity/                         # 实体类
│   │   ├── Account.java
│   │   ├── EquitySnapshot.java
│   │   ├── Order.java
│   │   ├── Position.java
│   │   ├── Signal.java
│   │   ├── StockPoolItem.java
│   │   ├── TickData.java
│   │   └── Trade.java
│   ├── fetcher/                        # 数据获取
│   │   ├── HistoryDataFetcher.java
│   │   └── MarketDataFetcher.java
│   ├── mapper/                         # MyBatis Mapper
│   │   ├── AccountMapper.java
│   │   ├── EquitySnapshotMapper.java
│   │   ├── OrderMapper.java
│   │   ├── PositionMapper.java
│   │   ├── StockPoolMapper.java
│   │   └── TradeMapper.java
│   ├── scheduler/                      # 定时任务
│   │   └── MarketDataScheduler.java
│   ├── service/                        # 服务层
│   │   ├── AccountService.java
│   │   ├── OrderService.java
│   │   ├── PositionService.java
│   │   ├── StockPoolService.java
│   │   └── TradeService.java
│   ├── strategy/                       # 交易策略
│   │   ├── CompositeStrategy.java
│   │   ├── MomentumStrategy.java
│   │   ├── Strategy.java
│   │   └── VWAPStrategy.java
│   ├── trading/                        # 交易相关
│   │   └── TradingHours.java
│   └── websocket/                      # WebSocket
│       ├── MarketDataBroadcaster.java
│       └── WebSocketController.java
├── src/main/resources/
│   ├── application.yml                 # 配置文件
│   └── db/
│       └── schema.sql                  # 数据库建表语句
└── pom.xml                             # Maven配置
```

## 快速开始

### 1. 环境准备

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 2. 创建数据库

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/quant_trading?...
    username: your_username
    password: your_password
```

### 4. 编译运行

```bash
cd java-backend
mvn clean install
mvn spring-boot:run
```

### 5. 访问

- HTTP API: http://localhost:8080/api/
- WebSocket: ws://localhost:8080/ws

## API 接口

### 账户相关
- `GET /api/account/latest` - 获取最新账户状态
- `GET /api/account/recent?limit=10` - 获取历史账户状态
- `POST /api/account/init?initialCapital=1000000` - 初始化账户

### 持仓相关
- `GET /api/position/active` - 获取活跃持仓
- `GET /api/position/{stockCode}` - 获取指定股票持仓
- `POST /api/position/open` - 开仓
- `POST /api/position/close` - 平仓

### 订单相关
- `GET /api/order/recent?limit=100` - 获取最近订单
- `GET /api/order/{orderId}` - 获取指定订单
- `POST /api/order/create` - 创建订单
- `POST /api/order/status` - 更新订单状态

### 股票池相关
- `GET /api/stock/pool` - 获取股票池
- `POST /api/stock/pool/add` - 添加股票
- `POST /api/stock/pool/remove` - 移除股票

### 行情相关
- `GET /api/market/tick/{stockCode}` - 获取单只股票行情
- `POST /api/market/tick/batch` - 批量获取行情

## WebSocket 订阅

使用 STOMP 协议：

```javascript
// 连接
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // 订阅行情
    stompClient.subscribe('/topic/tick/600519', function(message) {
        console.log(JSON.parse(message.body));
    });
    
    // 订阅股票池更新
    stompClient.subscribe('/topic/stockPool', function(message) {
        console.log(JSON.parse(message.body));
    });
});
```

## 交易策略

系统内置三种策略：

1. **MomentumStrategy** - 动量策略
2. **VWAPStrategy** - 成交量加权平均价策略
3. **CompositeStrategy** - 复合策略（组合动量和VWAP）

## 交易时间

A股交易时间：
- 上午: 9:30 - 11:30
- 下午: 13:00 - 15:00

系统会自动判断是否为交易时间，非交易时间不获取行情。
