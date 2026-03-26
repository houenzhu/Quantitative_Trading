-- ============================================
-- 量化交易系统数据库建表脚本
-- 数据库: MySQL 8.0+
-- 版本: 1.0
-- 字符集: utf8mb4
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 账户状态表 (account_state)
-- 用途: 保存账户资金、权益、盈亏等核心状态
-- 频率: 每次交易后更新，每日收盘后快照
-- ============================================
DROP TABLE IF EXISTS `account_state`;
CREATE TABLE `account_state` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `timestamp` DATETIME(3) NOT NULL COMMENT '记录时间',
    `initial_capital` DECIMAL(18,4) NOT NULL COMMENT '初始资金',
    `current_capital` DECIMAL(18,4) NOT NULL COMMENT '当前可用资金',
    `total_equity` DECIMAL(18,4) NOT NULL COMMENT '总权益',
    `cash` DECIMAL(18,4) NOT NULL COMMENT '现金',
    `positions_value` DECIMAL(18,4) DEFAULT 0 COMMENT '持仓市值',
    `total_pnl` DECIMAL(18,4) DEFAULT 0 COMMENT '总盈亏',
    `realized_pnl` DECIMAL(18,4) DEFAULT 0 COMMENT '已实现盈亏',
    `unrealized_pnl` DECIMAL(18,4) DEFAULT 0 COMMENT '未实现盈亏',
    `daily_pnl` DECIMAL(18,4) DEFAULT 0 COMMENT '当日盈亏',
    `daily_loss` DECIMAL(18,4) DEFAULT 0 COMMENT '当日亏损累计',
    `trade_count` INT DEFAULT 0 COMMENT '交易次数',
    `win_count` INT DEFAULT 0 COMMENT '盈利次数',
    `loss_count` INT DEFAULT 0 COMMENT '亏损次数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_account_state_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户状态表';

-- ============================================
-- 2. 持仓记录表 (positions)
-- 用途: 保存当前持仓和历史持仓
-- 频率: 交易实时更新
-- ============================================
DROP TABLE IF EXISTS `positions`;
CREATE TABLE `positions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `stock_code` VARCHAR(20) NOT NULL COMMENT '股票代码',
    `stock_name` VARCHAR(50) NOT NULL COMMENT '股票名称',
    `quantity` INT NOT NULL COMMENT '持仓数量',
    `available_quantity` INT NOT NULL COMMENT '可用数量',
    `avg_cost` DECIMAL(18,4) NOT NULL COMMENT '平均成本',
    `current_price` DECIMAL(18,4) DEFAULT 0 COMMENT '当前价格',
    `market_value` DECIMAL(18,4) DEFAULT 0 COMMENT '市值',
    `unrealized_pnl` DECIMAL(18,4) DEFAULT 0 COMMENT '未实现盈亏',
    `unrealized_pnl_pct` DECIMAL(10,4) DEFAULT 0 COMMENT '未实现盈亏百分比',
    `opened_at` DATETIME NOT NULL COMMENT '开仓时间',
    `updated_at` DATETIME NOT NULL COMMENT '更新时间',
    `closed_at` DATETIME DEFAULT NULL COMMENT '平仓时间',
    `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否活跃持仓',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_active` (`stock_code`, `is_active`),
    INDEX `idx_positions_stock_code` (`stock_code`),
    INDEX `idx_positions_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='持仓记录表';

-- ============================================
-- 3. 订单记录表 (orders)
-- 用途: 保存所有订单记录，用于审计和分析
-- 频率: 每次下单时插入
-- ============================================
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` VARCHAR(64) NOT NULL COMMENT '订单ID',
    `stock_code` VARCHAR(20) NOT NULL COMMENT '股票代码',
    `stock_name` VARCHAR(50) NOT NULL COMMENT '股票名称',
    `side` VARCHAR(10) NOT NULL COMMENT '买卖方向: buy/sell',
    `order_type` VARCHAR(20) NOT NULL COMMENT '订单类型: market/limit/stop',
    `price` DECIMAL(18,4) NOT NULL COMMENT '委托价格',
    `quantity` INT NOT NULL COMMENT '委托数量',
    `filled_quantity` INT DEFAULT 0 COMMENT '成交数量',
    `avg_fill_price` DECIMAL(18,4) DEFAULT 0 COMMENT '成交均价',
    `status` VARCHAR(20) NOT NULL COMMENT '状态: pending/submitted/filled/cancelled/rejected',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '策略原因',
    `created_at` DATETIME NOT NULL COMMENT '创建时间',
    `submitted_at` DATETIME DEFAULT NULL COMMENT '提交时间',
    `filled_at` DATETIME DEFAULT NULL COMMENT '成交时间',
    `cancelled_at` DATETIME DEFAULT NULL COMMENT '撤单时间',
    `commission` DECIMAL(18,4) DEFAULT 0 COMMENT '手续费',
    `slippage` DECIMAL(18,4) DEFAULT 0 COMMENT '滑点成本',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    INDEX `idx_orders_stock_code` (`stock_code`),
    INDEX `idx_orders_created_at` (`created_at`),
    INDEX `idx_orders_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单记录表';

-- ============================================
-- 4. 成交记录表 (trades)
-- 用途: 保存成交明细，计算盈亏
-- 频率: 订单成交时插入
-- ============================================
DROP TABLE IF EXISTS `trades`;
CREATE TABLE `trades` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `trade_id` VARCHAR(64) NOT NULL COMMENT '成交ID',
    `order_id` VARCHAR(64) NOT NULL COMMENT '关联订单ID',
    `stock_code` VARCHAR(20) NOT NULL COMMENT '股票代码',
    `stock_name` VARCHAR(50) NOT NULL COMMENT '股票名称',
    `side` VARCHAR(10) NOT NULL COMMENT '买卖方向',
    `quantity` INT NOT NULL COMMENT '成交数量',
    `price` DECIMAL(18,4) NOT NULL COMMENT '成交价格',
    `amount` DECIMAL(18,4) NOT NULL COMMENT '成交金额',
    `commission` DECIMAL(18,4) DEFAULT 0 COMMENT '手续费',
    `stamp_duty` DECIMAL(18,4) DEFAULT 0 COMMENT '印花税',
    `transfer_fee` DECIMAL(18,4) DEFAULT 0 COMMENT '过户费',
    `slippage_cost` DECIMAL(18,4) DEFAULT 0 COMMENT '滑点成本',
    `realized_pnl` DECIMAL(18,4) DEFAULT 0 COMMENT '已实现盈亏',
    `traded_at` DATETIME NOT NULL COMMENT '成交时间',
    `strategy_reason` VARCHAR(500) DEFAULT NULL COMMENT '策略原因',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trade_id` (`trade_id`),
    INDEX `idx_trades_order_id` (`order_id`),
    INDEX `idx_trades_stock_code` (`stock_code`),
    INDEX `idx_trades_traded_at` (`traded_at`),
    CONSTRAINT `fk_trades_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成交记录表';

-- ============================================
-- 5. 权益快照表 (equity_snapshots)
-- 用途: 保存每日权益快照，用于计算回撤、夏普比率
-- 频率: 每日收盘后
-- ============================================
DROP TABLE IF EXISTS `equity_snapshots`;
CREATE TABLE `equity_snapshots` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `snapshot_date` DATE NOT NULL COMMENT '快照日期',
    `timestamp` DATETIME NOT NULL COMMENT '记录时间',
    `total_equity` DECIMAL(18,4) NOT NULL COMMENT '总权益',
    `cash` DECIMAL(18,4) NOT NULL COMMENT '现金',
    `positions_value` DECIMAL(18,4) DEFAULT 0 COMMENT '持仓市值',
    `daily_pnl` DECIMAL(18,4) DEFAULT 0 COMMENT '当日盈亏',
    `daily_pnl_pct` DECIMAL(10,4) DEFAULT 0 COMMENT '当日盈亏百分比',
    `cumulative_pnl` DECIMAL(18,4) DEFAULT 0 COMMENT '累计盈亏',
    `cumulative_pnl_pct` DECIMAL(10,4) DEFAULT 0 COMMENT '累计盈亏百分比',
    `drawdown` DECIMAL(18,4) DEFAULT 0 COMMENT '回撤金额',
    `drawdown_pct` DECIMAL(10,4) DEFAULT 0 COMMENT '回撤百分比',
    `peak_equity` DECIMAL(18,4) DEFAULT 0 COMMENT '峰值权益',
    `trade_count_today` INT DEFAULT 0 COMMENT '当日交易次数',
    `position_count` INT DEFAULT 0 COMMENT '持仓数量',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_snapshot_date` (`snapshot_date`),
    INDEX `idx_equity_snapshots_date` (`snapshot_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益快照表';

-- ============================================
-- 6. 股票池配置表 (stock_pool)
-- 用途: 保存监控的股票列表
-- 频率: 添加/删除股票时更新
-- ============================================
DROP TABLE IF EXISTS `stock_pool`;
CREATE TABLE `stock_pool` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `stock_code` VARCHAR(20) NOT NULL COMMENT '股票代码',
    `stock_name` VARCHAR(50) NOT NULL COMMENT '股票名称',
    `added_at` DATETIME NOT NULL COMMENT '添加时间',
    `removed_at` DATETIME DEFAULT NULL COMMENT '移除时间',
    `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否活跃',
    `priority` INT DEFAULT 0 COMMENT '优先级',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_active` (`stock_code`, `is_active`),
    INDEX `idx_stock_pool_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='股票池配置表';

-- ============================================
-- 7. 策略配置表 (strategy_config)
-- 用途: 保存策略参数配置
-- 频率: 参数调整时更新
-- ============================================
DROP TABLE IF EXISTS `strategy_config`;
CREATE TABLE `strategy_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `strategy_name` VARCHAR(50) NOT NULL COMMENT '策略名称',
    `strategy_type` VARCHAR(50) NOT NULL COMMENT '策略类型: vwap/momentum/orderflow/composite',
    `config_json` JSON NOT NULL COMMENT '配置JSON',
    `weights_json` JSON DEFAULT NULL COMMENT '权重JSON',
    `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME NOT NULL COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_strategy_name` (`strategy_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略配置表';

-- ============================================
-- 8. 系统配置表 (system_config)
-- 用途: 保存系统全局配置
-- 频率: 配置变更时更新
-- ============================================
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(500) NOT NULL COMMENT '配置值',
    `config_type` VARCHAR(20) DEFAULT 'string' COMMENT '值类型: string/number/boolean/json',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `updated_at` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 初始系统配置
INSERT IGNORE INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`, `updated_at`) VALUES
('initial_capital', '1000000', 'number', '初始资金', NOW()),
('max_daily_loss_pct', '0.05', 'number', '单日最大亏损比例', NOW()),
('max_position_pct', '0.2', 'number', '单只股票最大仓位比例', NOW()),
('commission_rate', '0.0003', 'number', '佣金费率', NOW()),
('slippage_rate', '0.0001', 'number', '滑点率', NOW()),
('stamp_duty_rate', '0.001', 'number', '印花税率(仅卖出)', NOW()),
('min_commission', '5', 'number', '最低佣金', NOW());

-- ============================================
-- 9. 风控日志表 (risk_logs)
-- 用途: 记录风控事件
-- 频率: 风控触发时记录
-- ============================================
DROP TABLE IF EXISTS `risk_logs`;
CREATE TABLE `risk_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `timestamp` DATETIME(3) NOT NULL COMMENT '时间',
    `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型: position_limit/daily_loss/var_breach',
    `severity` VARCHAR(20) NOT NULL COMMENT '严重程度: info/warning/error/critical',
    `stock_code` VARCHAR(20) DEFAULT NULL COMMENT '股票代码(可选)',
    `message` VARCHAR(500) NOT NULL COMMENT '消息',
    `details` JSON DEFAULT NULL COMMENT '详情JSON',
    `action_taken` VARCHAR(200) DEFAULT NULL COMMENT '采取的措施',
    PRIMARY KEY (`id`),
    INDEX `idx_risk_logs_timestamp` (`timestamp`),
    INDEX `idx_risk_logs_event_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控日志表';

-- ============================================
-- 10. 系统日志表 (system_logs)
-- 用途: 记录系统运行日志
-- 频率: 实时
-- ============================================
DROP TABLE IF EXISTS `system_logs`;
CREATE TABLE `system_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `timestamp` DATETIME(3) NOT NULL COMMENT '时间',
    `level` VARCHAR(20) NOT NULL COMMENT '日志级别: DEBUG/INFO/WARNING/ERROR/CRITICAL',
    `module` VARCHAR(100) NOT NULL COMMENT '模块名',
    `message` VARCHAR(1000) NOT NULL COMMENT '消息',
    `details` JSON DEFAULT NULL COMMENT '详情JSON',
    PRIMARY KEY (`id`),
    INDEX `idx_system_logs_timestamp` (`timestamp`),
    INDEX `idx_system_logs_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志表';

-- ============================================
-- 11. Tick数据历史表 (tick_history)
-- 用途: 保存历史tick数据，用于回测和分析
-- 频率: 实时写入
-- ============================================
DROP TABLE IF EXISTS `tick_history`;
CREATE TABLE `tick_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `stock_code` VARCHAR(20) NOT NULL COMMENT '股票代码',
    `timestamp` DATETIME(3) NOT NULL COMMENT '时间',
    `price` DECIMAL(18,4) NOT NULL COMMENT '价格',
    `volume` INT DEFAULT 0 COMMENT '成交量',
    `amount` DECIMAL(18,4) DEFAULT 0 COMMENT '成交额',
    `buy_volume` INT DEFAULT 0 COMMENT '主动买入量',
    `sell_volume` INT DEFAULT 0 COMMENT '主动卖出量',
    `bid_price` DECIMAL(18,4) DEFAULT 0 COMMENT '买一价',
    `ask_price` DECIMAL(18,4) DEFAULT 0 COMMENT '卖一价',
    `change_percent` DECIMAL(10,4) DEFAULT 0 COMMENT '涨跌幅',
    PRIMARY KEY (`id`),
    INDEX `idx_tick_history_stock_time` (`stock_code`, `timestamp`),
    INDEX `idx_tick_history_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tick数据历史表';

-- ============================================
-- 12. 信号记录表 (signal_logs)
-- 用途: 记录策略产生的信号
-- 频率: 信号产生时记录
-- ============================================
DROP TABLE IF EXISTS `signal_logs`;
CREATE TABLE `signal_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `timestamp` DATETIME(3) NOT NULL COMMENT '时间',
    `stock_code` VARCHAR(20) NOT NULL COMMENT '股票代码',
    `stock_name` VARCHAR(50) NOT NULL COMMENT '股票名称',
    `strategy_name` VARCHAR(50) NOT NULL COMMENT '策略名称',
    `action` VARCHAR(10) NOT NULL COMMENT '动作: BUY/SELL/HOLD',
    `strength` DECIMAL(10,4) DEFAULT 0 COMMENT '信号强度',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '原因',
    `metrics` JSON DEFAULT NULL COMMENT '指标详情JSON',
    `executed` TINYINT(1) DEFAULT 0 COMMENT '是否执行',
    PRIMARY KEY (`id`),
    INDEX `idx_signal_logs_timestamp` (`timestamp`),
    INDEX `idx_signal_logs_stock_code` (`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信号记录表';

SET FOREIGN_KEY_CHECKS = 1;
