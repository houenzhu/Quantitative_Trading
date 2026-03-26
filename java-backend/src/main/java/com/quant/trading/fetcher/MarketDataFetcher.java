package com.quant.trading.fetcher;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.quant.trading.entity.TickData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class MarketDataFetcher {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataFetcher.class);
    
    private final OkHttpClient client;
    
    private static final String QUOTE_URL = "https://push2.eastmoney.com/api/qt/stock/get";
    private static final String BATCH_QUOTE_URL = "https://push2.eastmoney.com/api/qt/stock/list";
    
    public MarketDataFetcher() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }
    
    public TickData fetchTickData(String stockCode) {
        try {
            String secId = getSecId(stockCode);
            String url = QUOTE_URL + "?secid=" + secId + "&fields=f43,f44,f45,f46,f47,f48,f49,f50,f51,f52,f55,f57,f58,f60,f170,f171";
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Referer", "https://quote.eastmoney.com/")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("获取行情失败: {}", response.code());
                    return null;
                }
                
                String body = response.body().string();
                JSONObject json = JSON.parseObject(body);
                
                if (json == null || json.getJSONObject("data") == null) {
                    logger.error("行情数据为空: {}", stockCode);
                    return null;
                }
                
                return parseTickData(json.getJSONObject("data"), stockCode);
            }
        } catch (Exception e) {
            logger.error("获取行情异常: {}", stockCode, e);
            return null;
        }
    }
    
    public List<TickData> fetchBatchTickData(List<String> stockCodes) {
        List<TickData> result = new ArrayList<>();
        
        if (stockCodes == null || stockCodes.isEmpty()) {
            return result;
        }
        
        try {
            StringBuilder secIds = new StringBuilder();
            for (String code : stockCodes) {
                if (secIds.length() > 0) {
                    secIds.append(",");
                }
                secIds.append(getSecId(code));
            }
            
            String url = BATCH_QUOTE_URL + "?secids=" + secIds + "&fields=f43,f44,f45,f46,f47,f48,f49,f50,f51,f52,f55,f57,f58,f60,f170,f171";
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Referer", "https://quote.eastmoney.com/")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("批量获取行情失败: {}", response.code());
                    return result;
                }
                
                String body = response.body().string();
                JSONObject json = JSON.parseObject(body);
                
                if (json == null || json.getJSONObject("data") == null) {
                    return result;
                }
                
                JSONArray diff = json.getJSONObject("data").getJSONArray("diff");
                if (diff != null) {
                    for (int i = 0; i < diff.size(); i++) {
                        JSONObject item = diff.getJSONObject(i);
                        String code = item.getString("f57");
                        result.add(parseTickData(item, code));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("批量获取行情异常", e);
        }
        
        return result;
    }
    
    private TickData parseTickData(JSONObject data, String stockCode) {
        TickData tick = new TickData();
        tick.setCode(stockCode);
        tick.setName(data.getString("f58"));
        
        BigDecimal price = getBigDecimal(data, "f43");
        if (price != null) {
            price = price.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        tick.setPrice(price);
        
        BigDecimal preClose = getBigDecimal(data, "f60");
        if (preClose != null) {
            preClose = preClose.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        tick.setPreClose(preClose);
        
        BigDecimal open = getBigDecimal(data, "f46");
        if (open != null) {
            open = open.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        tick.setOpen(open);
        
        BigDecimal high = getBigDecimal(data, "f44");
        if (high != null) {
            high = high.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        tick.setHigh(high);
        
        BigDecimal low = getBigDecimal(data, "f45");
        if (low != null) {
            low = low.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        tick.setLow(low);
        
        tick.setVolume(getBigDecimal(data, "f47"));
        tick.setAmount(getBigDecimal(data, "f48"));
        
        if (price != null && preClose != null && preClose.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal change = price.subtract(preClose);
            tick.setChange(change);
            BigDecimal changePct = change.divide(preClose, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            tick.setChangePct(changePct);
        }
        
        tick.setTime(LocalDateTime.now());
        
        return tick;
    }
    
    private BigDecimal getBigDecimal(JSONObject data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    private String getSecId(String stockCode) {
        if (stockCode.startsWith("6")) {
            return "1." + stockCode;
        } else {
            return "0." + stockCode;
        }
    }
}
