package com.fc.v2.service.support;

import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fc.v2.model.auto.TMgisDutyTask;

/**
 * 默认发送通道：落应用日志即视为送达。对接短信/站内信时另写一个 {@link DutyReminderSender}
 * 实现并覆盖此 Bean，周期执行的流程与签名一概不动。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Component
public class LoggingDutyReminderSender implements DutyReminderSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingDutyReminderSender.class);

    private static final ThreadLocal<SimpleDateFormat> SDF =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    @Override
    public void send(TMgisDutyTask task) {
        // 默认通道无外部回执；真实通道在此处拿不到送达回执时直接抛异常，
        // 服务层便不会回写「已提醒」，下一轮按待提醒重发。
        log.info("【到期提醒】单号={} 地市={} 归口={} 日终截止={} 提前{}日，已送出",
                task.getItemNo(), task.getCityName(), task.getLiaison(),
                SDF.get().format(task.getDueAt()), task.getAmount());
    }
}
