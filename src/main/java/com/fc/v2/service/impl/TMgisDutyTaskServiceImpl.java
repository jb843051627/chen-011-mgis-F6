package com.fc.v2.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.conf.redis.RedisService;
import com.fc.v2.mapper.auto.TMgisDutyTaskMapper;
import com.fc.v2.mapper.custom.DutyTaskCycleMapper;
import com.fc.v2.model.auto.TMgisDutyTask;
import com.fc.v2.model.custom.DutyCycleSnapshot;
import com.fc.v2.model.custom.DutyCycleSnapshot.CityCount;
import com.fc.v2.model.custom.DutyCycleSnapshot.Item;
import com.fc.v2.service.ITMgisDutyTaskService;
import com.fc.v2.service.support.DutyDueRule;
import com.fc.v2.service.support.DutyReminderSender;
import com.fc.v2.util.StringUtils;

/**
 * 到期提醒派发条目 Service业务层处理（scheduling-job 形状：周期执行）。
 *
 * 五月处务会规矩全落在这一处：
 * 1. 到期判定全处只认 {@link DutyDueRule} 一把尺，挑选与回写同源；压线当日只有一种答案。
 * 2. 对外发送只在八时半至十七时半；窗外条子到期也不动，留到下一轮。
 * 3. 发出后必须回写成功才算已发；回写不成按未发，下一轮再来。
 * 4. 已提醒条目任何轮次不再挑出（待提醒状态才进候选）。
 * 5. 归口空着的条目自身转挂起、写清缘故，其余各条一条条走完，绝不一处空值中断整轮。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TMgisDutyTaskServiceImpl implements ITMgisDutyTaskService {

    private static final Logger log = LoggerFactory.getLogger(TMgisDutyTaskServiceImpl.class);

    /** 条目情形 0待提醒 1已提醒 2已挂起 */
    private static final int STATUS_WAIT = 0;
    private static final int STATUS_SENT = 1;
    private static final int STATUS_SUSPENDED = 2;

    /** 本轮局面在 Redis 的落点（看板只认服务回传，页面不预写死） */
    private static final String SNAPSHOT_KEY = "mgis:dutyTask:latestSnapshot";
    /** 轮次互斥锁：防止两轮并发把同一件事推两遍 */
    private static final String LOCK_KEY = "mgis:dutyTask:cycleLock";
    private static final long LOCK_MINUTES = 5L;

    private static final String SUSPEND_REASON_BLANK_LIAISON = "归口联络员空缺（人调走、岗位并了），暂无法送达";

    private static final ThreadLocal<SimpleDateFormat> TS_FMT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    private static final ThreadLocal<SimpleDateFormat> DAY_FMT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    @javax.annotation.Resource
    private TMgisDutyTaskMapper mgisDutyTaskMapper;

    /** 周期回写专用（绕开定时线程里会炸的自动填充，并在库层加 status=0 幂等闸） */
    @javax.annotation.Resource
    private DutyTaskCycleMapper dutyTaskCycleMapper;

    @javax.annotation.Resource
    private DutyReminderSender reminderSender;

    /** 单机无 Redis 时（如纯单测）退化为不缓存、不加锁，规矩本身不变 */
    @Autowired(required = false)
    private RedisService redisService;

    @Override
    public TMgisDutyTask selectTMgisDutyTaskById(Long id) {
        return this.mgisDutyTaskMapper.selectById(id);
    }

    @Override
    public DutyCycleSnapshot runCycle() {
        return runCycle(new Date());
    }

    @Override
    public DutyCycleSnapshot runCycle(Date at) {
        if (at == null) {
            at = new Date();
        }
        String token = lockCycle();
        if (token == null) {
            // 上一轮还没跑完：本轮不碰任何条目，避免同一件事被推两遍
            DutyCycleSnapshot skip = new DutyCycleSnapshot();
            skip.setRunAt(TS_FMT.get().format(at));
            skip.setConcurrentSkip(true);
            log.warn("到期提醒上一轮尚未跑完，本轮跳过");
            return skip;
        }
        try {
            DutyCycleSnapshot snapshot = doCycle(at);
            saveSnapshot(snapshot);
            return snapshot;
        } finally {
            unlockCycle(token);
        }
    }

    @Override
    public DutyCycleSnapshot latestSnapshot() {
        if (redisService == null) {
            return null;
        }
        try {
            return redisService.getCacheObject(SNAPSHOT_KEY);
        } catch (Exception e) {
            log.warn("读取上一轮局面失败（按空屏处理，不当故障）：{}", e.getMessage());
            return null;
        }
    }

    /** 一轮的主体。刻意不加事务：逐条提交，一条推不动不得回滚、不得绊住其余各条。 */
    private DutyCycleSnapshot doCycle(Date at) {
        DutyCycleSnapshot snap = new DutyCycleSnapshot();
        snap.setRunAt(TS_FMT.get().format(at));
        boolean inWindow = DutyDueRule.inSendWindow(at);
        snap.setInWindow(inWindow);

        List<TMgisDutyTask> all = mgisDutyTaskMapper.selectList(
                new QueryWrapper<TMgisDutyTask>().orderByAsc("due_at", "id"));
        String today = DAY_FMT.get().format(at);

        // 第一遍：唯一一把尺挑候选（已发、已挂起天然排除；未到开口的待提醒条目不进）
        List<TMgisDutyTask> candidates = new ArrayList<>();
        for (TMgisDutyTask t : all) {
            if (t.getDelFlag() != null && t.getDelFlag() != 0) {
                continue;
            }
            if (DutyDueRule.shouldRemind(t, at)) {
                candidates.add(t);
            }
        }

        // 没有一条该开口的：年初常见，报一句「本轮无事」，不当故障
        snap.setIdle(candidates.isEmpty());

        int sentThisRound = 0;
        int suspendedThisRound = 0;

        // 第二遍：逐条处理。每条各自 try/catch——一处空值或一处通道异常只绊自己这一条
        for (TMgisDutyTask t : candidates) {
            Item item = toItem(t, at);
            // 记住落笔前的样子：通道或回写失败时把内存对象恢复成待提醒，第三遍出账才不会虚算一笔已发
            Integer originStatus = t.getStatus();
            Date originSentAt = t.getSentAt();
            String originReason = t.getSuspendReason();
            try {
                if (!inWindow) {
                    // 窗外：条子到期也不动，留到下一轮
                    item.setAction("WAITING");
                    snap.getItems().add(item);
                    continue;
                }
                if (StringUtils.isEmpty(StringUtils.trim(t.getLiaison()))) {
                    // 归口空着（人调走了、岗位并了）：本条转挂起、写清缘故，其后的一条条照走
                    t.setStatus(STATUS_SUSPENDED);
                    t.setSuspendReason(SUSPEND_REASON_BLANK_LIAISON);
                    int rows = dutyTaskCycleMapper.markSuspended(t.getId(), SUSPEND_REASON_BLANK_LIAISON);
                    if (rows <= 0) {
                        throw new IllegalStateException("挂起回写影响行数为 0");
                    }
                    item.setStatus(STATUS_SUSPENDED);
                    item.setSuspendReason(t.getSuspendReason());
                    item.setAction("SUSPENDED");
                    suspendedThisRound++;
                    snap.getItems().add(item);
                    continue;
                }

                // 发出去；通道抛异常即视为没发，不回写
                reminderSender.send(t);

                // 回写自己的状态；回写不成便算没发（不能只在页面上亮一下），下一轮按待提醒重发。
                // 库层 status=0 再上一道幂等闸：并发轮次下只有一轮改写成功。
                int rows = dutyTaskCycleMapper.markSent(t.getId(), at);
                if (rows <= 0) {
                    throw new IllegalStateException("已发回写影响行数为 0");
                }
                t.setStatus(STATUS_SENT);
                t.setSuspendReason(null);
                t.setSentAt(at);
                item.setStatus(STATUS_SENT);
                item.setSentAt(TS_FMT.get().format(at));
                item.setAction("SENT");
                sentThisRound++;
            } catch (Exception e) {
                // 本条推不动：库中仍是待提醒，内存也恢复原状，下一轮再来；整轮不中断
                t.setStatus(originStatus);
                t.setSentAt(originSentAt);
                t.setSuspendReason(originReason);
                log.error("到期提醒条目 {} 本轮推送失败，留待下一轮：{}", t.getItemNo(), e.getMessage(), e);
                item.setAction("FAIL");
            }
            snap.getItems().add(item);
        }

        // 第三遍：按处理后的局面统一出账（候选对象内存中已是新状态，柱状与总数同源，不会两本账）
        Map<String, CityCount> cityMap = new LinkedHashMap<>();
        int totalSentToday = 0;
        int totalWaiting = 0;
        int totalSuspended = 0;
        for (TMgisDutyTask t : all) {
            if (t.getDelFlag() != null && t.getDelFlag() != 0) {
                continue;
            }
            CityCount cc = city(t, cityMap);
            int st = t.getStatus() == null ? STATUS_WAIT : t.getStatus();
            if (st == STATUS_SENT) {
                // 看板只讲本轮：今日已发才上柱；往日已发的条目只在台账里以 ALREADY 留痕
                if (t.getSentAt() != null && DAY_FMT.get().format(t.getSentAt()).equals(today)) {
                    cc.setSent(cc.getSent() + 1);
                    totalSentToday++;
                }
            } else if (st == STATUS_SUSPENDED) {
                cc.setSuspended(cc.getSuspended() + 1);
                totalSuspended++;
            } else {
                cc.setWaiting(cc.getWaiting() + 1);
                totalWaiting++;
            }
        }
        snap.setTotalSent(totalSentToday);
        snap.setTotalWaiting(totalWaiting);
        snap.setTotalSuspended(totalSuspended);

        // 未进本轮候选的条目（往日已发、已挂起、未到开口）也入台账，看板局面才完整
        for (TMgisDutyTask t : all) {
            if ((t.getDelFlag() != null && t.getDelFlag() != 0) || candidates.contains(t)) {
                continue;
            }
            Item item = toItem(t, at);
            int st = t.getStatus() == null ? STATUS_WAIT : t.getStatus();
            if (st == STATUS_SENT) {
                item.setAction("ALREADY");
            } else if (st == STATUS_SUSPENDED) {
                item.setAction("SUSPENDED");
            } else {
                item.setAction("WAITING");
            }
            snap.getItems().add(item);
        }

        snap.setSentThisRound(sentThisRound);
        snap.setSuspendedThisRound(suspendedThisRound);
        snap.setCities(new ArrayList<>(cityMap.values()));
        return snap;
    }

    /** 取（或建）该条归口地市的柱；地市也空着时单立「未归口」一桶，不因双空值中断 */
    private CityCount city(TMgisDutyTask t, Map<String, CityCount> cityMap) {
        String code = StringUtils.trim(t.getCityCode());
        if (StringUtils.isEmpty(code)) {
            code = "_NONE_";
        }
        CityCount cc = cityMap.get(code);
        if (cc == null) {
            String name = StringUtils.trim(t.getCityName());
            cc = new CityCount("_NONE_".equals(code) ? null : code,
                    StringUtils.isEmpty(name) ? "未归口" : name);
            cityMap.put(code, cc);
        }
        return cc;
    }

    private Item toItem(TMgisDutyTask t, Date at) {
        Item item = new Item();
        item.setItemNo(t.getItemNo());
        item.setCityName(t.getCityName());
        item.setLiaison(t.getLiaison());
        item.setDueAt(t.getDueAt() == null ? null : TS_FMT.get().format(t.getDueAt()));
        item.setLeadDays(t.getAmount() == null ? null : t.getAmount().stripTrailingZeros().toPlainString());
        if (t.getDueAt() != null) {
            item.setDaysLeft(DutyDueRule.daysLeft(t.getDueAt(), at));
        }
        item.setStatus(t.getStatus() == null ? STATUS_WAIT : t.getStatus());
        item.setSuspendReason(t.getSuspendReason());
        item.setSentAt(t.getSentAt() == null ? null : TS_FMT.get().format(t.getSentAt()));
        return item;
    }

    private String lockCycle() {
        if (redisService == null) {
            return "NO_REDIS";
        }
        try {
            String token = UUID.randomUUID().toString();
            Boolean ok = redisService.redisTemplate.opsForValue()
                    .setIfAbsent(LOCK_KEY, token, LOCK_MINUTES, TimeUnit.MINUTES);
            return Boolean.TRUE.equals(ok) ? token : null;
        } catch (Exception e) {
            log.warn("轮次加锁失败，按无锁继续：{}", e.getMessage());
            return "NO_REDIS";
        }
    }

    private void unlockCycle(String token) {
        if (redisService == null || "NO_REDIS".equals(token)) {
            return;
        }
        try {
            Object held = redisService.redisTemplate.opsForValue().get(LOCK_KEY);
            if (token.equals(held)) {
                redisService.redisTemplate.delete(LOCK_KEY);
            }
        } catch (Exception e) {
            log.warn("轮次解锁失败（锁将自行到期）：{}", e.getMessage());
        }
    }

    private void saveSnapshot(DutyCycleSnapshot snapshot) {
        if (redisService == null) {
            return;
        }
        try {
            redisService.setCacheObject(SNAPSHOT_KEY, snapshot);
        } catch (Exception e) {
            // 看板缓存写不进去不影响条目本身的发送与回写
            log.warn("本轮局面缓存失败：{}", e.getMessage());
        }
    }
}
