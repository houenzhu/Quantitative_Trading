-- =====================================================
-- 量化交易系统数据库初始化脚本
-- 创建日期: 2026-03-27
-- 说明: 合并了schema.sql, migration_add_user_id.sql, migration_strategy_engine.sql
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS quant_trading DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE quant_trading;

-- =====================================================
-- 1. 用户表
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(255),
    status TINYINT(1) NOT NULL DEFAULT 1,
    initial_capital DECIMAL(18, 4) NOT NULL DEFAULT 1000000,
    current_capital DECIMAL(18, 4) NOT NULL DEFAULT 1000000,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at DATETIME,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 2. 账户状态表
-- =====================================================
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    timestamp DATETIME NOT NULL,
    initial_capital DECIMAL(18, 4) NOT NULL DEFAULT 0,
    current_capital DECIMAL(18, 4) NOT NULL DEFAULT 0,
    total_equity DECIMAL(18, 4) NOT NULL DEFAULT 0,
    cash DECIMAL(18, 4) NOT NULL DEFAULT 0,
    positions_value DECIMAL(18, 4) NOT NULL DEFAULT 0,
    total_pnl DECIMAL(18, 4) NOT NULL DEFAULT 0,
    realized_pnl DECIMAL(18, 4) NOT NULL DEFAULT 0,
    unrealized_pnl DECIMAL(18, 4) NOT NULL DEFAULT 0,
    daily_pnl DECIMAL(18, 4) NOT NULL DEFAULT 0,
    daily_loss DECIMAL(18, 4) NOT NULL DEFAULT 0,
    trade_count INT NOT NULL DEFAULT 0,
    win_count INT NOT NULL DEFAULT 0,
    loss_count INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 3. 持仓表
-- =====================================================
CREATE TABLE IF NOT EXISTS positions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(50) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    available_quantity INT NOT NULL DEFAULT 0,
    avg_cost DECIMAL(18, 4) NOT NULL DEFAULT 0,
    current_price DECIMAL(18, 4) NOT NULL DEFAULT 0,
    market_value DECIMAL(18, 4) NOT NULL DEFAULT 0,
    unrealized_pnl DECIMAL(18, 4) NOT NULL DEFAULT 0,
    unrealized_pnl_pct DECIMAL(10, 4) NOT NULL DEFAULT 0,
    opened_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at DATETIME,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    INDEX idx_user_id (user_id),
    INDEX idx_stock_code (stock_code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 4. 订单表
-- =====================================================
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id VARCHAR(50) NOT NULL UNIQUE,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(50) NOT NULL,
    side VARCHAR(10) NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    price DECIMAL(18, 4) NOT NULL DEFAULT 0,
    quantity INT NOT NULL DEFAULT 0,
    filled_quantity INT NOT NULL DEFAULT 0,
    avg_fill_price DECIMAL(18, 4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    submitted_at DATETIME,
    filled_at DATETIME,
    cancelled_at DATETIME,
    commission DECIMAL(18, 4) NOT NULL DEFAULT 0,
    slippage DECIMAL(18, 4) NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    INDEX idx_user_id (user_id),
    INDEX idx_order_id (order_id),
    INDEX idx_stock_code (stock_code),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 5. 成交记录表
-- =====================================================
CREATE TABLE IF NOT EXISTS trades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    trade_id VARCHAR(50) NOT NULL UNIQUE,
    order_id VARCHAR(50) NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(50) NOT NULL,
    side VARCHAR(10) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    price DECIMAL(18, 4) NOT NULL DEFAULT 0,
    amount DECIMAL(18, 4) NOT NULL DEFAULT 0,
    commission DECIMAL(18, 4) NOT NULL DEFAULT 0,
    stamp_duty DECIMAL(18, 4) NOT NULL DEFAULT 0,
    transfer_fee DECIMAL(18, 4) NOT NULL DEFAULT 0,
    slippage_cost DECIMAL(18, 4) NOT NULL DEFAULT 0,
    realized_pnl DECIMAL(18, 4) NOT NULL DEFAULT 0,
    traded_at DATETIME NOT NULL,
    strategy_reason VARCHAR(500),
    INDEX idx_user_id (user_id),
    INDEX idx_trade_id (trade_id),
    INDEX idx_order_id (order_id),
    INDEX idx_stock_code (stock_code),
    INDEX idx_traded_at (traded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 6. 资金快照表
-- =====================================================
CREATE TABLE IF NOT EXISTS equity_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    timestamp DATETIME NOT NULL,
    total_equity DECIMAL(18, 4) NOT NULL DEFAULT 0,
    cash DECIMAL(18, 4) NOT NULL DEFAULT 0,
    positions_value DECIMAL(18, 4) NOT NULL DEFAULT 0,
    daily_pnl DECIMAL(18, 4) NOT NULL DEFAULT 0,
    daily_pnl_pct DECIMAL(10, 4) NOT NULL DEFAULT 0,
    cumulative_pnl DECIMAL(18, 4) NOT NULL DEFAULT 0,
    cumulative_pnl_pct DECIMAL(10, 4) NOT NULL DEFAULT 0,
    drawdown DECIMAL(18, 4) NOT NULL DEFAULT 0,
    drawdown_pct DECIMAL(10, 4) NOT NULL DEFAULT 0,
    peak_equity DECIMAL(18, 4) NOT NULL DEFAULT 0,
    trade_count_today INT NOT NULL DEFAULT 0,
    position_count INT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_snapshot_date (snapshot_date),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 7. 股票池表
-- =====================================================
CREATE TABLE IF NOT EXISTS stock_pool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(50) NOT NULL,
    added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    removed_at DATETIME,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    priority INT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_stock_code (stock_code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 8. 策略配置表
-- =====================================================
CREATE TABLE IF NOT EXISTS strategies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    strategy_type VARCHAR(50) NOT NULL COMMENT '策略类型: MA_CROSSOVER, MACD, RSI, BOLLINGER, MOMENTUM, VWAP, COMPOSITE, CUSTOM',
    status VARCHAR(20) NOT NULL DEFAULT 'inactive' COMMENT '状态: active, inactive, paused',
    
    parameters TEXT NOT NULL COMMENT '策略参数配置(JSON格式)',
    
    max_position_pct DECIMAL(10, 4) NOT NULL DEFAULT 0.1 COMMENT '单只股票最大仓位比例',
    stop_loss_pct DECIMAL(10, 4) NOT NULL DEFAULT 0.05 COMMENT '止损比例',
    take_profit_pct DECIMAL(10, 4) NOT NULL DEFAULT 0.1 COMMENT '止盈比例',
    max_trades_per_day INT NOT NULL DEFAULT 10 COMMENT '每日最大交易次数',
    
    trade_size INT NOT NULL DEFAULT 100 COMMENT '每次交易数量',
    allow_short TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许做空',
    auto_execute TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否自动执行',
    
    total_signals INT NOT NULL DEFAULT 0 COMMENT '总信号数',
    executed_signals INT NOT NULL DEFAULT 0 COMMENT '已执行信号数',
    win_count INT NOT NULL DEFAULT 0 COMMENT '盈利次数',
    loss_count INT NOT NULL DEFAULT 0 COMMENT '亏损次数',
    total_pnl DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '总盈亏',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_executed_at DATETIME COMMENT '最后执行时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_strategy_type (strategy_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 9. 交易信号表
-- =====================================================
CREATE TABLE IF NOT EXISTS signals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    strategy_id BIGINT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(50) NOT NULL,
    
    signal_type VARCHAR(10) NOT NULL COMMENT '信号类型: BUY, SELL',
    signal_strength DECIMAL(10, 4) NOT NULL DEFAULT 0 COMMENT '信号强度(0-1)',
    price DECIMAL(18, 4) NOT NULL COMMENT '信号触发价格',
    
    reason VARCHAR(500) COMMENT '信号生成原因',
    indicators TEXT COMMENT '触发时的指标数据(JSON格式)',
    
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending, executed, cancelled, expired',
    executed_at DATETIME COMMENT '执行时间',
    executed_price DECIMAL(18, 4) COMMENT '执行价格',
    order_id VARCHAR(50) COMMENT '关联订单ID',
    
    pnl DECIMAL(18, 4) COMMENT '盈亏金额',
    pnl_pct DECIMAL(10, 4) COMMENT '盈亏比例',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expired_at DATETIME COMMENT '信号过期时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_strategy_id (strategy_id),
    INDEX idx_stock_code (stock_code),
    INDEX idx_signal_type (signal_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 10. 策略执行日志表
-- =====================================================
CREATE TABLE IF NOT EXISTS strategy_execution_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    strategy_id BIGINT NOT NULL,
    stock_code VARCHAR(20),
    
    action VARCHAR(50) NOT NULL COMMENT '执行动作: SIGNAL_GENERATED, ORDER_CREATED, ORDER_FILLED, ERROR',
    message TEXT COMMENT '日志消息',
    details TEXT COMMENT '详细信息(JSON格式)',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_strategy_id (strategy_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 11. K线数据表
-- =====================================================
CREATE TABLE IF NOT EXISTS kline_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    period VARCHAR(10) NOT NULL COMMENT '周期: 1m, 5m, 15m, 30m, 1h, 1d, 1w',
    
    open_price DECIMAL(18, 4) NOT NULL,
    high_price DECIMAL(18, 4) NOT NULL,
    low_price DECIMAL(18, 4) NOT NULL,
    close_price DECIMAL(18, 4) NOT NULL,
    volume BIGINT NOT NULL DEFAULT 0,
    amount DECIMAL(18, 4) NOT NULL DEFAULT 0,
    
    trade_time DATETIME NOT NULL COMMENT '交易时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_stock_period_time (stock_code, period, trade_time),
    INDEX idx_stock_code (stock_code),
    INDEX idx_period (period),
    INDEX idx_trade_time (trade_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 初始化数据
-- =====================================================

-- 插入默认管理员用户 (密码: admin123，使用BCrypt加密)
INSERT INTO users (username, password, nickname, email, status, initial_capital, current_capital) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员', 'admin@quant.com', 1, 1000000, 1000000)
ON DUPLICATE KEY UPDATE username = username;

-- 插入默认股票池
INSERT INTO stock_pool (user_id, stock_code, stock_name, is_active, priority) VALUES
(1, '600519', '贵州茅台', 1, 10),
(1, '000858', '五粮液', 1, 9),
(1, '601318', '中国平安', 1, 8),
(1, '600036', '招商银行', 1, 7),
(1, '601899', '紫金矿业', 1, 6)
ON DUPLICATE KEY UPDATE stock_code = stock_code;

-- 插入默认账户状态
INSERT INTO accounts (user_id, timestamp, initial_capital, current_capital, total_equity, cash, positions_value, total_pnl, realized_pnl, unrealized_pnl, daily_pnl, daily_loss, trade_count, win_count, loss_count)
VALUES (1, NOW(), 1000000, 1000000, 1000000, 1000000, 0, 0, 0, 0, 0, 0, 0, 0, 0)
ON DUPLICATE KEY UPDATE user_id = user_id;

-- 插入示例策略
INSERT INTO strategies (user_id, name, description, strategy_type, status, parameters, max_position_pct, stop_loss_pct, take_profit_pct, auto_execute, trade_size) VALUES
(1, '均线交叉策略', '基于5日和20日均线交叉的交易策略(日K线)', 'MA_CROSSOVER', 'inactive', '{"shortPeriod":5,"longPeriod":20,"klinePeriod":"1d"}', 0.1, 0.05, 0.1, 0, 100),
(1, 'MACD策略', '基于MACD指标的交易策略(日K线)', 'MACD', 'inactive', '{"fastPeriod":12,"slowPeriod":26,"signalPeriod":9}', 0.1, 0.05, 0.1, 0, 100),
(1, 'RSI策略', '基于RSI指标的交易策略(日K线)', 'RSI', 'inactive', '{"period":14,"overbought":70,"oversold":30}', 0.1, 0.05, 0.1, 0, 100),
(1, '动量策略', '基于实时价格动量的交易策略(实时)', 'MOMENTUM', 'inactive', '{"lookbackPeriod":5,"momentumThreshold":0.02}', 0.1, 0.05, 0.1, 0, 100),
(1, 'VWAP策略', '基于成交量加权平均价的交易策略(实时)', 'VWAP', 'inactive', '{"deviationThreshold":0.01}', 0.1, 0.05, 0.1, 0, 100),
(1, '复合策略', '结合动量和VWAP的综合交易策略(实时)', 'COMPOSITE', 'inactive', '{"momentumThreshold":0.02,"deviationThreshold":0.01}', 0.1, 0.05, 0.1, 0, 100)
ON DUPLICATE KEY UPDATE name = name;

-- =====================================================
-- 完成
-- =====================================================
