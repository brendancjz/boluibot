package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Event {
    private static final int FINAL_EVENT_STATE = 5;
    private static final String INITIAL_ENTRY_COMMAND = "/null";
    private final SendMessage message;
    private final PSQL psql;
    private final int chatId;


    protected Event(SendMessage message, PSQL psql, int chatId) throws URISyntaxException, SQLException {
        this.message = message;
        this.psql = psql;
        this.chatId = chatId;

    }

    public SendMessage getMessage() {
        return this.message;
    }

    public PSQL getPSQL() {
        return this.psql;
    }

    public int getChatId() {
        return this.chatId;
    }

    public void updateDatabase() throws SQLException { }

    public void generateEvent() throws SQLException, URISyntaxException { }

    public void generateOtherEvents() throws SQLException, URISyntaxException { }

    /**
     * This method removes the dollar sign, if have
     * @param text String variable that stores the text of the user.
     * @return Returns the string variable of text without the dollar sign
     */
    public String removingDollarSign(String text) {
        if (text.charAt(0) == '$') {
            return text.substring(1);
        } else {
            return text;
        }
    }

    /**
     * This method shows if the string inputted is numeric
     * @param strNum String variable that stores the text of the user.
     * @return Returns a boolean to show the text is numeric
     */
    public boolean isNumericAndPositive(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
            if (d <= 0) {
                return false;
            }
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public void resetSystemToEventStateOne(int chatId, boolean isInputtingEntry) throws SQLException {
        //SQL Queries
        psql.updateUserEventState(chatId, FINAL_EVENT_STATE);
        psql.updateIsUserInputting(chatId, isInputtingEntry);
        psql.updateUserEntryType(chatId, INITIAL_ENTRY_COMMAND);
        psql.resetEntryList(chatId);
        removeReplyKeyboardFromMessage(message);
    }

    private void removeReplyKeyboardFromMessage(SendMessage message) {
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        message.setReplyMarkup(remove);
    }
    public boolean validateCategoryInput(String category) {
        return !category.contains(" ");
    }
    public boolean validateCostInput(String cost) {
        return !cost.contains(".") || (cost.contains(".") && (cost.length() - 1 - cost.indexOf(".")) <= 2);
    }


}
