package com.fc.v2.model.custom.duty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 一轮「周期执行」跑完后的局面：只讲本轮，旁的一概不管。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class DutyBoard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 轮次号 */
    private String roundNo;

    /** 本轮执行时刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date runAt;

    /** 本轮无事（年初无条子到期的日子是常态，不是故障） */
    private boolean idle;

    /** 各地市分行，按积压量降序 */
    private List<CityRow> cities = new ArrayList<>();

    /** 本轮已发合计 */
    private int totalSent;

    /** 本轮待发合计（到点未发：窗口外留轮或发送/回写未成） */
    private int totalPending;

    /** 本轮挂起合计（归口空着，推不动） */
    private int totalSuspended;

    /**
     * 一地市一行。
     */
    public static class CityRow implements Serializable {
        private static final long serialVersionUID = 1L;

        private String cityCode;
        private String cityName;
        private int sent;
        private int pending;
        private int suspended;

        public CityRow() {
        }

        public CityRow(String cityCode, String cityName) {
            this.cityCode = cityCode;
            this.cityName = cityName;
        }

        public int weight() {
            return sent + pending + suspended;
        }

        public String getCityCode() {
            return cityCode;
        }

        public void setCityCode(String cityCode) {
            this.cityCode = cityCode;
        }

        public String getCityName() {
            return cityName;
        }

        public void setCityName(String cityName) {
            this.cityName = cityName;
        }

        public int getSent() {
            return sent;
        }

        public void setSent(int sent) {
            this.sent = sent;
        }

        public int getPending() {
            return pending;
        }

        public void setPending(int pending) {
            this.pending = pending;
        }

        public int getSuspended() {
            return suspended;
        }

        public void setSuspended(int suspended) {
            this.suspended = suspended;
        }
    }

    public String getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(String roundNo) {
        this.roundNo = roundNo;
    }

    public Date getRunAt() {
        return runAt;
    }

    public void setRunAt(Date runAt) {
        this.runAt = runAt;
    }

    public boolean isIdle() {
        return idle;
    }

    public void setIdle(boolean idle) {
        this.idle = idle;
    }

    public List<CityRow> getCities() {
        return cities;
    }

    public void setCities(List<CityRow> cities) {
        this.cities = cities;
    }

    public int getTotalSent() {
        return totalSent;
    }

    public void setTotalSent(int totalSent) {
        this.totalSent = totalSent;
    }

    public int getTotalPending() {
        return totalPending;
    }

    public void setTotalPending(int totalPending) {
        this.totalPending = totalPending;
    }

    public int getTotalSuspended() {
        return totalSuspended;
    }

    public void setTotalSuspended(int totalSuspended) {
        this.totalSuspended = totalSuspended;
    }
}
