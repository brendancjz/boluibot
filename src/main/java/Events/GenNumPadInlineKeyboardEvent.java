package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;

public class GenNumPadInlineKeyboardEvent extends Event{
    private final EditMessageText newMessage;
    private final String callData;
    private final String prevAnswer;

    public GenNumPadInlineKeyboardEvent(SendMessage message, ArrayList<String> errorlogs, int chatId,
                                        String callData, String prevAnswer, EditMessageText newMessage) throws URISyntaxException, SQLException {
        super(message, errorlogs, chatId);
        this.newMessage = newMessage;
        this.callData = callData;
        this.prevAnswer = prevAnswer;
    }

    @Override
    public void generateEvent() throws SQLException, URISyntaxException {
        boolean below2DP = prevAnswer.contains(".") && (prevAnswer.length() - 1 - prevAnswer.indexOf(".")) < 2;
        boolean already2DP = prevAnswer.contains(".") && (prevAnswer.length() - 1 - prevAnswer.indexOf(".")) == 2;

        String answer = prevAnswer;
        super.getErrorLogs().add("ADDING " + callData.substring(7));
        int chatId = super.getChatId();
        PSQL psql = super.getPSQL();
        SendMessage message = super.getMessage();
        ArrayList<String> errorLogs = super.getErrorLogs();

        //Verifying
        if (prevAnswer.contains("?") || prevAnswer.equals("Input: $")) answer = "Input: $"; //Remove the prompt and replace with $ sign

        if (callData.equals("numpad_done")) {
            super.getErrorLogs().add(prevAnswer.substring(1) + " FINAL VALUE");

            //Events.Event state 3 now
            //Update Database
            psql.addEntryListItem(chatId, prevAnswer.substring(8), 3); //Remove Input: $
            psql.updateUserEventState(chatId, 3);

            String entryType = psql.getUserEntryType(chatId);
            Event event = null;
            switch (entryType) {
                case "spend":
                    event = new SpendEvent(message, errorLogs, chatId);
                    break;
                case "earn":
                    event = new EarnEvent(message, errorLogs, chatId);
                    break;
                case "edit":
                    event = new EditEvent(message, errorLogs, chatId);
                    break;
                case "delete":
                    event = new DeleteEvent(message, errorLogs, chatId);
                    break;
            }
            assert event != null;
            event.generateEvent();

            newMessage.setText(answer);
            return;
        }
        else if (callData.equals("numpad_del")) {
            answer = prevAnswer.substring(0, prevAnswer.length() - 1);

        }
        else if (already2DP) {
            message.setText("Error, exceeding 2 decimal places.");
        } else if (callData.equals("numpad_dot")) {
            if (!prevAnswer.contains(".")) {
                answer += ".";
            } else {
                message.setText("Error, only one decimal input is allowed.");
            }
        } else if (callData.startsWith("numpad") && (!prevAnswer.contains(".") || below2DP)){ //Numbers
            answer += callData.substring(7);

        }

        newMessage.setReplyMarkup(Events.GetInlineKeyboardMarkup.numpadKB());
        newMessage.setText(answer);
    }
}