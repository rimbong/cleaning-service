package com.boot.cleanhub.util.date;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DateUtil {

    // 자주 사용되는 날짜 포맷 정의 (필요에 따라 포맷터 추가 가능)
    private static final String YYYY_MM_DD = "yyyy-MM-dd";
    private static final String YYYYMMDD = "yyyyMMdd";
    private static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
    private static final String YYYYMMDD_HHMMSS = "yyyyMMdd HHmmss";
    private static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    private static final String YYYY_MM_DD_HH_MM_DOT = "yyyy.MM.dd HH:mm";
    private static final String HH_MM = "HH:mm";

    // 자주 사용되는 DateTimeFormatter 객체 정의 (성능 향상)
    public static final DateTimeFormatter YYYY_MM_DD_FORMATTER = DateTimeFormatter.ofPattern(YYYY_MM_DD);
    public static final DateTimeFormatter YYYYMMDD_FORMATTER = DateTimeFormatter.ofPattern(YYYYMMDD);
    public static final DateTimeFormatter YYYYMMDDHHMMSS_FORMATTER = DateTimeFormatter.ofPattern(YYYYMMDDHHMMSS);
    public static final DateTimeFormatter YYYYMMDD_HHMMSS_FORMATTER = DateTimeFormatter.ofPattern(YYYYMMDD_HHMMSS);
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_FORMATTER = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_DOT_FORMATTER = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_DOT);
    public static final DateTimeFormatter HH_MM_FORMATTER = DateTimeFormatter.ofPattern(HH_MM);
    /**
     * private 생성자로 인스턴스화 방지
     */
    private DateUtil() {}

    /**
     * <pre>
     *   format : LocalDateTime 객체를 지정된 포맷의 문자열로 변환합니다. 
     * </pre>
     * @author In-seong Hwang
     * @since 2025.09.12
     * @version 1.0
     */
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime != null ? dateTime.format(formatter) : null;
    }
    public static String format(LocalDate date, DateTimeFormatter formatter) {
        return date != null ? date.format(formatter) : null;
    }
    public static String format(LocalTime time, DateTimeFormatter formatter) {
        return time != null ? time.format(formatter) : null;
    }
    // 예외 없는 Safe 버전
    public static String formatSafe(LocalDateTime dateTime, DateTimeFormatter formatter) {
        try {
            return format(dateTime, formatter);
        } catch (Exception e) {
            return null;
        }
    }
    public static String formatSafe(LocalDate date, DateTimeFormatter formatter) {
        try {
            return format(date, formatter);
        } catch (Exception e) {
            return null;
        }
    }
    public static String formatSafe(LocalTime time, DateTimeFormatter formatter) {
        try {
            return format(time, formatter);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * <pre>
     *   지정된 포맷의 문자열을 LocalDateTime 객체로 변환합니다. 
     * </pre>
     * @author In-seong Hwang
     * @since 2025.09.12
     * @version 1.0
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, DateTimeFormatter formatter) {
        if (dateTimeStr == null || formatter == null) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, formatter);
    }
    public static LocalDateTime parseDateTimeSafe(String dateTimeStr, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            return null;
        }
    }
    public static LocalDate parseDate(String dateStr, DateTimeFormatter formatter) {
        if (dateStr == null || formatter == null) {
            return null;
        }
        return LocalDate.parse(dateStr, formatter);
    }
    public static LocalDate parseDateSafe(String dateStr, DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            return null;
        }
    }
    public static LocalTime parseTime(String timeStr, DateTimeFormatter formatter) {
        if (timeStr == null || formatter == null) {
            return null;
        }
        return LocalTime.parse(timeStr, formatter);
    }
    public static LocalTime parseTimeSafe(String timeStr, DateTimeFormatter formatter) {
        try {
            return LocalTime.parse(timeStr, formatter);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * <pre>
     *   DateUtil Helper 메서드 
     * </pre>
     * @author In-seong Hwang
     * @since 2025.09.12
     * @version 1.0
     */
    public static int getYear(LocalDate date) {
        return date != null ? date.getYear() : -1;
    }
    public static int getMonthValue(LocalDate date) {
        return date != null ? date.getMonthValue() : -1;
    }
    public static int getDayOfMonth(LocalDate date) {
        return date != null ? date.getDayOfMonth() : -1;
    }
    public static int getHour(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.getHour() : -1;
    }
    public static int getMinute(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.getMinute() : -1;
    }
    public static int getSecond(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.getSecond() : -1;
    }
    public static LocalDate getStartOfWeek(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : null;
    }
    public static LocalDate getEndOfWeek(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)) : null;
    }
    public static LocalDate getStartOfWeekSunday(LocalDate date) {
        // SUNDAY를 기준으로 이전이거나 같은 SUNDAY를 찾음
        return date != null ? date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) : null;
    }
    public static LocalDate getEndOfWeekSaturday(LocalDate date) {
        // SATURDAY를 기준으로 다음이거나 같은 SATURDAY를 찾음
        return date != null ? date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)) : null;
    }
    public static LocalDate getStartOfMonth(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.firstDayOfMonth()) : null;
    }
    public static LocalDate getEndOfMonth(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.lastDayOfMonth()) : null;
    }
     
    // 날짜의 요일을 한국어 약어(월, 화, ...)로 반환합니다.
    public static String getDayOfWeekKorean(LocalDate date) {
        if (date == null) return null;
        switch (date.getDayOfWeek()) {
            case MONDAY:    return "월";
            case TUESDAY:   return "화";
            case WEDNESDAY: return "수";
            case THURSDAY:  return "목";
            case FRIDAY:    return "금";
            case SATURDAY:  return "토";
            case SUNDAY:    return "일";
            default:        return null;
        }
    }
    public static String getDayOfWeekKorean(LocalDateTime dateTime) {
        return dateTime != null ? getDayOfWeekKorean(dateTime.toLocalDate()) : null;
    }
    
    // 시간의 오전/오후를 한국어(오전, 오후)로 반환합니다.
    public static String getAmPmKorean(LocalTime time) {
        if (time == null) return null;
        return time.getHour() < 12 ? "오전" : "오후";
    }
    public static String getAmPmKorean(LocalDateTime dateTime) {
        return dateTime != null ? getAmPmKorean(dateTime.toLocalTime()) : null;
    }
    
    /**
     * 특정 년/월의 달력에 표시될 날짜(LocalDate) 리스트를 생성합니다.
     * 달력은 해당 월의 첫째 날이 포함된 주(일요일 시작)부터
     * 마지막 날이 포함된 주(토요일 종료)까지의 모든 날짜를 포함합니다.
     * (보통 5주 또는 6주 분량의 날짜가 반환됩니다)
     *
     * @param year  년도
     * @param month 월 (1-12)
     * @return 달력에 표시될 날짜가 담긴 리스트
     */
    public static List<LocalDate> getCalendarMonthDays(int year, int month) {
        if (month < 1 || month > 12) {
            return Collections.emptyList();
        }
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        // 달력의 시작일: 해당 월의 첫날이 속한 주의 일요일
        LocalDate firstDayOfCalendar = getStartOfWeekSunday(firstDayOfMonth);
        // 달력의 종료일: 해당 월의 마지막 날이 속한 주의 토요일
        LocalDate lastDayOfCalendar = getEndOfWeekSaturday(lastDayOfMonth);
        return getDateRange(firstDayOfCalendar, lastDayOfCalendar);
    }
    
    /**
     * 시작일부터 종료일까지의 모든 날짜(LocalDate) 리스트를 반환합니다.
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 날짜 리스트, endDate가 startDate보다 이전이면 빈 리스트 반환
     */
    public static List<LocalDate> getDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return Collections.emptyList();
        }
        List<LocalDate> list = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            list.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        return list;
    }
    
    // --- Calculation ---
    public static LocalDateTime plusYears(LocalDateTime dt, long years) { return dt != null ? dt.plusYears(years) : null; }
    public static LocalDateTime plusMonths(LocalDateTime dt, long months) { return dt != null ? dt.plusMonths(months) : null; }
    public static LocalDateTime plusDays(LocalDateTime dt, long days) { return dt != null ? dt.plusDays(days) : null; }
    public static LocalDateTime plusHours(LocalDateTime dt, long hours) { return dt != null ? dt.plusHours(hours) : null; }
    public static LocalDateTime plusMinutes(LocalDateTime dt, long minutes) { return dt != null ? dt.plusMinutes(minutes) : null; }
    public static LocalDateTime plusSeconds(LocalDateTime dt, long seconds) { return dt != null ? dt.plusSeconds(seconds) : null; }

    public static LocalDateTime minusYears(LocalDateTime dt, long years) { return dt != null ? dt.minusYears(years) : null; }
    public static LocalDateTime minusMonths(LocalDateTime dt, long months) { return dt != null ? dt.minusMonths(months) : null; }
    public static LocalDateTime minusDays(LocalDateTime dt, long days) { return dt != null ? dt.minusDays(days) : null; }
    public static LocalDateTime minusHours(LocalDateTime dt, long hours) { return dt != null ? dt.minusHours(hours) : null; }
    public static LocalDateTime minusMinutes(LocalDateTime dt, long minutes) { return dt != null ? dt.minusMinutes(minutes) : null; }
    public static LocalDateTime minusSeconds(LocalDateTime dt, long seconds) { return dt != null ? dt.minusSeconds(seconds) : null; }
    
    // --- Comparison ---
    /**
     * 두 날짜 범위가 서로 겹치는지 확인합니다. (경계 포함)
     * @param start1 첫 번째 기간의 시작일
     * @param end1   첫 번째 기간의 종료일
     * @param start2 두 번째 기간의 시작일
     * @param end2   두 번째 기간의 종료일
     * @return 중첩되면 true
     */
    public static boolean isOverlapping(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) return false;
        // 첫 번째 기간의 시작이 두 번째 기간의 종료보다 뒤에 있거나,
        // 첫 번째 기간의 종료가 두 번째 기간의 시작보다 앞에 있으면 중첩되지 않음.
        // 그 외의 경우는 모두 중첩됨.
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }
    public static boolean isOverlapping(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) return false;
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }
    public static boolean isBefore(LocalDateTime d1, LocalDateTime d2) { return (d1 != null && d2 != null) && d1.isBefore(d2); }
    public static boolean isAfter(LocalDateTime d1, LocalDateTime d2) { return (d1 != null && d2 != null) && d1.isAfter(d2); }
    public static boolean isEqual(LocalDateTime d1, LocalDateTime d2) { return (d1 != null && d2 != null) && d1.isEqual(d2); }
    public static boolean isBetween(LocalDateTime target, LocalDateTime start, LocalDateTime end, boolean inclusive) {
        if (target == null || start == null || end == null) return false;
        return inclusive ? !target.isBefore(start) && !target.isAfter(end) : target.isAfter(start) && target.isBefore(end);
    }
    
    // --- Difference ---
    public static long yearsBetween(LocalDate start, LocalDate end) { return (start != null && end != null) ? ChronoUnit.YEARS.between(start, end) : 0; }
    public static long monthsBetween(LocalDate start, LocalDate end) { return (start != null && end != null) ? ChronoUnit.MONTHS.between(start, end) : 0; }
    public static long daysBetween(LocalDate start, LocalDate end) { return (start != null && end != null) ? ChronoUnit.DAYS.between(start, end) : 0; }
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) { return (start != null && end != null) ? ChronoUnit.HOURS.between(start, end) : 0; }
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) { return (start != null && end != null) ? ChronoUnit.MINUTES.between(start, end) : 0; }
    public static long secondsBetween(LocalDateTime start, LocalDateTime end) { return (start != null && end != null) ? ChronoUnit.SECONDS.between(start, end) : 0; }
}