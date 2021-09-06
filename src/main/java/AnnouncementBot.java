import Events.PSQL;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;

public class AnnouncementBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "bo_lui_bot";
    }

    @Override
    public String getBotToken() {
        return "1763203814:AAFbCkdniUC5EJpiLMb3Uq5GUIP7xj60Mpw";
    }

    @Override
    public void onUpdateReceived(Update update) {
    }

    public void run() {
        try {
            PSQL psql = new PSQL();
            ArrayList<String> chatIds = psql.getAllChatId();

            for (String chatId : chatIds) {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(announcement());
                message.enableHtml(true);

                execute(message);
            }

            psql.closeConnection();
        } catch (URISyntaxException | SQLException | TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String announcement() {
        return "[Tip] Hi there! Bo would like to give you a tip when inputting an entry. " +
                "To input an amount, you can use either the numbers on your keyboard or the given number pad. " +
                "\n\nBought anything today? Type /spend to get started. <code>-Bo</code>";
    }

    //"Hello there! Thank you for using me as your personal finance tracking bot. It's a new month! Did you spend on anything today? Type /spend to get started. <code>-Bo</code>"
}
