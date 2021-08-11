package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;

public class ErrorLogsEvent extends Event{

    public ErrorLogsEvent(SendMessage message, PSQL psql, int chatId) throws URISyntaxException, SQLException {
        super(message, psql, chatId);
    }

    @Override
    public void generateEvent() throws SQLException {

    }
}