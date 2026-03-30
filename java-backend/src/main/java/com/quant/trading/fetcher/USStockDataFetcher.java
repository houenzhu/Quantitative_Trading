package com.quant.trading.fetcher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.quant.trading.entity.TickData;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 美股行情数据获取器
 * 使用新浪财经API获取美股实时行情
 */
@Component
public class USStockDataFetcher {
    
    private static final Logger logger = LoggerFactory.getLogger(USStockDataFetcher.class);
    
    private final OkHttpClient client;
    
    private static final String SINA_US_QUOTE_URL = "https://hq.sinajs.cn/list=gb_";
    
    public USStockDataFetcher() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 获取单只美股行情
     * @param symbol 股票代码（如：AAPL, TSLA）
     * @return 行情数据
     */
    public TickData fetchTickData(String symbol) {
        List<TickData> result = fetchBatchTickData(List.of(symbol));
        return result.isEmpty() ? null : result.get(0);
    }
    
    /**
     * 批量获取美股行情
     * @param symbols 股票代码列表
     * @return 行情数据列表
     */
    public List<TickData> fetchBatchTickData(List<String> symbols) {
        List<TickData> result = new ArrayList<>();
        
        if (symbols == null || symbols.isEmpty()) {
            return result;
        }
        
        try {
            StringBuilder symbolList = new StringBuilder();
            for (String symbol : symbols) {
                if (symbolList.length() > 0) {
                    symbolList.append(",");
                }
                symbolList.append("gb_").append(symbol.toLowerCase());
            }
            
            String url = SINA_US_QUOTE_URL + symbolList.toString();
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Referer", "https://finance.sina.com.cn/stock/usstock/")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Accept", "*/*")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .addHeader("Accept-Encoding", "gzip, deflate, br")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Host", "hq.sinajs.cn")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("获取美股行情失败: {}", response.code());
                    return result;
                }
                
                String body = response.body().string();
                result = parseSinaUSResponse(body, symbols);
                
                logger.info("成功获取 {} 只美股行情", result.size());
            }
        } catch (Exception e) {
            logger.error("批量获取美股行情异常", e);
        }
        
        return result;
    }
    
    /**
     * 解析新浪美股API响应
     * @param response API响应
     * @param symbols 股票代码列表
     * @return 行情数据列表
     */
    private List<TickData> parseSinaUSResponse(String response, List<String> symbols) {
        List<TickData> result = new ArrayList<>();
        
        String[] lines = response.split("\n");
        
        for (int i = 0; i < lines.length && i < symbols.size(); i++) {
            String line = lines[i].trim();
            String symbol = symbols.get(i);
            
            if (line.isEmpty() || !line.contains("=")) {
                continue;
            }
            
            try {
                int start = line.indexOf("\"") + 1;
                int end = line.lastIndexOf("\"");
                if (start >= end) {
                    continue;
                }
                
                String dataStr = line.substring(start, end);
                if (dataStr.isEmpty()) {
                    continue;
                }
                
                String[] data = dataStr.split(",");
                if (data.length < 6) {
                    continue;
                }
                
                TickData tick = new TickData();
                tick.setCode(symbol);
                tick.setName(data[0]);
                tick.setPrice(parseBigDecimal(data[1]));
                tick.setChange(parseBigDecimal(data[2]));
                tick.setChangePct(parseBigDecimal(data[3]));
                tick.setOpen(parseBigDecimal(data[4]));
                tick.setHigh(parseBigDecimal(data[5]));
                tick.setLow(parseBigDecimal(data[6]));
                tick.setVolume(parseBigDecimal(data[7]));
                tick.setPreClose(parseBigDecimal(data[8]));
                tick.setAmount(parseBigDecimal(data[9]));
                tick.setTime(LocalDateTime.now());
                
                result.add(tick);
            } catch (Exception e) {
                logger.warn("解析美股 {} 行情失败: {}", symbol, e.getMessage());
            }
        }
        
        return result;
    }
    
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty() || "--".equals(value) || "NaN".equalsIgnoreCase(value)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 判断是否为美股代码
     * @param code 股票代码
     * @return true表示美股
     */
    public static boolean isUSStock(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        return code.matches("^[A-Za-z]{1,5}$");
    }
    
    /**
     * 判断当前是否为美股交易时间
     * @return true表示在交易时间
     */
    public static boolean isUSTradingTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        int hour = now.getHour();
        int minute = now.getMinute();
        
        int currentTime = hour * 60 + minute;
        int marketOpen = 9 * 60 + 30;
        int marketClose = 16 * 60;
        
        return currentTime >= marketOpen && currentTime < marketClose;
    }
    
    /**
     * 获取美股交易状态
     * @return 交易状态描述
     */
    public static String getUSTradingStatus() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        int hour = now.getHour();
        int minute = now.getMinute();
        int currentTime = hour * 60 + minute;
        
        int preMarket = 4 * 60;
        int marketOpen = 9 * 60 + 30;
        int marketClose = 16 * 60;
        int afterMarket = 20 * 60;
        
        if (currentTime < preMarket) {
            return "休市";
        } else if (currentTime < marketOpen) {
            return "盘前交易";
        } else if (currentTime < marketClose) {
            return "交易中";
        } else if (currentTime < afterMarket) {
            return "盘后交易";
        } else {
            return "休市";
        }
    }
}
