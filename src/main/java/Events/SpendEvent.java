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

    public SpendEvent(SendMessage message, PSQL psql, int chatId) throws URISyntaxException, SQLException {
        super(message, psql, chatId);
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
                System.out.println(" === Events.Event State One Called === ");
                prompt = Prompts.generateEventOneSpendPrompt();
                message.setText(prompt);
                message.setReplyMarkup(KeyboardMarkups.getSpendReplyKeyboardMarkup());
                break;
            case 2: //Getting Cost. It has gotten Command and Category
                System.out.println(" === Events.Event State Two Called === ");

                if (validateCategoryInput(entryList[0])) {
                    prompt = Prompts.generateEventTwoSpendPrompt(entryList[0]); //Category
                    message.setText(prompt);
                    message.setReplyMarkup(KeyboardMarkups.numpadKB());
                } else {
                    psql.updateUserEventState(chatId, 1); //Decrement Events.Event State.

                    message.setText(Prompts.generateInputtingCategoryErrorPrompt());
                }

                break;
            case 3: //Getting Comment or Cost again. It has gotten Command and Category and Cost
                System.out.println(" === Events.Event State Three Called === ");
                String cost = removingDollarSign(psql.getUsersEntryList(chatId)[1]);
                if (isNumericAndPositive(cost) && validateCostInput(cost)) {
                    prompt = Prompts.generateEventThreeSpendPrompt(cost);
                    message.setText(prompt);
                } else {
                    psql.updateUserEventState(chatId, 2); //Decrement Events.Event State.

                    System.out.println("Cost inputting is not numeric or negative value!");
                    message.setText(Prompts.generateInputtingCostErrorPrompt());
                }
                break;
            case 4: //Output entry and reset
                System.out.println(" === Events.Event State Four Called === ");
                message.setText("Thanks! You have added a new entry: \nSpent $" + entryList[1] +
                        " on " + entryList[0] + " - \"" + entryList[2] + "\"");
                resetSystemToEventStateOne(chatId, true); //Reset System
                break;
        }
    }
}
