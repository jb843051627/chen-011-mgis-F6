package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TMgisDutyTask;
import com.fc.v2.model.custom.duty.DutyBoard;

/**
 * 到期提醒派发条目 Service接口（scheduling-job 形状：周期执行，无增删改查入口）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITMgisDutyTaskService {

    /** 按主键回查条目 */
    TMgisDutyTask selectTMgisDutyTaskById(Long id);

    /** 已到开口日且仍待提醒的条目（含压线当日/逾期；窗口内外都在此列，窗口外的留轮；归口空缺在此转挂起） */
    List<TMgisDutyTask> listDue(Date at);

    /** 执行一次，返回**成功发出条数**；单条失败跳过继续 */
    int runOnce(Date at);

    /** 按当前时刻跑完一轮「周期执行」，回传本轮局面 */
    DutyBoard runCycle();

    /** 在指定时刻跑完一轮（窗口/到点全按该时刻判），回传本轮局面 */
    DutyBoard runCycle(Date at);

    /** 最近一轮落库的局面（看板只认服务回传，不预置）；从未跑过返回 null */
    DutyBoard latestBoard();
}
