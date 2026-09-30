package com.fc.v2.service.support;

import com.fc.v2.model.auto.TMgisDutyTask;

/**
 * 对外发送通道。默认实现只落日志（短信/站内信通道另辟实现，不改周期执行的骨架与签名）。
 * 约定：发出去才算数；抛异常 = 没发出去，服务层不得回写「已提醒」。
 *
 * @author fuce
 * @date 2026-09-30
 */
public interface DutyReminderSender {

    /**
     * 把这一条提醒送到归口联络员手上。
     *
     * @throws Exception 送不出去（通道异常、回执失败等），调用方按未发处理，下一轮再来
     */
    void send(TMgisDutyTask task) throws Exception;
}
