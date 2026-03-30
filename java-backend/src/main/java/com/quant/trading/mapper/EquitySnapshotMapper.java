package com.quant.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.trading.entity.EquitySnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface EquitySnapshotMapper extends BaseMapper<EquitySnapshot> {
    
    @Select("SELECT * FROM equity_snapshots ORDER BY timestamp DESC LIMIT #{limit}")
    List<EquitySnapshot> findRecent(int limit);
    
    @Select("SELECT * FROM equity_snapshots WHERE user_id = #{userId} ORDER BY timestamp DESC LIMIT #{limit}")
    List<EquitySnapshot> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
    
    @Select("SELECT * FROM equity_snapshots WHERE snapshot_date = #{date}")
    EquitySnapshot findByDate(String date);
    
    @Select("SELECT * FROM equity_snapshots WHERE user_id = #{userId} AND snapshot_date = #{date}")
    EquitySnapshot findByUserIdAndDate(@Param("userId") Long userId, @Param("date") String date);
}
