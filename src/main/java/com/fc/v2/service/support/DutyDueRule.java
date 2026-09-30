package com.fc.v2.service.support;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.fc.v2.model.auto.TMgisDutyTask;

/**
 * 到期提醒全处唯一的一把尺：挑选该发哪些条用它，回写「已发」前也用它，两头不许各算各的。
 *
 * 口径（五月处务会定死，要挪只改条上的提前天数 {@code amount}，不动日终时刻）：
 * 1. 到期那一日以当日最后一刻 23:59:59 为准，不是零点的头一格；只填日子没填钟点的历史条目按同一把尺。
 * 2. 「还剩几日」从到期日最末往回数自然日：到期当日 = 0，提前一日 = 1；已过期为负数，照样在开口内，必须补发。
 * 3. 压线当日算不算该提醒，只许有一种答案：剩余日数 ≤ 提前天数，即算。
 *
 * 时段比较统一走墙钟字符串：JDBC 的 serverTimezone 与本机会不对称，直接比 java.util.Date 毫秒会整体偏移，
 * 「恰好到期」这类边界会错判（旧实现已踩过）。
 *
 * @author fuce
 * @date 2026-09-30
 */
public final class DutyDueRule {

    /** 对外发送窗口起：八时半 */
    public static final String WINDOW_FROM = "08:30:00";
    /** 对外发送窗口止：十七时半 */
    public static final String WINDOW_TO = "17:30:00";

    private static final ThreadLocal<SimpleDateFormat> DAY_FMT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    private static final ThreadLocal<SimpleDateFormat> TIME_FMT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("HH:mm:ss"));

    private DutyDueRule() {
    }

    /** 墙钟「日」串，到期日与今天各取各的日历日，不掺时分秒。 */
    private static String day(Date d) {
        return DAY_FMT.get().format(d);
    }

    /**
     * 还剩几日：从到期那一日的最末往回数到「今天」的自然日数。
     * 到期当日 0；明天到期 1；昨天已过期 -1。一天之内任何时刻问，答案一致。
     */
    public static long daysLeft(Date dueAt, Date now) {
        LocalDate dueDay = LocalDate.parse(day(dueAt));
        LocalDate today = LocalDate.parse(day(now));
        return ChronoUnit.DAYS.between(today, dueDay);
    }

    /**
     * 这一条此刻该不该进提醒开口：待提醒、未删、到期日已落在「提前天数」罩住的范围里（含压线当日，含已过期）。
     * 已发（1）、已挂起（2）任何轮次都不再被挑出来。
     */
    public static boolean shouldRemind(TMgisDutyTask t, Date now) {
        if (t == null || t.getDueAt() == null || t.getAmount() == null) {
            return false;
        }
        if (t.getStatus() == null || t.getStatus() != 0) {
            return false;
        }
        if (t.getDelFlag() != null && t.getDelFlag() != 0) {
            return false;
        }
        // 剩余日数 ≤ 提前天数（提前天数按自然日落库，amount=3.00 即三日）
        return java.math.BigDecimal.valueOf(daysLeft(t.getDueAt(), now)).compareTo(t.getAmount()) <= 0;
    }

    /**
     * 此刻在不在对外发送时段（八时半至十七时半，含两头）。这一段之外，条子到期也不动，留到下一轮。
     */
    public static boolean inSendWindow(Date at) {
        String hm = TIME_FMT.get().format(at);
        return hm.compareTo(WINDOW_FROM) >= 0 && hm.compareTo(WINDOW_TO) <= 0;
    }
}
