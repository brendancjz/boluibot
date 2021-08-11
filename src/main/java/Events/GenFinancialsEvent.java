package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;

public class GenFinancialsEvent extends Event{
    private YearMonth targetYM;

    public GenFinancialsEvent(SendMessage message, PSQL psql, int chatId) throws URISyntaxException, SQLException {
        super(message, psql, chatId);
        LocalDate dateToday = LocalDate.now();
        this.targetYM = YearMonth.of(dateToday.getYear(), dateToday.getMonthValue());
    }

    @Override
    public void generateEvent() throws SQLException {
        String monthFinancials = super.getPSQL().getMonthFinancials(super.getChatId(), getYearMonth());
        //The goal is to move information from DB to the unique keyboard that is generated. Because relying on DB can only provide one information.
        super.getMessage().setText(monthFinancials);  
        super.getMessage().setReplyMarkup(KeyboardMarkups.financeKB(getYearMonth().minusMonths(1), getYearMonth(), getYearMonth().plusMonths(1)));
    }

    public YearMonth getYearMonth(){
        return this.targetYM;
    }

}