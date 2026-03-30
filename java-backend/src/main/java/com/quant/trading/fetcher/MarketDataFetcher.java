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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class MarketDataFetcher {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataFetcher.class);
    
    private final OkHttpClient client;
    
    private static final String SINA_QUOTE_URL = "https://hq.sinajs.cn/list=";
    
    @Autowired
    private USStockDataFetcher usStockDataFetcher;
    
    public MarketDataFetcher() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }
    
    public TickData fetchTickData(String stockCode) {
        List<TickData> result = fetchBatchTickData(List.of(stockCode));
        return result.isEmpty() ? null : result.get(0);
    }
    
    public List<TickData> fetchBatchTickData(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> aStocks = stockCodes.stream()
                .filter(code -> !USStockDataFetcher.isUSStock(code))
                .collect(Collectors.toList());
        
        List<String> usStocks = stockCodes.stream()
                .filter(code -> USStockDataFetcher.isUSStock(code))
                .collect(Collectors.toList());
        
        List<TickData> result = new ArrayList<>();
        
        if (!aStocks.isEmpty()) {
            result.addAll(fetchAStocks(aStocks));
        }
        
        if (!usStocks.isEmpty()) {
            result.addAll(usStockDataFetcher.fetchBatchTickData(usStocks));
        }
        
        return result;
    }
    
    private List<TickData> fetchAStocks(List<String> stockCodes) {
        List<TickData> result = new ArrayList<>();
        
        try {
            StringBuilder symbols = new StringBuilder();
            for (String code : stockCodes) {
                if (symbols.length() > 0) {
                    symbols.append(",");
                }
                symbols.append(getSinaSymbol(code));
            }
            
            String url = SINA_QUOTE_URL + symbols.toString();
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Referer", "https://finance.sina.com.cn")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("获取行情失败: {}", response.code());
                    return result;
                }
                
                String body = response.body().string();
                result = parseSinaResponse(body, stockCodes);
                
                logger.info("成功获取 {} 只股票行情", result.size());
            }
        } catch (Exception e) {
            logger.error("批量获取行情异常", e);
        }
        
        return result;
    }
    
    private List<TickData> parseSinaResponse(String response, List<String> stockCodes) {
        List<TickData> result = new ArrayList<>();
        
        String[] lines = response.split("\n");
        
        for (int i = 0; i < lines.length && i < stockCodes.size(); i++) {
            String line = lines[i].trim();
            String stockCode = stockCodes.get(i);
            
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
                if (data.length < 33) {
                    continue;
                }
                
                TickData tick = new TickData();
                tick.setCode(stockCode);
                tick.setName(data[0]);
                tick.setOpen(parseBigDecimal(data[1]));
                tick.setPreClose(parseBigDecimal(data[2]));
                tick.setPrice(parseBigDecimal(data[3]));
                tick.setHigh(parseBigDecimal(data[4]));
                tick.setLow(parseBigDecimal(data[5]));
                tick.setVolume(parseBigDecimal(data[8]));
                tick.setAmount(parseBigDecimal(data[9]));
                tick.setTime(LocalDateTime.now());
                
                if (tick.getPreClose() != null && tick.getPreClose().compareTo(BigDecimal.ZERO) > 0 
                        && tick.getPrice() != null) {
                    BigDecimal change = tick.getPrice().subtract(tick.getPreClose());
                    tick.setChange(change);
                    BigDecimal changePct = change.divide(tick.getPreClose(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    tick.setChangePct(changePct);
                }
                
                result.add(tick);
            } catch (Exception e) {
                logger.warn("解析股票 {} 行情失败: {}", stockCode, e.getMessage());
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
    
    private String getSinaSymbol(String stockCode) {
        if (stockCode.startsWith("6")) {
            return "sh" + stockCode;
        } else {
            return "sz" + stockCode;
        }
    }
    
    public List<Map<String, String>> searchStocks(String keyword) {
        List<Map<String, String>> results = new ArrayList<>();
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }
        
        if (USStockDataFetcher.isUSStock(keyword.toUpperCase())) {
            return searchUSStocks(keyword.toUpperCase());
        }
        
        try {
            String url = "https://suggest3.sinajs.cn/suggest/type=11,13,15,17,21,23,24,25,26,27,33,39&key=" + keyword;
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Referer", "https://finance.sina.com.cn")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("搜索股票失败: {}", response.code());
                    return results;
                }
                
                String body = response.body().string();
                
                if (body == null || body.isEmpty() || !body.contains("suggestvalue")) {
                    return results;
                }
                
                int startIdx = body.indexOf("var suggestvalue=\"");
                if (startIdx == -1) {
                    return results;
                }
                
                startIdx += "var suggestvalue=\"".length();
                int endIdx = body.indexOf("\";", startIdx);
                if (endIdx == -1) {
                    return results;
                }
                
                String content = body.substring(startIdx, endIdx);
                String[] lines = content.split(";");
                
                for (String line : lines) {
                    if (line == null || line.trim().isEmpty()) {
                        continue;
                    }
                    
                    String[] parts = line.split(",");
                    if (parts.length < 5) {
                        continue;
                    }
                    
                    String code = parts[2].trim();
                    String name = parts[4].trim();
                    
                    if (code != null && !code.isEmpty() && name != null && !name.isEmpty()) {
                        if (code.startsWith("6") || code.startsWith("0") || code.startsWith("3") || code.startsWith("68")) {
                            Map<String, String> item = new HashMap<>();
                            item.put("code", code);
                            item.put("name", name);
                            item.put("display", code + " - " + name);
                            results.add(item);
                            
                            if (results.size() >= 10) {
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("搜索股票异常", e);
        }
        
        return results;
    }
    
    private List<Map<String, String>> searchUSStocks(String keyword) {
        List<Map<String, String>> results = new ArrayList<>();
        
        try {
            String url = "https://suggest3.sinajs.cn/suggest/type=11,13,15,17,21,23,24,25,26,27,33,39,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58&key=" + keyword;
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Referer", "https://finance.sina.com.cn")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("搜索美股失败: {} - {}", response.code(), response.message());
                    return results;
                }
                
                String body = response.body().string();
                
                if (body == null || body.isEmpty() || !body.contains("suggestvalue")) {
                    return results;
                }
                
                int startIdx = body.indexOf("var suggestvalue=\"");
                if (startIdx == -1) {
                    return results;
                }
                
                startIdx += "var suggestvalue=\"".length();
                int endIdx = body.indexOf("\";", startIdx);
                if (endIdx == -1) {
                    return results;
                }
                
                String content = body.substring(startIdx, endIdx);
                String[] lines = content.split(";");
                
                for (String line : lines) {
                    if (line == null || line.trim().isEmpty()) {
                        continue;
                    }
                    
                    String[] parts = line.split(",");
                    if (parts.length < 5) {
                        continue;
                    }
                    
                    String code = parts[2].trim();
                    String name = parts[4].trim();
                    
                    if (code != null && !code.isEmpty() && name != null && !name.isEmpty()) {
                        if (code.startsWith("gb_") || USStockDataFetcher.isUSStock(code)) {
                            String displayCode = code.startsWith("gb_") ? code.substring(3).toUpperCase() : code.toUpperCase();
                            Map<String, String> item = new HashMap<>();
                            item.put("code", displayCode);
                            item.put("name", name);
                            item.put("display", displayCode + " - " + name + " (美股)");
                            results.add(item);
                            
                            if (results.size() >= 10) {
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("搜索美股异常", e);
        }
        
        return results;
    }
}
