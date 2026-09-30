package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fc.v2.mapper.auto.TMgisDutyTaskMapper;
import com.fc.v2.mapper.custom.DutyTaskCycleMapper;
import com.fc.v2.model.auto.TMgisDutyTask;
import com.fc.v2.model.custom.DutyCycleSnapshot;
import com.fc.v2.service.support.DutyReminderSender;

/**
 * 周期执行整轮规矩：窗口、幂等、回写不成算未发、空归口只挂起本条不中断其余、本轮无事。
 * 不起 Spring：Mapper 与发送通道均为 Mock，RedisService 留空（退化为无锁无缓存，规矩不变）。
 */
class TMgisDutyTaskServiceImplTest {

    private TMgisDutyTaskServiceImpl service;

    @Mock
    private TMgisDutyTaskMapper mapper;
    @Mock
    private DutyTaskCycleMapper cycleMapper;
    @Mock
    private DutyReminderSender sender;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Date at(String s) {
        try {
            return SDF.parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TMgisDutyTask t(long id, String due, int lead, int status, String liaison) {
        TMgisDutyTask x = new TMgisDutyTask();
        x.setId(id);
        x.setItemNo("TX-" + id);
        x.setDueAt(at(due));
        x.setAmount(BigDecimal.valueOf(lead));
        x.setStatus(status);
        x.setDelFlag(0);
        x.setLiaison(liaison);
        x.setCityCode("C" + id);
        x.setCityName("临川市");
        return x;
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TMgisDutyTaskServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "mgisDutyTaskMapper", mapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "dutyTaskCycleMapper", cycleMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "reminderSender", sender);
        when(cycleMapper.markSent(any(), any())).thenReturn(1);
        when(cycleMapper.markSuspended(any(), any())).thenReturn(1);
    }

    private void stubRows(TMgisDutyTask... rows) {
        when(mapper.selectList(any())).thenReturn(new ArrayList<>(Arrays.asList(rows)));
    }

    @Test
    void inWindow_sendsDue_suspendsBlankLiaison_leavesFutureWaiting_andNeverRepicksSent() throws Exception {
        Date now = at("2026-09-30 10:00:00");
        TMgisDutyTask dueToday = t(1, "2026-09-30 23:59:59", 0, 0, "王联络");   // 压线当日
        TMgisDutyTask future = t(2, "2026-10-10 23:59:59", 3, 0, "王联络");    // 未到开口
        TMgisDutyTask blankLiaison = t(3, "2026-09-30 23:59:59", 3, 0, "  "); // 归口空着
        TMgisDutyTask already = t(4, "2026-09-28 10:00:00", 3, 1, "王联络");
        already.setSentAt(at("2026-09-28 10:00:00"));                          // 往日已发
        stubRows(dueToday, future, blankLiaison, already);

        DutyCycleSnapshot snap = service.runCycle(now);

        assertTrue(snap.isInWindow());
        assertFalse(snap.isIdle());
        assertEquals(1, snap.getSentThisRound());
        assertEquals(1, snap.getSuspendedThisRound());

        // 库层定向回写：一条已发（带发出时刻），一条挂起（写清缘故）；不碰 updateById 自动填充
        verify(cycleMapper, times(1)).markSent(eq(1L), eq(now));
        verify(cycleMapper, times(1)).markSuspended(eq(3L), contains("归口联络员空缺"));

        // 内存对象也已落成新状态
        assertEquals(1, dueToday.getStatus());
        assertEquals(now, dueToday.getSentAt());
        assertEquals(2, blankLiaison.getStatus());
        assertTrue(blankLiaison.getSuspendReason().contains("归口联络员空缺"));

        // 发出去的只可能是压线那一条；已发条任何轮次不再挑出
        verify(sender, times(1)).send(dueToday);
        verify(sender, never()).send(already);
        verify(sender, never()).send(future);

        // 柱状：两市桶（C3 与 C1/C4 不同 code），当日已发=1，待发只含未开口的 future=1，挂起累计=1
        assertEquals(1, snap.getTotalSent());
        assertEquals(1, snap.getTotalWaiting());
        assertEquals(1, snap.getTotalSuspended());
        // 往日已发条目只在台账留痕，标 ALREADY
        assertTrue(snap.getItems().stream().anyMatch(i -> "TX-4".equals(i.getItemNo()) && "ALREADY".equals(i.getAction())));
    }

    @Test
    void outsideWindow_nothingMoves_notAFailure() throws Exception {
        Date now = at("2026-09-30 18:00:00"); // 十七时半已过
        TMgisDutyTask due = t(1, "2026-09-30 23:59:59", 0, 0, "王联络");
        stubRows(due);

        DutyCycleSnapshot snap = service.runCycle(now);

        assertFalse(snap.isInWindow());
        assertFalse(snap.isIdle());            // 有条子到期，不算「无事」，是「留到下一轮」
        assertEquals(0, snap.getSentThisRound());
        verify(sender, never()).send(any());
        verify(cycleMapper, never()).markSent(anyLong(), any());
        verify(cycleMapper, never()).markSuspended(anyLong(), any());
        assertEquals("WAITING", snap.getItems().get(0).getAction());
    }

    @Test
    void nothingDue_reportsIdle_notError() throws Exception {
        Date now = at("2026-09-30 10:00:00");
        stubRows(t(1, "2026-12-31 23:59:59", 3, 0, "王联络"));

        DutyCycleSnapshot snap = service.runCycle(now);

        assertTrue(snap.isIdle());
        assertEquals(0, snap.getSentThisRound());
        verify(sender, never()).send(any());
    }

    @Test
    void oneFailingRow_doesNotStopTheRest() throws Exception {
        Date now = at("2026-09-30 10:00:00");
        TMgisDutyTask bad = t(1, "2026-09-30 23:59:59", 0, 0, "王联络");
        TMgisDutyTask good = t(2, "2026-09-30 23:59:59", 0, 0, "王联络");
        stubRows(bad, good);
        org.mockito.Mockito.doThrow(new RuntimeException("通道断了")).when(sender).send(bad);

        DutyCycleSnapshot snap = service.runCycle(now);

        // 坏条没回写、仍待发；好条照常发出
        verify(cycleMapper, times(1)).markSent(eq(2L), any());
        assertEquals(1, snap.getSentThisRound());
        assertEquals(0, bad.getStatus());
        assertNull(bad.getSentAt());
        assertEquals("FAIL", snap.getItems().stream()
                .filter(i -> "TX-1".equals(i.getItemNo())).findFirst().orElseThrow(AssertionError::new).getAction());
    }

    @Test
    void writebackFailure_countsAsNotSent() throws Exception {
        Date now = at("2026-09-30 10:00:00");
        TMgisDutyTask due = t(1, "2026-09-30 23:59:59", 0, 0, "王联络");
        stubRows(due);
        when(cycleMapper.markSent(any(), any())).thenReturn(0); // 回写不成

        DutyCycleSnapshot snap = service.runCycle(now);

        // 通道确实送过，但回写不成便算没发：不计本轮发出，下一轮仍按待提醒再发
        verify(sender, times(1)).send(due);
        assertEquals(0, snap.getSentThisRound());
        // 汇总也不得虚算一笔当日已发：仍是待发
        assertEquals(0, snap.getTotalSent());
        assertEquals(1, snap.getTotalWaiting());
        assertEquals("FAIL", snap.getItems().get(0).getAction());
    }
}
