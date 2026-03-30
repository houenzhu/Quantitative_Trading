package com.quant.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.trading.entity.KlineData;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;

/**
 * K线数据Mapper接口
 * 提供K线数据的数据库操作方法
 */
@Mapper
public interface KlineDataMapper extends BaseMapper<KlineData> {
    
    /**
     * 查询指定股票的最近N条K线数据
     * @param stockCode 股票代码
     * @param period 周期
     * @param limit 限制数量
     * @return K线数据列表
     */
    @Select("SELECT * FROM kline_data WHERE stock_code = #{stockCode} AND period = #{period} " +
            "ORDER BY trade_time DESC LIMIT #{limit}")
    List<KlineData> findLatestKlines(@Param("stockCode") String stockCode, 
                                      @Param("period") String period, 
                                      @Param("limit") int limit);
    
    /**
     * 查询指定时间范围内的K线数据
     * @param stockCode 股票代码
     * @param period 周期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return K线数据列表
     */
    @Select("SELECT * FROM kline_data WHERE stock_code = #{stockCode} AND period = #{period} " +
            "AND trade_time >= #{startTime} AND trade_time <= #{endTime} " +
            "ORDER BY trade_time ASC")
    List<KlineData> findByTimeRange(@Param("stockCode") String stockCode,
                                     @Param("period") String period,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询指定股票的最新收盘价
     * @param stockCode 股票代码
     * @param period 周期
     * @return 最新K线数据
     */
    @Select("SELECT * FROM kline_data WHERE stock_code = #{stockCode} AND period = #{period} " +
            "ORDER BY trade_time DESC LIMIT 1")
    KlineData findLatestKline(@Param("stockCode") String stockCode, @Param("period") String period);
    
    /**
     * 查询指定股票最近N条收盘价
     * @param stockCode 股票代码
     * @param period 周期
     * @param limit 限制数量
     * @return 收盘价列表(按时间升序)
     */
    @Select("SELECT close_price FROM kline_data WHERE stock_code = #{stockCode} AND period = #{period} " +
            "ORDER BY trade_time DESC LIMIT #{limit}")
    List<java.math.BigDecimal> findLatestClosePrices(@Param("stockCode") String stockCode,
                                                      @Param("period") String period,
                                                      @Param("limit") int limit);
    
    /**
     * 插入或更新K线数据
     * 如果存在相同股票代码、周期和时间的记录，则更新；否则插入新记录
     * @param klineData K线数据
     * @return 影响行数
     */
    @Insert("INSERT INTO kline_data (stock_code, period, open_price, high_price, low_price, close_price, volume, amount, trade_time, created_at) " +
            "VALUES (#{stockCode}, #{period}, #{openPrice}, #{highPrice}, #{lowPrice}, #{closePrice}, #{volume}, #{amount}, #{tradeTime}, NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "open_price = VALUES(open_price), " +
            "high_price = VALUES(high_price), " +
            "low_price = VALUES(low_price), " +
            "close_price = VALUES(close_price), " +
            "volume = VALUES(volume), " +
            "amount = VALUES(amount)")
    int insertOrUpdate(KlineData klineData);
}
