package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.common.duty.RemindSender;
import com.fc.v2.mapper.auto.TMgisDutyRoundMapper;
import com.fc.v2.mapper.auto.TMgisDutyTaskMapper;
import com.fc.v2.mapper.auto.TSysCityMapper;
import com.fc.v2.model.auto.TMgisDutyRound;
import com.fc.v2.model.auto.TMgisDutyTask;
import com.fc.v2.model.custom.duty.DutyBoard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 周期执行服务层分支：归口空缺挂起、窗口外留轮、发送/回写失败留轮、无事落轮、不中断整轮。
 *
 * @author fuce
 * @date 2026-09-30
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TMgisDutyTaskServiceImplTest {

    @Mock
    private TMgisDutyTaskMapper taskMapper;
    @Mock
    private TMgisDutyRoundMapper roundMapper;
    @Mock
    private TSysCityMapper cityMapper;
    @Mock
    private RemindSender sender;

    @InjectMocks
    private TMgisDutyTaskServiceImpl service;

    private static Date gmt(int month, int day, int hh, int mm) {
        return Date.from(LocalDate.of(2026, month, day).atTime(hh, mm, 0)
                .atZone(java.time.ZoneId.of("GMT+8")).toInstant());
    }

    /** 到期日 2026-10-05，提前 3 天（10-02 开口）。 */
    private TMgisDutyTask item(long id, String contact) {
        TMgisDutyTask t = new TMgisDutyTask();
        t.setId(id);
        t.setItemNo("TX" + id);
        t.setDueAt(gmt(10, 5, 23, 59));
        t.setAmount(new BigDecimal("3"));
        t.setCityCode("361000");
        t.setContact(contact);
        t.setStatus(0);
        t.setDelFlag(0);
        return t;
    }

    @BeforeEach
    void stubCityAndWriteback() {
        when(cityMapper.selectList(any(Wrapper.class))).thenReturn(new ArrayList<>());
        // 默认回写成功
        when(taskMapper.update(any(TMgisDutyTask.class), any(Wrapper.class))).thenReturn(1);
    }

    @Test
    void emptyDayIsIdleNotFailure() throws Exception {
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        DutyBoard board = service.runCycle(gmt(1, 3, 10, 0));

        assertTrue(board.isIdle());
        // 只落一条无事标记
        verify(roundMapper, times(1)).insert(any(TMgisDutyRound.class));
        verify(sender, never()).send(any(), any());
    }

    @Test
    void blankContactSuspendsWithReasonAndRestProceed() throws Exception {
        TMgisDutyTask orphan = item(1L, "  ");
        TMgisDutyTask normal = item(2L, "王联络");
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(orphan, normal));

        DutyBoard board = service.runCycle(gmt(10, 2, 10, 0));

        assertEquals(2, orphan.getStatus().intValue(), "归口空缺应转挂起");
        assertTrue(orphan.getSuspendReason() != null && !orphan.getSuspendReason().isEmpty());
        assertEquals(1, board.getTotalSuspended());
        assertEquals(1, board.getTotalSent());
        // 空归口那条不发送；正常那条发了
        verify(sender, times(1)).send(any(), any());
    }

    @Test
    void outsideWindowHeldForNextRound() throws Exception {
        TMgisDutyTask t = item(1L, "王联络");
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(t));

        DutyBoard board = service.runCycle(gmt(10, 2, 17, 31));

        assertEquals(0, board.getTotalSent());
        assertEquals(1, board.getTotalPending());
        verify(sender, never()).send(any(), any());
        verify(taskMapper, never()).update(any(TMgisDutyTask.class), any(Wrapper.class));
    }

    @Test
    void sendFailureKeepsItemPendingAndCycleContinues() throws Exception {
        TMgisDutyTask broken = item(1L, "王联络");
        TMgisDutyTask ok = item(2L, "李联络");
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(broken, ok));
        org.mockito.Mockito.doThrow(new RuntimeException("通道断了"))
                .when(sender).send(org.mockito.ArgumentMatchers.eq(broken), any(Date.class));

        DutyBoard board = service.runCycle(gmt(10, 2, 10, 0));

        assertEquals(1, board.getTotalSent());
        assertEquals(1, board.getTotalPending(), "发送异常的条留轮，不中断整轮");
        assertEquals(0, board.getTotalSuspended());
        verify(sender, times(2)).send(any(), any());
    }

    @Test
    void writebackFailureCountsAsNotSent() throws Exception {
        TMgisDutyTask t = item(1L, "王联络");
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(t));
        // 发出后回写未成
        when(taskMapper.update(any(TMgisDutyTask.class), any(Wrapper.class))).thenReturn(0);

        DutyBoard board = service.runCycle(gmt(10, 2, 10, 0));

        assertEquals(0, board.getTotalSent());
        assertEquals(1, board.getTotalPending());
    }

    @Test
    void deadlineDayInWindowStillDispatched() throws Exception {
        TMgisDutyTask t = item(1L, "王联络");
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(t));

        DutyBoard board = service.runCycle(gmt(10, 5, 8, 30));

        assertEquals(1, board.getTotalSent());
        assertEquals(0, board.getTotalPending());
    }

    @Test
    void boardPersistsOneRowPerCity() throws Exception {
        TMgisDutyTask a = item(1L, "王联络");
        a.setCityCode("361000");
        TMgisDutyTask b = item(2L, "李联络");
        b.setCityCode("360100");
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(a, b));

        DutyBoard board = service.runCycle(gmt(10, 2, 10, 0));

        assertFalse(board.isIdle());
        assertEquals(2, board.getCities().size());
        verify(roundMapper, times(2)).insert(any(TMgisDutyRound.class));
    }
}
