import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Timer;

public class AnnouncementTimer {
    private final int scheduledTime;
    private final Timer timer;
    private final AnnouncementTask task;

    AnnouncementTimer(int scheduledTime) {
        this.timer = new Timer();
        this.task = new AnnouncementTask();
        this.scheduledTime = scheduledTime;
    }

    public void start() {
        System.out.println("Announcement has been scheduled.");
        timer.schedule(task, getScheduledDate(this.scheduledTime));
    }

    private long getPeriod(String typeOfTimer) {
        switch (typeOfTimer) {
            case "hourly":
                return 1000L * 60 * 60; //Hourly msg
            case "daily":
                return 1000L * 60 * 60 * 24; //Daily msg
            case "weekly":
                return 1000L * 60 * 60 * 24 * 7; //Weekly msg
            case "fortnightly":
                return 1000L * 60 * 60 * 24 * 14; //Fortnightly msg
            case "monthly":
                return 1000L * 60 * 60 * 24 * 30; //Monthly msg [Estimated]
        }

        return 0;
    }

    private static Date getScheduledDate(int scheduledTime) {
        //Theres a time difference in the time diff and SG time of 8 hours
        int hour = (scheduledTime / 100) - 8; //Equiv to 19hours
        int min = scheduledTime % 100;
        int sec = 0;


        //If current time is before hour = 11, it will wait till 7pm. But if it is past 7pm, it will sent msg right away and the timer starts then
        //So, we need a condition to check and always wait till 7pm to send the msg.
        LocalDateTime dateNow = LocalDateTime.now();
        int yearNow = dateNow.getYear();
        int monthNow = dateNow.getMonthValue();
        int dayOfMonthNow = dateNow.getDayOfMonth();
        int hourNow = dateNow.getHour();
        int minNow = dateNow.getMinute();
        int secNow = dateNow.getSecond();
        System.out.println("dayOfMonthNow: " + dayOfMonthNow + " hourNow: " + ((hourNow + 8) % 24) + " minNow: " + minNow + " secNow: " + secNow);

        LocalDateTime startDate;
        if (isBeforeScheduledTime(hour, min, sec, hourNow, minNow, secNow)) { //Before timing
            startDate = LocalDateTime.of(yearNow, monthNow, dayOfMonthNow, hour, min, sec);
            System.out.println("It has not passed the timing. Setting timer for later: " + startDate.toString());
        } else { //After timing
            startDate = LocalDateTime.of(yearNow, monthNow, dayOfMonthNow, hourNow, min, sec).plusHours(24 - (hourNow - hour));
            System.out.println("It has passed the timing. Setting timer for the next interval: " + startDate.toString());
        }

        return Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static boolean isBeforeScheduledTime(int scheduledHour, int scheduledMin, int scheduledSec, int hourNow, int minNow, int secNow) {
        return hourNow < scheduledHour || minNow < scheduledMin || secNow < scheduledSec;
    }
}
