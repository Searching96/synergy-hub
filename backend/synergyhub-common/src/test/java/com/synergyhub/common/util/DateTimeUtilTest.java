package com.synergyhub.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilTest {

    @Test
    void now_ShouldReturnCurrentTime() {
        LocalDateTime now = DateTimeUtil.now();
        assertNotNull(now);
        // Verify it's close to current time (within 1 second)
        LocalDateTime expected = LocalDateTime.now();
        assertTrue(now.isAfter(expected.minusSeconds(1)));
        assertTrue(now.isBefore(expected.plusSeconds(1)));
    }

    @Test
    void format_ShouldFormatDateTimeCorrectly() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 10, 24, 14, 24, 1);
        String formatted = DateTimeUtil.format(dateTime);
        assertEquals("2025-10-24 14:24:01", formatted);
    }

    @Test
    void format_WithNullDateTime_ShouldReturnNull() {
        LocalDateTime nullDateTime = null;
        String result = DateTimeUtil.format(nullDateTime);
        assertNull(result);
    }

    @Test
    void format_WithCustomPattern_ShouldFormatCorrectly() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 10, 24, 14, 24, 1);
        String formatted = DateTimeUtil.format(dateTime, "yyyy/MM/dd");
        assertEquals("2025/10/24", formatted);

        formatted = DateTimeUtil.format(dateTime, "dd-MM-yyyy HH:mm");
        assertEquals("24-10-2025 14:24", formatted);
    }

    @Test
    void parse_ShouldParseStringToDateTime() {
        String dateTimeStr = "2025-10-24 14:24:01";
        LocalDateTime parsed = DateTimeUtil.parse(dateTimeStr);
        assertNotNull(parsed);
        assertEquals(2025, parsed.getYear());
        assertEquals(10, parsed.getMonthValue());
        assertEquals(24, parsed.getDayOfMonth());
        assertEquals(14, parsed.getHour());
        assertEquals(24, parsed.getMinute());
        assertEquals(1, parsed.getSecond());
    }

    @Test
    void parse_WithNullString_ShouldReturnNull() {
        String nullString = null;
        LocalDateTime result = DateTimeUtil.parse(nullString);
        assertNull(result);
    }

    @Test
    void parse_WithEmptyString_ShouldReturnNull() {
        assertNull(DateTimeUtil.parse(""));
        assertNull(DateTimeUtil.parse("   "));
    }

    @Test
    void isPast_WithPastDate_ShouldReturnTrue() {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        assertTrue(DateTimeUtil.isPast(pastDate));

        LocalDateTime pastHour = LocalDateTime.now().minusHours(1);
        assertTrue(DateTimeUtil.isPast(pastHour));

        LocalDateTime pastSecond = LocalDateTime.now().minusSeconds(5);
        assertTrue(DateTimeUtil.isPast(pastSecond));
    }

    @Test
    void isPast_WithFutureDate_ShouldReturnFalse() {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        assertFalse(DateTimeUtil.isPast(futureDate));

        LocalDateTime futureHour = LocalDateTime.now().plusHours(1);
        assertFalse(DateTimeUtil.isPast(futureHour));
    }

    @Test
    void isPast_WithNull_ShouldReturnFalse() {
        LocalDateTime nullDateTime = null;
        assertFalse(DateTimeUtil.isPast(nullDateTime));
    }

    @Test
    void isFuture_WithFutureDate_ShouldReturnTrue() {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        assertTrue(DateTimeUtil.isFuture(futureDate));

        LocalDateTime futureMinute = LocalDateTime.now().plusMinutes(1);
        assertTrue(DateTimeUtil.isFuture(futureMinute));

        LocalDateTime futureSecond = LocalDateTime.now().plusSeconds(5);
        assertTrue(DateTimeUtil.isFuture(futureSecond));
    }

    @Test
    void isFuture_WithPastDate_ShouldReturnFalse() {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        assertFalse(DateTimeUtil.isFuture(pastDate));

        LocalDateTime pastMinute = LocalDateTime.now().minusMinutes(1);
        assertFalse(DateTimeUtil.isFuture(pastMinute));
    }

    @Test
    void isFuture_WithNull_ShouldReturnFalse() {
        LocalDateTime nullDateTime = null;
        assertFalse(DateTimeUtil.isFuture(nullDateTime));
    }

    @Test
    void nowPlusMinutes_ShouldAddMinutesCorrectly() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime result = DateTimeUtil.nowPlusMinutes(30);
        LocalDateTime after = LocalDateTime.now().plusMinutes(30);

        assertNotNull(result);
        assertTrue(result.isAfter(before) || result.isEqual(before));
        assertTrue(result.isBefore(after.plusSeconds(1)) || result.isEqual(after));
    }

    @Test
    void nowPlusHours_ShouldAddHoursCorrectly() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime result = DateTimeUtil.nowPlusHours(2);
        LocalDateTime after = LocalDateTime.now().plusHours(2);

        assertNotNull(result);
        assertTrue(result.isAfter(before) || result.isEqual(before));
        assertTrue(result.isBefore(after.plusSeconds(1)) || result.isEqual(after));
    }

    @Test
    void nowPlusDays_ShouldAddDaysCorrectly() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime result = DateTimeUtil.nowPlusDays(7);
        LocalDateTime after = LocalDateTime.now().plusDays(7);

        assertNotNull(result);
        assertTrue(result.isAfter(before) || result.isEqual(before));
        assertTrue(result.isBefore(after.plusSeconds(1)) || result.isEqual(after));
    }

    @Test
    void nowPlusMinutes_WithNegativeValue_ShouldSubtract() {
        LocalDateTime result = DateTimeUtil.nowPlusMinutes(-30);
        assertNotNull(result);
        assertTrue(result.isBefore(LocalDateTime.now()));
    }
}