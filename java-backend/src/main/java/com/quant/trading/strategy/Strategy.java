package com.quant.trading.strategy;

import com.quant.trading.entity.Signal;
import com.quant.trading.entity.TickData;
import java.util.List;

public interface Strategy {
    
    String getName();
    
    Signal analyze(String stockCode, String stockName, TickData currentTick, List<TickData> history);
    
    void reset();
}
