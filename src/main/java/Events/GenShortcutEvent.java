package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;

public class GenShortcutEvent extends Event {

    public GenShortcutEvent(SendMessage message, ArrayList<String> errorlogs, int chatId) throws URISyntaxException, SQLException {
        super(message, errorlogs, chatId);
    }

    @Override
    public void generateEvent() throws SQLException {
        int chatId = super.getChatId();
        String text = super.getPSQL().getUserText(chatId);
        PSQL psql = super.getPSQL();
        SendMessage message = super.getMessage();
        ArrayList<String> errorLogs = super.getErrorLogs();

        //Text is confirmed to have a /.
        String[] textArr = text.split(" ");

        if (textArr[0].equals("/s") || textArr[0].equals("/e")) {
            //Imagine this is the Events.Event 1
            if ((textArr[0].equals("/s"))) {
                psql.updateUserEntryType(chatId, "/spend");
            } else {
                psql.updateUserEntryType(chatId, "/earn");
            }

            if (textArr.length < 4) { //Catch invalid inputs length
                errorLogs.add("GENSHORTCUTEVENT Length less than 4");
                generateErrorMsgAndReturn(textArr);
                return;
            }

            psql.updateUserEventState(chatId, 1);
            psql.updateIsUserInputting(chatId, false); //Will change to True

            //Now this is Events.Event 2
            psql.addEntryListItem(chatId, textArr[1], 2);
            psql.updateUserEventState(chatId, 2);

            //Now this is Events.Event 3
            String cost = removingDollarSign(textArr[2]);
            boolean isNum = isNumericAndPositive(cost);

            //SQL Queries
            psql.addEntryListItem(chatId, cost, 3);

            if (isNumericAndPositive(cost) && validateCostInput(cost)) {
                //SQL Queries
                psql.updateUserEventState(chatId, 3);
            } else {
                errorLogs.add("GENSHORTCUTEVENT isNum is False");

                generateErrorMsgAndReturn(textArr);
                return;
            }

            //Now this is Events.Event 4
            //Spend shortcut: /s cate cost comment
            String comment = "";
            for (int i = 3; i < textArr.length; i++) {
                comment += textArr[i];
                if (i != (textArr.length - 1)) {
                    comment += " ";
                }
            }
            psql.addEntryListItem(chatId, comment, 4);
            psql.updateUserEventState(chatId, 4); //Change Events.Event State 4 to 5
            psql.updateEntries(chatId);

            String[] entryListArr = {textArr[1], textArr[2], comment};

            errorLogs.add("GENSHORTCUTEVENT reached to the end of generateEvent");
            message.setText("Reminder: your input should be as follows: \n[command] [category] [cost/earning] [comment]");
        }
    }

    private void generateErrorMsgAndReturn(String[] textArr) throws SQLException {
        int chatId = super.getChatId();
        SendMessage message = super.getMessage();

        resetSystemToEventStateOne(chatId, true); //Cancel Entry

        //Reminder for user
        String reminderText = "Reminder: your input should be as follows: \n[command] [category] [cost/earning] [comment]\n\n";

        if (textArr.length == 2) {
            message.setText(reminderText + "Oops, that doesn't seem right. Let's try another. " +
                    "Perhaps you have not inputted cost/earning and comment. Ensure that cost/earning is numeric and absolute.");
        } else if (textArr.length >= 3 && (!isNumericAndPositive(textArr[2]) || !validateCostInput(textArr[2]))) {
            message.setText(reminderText + "Oops, that doesn't seem right. Let's try another. " +
                    "Perhaps your category input has more than one word. Or perhaps, your cost input is not numeric or is negative or more than 2d.p.");
        } else if (textArr.length == 3) {
            message.setText(reminderText + "Oops, that doesn't seem right. Let's try another. " +
                    "Perhaps your category input has more than one word. Or perhaps, your cost input is not numeric or is negative. " +
                    "Or perhaps, you did not write a comment. If no comment, enter 'NA'.");
        }
    }

    @Override
    public void generateOtherEvents() throws SQLException, URISyntaxException {
        SendMessage message = super.getMessage();
        int chatId = super.getChatId();
        Events.PSQL psql = super.getPSQL();
        ArrayList<String> errorLogs = super.getErrorLogs();

        String entryType = psql.getUserEntryType(chatId);
        Events.Event event = null;
        switch (entryType) {
            case "spend":
                event = new Events.SpendEvent(message, errorLogs, chatId);
                break;
            case "earn":
                event = new Events.EarnEvent(message, errorLogs, chatId);
                break;
            case "edit":
                event = new Events.EditEvent(message, errorLogs, chatId);
                break;
            case "delete":
                event = new Events.DeleteEvent(message, errorLogs, chatId);
                break;
        }
        assert event != null;
        event.updateDatabase();
        event.generateEvent();
        resetSystemToEventStateOne(chatId, true); //Will reset no matter what.
        errorLogs.add("GENSHORTCUTEVENT reached to the start of generateOtherEvent");
    }
}