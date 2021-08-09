package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;

public class CancelEvent extends Event{
    private final boolean CANCEL_INPUTTING = true;

    public CancelEvent(SendMessage message, ArrayList<String> errorlogs, int chatId) throws URISyntaxException, SQLException {
        super(message, errorlogs, chatId);
    }

    @Override
    public void updateDatabase() throws SQLException {
        resetSystemToEventStateOne(super.getChatId(), CANCEL_INPUTTING);
    }

    @Override
    public void generateEvent() throws SQLException {
        super.getMessage().setText(Prompts.generateCancelPrompt());
    }
}