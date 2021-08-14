package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

public class StartEvent extends Event{
    private final int targetYear;
    private final int targetMonth;
    private final String name;
    private boolean userAlreadyExists = false;

    public StartEvent(SendMessage message, PSQL psql, int chatId, String name) throws URISyntaxException, SQLException {
        super(message, psql, chatId);
        LocalDate dateToday = LocalDate.now();
        this.targetYear = dateToday.getYear();
        this.targetMonth = dateToday.getMonthValue();
        this.name = name;
    }

    @Override
    public void updateDatabase() throws SQLException {
        userAlreadyExists = super.getPSQL().isUserRegistered(super.getChatId());
        super.getPSQL().addNewUser(super.getChatId(), super.getPSQL().getUserText(super.getChatId()), targetYear, targetMonth);

    }

    @Override
    public void generateEvent() throws SQLException {
        if (!userAlreadyExists) {
            super.getMessage().setText(Prompts.generateIntro(name));
        } else {
            super.getMessage().setText(Prompts.generateIntro(name) + " <em>It looks like you are already registered in the database!</em>");
        }

    }
}
