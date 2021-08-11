package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class EditEvent extends Event{

    public EditEvent(SendMessage message, PSQL psql, int chatId) throws URISyntaxException, SQLException {
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
            case 3:
                psql.addEntryListItem(chatId, text, currEventState);
                psql.updateUserEventState(chatId, currEventState); // Change Events.Event State 2 to 3 or 3 to 4
                break;
        }

    }

    @Override
    public void generateEvent() throws SQLException, URISyntaxException {
        int chatId = super.getChatId();
        int currEventState = super.getPSQL().getUserEventState(chatId);
        PSQL psql = super.getPSQL();
        String prompt;
        SendMessage message = super.getMessage();
        String[] entryList = psql.getUsersEntryList(chatId);

        switch (currEventState - 1) { //Very important
            case 1:
                System.out.println(" === Events.Event State One Called === ");
                int numEntries = psql.getUserEntryCount(chatId);
                if (numEntries > 0){
                    message.setText(Prompts.generateEventOneEditPrompt());
                } else {
                    message.setText(Prompts.generateNoEntriesToEditPrompt());
                    resetSystemToEventStateOne(chatId, true);
                }
                break;
            case 2:
                System.out.println("========= Events.Event State Two Called ========= ");
                    if (isNumericAndPositive(entryList[0]) && psql.checkEntryCountRange(chatId, Integer.parseInt(entryList[0]))) {
                        message.setText(Prompts.generateEventTwoEditPrompt());
                    } else {
                        psql.updateUserEventState(chatId, 1); //Decrement Events.Event State
                        message.setText(Prompts.generateInputtingEntryNumErrorPrompt(entryList[0]));
                    }
                break;
            case 3:
                System.out.println("========= Events.Event State Three Called ========= ");
                String text = super.getPSQL().getUserText(chatId);

                System.out.println(text);

                boolean isGood = validateEditedEntry(text);
                boolean isDeleteInstead = isDeleteEntryInstead(text);
                System.out.println("validate Edited Entry Good? " + isGood);
                if (isDeleteInstead) {
                    Event delEvent = new DeleteEvent(message, super.getPSQL(), chatId);
                    delEvent.generateEvent();
                } else if (isGood) { 
                    ArrayList<String> editEntry = getEditedEntryList(text);
                    if (!editEntry.isEmpty()) {
                        editEntry.add(entryList[0]); //ADDING Entry Number of this entry.
                        System.out.println("edit entry: " + editEntry.toString());

                        //getEditedEntry returns a string: display edited from ___ to XXX
                        entryList[1] = psql.getEditedEntry(chatId, editEntry);
                        resetSystemToEventStateOne(chatId, true);

                        message.setText(entryList[1]);
                    }
                } else {
                    //REPEAT EVENT STATE TWO
                    psql.updateUserEventState(chatId, 2); //Decrement Events.Event State

                    System.out.println("Edit Entry List has issues!");
                    message.setText(Prompts.generateEventThreeEditErrorPrompt());
                }
                break;
        }
    }

    @Override
    public void generateOtherEvents() throws SQLException, URISyntaxException {
        int chatId = super.getChatId();
        int currEventState = super.getPSQL().getUserEventState(chatId);
        SendMessage message = super.getMessage();
        PSQL psql = super.getPSQL();
        String[] entryList = psql.getUsersEntryList(chatId);

        switch (currEventState - 1) { //Very important
            case 1:
                GenEntriesEvent genEntriesEvent = new GenEntriesEvent(super.getMessage(), super.getPSQL(), super.getChatId());
                genEntriesEvent.genMonthPlainEntries();
                break;
            case 2:
                message.setText(String.join(" : ", psql.getSpecificEntry(chatId,Integer.parseInt(entryList[0]))));
            case 3:
                break;
        }
    }

    private ArrayList<String> getEditedEntryList(String text){
        String[] inputText = text.split(":", 4);
        ArrayList<String> inputTextArray = new ArrayList<>(Arrays.asList(inputText));

        //Making sure inputs have no space
        inputTextArray.set(0, inputTextArray.get(0).replaceAll(" ", ""));
        inputTextArray.set(1, inputTextArray.get(1).replaceAll(" ", ""));
        inputTextArray.set(2, inputTextArray.get(2).replaceAll(" ", ""));

        return inputTextArray;
    }

    private boolean validateEditedEntry(String text) {
        String[] inputText = text.split(" : " , 4);

        return  isNumericAndPositive(removingDollarSign(inputText[2])) && validateCostInput(removingDollarSign(inputText[2]))
                && (inputText[0].equals("spend") || inputText[0].equals("earn"));
    }

    private boolean isDeleteEntryInstead(String text){
        String[] inputText = text.split(" : ", 4);

        return Double.parseDouble(removingDollarSign(inputText[2])) == 0;
    }
}