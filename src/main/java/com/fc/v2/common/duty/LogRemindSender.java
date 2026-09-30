package com.fc.v2.common.duty;

import com.fc.v2.model.auto.TMgisDutyTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 默认发送通道：落日志留痕。扩容（短信/站内信/邮件）时另写一个 {@link RemindSender}
 * 实现替换此 bean，周期执行里的其余规矩不碰。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Component
public class LogRemindSender implements RemindSender {

    private static final Logger logger = LoggerFactory.getLogger(LogRemindSender.class);

    @Override
    public void send(TMgisDutyTask item, Date at) {
        logger.info("到期提醒已发出 单号={} 归口地市={} 联络员={} 到期日终={} 发出时刻={}",
                item.getItemNo(), item.getCityCode(), item.getContact(),
                RemindRule.wallClock(item.getDueAt()), RemindRule.wallClock(at));
    }
}
