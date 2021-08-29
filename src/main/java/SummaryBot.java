import Events.PSQL;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;

public class SummaryBot extends TelegramLongPollingBot {

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
    }

    public void runDailySummary() {
        try {
            PSQL psql = new PSQL();
            ArrayList<String> chatIds = psql.getAllChatId();

            SendMessage message = new SendMessage();
            message.setChatId("" + 107270014);
            //message.setText(chatIds.toString());
            message.setText("Update.");

            execute(message);
            psql.closeConnection();
        } catch (URISyntaxException | SQLException | TelegramApiException e) {
            e.printStackTrace();
        }
    }
}