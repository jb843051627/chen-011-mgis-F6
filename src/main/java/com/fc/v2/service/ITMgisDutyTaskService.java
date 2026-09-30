package com.fc.v2.service;

import java.util.Date;

import com.fc.v2.model.auto.TMgisDutyTask;
import com.fc.v2.model.custom.DutyCycleSnapshot;

/**
 * 到期提醒派发条目 Service接口（scheduling-job 形状：唯「周期执行」一门，无手工立单改单入口）。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITMgisDutyTaskService {

    /** 按主键回查条目 */
    TMgisDutyTask selectTMgisDutyTaskById(Long id);

    /**
     * 周期执行（系统自己找、自己发），以当前时刻为「本轮」。
     * 整套执行压在服务层：窗口判定、到期挑选、发送、回写、挂起、看板汇总全在这里，
     * 定时任务/控制器只唤起、只看回传。
     */
    DutyCycleSnapshot runCycle();

    /**
     * 周期执行的重载：指定本轮时刻（扩容用，签名格局不动，正式轮次仍走无参 {@link #runCycle()}）。
     */
    DutyCycleSnapshot runCycle(Date at);

    /** 上一轮跑完留存的局面（看板只讲本轮；尚未跑过返回 null，页面按空屏处理，不当故障） */
    DutyCycleSnapshot latestSnapshot();
}
