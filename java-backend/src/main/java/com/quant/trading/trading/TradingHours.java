package com.quant.trading.trading;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Component
public class TradingHours {
    
    private static final LocalTime MORNING_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MORNING_CLOSE = LocalTime.of(11, 30);
    private static final LocalTime AFTERNOON_OPEN = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_CLOSE = LocalTime.of(15, 0);
    
    private final Set<LocalDate> holidays = new HashSet<>();
    
    public TradingHours() {
        initHolidays(2026);
    }
    
    private void initHolidays(int year) {
        holidays.add(LocalDate.of(year, 1, 1));
        holidays.add(LocalDate.of(year, 1, 28));
        holidays.add(LocalDate.of(year, 1, 29));
        holidays.add(LocalDate.of(year, 1, 30));
        holidays.add(LocalDate.of(year, 1, 31));
        holidays.add(LocalDate.of(year, 2, 1));
        holidays.add(LocalDate.of(year, 2, 2));
        holidays.add(LocalDate.of(year, 2, 3));
        holidays.add(LocalDate.of(year, 2, 4));
        holidays.add(LocalDate.of(year, 4, 4));
        holidays.add(LocalDate.of(year, 4, 5));
        holidays.add(LocalDate.of(year, 4, 6));
        holidays.add(LocalDate.of(year, 5, 1));
        holidays.add(LocalDate.of(year, 5, 2));
        holidays.add(LocalDate.of(year, 5, 3));
        holidays.add(LocalDate.of(year, 5, 4));
        holidays.add(LocalDate.of(year, 5, 5));
        holidays.add(LocalDate.of(year, 6, 1));
        holidays.add(LocalDate.of(year, 10, 1));
        holidays.add(LocalDate.of(year, 10, 2));
        holidays.add(LocalDate.of(year, 10, 3));
        holidays.add(LocalDate.of(year, 10, 4));
        holidays.add(LocalDate.of(year, 10, 5));
        holidays.add(LocalDate.of(year, 10, 6));
        holidays.add(LocalDate.of(year, 10, 7));
        holidays.add(LocalDate.of(year, 10, 8));
    }
    
    public boolean isTradingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        return !holidays.contains(date);
    }
    
    public boolean isTradingTime(LocalDateTime dateTime) {
        if (!isTradingDay(dateTime.toLocalDate())) {
            return false;
        }
        
        LocalTime time = dateTime.toLocalTime();
        
        boolean morningSession = !time.isBefore(MORNING_OPEN) && time.isBefore(MORNING_CLOSE);
        boolean afternoonSession = !time.isBefore(AFTERNOON_OPEN) && time.isBefore(AFTERNOON_CLOSE);
        
        return morningSession || afternoonSession;
    }
    
    public boolean isMarketOpen(LocalDateTime dateTime) {
        return isTradingTime(dateTime);
    }
    
    public LocalDateTime getNextTradingTime(LocalDateTime from) {
        LocalDateTime next = from.plusMinutes(1);
        
        while (!isTradingTime(next)) {
            next = next.plusMinutes(1);
            if (next.getHour() >= 16) {
                next = next.plusDays(1).withHour(9).withMinute(30).withSecond(0);
            }
        }
        
        return next;
    }
}
