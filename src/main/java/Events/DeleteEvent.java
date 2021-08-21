package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;

public class DeleteEvent extends Event{
    private final LocalDate dateToday;
    private YearMonth targetYM;

    public DeleteEvent(SendMessage message, PSQL psql, int chatId) throws URISyntaxException, SQLException {
        super(message, psql, chatId);
        dateToday = LocalDate.now();
        this.targetYM = YearMonth.of(dateToday.getYear(), dateToday.getMonthValue());
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
                psql.addEntryListItem(chatId, text, currEventState); //Getting the Entry Number to delete.
                psql.updateUserEventState(chatId, currEventState); // Change Events.Event State 2 to 3
                break;
            case 3:
                psql.updateUserEventState(chatId, currEventState); // Change Events.Event State 3 to 4
                psql.addEntryListItem(chatId, text, currEventState);
                break;
        }

    }

    @Override
    public void generateEvent() throws SQLException {
        int chatId = super.getChatId();
        int currEventState = super.getPSQL().getUserEventState(chatId);
        PSQL psql = super.getPSQL();
        String prompt = "";
        SendMessage message = super.getMessage();
        String[] entryList = psql.getUsersEntryList(chatId);

        switch (currEventState - 1) { //Very important
            case 1:
                System.out.println(" === Events.Event State One Called === ");

                int numEntries = psql.getUserEntryCount(chatId);
                if (numEntries > 0) {
                    message.setText(Prompts.generateEventOneDeletePrompt());
                } else {
                    message.setText(Prompts.generateNoEntriesToDeletePrompt());
                    resetSystemToEventStateOne(chatId, true);
                }

                break;
            case 2:
                System.out.println("========= Events.Event State Two Called ========= ");
                if (isAllEntryNumValid(entryList[0])) {
                    message.setText(Prompts.generateEventTwoDeletePrompt(entryList[0]));
                    message.setReplyMarkup(KeyboardMarkups.getDelReplyKeyboardMarkup());
                } else {
                    super.getPSQL().updateUserEventState(super.getChatId(), 1); //Decrement Events.Event State
                    super.getMessage().setText(Prompts.generateInputtingEntryNumErrorPrompt(entryList[0]));
                }
                break;
            case 3:
                System.out.println("========= Events.Event State Three Called ========= ");
                int[] delEntryNum = convertStringArrtoIntArr(entryList[0]);
                message.setText(psql.getDeleteEntry(chatId, delEntryNum));
                resetSystemToEventStateOne(chatId, true);
                break;
        }
    }

    @Override
    public void generateOtherEvents() throws SQLException, URISyntaxException {
        int chatId = super.getChatId();
        int currEventState = super.getPSQL().getUserEventState(chatId);
        SendMessage message = super.getMessage();

        switch (currEventState - 1) { //Very important
            case 1:
                GenEntriesEvent genEntriesEvent = new GenEntriesEvent(super.getMessage(), super.getPSQL(), super.getChatId());
                genEntriesEvent.genMonthPlainEntries();
                super.getMessage().setReplyMarkup(KeyboardMarkups.deleteKB(this.targetYM.minusMonths(1), this.targetYM, this.targetYM.plusMonths(1)));
                break;
            case 2:
            case 3:
                break;
        }
    }

    public boolean isAllEntryNumValid(String strNum) throws SQLException{
        System.out.println(strNum + " this is entrynumlist");
        boolean isValidInput = true;

        String[] delNumArr = removeExtraSpace(strNum);

        System.out.println(strNum + " this is updated entrynumlist");

        for (int i = 0; i < delNumArr.length; i ++){
            if (!isNumericAndPositive(delNumArr[i]) || !super.getPSQL().checkEntryCountRange(super.getChatId(), Integer.parseInt(delNumArr[i]))){
                isValidInput = false;
            }
        }
        return isValidInput;
    }

    private String[] removeExtraSpace(String strNum) {
        ArrayList<String> delNumList = new ArrayList<>(Arrays.asList(strNum.split(":")));
        System.out.println(delNumList.toString());

        for (int j = 0; j < delNumList.size(); j++) {
            delNumList.set(j, delNumList.get(j).replaceAll(" ", ""));
            System.out.println(delNumList.get(j));
        }

        return delNumList.toArray(new String[delNumList.size()]);
    }

    public int[] convertStringArrtoIntArr(String strEntryNum){
        String[] delNumArr = removeExtraSpace(strEntryNum);
        int numDelEntries = delNumArr.length;
        int[] delEntryNum = new int[numDelEntries];
        for (int i = 0; i < numDelEntries; i++){
            delEntryNum[i] = Integer.parseInt(delNumArr[i]);
        }
        return delEntryNum;
    }


}