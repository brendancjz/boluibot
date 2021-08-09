package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;

public class ErrorLogsEvent extends Event{

    public ErrorLogsEvent(SendMessage message, ArrayList<String> errorlogs, int chatId) throws URISyntaxException, SQLException {
        super(message, errorlogs, chatId);
    }

    @Override
    public void generateEvent() throws SQLException {
        String log = "------ Generating Error Logs Program Code ------ \n";

        ArrayList<String> SQLerrorLogs = super.getPSQL().getErrorLogs();
        for (String error : super.getErrorLogs()) {
            log += error + "\n";
        }

        log += "------ Generating Error Logs SQL Queries ------ \n";

        for (String error : SQLerrorLogs) {
            log += error + "\n";
        }
        super.getMessage().setText(log);
        super.getErrorLogs().clear();
    }
}