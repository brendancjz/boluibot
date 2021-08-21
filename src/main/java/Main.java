import Events.PSQL;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Timer;
import java.util.Date;

public class Main {

    public static void main(String[] args) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            BoLuiBot boLuiBot = new BoLuiBot();
            telegramBotsApi.registerBot(boLuiBot); //botSession has started

            //setSummaryTimer();

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    private static void setSummaryTimer() {
        Timer timer = new Timer();
        SummaryTask task = new SummaryTask();

        long period = 1000L * 60 * 60 * 24; //Daily msg at 7pm
        timer.schedule(task, getDateForScheduling(), period);
    }

    private static Date getDateForScheduling() {
        //Always send msg on a friday 7pm
        //Theres a time difference in the time diff and SG time of 8 hours
        int hour = 11 + 8; //Equiv to 19hours
        int min = 0;
        int sec = 0;

        LocalDateTime dateNow = LocalDateTime.now();
        int yearNow = dateNow.getYear();
        int monthNow = dateNow.getMonthValue();
        int dayOfMonthNow = dateNow.getDayOfMonth();
        int hourNow = dateNow.getHour() + 8;
        int minNow = dateNow.getMinute();
        int secNow = dateNow.getSecond();

        //If current time is before hour = 11, it will wait till 7pm. But if it is past 7pm, it will sent msg right away and the timer starts then
        //So, we need a condition to check and always wait till 7pm to send the msg.
        System.out.println("dayOfMonthNow: " + dayOfMonthNow + " hourNow: " + hourNow + " minNow: " + minNow + " secNow: " + secNow);

        LocalDateTime startOnFriday;
        if (hourNow >= hour) { //Past 7pm
            startOnFriday = LocalDateTime.of(yearNow, monthNow, dayOfMonthNow, hour - 8, min, sec).plusDays(1); //The next day
        } else { //Before 7pm
            startOnFriday = LocalDateTime.of(yearNow, monthNow, dayOfMonthNow, hour - 8, min, sec);
        }

        return Date.from(startOnFriday.atZone(ZoneId.systemDefault()).toInstant());
    }

}

class SummaryTask extends TimerTask {

    @Override
    public void run() {
        System.out.println("SummaryTask is running...");
        SummaryBot bot = new SummaryBot();
        bot.run();

    }
}

class SummaryBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "bo_lui_test_bot";
    }

    @Override
    public String getBotToken() {
        return "1940879720:AAEC1mYkoQS-lHhOlgYy__bagB9PHbd8SNQ";
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Inside Bot class. This means that two update receives are called.");
    }

    public void run() {
        try {
            PSQL psql = new PSQL();
            ArrayList<String> chatIds = psql.getAllChatId();

            SendMessage message = new SendMessage();
            message.setChatId("" + 107270014);
            //message.setText(chatIds.toString());
            message.setText("Update: Made some bug fixes with the /finance. ");

            execute(message);
            psql.closeConnection();
        } catch (URISyntaxException | SQLException | TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

