-- 创建数据库
CREATE DATABASE IF NOT EXISTS quant_trading DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE quant_trading;

-- 账户状态表
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 持仓表
CREATE TABLE IF NOT EXISTS positions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    INDEX idx_stock_code (stock_code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    INDEX idx_order_id (order_id),
    INDEX idx_stock_code (stock_code),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 成交记录表
CREATE TABLE IF NOT EXISTS trades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    INDEX idx_trade_id (trade_id),
    INDEX idx_order_id (order_id),
    INDEX idx_stock_code (stock_code),
    INDEX idx_traded_at (traded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 资金快照表
CREATE TABLE IF NOT EXISTS equity_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    INDEX idx_snapshot_date (snapshot_date),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 股票池表
CREATE TABLE IF NOT EXISTS stock_pool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(50) NOT NULL,
    added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    removed_at DATETIME,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    priority INT NOT NULL DEFAULT 0,
    INDEX idx_stock_code (stock_code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化账户
INSERT INTO accounts (timestamp, initial_capital, current_capital, total_equity, cash, positions_value, total_pnl, realized_pnl, unrealized_pnl, daily_pnl, daily_loss, trade_count, win_count, loss_count)
VALUES (NOW(), 1000000, 1000000, 1000000, 1000000, 0, 0, 0, 0, 0, 0, 0, 0, 0);

-- 初始化股票池
INSERT INTO stock_pool (stock_code, stock_name, is_active, priority) VALUES
('600519', '贵州茅台', 1, 10),
('000858', '五粮液', 1, 9),
('601318', '中国平安', 1, 8),
('600036', '招商银行', 1, 7),
('601899', '紫金矿业', 1, 6);
