package com.fc.v2.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.common.duty.RemindRule;
import com.fc.v2.common.duty.RemindSender;
import com.fc.v2.mapper.auto.TMgisDutyRoundMapper;
import com.fc.v2.mapper.auto.TMgisDutyTaskMapper;
import com.fc.v2.mapper.auto.TSysCityMapper;
import com.fc.v2.model.auto.TMgisDutyRound;
import com.fc.v2.model.auto.TMgisDutyTask;
import com.fc.v2.model.auto.TSysCity;
import com.fc.v2.model.custom.duty.DutyBoard;
import com.fc.v2.service.ITMgisDutyTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 到期提醒派发条目 Service业务层处理（scheduling-job 形状：周期执行）。
 *
 * <p>处务会五月定的规矩全收在这里：</p>
 * <ul>
 *     <li>挑哪几条、回写「已发」用同一把尺 {@link RemindRule}，没有第二种算法；</li>
 *     <li>窗口（八时半至十七时半）之外到期也留轮，不动不发；</li>
 *     <li>先发后回写：回写不成算没发，下一轮接着挑；已发条目任何轮次都不会再入选；</li>
 *     <li>归口联络员空缺的条目各自转挂起、写清缘故，一条推不动不碍整轮；</li>
 *     <li>跑完按地市落一屏本轮局面，无条到期就落「本轮无事」，不当故障。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TMgisDutyTaskServiceImpl implements ITMgisDutyTaskService {

    private static final Logger logger = LoggerFactory.getLogger(TMgisDutyTaskServiceImpl.class);

    /** 条目情形：待提醒。 */
    private static final int STATUS_WAIT = 0;
    /** 条目情形：已提醒（回写落库才算）。 */
    private static final int STATUS_SENT = 1;
    /** 条目情形：已挂起（归口空着，推不动）。 */
    private static final int STATUS_SUSPENDED = 2;

    /** 归口地市空着时看板上用的名。 */
    private static final String NO_CITY = "";

    @Resource
    private TMgisDutyTaskMapper mgisDutyTaskMapper;

    @Resource
    private TMgisDutyRoundMapper mgisDutyRoundMapper;

    @Resource
    private TSysCityMapper sysCityMapper;

    /** 对外通道；扩容时另出实现换 bean，本类不动。 */
    @Resource
    private RemindSender remindSender;

    @Override
    public TMgisDutyTask selectTMgisDutyTaskById(Long id) {
        return this.mgisDutyTaskMapper.selectById(id);
    }

    /**
     * 本轮候选：仍待提醒、已到开口日（压线当日算，逾期也算），不含已发、已挂起。
     * 窗口内外都在此列——窗口外的留轮计入待发；归口空缺的到这里转挂起。
     * 判定一律走 {@link RemindRule}，与回写同尺。
     */
    @Override
    public List<TMgisDutyTask> listDue(Date at) {
        List<TMgisDutyTask> all = mgisDutyTaskMapper.selectList(new QueryWrapper<TMgisDutyTask>()
                .eq("status", STATUS_WAIT)
                .and(w -> w.eq("del_flag", 0).or().isNull("del_flag")));
        List<TMgisDutyTask> due = new ArrayList<>();
        for (TMgisDutyTask r : all) {
            if (RemindRule.isDue(r.getDueAt(), r.getAmount(), at)) {
                due.add(r);
            }
        }
        return due;
    }

    /**
     * @deprecated 签名照旧保留，等价于跑完一轮后取本轮已发数；新调用走 {@link #runCycle(Date)}。
     */
    @Override
    @Deprecated
    public int runOnce(Date at) {
        return runCycle(at).getTotalSent();
    }

    @Override
    public DutyBoard runCycle() {
        return runCycle(new Date());
    }

    @Override
    public DutyBoard runCycle(Date at) {
        Date runAt = at == null ? new Date() : at;
        String roundNo = new SimpleDateFormat("yyyyMMddHHmmss").format(runAt);

        List<TMgisDutyTask> candidates = listDue(runAt);
        Map<String, CityTally> tallies = new TreeMap<>();

        for (TMgisDutyTask r : candidates) {
            String cityKey = r.getCityCode() == null ? NO_CITY : r.getCityCode();
            CityTally tally = tallies.computeIfAbsent(cityKey, CityTally::new);
            // 个别条推不动不碍整轮：每条各自兜异常，绝不让一轮因一条中断。
            try {
                if (isBlank(r.getContact())) {
                    suspend(r, runAt);
                    tally.suspended++;
                } else if (!RemindRule.inWindow(runAt)) {
                    // 窗口之外到期也不动，留到下一轮。
                    tally.pending++;
                } else if (dispatch(r, runAt)) {
                    tally.sent++;
                } else {
                    tally.pending++;
                }
            } catch (Exception e) {
                logger.error("周期执行单条失败，留待下一轮 单号={} 联络员={}", r.getItemNo(), r.getContact(), e);
                tally.pending++;
            }
        }

        DutyBoard board = assemble(roundNo, runAt, tallies);
        persistRound(board, candidates.isEmpty());
        return board;
    }

    @Override
    public DutyBoard latestBoard() {
        TMgisDutyRound newest = mgisDutyRoundMapper.selectOne(new QueryWrapper<TMgisDutyRound>()
                .and(w -> w.eq("del_flag", 0).or().isNull("del_flag"))
                .orderByDesc("run_at")
                .last("limit 1"));
        if (newest == null) {
            return null;
        }
        DutyBoard board = new DutyBoard();
        board.setRoundNo(newest.getRoundNo());
        board.setRunAt(newest.getRunAt());
        if (Integer.valueOf(1).equals(newest.getIdleFlag())) {
            board.setIdle(true);
            return board;
        }
        List<TMgisDutyRound> rows = mgisDutyRoundMapper.selectList(new QueryWrapper<TMgisDutyRound>()
                .eq("round_no", newest.getRoundNo())
                .and(w -> w.eq("del_flag", 0).or().isNull("del_flag")));
        List<String> codes = new ArrayList<>();
        for (TMgisDutyRound row : rows) {
            if (!isBlank(row.getCityCode())) {
                codes.add(row.getCityCode());
            }
        }
        Map<String, String> names = cityNames(codes);
        for (TMgisDutyRound row : rows) {
            String code = row.getCityCode() == null ? NO_CITY : row.getCityCode();
            DutyBoard.CityRow cr = new DutyBoard.CityRow(code, names.getOrDefault(code, code));
            cr.setSent(nz(row.getSentCount()));
            cr.setPending(nz(row.getPendingCount()));
            cr.setSuspended(nz(row.getSuspendedCount()));
            board.getCities().add(cr);
            board.setTotalSent(board.getTotalSent() + cr.getSent());
            board.setTotalPending(board.getTotalPending() + cr.getPending());
            board.setTotalSuspended(board.getTotalSuspended() + cr.getSuspended());
        }
        sortRows(board);
        return board;
    }

    /**
     * 发出去并回写自己的状态。先发后写：回写不成（含并发下已被别轮抢先）算没发，
     * 条目仍是待提醒，下一轮还会被挑出来。
     *
     * @return true 已发且回写落库
     */
    private boolean dispatch(TMgisDutyTask r, Date at) throws Exception {
        remindSender.send(r, at);
        r.setStatus(STATUS_SENT);
        r.setSentAt(at);
        // 条件更新守住「待提醒」底态：已发的不会被第二轮重复推。
        UpdateWrapper<TMgisDutyTask> guard = new UpdateWrapper<TMgisDutyTask>()
                .eq("id", r.getId())
                .eq("status", STATUS_WAIT)
                .and(w -> w.eq("del_flag", 0).or().isNull("del_flag"));
        int written = mgisDutyTaskMapper.update(r, guard);
        if (written <= 0) {
            logger.warn("提醒已发出但回写未成，按没发处理 单号={}", r.getItemNo());
            return false;
        }
        return true;
    }

    /** 归口空缺：转挂起并写清缘故。 */
    private void suspend(TMgisDutyTask r, Date at) {
        r.setStatus(STATUS_SUSPENDED);
        r.setSuspendReason("归口联络员空缺（人调走/岗位并），无处可送，挂起待归口补齐");
        UpdateWrapper<TMgisDutyTask> guard = new UpdateWrapper<TMgisDutyTask>()
                .eq("id", r.getId())
                .eq("status", STATUS_WAIT)
                .and(w -> w.eq("del_flag", 0).or().isNull("del_flag"));
        int written = mgisDutyTaskMapper.update(r, guard);
        if (written <= 0) {
            throw new IllegalStateException("挂起回写未成，单号=" + r.getItemNo());
        }
        logger.warn("条目归口空缺，转挂起 单号={} 地市={}", r.getItemNo(), r.getCityCode());
    }

    private DutyBoard assemble(String roundNo, Date runAt, Map<String, CityTally> tallies) {
        DutyBoard board = new DutyBoard();
        board.setRoundNo(roundNo);
        board.setRunAt(runAt);
        if (tallies.isEmpty()) {
            // 年初常有无条到期的日子：本轮无事，不是故障。
            board.setIdle(true);
            return board;
        }
        Map<String, String> names = cityNames(tallies.keySet());
        for (Map.Entry<String, CityTally> e : tallies.entrySet()) {
            String code = e.getKey();
            CityTally t = e.getValue();
            DutyBoard.CityRow row = new DutyBoard.CityRow(code, names.getOrDefault(code, code));
            row.setSent(t.sent);
            row.setPending(t.pending);
            row.setSuspended(t.suspended);
            board.getCities().add(row);
        }
        sortRows(board);
        for (DutyBoard.CityRow row : board.getCities()) {
            board.setTotalSent(board.getTotalSent() + row.getSent());
            board.setTotalPending(board.getTotalPending() + row.getPending());
            board.setTotalSuspended(board.getTotalSuspended() + row.getSuspended());
        }
        return board;
    }

    /** 本轮局面落库：有动作按地市一行；无事落一条 idle 标记，看板据此回话。 */
    private void persistRound(DutyBoard board, boolean idle) {
        if (idle) {
            TMgisDutyRound marker = newRound(board.getRoundNo(), board.getRunAt(), null, 0, 0, 0);
            marker.setIdleFlag(1);
            mgisDutyRoundMapper.insert(marker);
            return;
        }
        for (DutyBoard.CityRow row : board.getCities()) {
            mgisDutyRoundMapper.insert(newRound(board.getRoundNo(), board.getRunAt(),
                    row.getCityCode(), row.getSent(), row.getPending(), row.getSuspended()));
        }
    }

    private TMgisDutyRound newRound(String roundNo, Date runAt, String cityCode,
                                    int sent, int pending, int suspended) {
        TMgisDutyRound round = new TMgisDutyRound();
        round.setRoundNo(roundNo);
        round.setRunAt(runAt);
        round.setCityCode(cityCode);
        round.setSentCount(sent);
        round.setPendingCount(pending);
        round.setSuspendedCount(suspended);
        round.setIdleFlag(0);
        round.setDelFlag(0);
        return round;
    }

    private Map<String, String> cityNames(Iterable<String> codes) {
        List<String> codeList = new ArrayList<>();
        for (String c : codes) {
            if (!isBlank(c)) {
                codeList.add(c);
            }
        }
        Map<String, String> names = new LinkedHashMap<>();
        if (codeList.isEmpty()) {
            return names;
        }
        List<TSysCity> cities = sysCityMapper.selectList(new QueryWrapper<TSysCity>()
                .in("city_code", codeList));
        for (TSysCity c : cities) {
            names.put(c.getCityCode(), c.getCityName());
        }
        return names;
    }

    private void sortRows(DutyBoard board) {
        // 哪个市压得多一眼看得见：按本轮总量倒序。
        board.getCities().sort((a, b) -> {
            int byWeight = Integer.compare(b.weight(), a.weight());
            return byWeight != 0 ? byWeight : String.valueOf(a.getCityCode()).compareTo(String.valueOf(b.getCityCode()));
        });
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** 一地市一把计数。 */
    private static class CityTally {
        final String code;
        int sent;
        int pending;
        int suspended;

        CityTally(String code) {
            this.code = code;
        }
    }
}
