package Events;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class FeedbackEvent extends Event{

    public FeedbackEvent(SendMessage message, PSQL psql, int chatId) throws URISyntaxException, SQLException {
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
        }

    }

    @Override
    public void generateEvent() throws SQLException {
        int chatId = super.getChatId();
        String prompt;
        int currEventState = super.getPSQL().getUserEventState(chatId);
        PSQL psql = super.getPSQL();
        SendMessage message = super.getMessage();
        String[] entryList = psql.getUsersEntryList(chatId);

        switch (currEventState - 1) { //Very important
            case 1: //Getting Feedback
                System.out.println(" === Events.Event State One Called === ");
                prompt = Prompts.generateEventOneFeedbackPrompt();
                super.getMessage().setText(prompt);
                break;
            case 2: 
                System.out.println(" === Events.Event State Two Called === ");
                message.setText(Prompts.generateEventTwoFeedbackPrompt());
                psql.addNewFeedbackRow(chatId, entryList[0]);
                resetSystemToEventStateOne(chatId, true); //Reset System
                break;
        }
    }
    
}
