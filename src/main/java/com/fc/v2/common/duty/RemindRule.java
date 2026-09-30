package com.fc.v2.common.duty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 到期提醒唯一一把尺（处务会五月定规）。
 *
 * <p>三处口径在此写死，全处只许用这一个答案：</p>
 * <ol>
 *     <li>到期是「那一天的最后一刻」（23:59:59），不是零点头一格；老清册迁进来
 *     只填日子（00:00:00）的条目同样截到当日最末，新旧条目同尺判断。</li>
 *     <li>「还剩几日」从到期日最末往回按自然日数：到期当日为 0、提前 N 天开口、
 *     逾期为负。系统挑选与回写一律走 {@link #isDue}，不许两头各算各的。
 *         压线当日算不算该提醒，只许有一种答案：算（0 &le; 提前天数）。</li>
 *     <li>要挪提醒口径只改每条的提前天数，{@link #DAY_END} 这个截止时刻不许动。</li>
 * </ol>
 *
 * <p>对外发送只认 {@link #WINDOW_FROM}–{@link #WINDOW_TO} 这段墙钟
 * （两端都含），窗口之外条子到期也留到下一轮。</p>
 *
 * <p>时间一律按东八区墙钟解释：库连接 serverTimezone 与运行机时区可能不对称，
 * 不把瞬时直接比较，统一在本类换成业务墙上的日期/钟点后再判。</p>
 *
 * @author fuce
 * @date 2026-09-30
 */
public final class RemindRule {

    /** 业务时区：东八区墙钟，无夏令时。 */
    public static final ZoneId ZONE = ZoneId.of("GMT+8");

    /** 一日之末：到期按这一刹算，不许为挪口径而改动。 */
    public static final LocalTime DAY_END = LocalTime.of(23, 59, 59);

    /** 对外发送窗口起（含）：八时半。 */
    public static final LocalTime WINDOW_FROM = LocalTime.of(8, 30);

    /** 对外发送窗口止（含）：十七时半。 */
    public static final LocalTime WINDOW_TO = LocalTime.of(17, 30);

    private static final DateTimeFormatter WALL_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private RemindRule() {
    }

    /** 把 {@link Date} 按东八区墙钟读出来（库里读出的时刻也走这一道）。 */
    public static ZonedDateTime zoned(Date at) {
        return at.toInstant().atZone(ZONE);
    }

    /** 条目到期的是哪一个自然日。老数据只存了 00:00:00，日子也不会错。 */
    public static LocalDate dueDate(Date dueAt) {
        return zoned(dueAt).toLocalDate();
    }

    /**
     * 到期日的最末一刻（东八区当日 23:59:59）。
     * 无论库存的是 00:00:00 还是 23:59:59，归一后同一结果。
     */
    public static ZonedDateTime deadline(Date dueAt) {
        return dueDate(dueAt).atTime(DAY_END).atZone(ZONE);
    }

    /** {@link #deadline} 的 {@link Date} 形态。 */
    public static Date deadlineAsDate(Date dueAt) {
        return Date.from(deadline(dueAt).toInstant());
    }

    /**
     * 还剩几日：从到期日最末往回按自然日数。
     * 到期当日 = 0；提前一天 = 1；已逾期为负数。同尺用于挑选与回写。
     */
    public static long remainingDays(Date dueAt, Date at) {
        return ChronoUnit.DAYS.between(zoned(at).toLocalDate(), dueDate(dueAt));
    }

    /**
     * 到没到该开口的时候：还剩日数不大于本条定的提前天数即该提醒。
     * 压线当日（还剩 0）算；逾期未发（负值）也算——漏过的不能从此装聋。
     * 到期日或提前天数缺失的，无法成尺，不挑。
     */
    public static boolean isDue(Date dueAt, BigDecimal leadDays, Date at) {
        if (dueAt == null || leadDays == null || at == null) {
            return false;
        }
        return BigDecimal.valueOf(remainingDays(dueAt, at)).compareTo(leadDays) <= 0;
    }

    /** 此刻在不在对外发送窗口内（八时半至十七时半，两端含）。 */
    public static boolean inWindow(Date at) {
        if (at == null) {
            return false;
        }
        LocalTime t = zoned(at).toLocalTime();
        return !t.isBefore(WINDOW_FROM) && !t.isAfter(WINDOW_TO);
    }

    /**
     * 本轮该不该挑这条：窗口内且到了提前开口的日子。
     * 窗口之外即便到期也返回 false，留到下一轮。
     */
    public static boolean shouldPick(Date dueAt, BigDecimal leadDays, Date at) {
        return inWindow(at) && isDue(dueAt, leadDays, at);
    }

    /** 东八区墙钟字符串（yyyy-MM-dd HH:mm:ss），落库/比对都用这一面上的字。 */
    public static String wallClock(Date at) {
        return WALL_FMT.format(zoned(at));
    }
}
