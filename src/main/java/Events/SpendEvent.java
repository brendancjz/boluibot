package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SpendEvent extends Event{

    public SpendEvent(SendMessage message, ArrayList<String> errorlogs, int chatId) throws URISyntaxException, SQLException {
        super(message, errorlogs, chatId);
    }

    @Override
    public void updateDatabase() throws SQLException {
        int chatId = super.getChatId();
        int currEventState = super.getPSQL().getUserEventState(chatId);
        String text = super.getPSQL().getUserText(chatId);
        PSQL psql = super.getPSQL();

        switch (currEventState) {
            case 1:
                psql.updateUserEntryType(chatId, text); //Update entry type
                psql.updateUserEventState(chatId, currEventState); //Change Events.Event State 1 to 2
                psql.updateIsUserInputting(chatId, false); //Change is inputting to True
                break;
            case 2:
                psql.addEntryListItem(chatId, text, currEventState); //Add Category into Entry List
                psql.updateUserEventState(chatId, currEventState); //Change Events.Event State 2 to 3
                break;
            case 3:
                String cost = removingDollarSign(text).toLowerCase();
                psql.addEntryListItem(chatId, cost, currEventState); //Add Cost into Entry List
                psql.updateUserEventState(chatId, currEventState); //Change Events.Event State 3 to 4
                break;
            case 4:
                psql.addEntryListItem(chatId, text, currEventState); //Add Comment into Entry List
                psql.updateUserEventState(chatId, currEventState); //Change Events.Event State 4 to 5
                psql.updateEntries(chatId); //Add entry list into Entries table
                break;
        }

    }

    @Override
    public void generateEvent() throws SQLException {
        int chatId = super.getChatId();
        int currEventState = super.getPSQL().getUserEventState(chatId);
        PSQL psql = super.getPSQL();
        String prompt;
        SendMessage message = super.getMessage();
        String[] entryList = psql.getUsersEntryList(chatId);

        switch (currEventState - 1) { //Very important
            case 1: //Getting Category. It has gotten Command
                super.getErrorLogs().add(" === Events.Event State One Called === ");
                prompt = Prompts.generateEventOneSpendPrompt();
                message.setText(prompt);
                message.setReplyMarkup(getSpendReplyKeyboardMarkup());
                break;
            case 2: //Getting Cost. It has gotten Command and Category
                super.getErrorLogs().add(" === Events.Event State Two Called === ");

                if (validateCategoryInput(entryList[0])) {
                    prompt = Prompts.generateEventTwoSpendPrompt(entryList[0]); //Category
                    message.setText(prompt);
                    message.setReplyMarkup(Events.GetInlineKeyboardMarkup.numpadKB());
                } else {
                    psql.updateUserEventState(chatId, 1); //Decrement Events.Event State.

                    message.setText(Prompts.generateInputtingCategoryErrorPrompt());
                }

                break;
            case 3: //Getting Comment or Cost again. It has gotten Command and Category and Cost
                super.getErrorLogs().add(" === Events.Event State Three Called === ");
                String cost = removingDollarSign(psql.getUsersEntryList(chatId)[1]);
                if (isNumericAndPositive(cost) && validateCostInput(cost)) {
                    prompt = Prompts.generateEventThreeSpendPrompt(cost);
                    message.setText(prompt);
                } else {
                    psql.updateUserEventState(chatId, 2); //Decrement Events.Event State.

                    super.getErrorLogs().add("Cost inputting is not numeric or negative value!");
                    message.setText(Prompts.generateInputtingCostErrorPrompt());
                }
                break;
            case 4: //Output entry and reset
                super.getErrorLogs().add(" === Events.Event State Four Called === ");
                message.setText("Thanks! You have added a new entry: \nSpent $" + entryList[1] +
                        " on " + entryList[0] + " - \"" + entryList[2] + "\"");
                resetSystemToEventStateOne(chatId, true); //Reset System
                break;
        }
    }

    private ReplyKeyboardMarkup getSpendReplyKeyboardMarkup() {
        String[] sCategory = {"Entertainment","Food","Gift","Shopping","Transport", "Utilities"};


        KeyboardRow row = new KeyboardRow();

        List<KeyboardRow> keyboard = new ArrayList<>();
        int count = 1;
        for (String category : sCategory) {
            if (count > 1 && count % 2 == 1) {
                row = new KeyboardRow();
            }

            KeyboardButton button = new KeyboardButton();
            button.setText(category);
            row.add(button);
            if (count % 2 == 0) {
                keyboard.add(row);
            }
            count++;
        }

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);
        return markup;
    }
}
