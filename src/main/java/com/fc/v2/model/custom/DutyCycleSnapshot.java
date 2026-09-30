package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * 一轮「周期执行」跑完的局面。看板只认服务回传的这一屏，页面不预写死；
 * 没有条子到期的日子照常产出本对象（idle=true 即「本轮无事」），不当故障。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class DutyCycleSnapshot implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 本轮开跑时刻（墙钟串 yyyy-MM-dd HH:mm:ss） */
    private String runAt;
    /** 本轮是否落在对外发送窗口（八时半至十七时半）内；窗外条子不动，留到下一轮 */
    private boolean inWindow;
    /** 本轮无事：窗口内没有任何一条该开口的待提醒条目（年初常见，不是故障） */
    private boolean idle;
    /** 上一轮尚未跑完，本轮直接跳过（防止同一件事被并发推两遍） */
    private boolean concurrentSkip;

    /** 本轮实际发出条数 */
    private int sentThisRound;
    /** 本轮新挂起条数（归口空着等） */
    private int suspendedThisRound;

    /** 系统内全部未删条目截至本轮的局面：已发 / 待发 / 挂起 */
    private int totalSent;
    private int totalWaiting;
    private int totalSuspended;

    /** 本轮逐条明细（看板下的台账） */
    private List<Item> items = new ArrayList<>();
    /** 各地市汇总（柱状铺开，哪个市压得多一眼看得见） */
    private List<CityCount> cities = new ArrayList<>();

    /** 一条提醒条目在本轮局面里的样子 */
    public static class Item implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        /** 提醒单号 */
        private String itemNo;
        /** 归口地市 */
        private String cityName;
        /** 归口联络员 */
        private String liaison;
        /** 日终截止时刻 */
        private String dueAt;
        /** 处里定的提前天数 */
        private String leadDays;
        /** 全处唯一一把尺算出的还剩几日（负数为已过期） */
        private long daysLeft;
        /** 情形 0待提醒 1已提醒 2已挂起 */
        private int status;
        /** 挂起缘故 */
        private String suspendReason;
        /** 实际发出时刻 */
        private String sentAt;
        /** 本轮该条走到哪一步：SENT 发出 / SUSPENDED 挂起 / WAITING 未到开口或窗外留轮 / ALREADY 已发不再挑 */
        private String action;

        public String getItemNo() { return itemNo; }
        public void setItemNo(String itemNo) { this.itemNo = itemNo; }
        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }
        public String getLiaison() { return liaison; }
        public void setLiaison(String liaison) { this.liaison = liaison; }
        public String getDueAt() { return dueAt; }
        public void setDueAt(String dueAt) { this.dueAt = dueAt; }
        public String getLeadDays() { return leadDays; }
        public void setLeadDays(String leadDays) { this.leadDays = leadDays; }
        public long getDaysLeft() { return daysLeft; }
        public void setDaysLeft(long daysLeft) { this.daysLeft = daysLeft; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getSuspendReason() { return suspendReason; }
        public void setSuspendReason(String suspendReason) { this.suspendReason = suspendReason; }
        public String getSentAt() { return sentAt; }
        public void setSentAt(String sentAt) { this.sentAt = sentAt; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }

    /** 一个地市在本轮的柱 */
    public static class CityCount implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private String cityCode;
        private String cityName;
        private int sent;
        private int waiting;
        private int suspended;

        public CityCount() {
        }

        public CityCount(String cityCode, String cityName) {
            this.cityCode = cityCode;
            this.cityName = cityName;
        }

        public String getCityCode() { return cityCode; }
        public void setCityCode(String cityCode) { this.cityCode = cityCode; }
        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }
        public int getSent() { return sent; }
        public void setSent(int sent) { this.sent = sent; }
        public int getWaiting() { return waiting; }
        public void setWaiting(int waiting) { this.waiting = waiting; }
        public int getSuspended() { return suspended; }
        public void setSuspended(int suspended) { this.suspended = suspended; }
    }

    public String getRunAt() { return runAt; }
    public void setRunAt(String runAt) { this.runAt = runAt; }
    public boolean isInWindow() { return inWindow; }
    public void setInWindow(boolean inWindow) { this.inWindow = inWindow; }
    public boolean isIdle() { return idle; }
    public void setIdle(boolean idle) { this.idle = idle; }
    public boolean isConcurrentSkip() { return concurrentSkip; }
    public void setConcurrentSkip(boolean concurrentSkip) { this.concurrentSkip = concurrentSkip; }
    public int getSentThisRound() { return sentThisRound; }
    public void setSentThisRound(int sentThisRound) { this.sentThisRound = sentThisRound; }
    public int getSuspendedThisRound() { return suspendedThisRound; }
    public void setSuspendedThisRound(int suspendedThisRound) { this.suspendedThisRound = suspendedThisRound; }
    public int getTotalSent() { return totalSent; }
    public void setTotalSent(int totalSent) { this.totalSent = totalSent; }
    public int getTotalWaiting() { return totalWaiting; }
    public void setTotalWaiting(int totalWaiting) { this.totalWaiting = totalWaiting; }
    public int getTotalSuspended() { return totalSuspended; }
    public void setTotalSuspended(int totalSuspended) { this.totalSuspended = totalSuspended; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public List<CityCount> getCities() { return cities; }
    public void setCities(List<CityCount> cities) { this.cities = cities; }
}
