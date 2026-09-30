package com.fc.v2.common.quartz.task;

import com.fc.v2.model.custom.duty.DutyBoard;
import com.fc.v2.service.ITMgisDutyTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 到期提醒「周期执行」入口。
 *
 * <p>进出的门唯此一处（Quartz invoke_target = {@code dutyCycleTask.run()}）。
 * 这里不挑条、不发消息、不写状态，整套压给服务层；每轮跑出什么局面全看服务回传，
 * 本 bean 只记账一行。手工立单改单不开口子，扩容另辟 {@code RemindSender} 重载。</p>
 *
 * @author fuce
 * @date 2026-09-30
 */
@Component("dutyCycleTask")
public class DutyCycleTask {

    private static final Logger logger = LoggerFactory.getLogger(DutyCycleTask.class);

    @Autowired
    private ITMgisDutyTaskService mgisDutyTaskService;

    /** Quartz 反射调用的无参方法：跑完一轮，只报本轮局面。 */
    public void run() {
        DutyBoard board = mgisDutyTaskService.runCycle();
        if (board.isIdle()) {
            // 无条到期是常态，不是故障，不当异常抛（去年半夜被叫醒查过一回）。
            logger.info("到期提醒本轮无事 轮次={}", board.getRoundNo());
            return;
        }
        logger.info("到期提醒本轮局面 轮次={} 已发={} 待发={} 挂起={} 地市数={}",
                board.getRoundNo(), board.getTotalSent(), board.getTotalPending(),
                board.getTotalSuspended(), board.getCities().size());
    }
}
