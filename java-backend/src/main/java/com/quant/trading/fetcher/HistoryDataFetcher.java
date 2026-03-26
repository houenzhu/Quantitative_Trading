package com.quant.trading.fetcher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.quant.trading.entity.TickData;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
public class HistoryDataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(HistoryDataFetcher.class);

    private final OkHttpClient client;

    private static final String KLINE_URL = "https://push2his.eastmoney.com/api/qt/stock/kline/get";

    public HistoryDataFetcher() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public List<TickData> fetchHistoryKline(String stockCode, int days) {
        List<TickData> result = new ArrayList<>();

        try {
            String secId = getSecId(stockCode);
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days);

            String url = KLINE_URL + "?secid=" + secId +
                    "&fields1=f1,f2,f3,f4,f5,f6" +
                    "&fields2=f51,f52,f53,f54,f55,f56,f57" +
                    "&klt=101" +
                    "&fqt=1" +
                    "&beg=" + startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                    "&end=" + endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Referer", "https://quote.eastmoney.com/")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("获取历史K线失败: {}", response.code());
                    return result;
                }

                String body = response.body().string();
                JSONObject json = JSON.parseObject(body);

                if (json == null || json.getJSONObject("data") == null) {
                    return result;
                }

                JSONArray klines = json.getJSONObject("data").getJSONArray("klines");
                if (klines != null) {
                    for (int i = 0; i < klines.size(); i++) {
                        String kline = klines.getString(i);
                        String[] parts = kline.split(",");
                        if (parts.length >= 7) {
                            TickData tick = new TickData();
                            tick.setCode(stockCode);
                            tick.setOpen(new BigDecimal(parts[1]));
                            tick.setPrice(new BigDecimal(parts[2]));
                            tick.setHigh(new BigDecimal(parts[3]));
                            tick.setLow(new BigDecimal(parts[4]));
                            tick.setVolume(new BigDecimal(parts[5]));
                            tick.setAmount(new BigDecimal(parts[6]));

                            LocalDate date = LocalDate.parse(parts[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            tick.setTime(date.atStartOfDay());

                            result.add(tick);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("获取历史K线异常: {}", stockCode, e);
        }

        return result;
    }

    private String getSecId(String stockCode) {
        if (stockCode.startsWith("6")) {
            return "1." + stockCode;
        } else {
            return "0." + stockCode;
        }
    }
}
