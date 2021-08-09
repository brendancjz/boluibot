package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;

public class GenShortcutHelpEvent extends Event{

    public GenShortcutHelpEvent(SendMessage message, ArrayList<String> errorlogs, int chatId) throws URISyntaxException, SQLException {
        super(message, errorlogs, chatId);
    }

    @Override
    public void generateEvent() throws SQLException {
        super.getMessage().setText(Prompts.generateShortcutHelpPrompt());
    }
}