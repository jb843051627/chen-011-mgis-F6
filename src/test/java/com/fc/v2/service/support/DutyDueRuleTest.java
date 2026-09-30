package com.fc.v2.service.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TMgisDutyTask;

/**
 * 全处唯一一把尺的边界：日终语义、压线当日、历史无钟点条目同尺、发送窗口两头。
 */
class DutyDueRuleTest {

    private static Date at(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TMgisDutyTask task(String dueAt, int leadDays, Integer status, Integer delFlag) {
        TMgisDutyTask t = new TMgisDutyTask();
        t.setItemNo("X");
        t.setDueAt(at(dueAt));
        t.setAmount(BigDecimal.valueOf(leadDays));
        t.setStatus(status);
        t.setDelFlag(delFlag);
        t.setLiaison("联络员");
        return t;
    }

    @Test
    void daysLeft_countsFromEndOfDueDay_invariantWithinDay() {
        // 到期当日：凌晨问、临到日终问，答案都只有一个——0
        assertEquals(0, DutyDueRule.daysLeft(at("2026-09-30 23:59:59"), at("2026-09-30 00:00:00")));
        assertEquals(0, DutyDueRule.daysLeft(at("2026-09-30 23:59:59"), at("2026-09-30 08:30:00")));
        assertEquals(0, DutyDueRule.daysLeft(at("2026-09-30 23:59:59"), at("2026-09-30 17:30:00")));
        // 往回数：明日 1，昨日 -1（过期仍在开口内）
        assertEquals(1, DutyDueRule.daysLeft(at("2026-10-01 23:59:59"), at("2026-09-30 12:00:00")));
        assertEquals(-1, DutyDueRule.daysLeft(at("2026-09-29 23:59:59"), at("2026-09-30 12:00:00")));
    }

    @Test
    void deadlineLine_dayOf_dueTodayIsDue_whateverTheStoredClock() {
        // 压线当日算不算该提醒，只许有一种答案：算
        assertTrue(DutyDueRule.shouldRemind(task("2026-09-30 23:59:59", 0, 0, 0), at("2026-09-30 08:30:00")));
        // 纸质清册老条目只填了日子（落成 00:00:00）跟系统新条目同一把尺，也算压线
        assertTrue(DutyDueRule.shouldRemind(task("2026-09-30 00:00:00", 0, 0, 0), at("2026-09-30 08:30:00")));
        // 明天才到期、提前 0 日：未到
        assertFalse(DutyDueRule.shouldRemind(task("2026-10-01 23:59:59", 0, 0, 0), at("2026-09-30 17:30:00")));
        // 提前 3 日：今天 9/30，10/3 到期即开口
        assertTrue(DutyDueRule.shouldRemind(task("2026-10-03 23:59:59", 3, 0, 0), at("2026-09-30 08:30:00")));
        assertFalse(DutyDueRule.shouldRemind(task("2026-10-04 23:59:59", 3, 0, 0), at("2026-09-30 08:30:00")));
        // 已过期：照样在开口内，必须补
        assertTrue(DutyDueRule.shouldRemind(task("2026-09-20 23:59:59", 3, 0, 0), at("2026-09-30 08:30:00")));
    }

    @Test
    void onlyWaitingLiveRowsArePicked() {
        // 已发、已挂起任何轮次不再挑出
        assertFalse(DutyDueRule.shouldRemind(task("2026-09-20 23:59:59", 3, 1, 0), at("2026-09-30 08:30:00")));
        assertFalse(DutyDueRule.shouldRemind(task("2026-09-20 23:59:59", 3, 2, 0), at("2026-09-30 08:30:00")));
        // 删了的不挑
        assertFalse(DutyDueRule.shouldRemind(task("2026-09-20 23:59:59", 3, 0, 1), at("2026-09-30 08:30:00")));
    }

    @Test
    void sendWindow_halfPastEightToHalfPastFive_inclusive() {
        assertFalse(DutyDueRule.inSendWindow(at("2026-09-30 08:29:59")));
        assertTrue(DutyDueRule.inSendWindow(at("2026-09-30 08:30:00")));
        assertTrue(DutyDueRule.inSendWindow(at("2026-09-30 12:00:00")));
        assertTrue(DutyDueRule.inSendWindow(at("2026-09-30 17:30:00")));
        assertFalse(DutyDueRule.inSendWindow(at("2026-09-30 17:30:01")));
        assertFalse(DutyDueRule.inSendWindow(at("2026-09-30 23:59:59")));
    }
}
