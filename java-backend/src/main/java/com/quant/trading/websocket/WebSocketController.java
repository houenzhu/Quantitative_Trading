package com.quant.trading.websocket;

import com.quant.trading.service.StockPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class WebSocketController {
    
    @Autowired
    private StockPoolService stockPoolService;
    
    @Autowired
    private MarketDataBroadcaster broadcaster;
    
    @MessageMapping("/subscribe/{stockCode}")
    @SendTo("/topic/tick/{stockCode}")
    public String subscribeStock(@DestinationVariable String stockCode) {
        return "Subscribed to " + stockCode;
    }
}
