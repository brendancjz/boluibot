package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.YearMonth;


public class GenFinInlineKeyboardEvent extends Event{
    private final EditMessageText editMessage;
    private YearMonth targetYM;

    public GenFinInlineKeyboardEvent(SendMessage message, EditMessageText newMessage, ArrayList<String> errorlogs, int chatId, String callData) throws URISyntaxException, SQLException {
        super(message,errorlogs, chatId);
        this.editMessage = newMessage;
        setTargetYearMonth(callData);
    }

    @Override
    public void generateEvent() throws SQLException, URISyntaxException {
        //Displaying financials for selected month
        String monthFinancials = super.getPSQL().getMonthFinancials(super.getChatId(), this.targetYM);
        //The goal is to move information from DB to the unique keyboard that is generated. Because relying on DB can only provide one information.
        this.editMessage.setText(monthFinancials);  
        super.getErrorLogs().add(monthFinancials);
        this.editMessage.setReplyMarkup(Events.GetInlineKeyboardMarkup.financeKB(this.targetYM.minusMonths(1), this.targetYM, this.targetYM.plusMonths(1)));
    }

    public void setTargetYearMonth(String callData){
        //Moving from accessing DB (only one set of data) to accessing info from the unique keyboard generated
        //example of callData: fin_yyyy_MM, fin_refresh_yyyy_MM
        String[] cdArray = callData.split("_");
        //To navigate to current month's financials
        if (cdArray[1].equals("revert")){ 
            this.targetYM = YearMonth.of(LocalDate.now().getYear(), LocalDate.now().getMonth());
        }
        // To refresh current financials (perhaps after adding new entry) 
        else if (cdArray[1].equals("refresh")){
            this.targetYM = YearMonth.of(Integer.parseInt(cdArray[2]), Integer.parseInt(cdArray[3])); 
        } else {
            this.targetYM = YearMonth.of(Integer.parseInt(cdArray[1]) , Integer.parseInt(cdArray[2]));
        }
    }

}