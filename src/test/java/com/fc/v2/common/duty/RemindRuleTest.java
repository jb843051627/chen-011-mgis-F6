package com.fc.v2.common.duty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link RemindRule} 边界用例：压线管不管、窗口两端、旧条无钟点同尺。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class RemindRuleTest {

    private static final BigDecimal LEAD3 = new BigDecimal("3");

    /** 规则只认东八区，测试在任何机器时区下结果都得一致。 */
    @BeforeAll
    static void pinJvmZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    private static Date at(String ymdhm) {
        // 2026-10-05 09:00 形态，按东八区解释
        String[] p = ymdhm.split(" ");
        String[] d = p[0].split("-");
        String[] t = p[1].split(":");
        ZonedDateTime z = LocalDate.of(Integer.parseInt(d[0]), Integer.parseInt(d[1]), Integer.parseInt(d[2]))
                .atTime(Integer.parseInt(t[0]), Integer.parseInt(t[1]), 0)
                .atZone(RemindRule.ZONE);
        return Date.from(z.toInstant());
    }

    /** 系统落的新条：到期存的是日终 23:59:59。 */
    private static Date newEntryDeadline() {
        return Date.from(LocalDate.of(2026, 10, 5).atTime(23, 59, 59).atZone(RemindRule.ZONE).toInstant());
    }

    /** 纸清册搬进来的旧条：只填日子，库存即 00:00:00。 */
    private static Date legacyEntryDeadline() {
        return Date.from(LocalDate.of(2026, 10, 5).atTime(0, 0, 0).atZone(RemindRule.ZONE).toInstant());
    }

    @Test
    void deadlineNormalizesToDayEnd() {
        assertEquals(LocalDate.of(2026, 10, 5), RemindRule.dueDate(newEntryDeadline()));
        assertEquals(RemindRule.DAY_END, RemindRule.deadline(newEntryDeadline()).toLocalTime());
        // 旧条 00:00:00 归一后与新条同一刹
        assertEquals(RemindRule.deadline(newEntryDeadline()).toInstant(),
                RemindRule.deadline(legacyEntryDeadline()).toInstant());
    }

    @Test
    void remainingCountedBackFromDayEnd() {
        assertEquals(3L, RemindRule.remainingDays(newEntryDeadline(), at("2026-10-02 09:00")));
        assertEquals(0L, RemindRule.remainingDays(newEntryDeadline(), at("2026-10-05 09:00")));
        // 压线当日最后一刻仍是 0，不翻负
        assertEquals(0L, RemindRule.remainingDays(newEntryDeadline(), at("2026-10-05 23:00")));
        assertEquals(-1L, RemindRule.remainingDays(newEntryDeadline(), at("2026-10-06 00:30")));
        // 旧条与新条同一把尺
        assertEquals(0L, RemindRule.remainingDays(legacyEntryDeadline(), at("2026-10-05 09:00")));
    }

    @Test
    void dueOpensLeadDaysAndCountsDeadlineDay() {
        // 提前三天开口：10-02 该发
        assertTrue(RemindRule.isDue(newEntryDeadline(), LEAD3, at("2026-10-02 08:30")));
        // 再往前一天不动
        assertFalse(RemindRule.isDue(newEntryDeadline(), LEAD3, at("2026-10-01 17:30")));
        // 压线当日只有一种答案：算
        assertTrue(RemindRule.isDue(newEntryDeadline(), LEAD3, at("2026-10-05 08:30")));
        assertTrue(RemindRule.isDue(newEntryDeadline(), LEAD3, at("2026-10-05 23:59")));
        // 逾期未发仍在网里
        assertTrue(RemindRule.isDue(newEntryDeadline(), LEAD3, at("2026-10-08 10:00")));
        // 旧条同样在压线当日算该提醒
        assertTrue(RemindRule.isDue(legacyEntryDeadline(), LEAD3, at("2026-10-05 08:30")));
    }

    @Test
    void missingFieldsNeverDue() {
        assertFalse(RemindRule.isDue(null, LEAD3, at("2026-10-05 09:00")));
        assertFalse(RemindRule.isDue(newEntryDeadline(), null, at("2026-10-05 09:00")));
    }

    @Test
    void windowEdgesAreInclusive() {
        assertTrue(RemindRule.inWindow(at("2026-10-05 08:30")));
        assertTrue(RemindRule.inWindow(at("2026-10-05 17:30")));
        assertFalse(RemindRule.inWindow(at("2026-10-05 08:29")));
        assertFalse(RemindRule.inWindow(at("2026-10-05 17:31")));
        assertFalse(RemindRule.inWindow(at("2026-10-05 00:00")));
    }

    @Test
    void dueOutsideWindowIsHeldForNextRound() {
        // 已到开口日，但八时半之前：不挑
        assertFalse(RemindRule.shouldPick(newEntryDeadline(), LEAD3, at("2026-10-02 08:29")));
        // 十七时半之后也不挑，留到下一轮
        assertFalse(RemindRule.shouldPick(newEntryDeadline(), LEAD3, at("2026-10-02 17:31")));
        // 窗口内 + 到日子，才挑
        assertTrue(RemindRule.shouldPick(newEntryDeadline(), LEAD3, at("2026-10-02 08:30")));
        // 窗口内但日子没到，不挑
        assertFalse(RemindRule.shouldPick(newEntryDeadline(), LEAD3, at("2026-10-01 12:00")));
    }

    @Test
    void changingLeadDaysMovesTheOpeningOnly() {
        // 提前 0 天：只在压线当日及以后开口，截止时刻本身不参与挪动
        assertTrue(RemindRule.isDue(newEntryDeadline(), BigDecimal.ZERO, at("2026-10-05 09:00")));
        assertFalse(RemindRule.isDue(newEntryDeadline(), BigDecimal.ZERO, at("2026-10-04 23:59")));
    }
}
