package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

public class StartEvent extends Event{
    private int targetYear;
    private int targetMonth;
    private final String name;

    public StartEvent(SendMessage message, ArrayList<String> errorlogs, int chatId, String name) throws URISyntaxException, SQLException {
        super(message, errorlogs, chatId);
        LocalDate dateToday = LocalDate.now();
        this.targetYear = dateToday.getYear();
        this.targetMonth = dateToday.getMonthValue();
        this.name = name;
    }

    @Override
    public void updateDatabase() throws SQLException {
        super.getPSQL().addNewUser(super.getChatId(),
                super.getPSQL().getUserText(super.getChatId()), targetYear, targetMonth);
    }

    @Override
    public void generateEvent() throws SQLException {
        super.getErrorLogs().add("=== Start Events.Event Called === ");
        super.getMessage().setText(Prompts.generateIntro(name));
    }
}
