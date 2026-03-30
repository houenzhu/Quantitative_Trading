# 美股功能测试指南

## 🧪 测试美股功能

### 1. 搜索美股

**API请求:**
```bash
GET /api/market/search?keyword=AAPL
```

**返回示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "code": "AAPL",
      "name": "Apple Inc.",
      "display": "AAPL - Apple Inc. (美股)"
    }
  ]
}
```

### 2. 获取美股实时行情

**API请求:**
```bash
GET /api/market/tick/AAPL
```

**返回示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "code": "AAPL",
    "name": "AAPL",
    "price": 178.52,
    "preClose": 177.30,
    "open": 178.00,
    "high": 179.25,
    "low": 177.80,
    "volume": 52345678,
    "change": 1.22,
    "changePct": 0.6883,
    "time": "2026-03-30T10:30:00"
  }
}
```

### 3. 批量获取行情（A股+美股混合）

**API请求:**
```bash
POST /api/market/tick/batch
Content-Type: application/json

["600547", "AAPL", "000001", "TSLA"]
```

**返回示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "code": "600547",
      "name": "山东黄金",
      "price": 25.30,
      ...
    },
    {
      "code": "AAPL",
      "name": "AAPL",
      "price": 178.52,
      ...
    },
    {
      "code": "000001",
      "name": "平安银行",
      "price": 12.45,
      ...
    },
    {
      "code": "TSLA",
      "name": "TSLA",
      "price": 245.67,
      ...
    }
  ]
}
```

### 4. 查看市场交易状态

**API请求:**
```bash
GET /api/market/status
```

**返回示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "aStock": {
      "status": "休市",
      "isTrading": false,
      "currentTime": "2026-03-30T22:30:00",
      "timezone": "Asia/Shanghai"
    },
    "usStock": {
      "status": "交易中",
      "isTrading": true,
      "currentTime": "2026-03-30T10:30:00",
      "timezone": "America/New_York"
    }
  }
}
```

## 📝 前端使用示例

### 在股票池中添加美股

```javascript
// 添加美股到股票池
const addUSStock = async () => {
  const stockCode = 'AAPL'; // 苹果公司
  
  // 搜索美股
  const searchResult = await api.searchStocks(stockCode);
  
  // 添加到股票池
  await api.addToStockPool({
    code: stockCode,
    name: searchResult.data[0].name
  });
  
  // 获取实时行情
  const tickData = await api.getTickData(stockCode);
  console.log('AAPL当前价格:', tickData.data.price);
};
```

### 创建美股策略

```javascript
// 创建美股均线策略
const createUSStrategy = async () => {
  await api.createStrategy({
    name: '苹果均线策略',
    type: 'MA_CROSSOVER',
    stockCode: 'AAPL', // 美股代码
    parameters: {
      shortPeriod: 5,
      longPeriod: 20,
      klinePeriod: '1d'
    },
    status: 'ACTIVE'
  });
};
```

## 🎯 常用美股代码

| 代码 | 公司名称 | 行业 |
|------|---------|------|
| AAPL | 苹果公司 | 科技 |
| TSLA | 特斯拉 | 电动汽车 |
| GOOGL | 谷歌 | 科技 |
| MSFT | 微软 | 科技 |
| AMZN | 亚马逊 | 电商 |
| META | Meta(Facebook) | 社交媒体 |
| NVDA | 英伟达 | 半导体 |
| NFLX | 奈飞 | 流媒体 |
| BABA | 阿里巴巴 | 电商 |
| JD | 京东 | 电商 |

## ⚠️ 注意事项

1. **数据源**: 使用新浪财经API获取美股数据，稳定可靠
2. **交易时间**: 美股交易时间为北京时间21:30-04:00（夏令时）/ 22:30-05:00（冬令时）
3. **货币**: 美股价格为美元计价，A股为人民币计价
4. **盘前盘后**: 美股支持盘前（04:00-09:30）和盘后（16:00-20:00）交易

## 🔧 数据源说明

系统使用新浪财经API获取美股数据：
- **API地址**: `https://hq.sinajs.cn/list=gb_{symbol}`
- **数据格式**: 与A股数据格式类似，易于解析
- **稳定性**: 新浪财经是国内知名的金融数据提供商，数据稳定可靠
- **无需API Key**: 免费使用，无需申请API密钥

## 📊 数据字段说明

美股行情数据包含以下字段：
- **name**: 公司名称
- **price**: 当前价格
- **change**: 涨跌额
- **changePct**: 涨跌幅（百分比）
- **open**: 开盘价
- **high**: 最高价
- **low**: 最低价
- **volume**: 成交量
- **preClose**: 前收盘价
- **amount**: 成交额
