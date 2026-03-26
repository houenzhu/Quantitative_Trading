package com.quant.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.trading.entity.EquitySnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface EquitySnapshotMapper extends BaseMapper<EquitySnapshot> {
    
    @Select("SELECT * FROM equity_snapshots ORDER BY timestamp DESC LIMIT #{limit}")
    List<EquitySnapshot> findRecent(int limit);
    
    @Select("SELECT * FROM equity_snapshots WHERE snapshot_date = #{date}")
    EquitySnapshot findByDate(String date);
}
