import java.util.TimerTask;

public class SummaryTask extends TimerTask {

    @Override
    public void run() {
        System.out.println("SummaryTask is running...");
        SummaryBot bot = new SummaryBot();
        bot.runDailySummary();
    }
}