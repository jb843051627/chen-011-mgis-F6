package com.fc.v2.common.duty;

import com.fc.v2.model.auto.TMgisDutyTask;
import java.util.Date;

/**
 * 对外提醒的发送端口。真正的通道（短信/站内信/邮件）扩容时另出实现替换本 bean，
 * 服务层的挑选、回写、挂起、局面口径一律不动。
 *
 * <p>约定：抛任何异常都算没发出去，服务层按「待发」留给下一轮，不得只当成功亮一下。</p>
 *
 * @author fuce
 * @date 2026-09-30
 */
public interface RemindSender {

    /**
     * 把这一条提醒送到归口联络员手上。
     *
     * @param item 待提醒条目（状态仍为待提醒）
     * @param at  本轮发送时刻（东八区窗口内）
     * @throws Exception 送不出去时抛出；服务层据此保留待提醒、计本轮待发
     */
    void send(TMgisDutyTask item, Date at) throws Exception;
}
