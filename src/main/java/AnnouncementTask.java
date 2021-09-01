import java.util.TimerTask;

public class AnnouncementTask extends TimerTask {

    @Override
    public void run() {
        System.out.println("AnnouncementTask ran...");
        AnnouncementBot bot = new AnnouncementBot();
        bot.run();
    }
}