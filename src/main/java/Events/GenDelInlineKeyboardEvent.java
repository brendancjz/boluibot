package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;


public class GenDelInlineKeyboardEvent extends Event{
    private final EditMessageText editMessage;
    private YearMonth targetYM;
    private LocalDate targetStartDate;
    private LocalDate targetEndDate;
    private boolean delConfirm;
    private boolean delMonth;
    private boolean delCancel;

    public GenDelInlineKeyboardEvent(SendMessage message, EditMessageText newMessage, ArrayList<String> errorlogs, int chatId, String callData) throws URISyntaxException, SQLException {
        super(message, errorlogs, chatId);
        this.editMessage = newMessage;
        this.delMonth = false;
        this.delConfirm = false;
        this.delCancel = false;
        setInlineDeleteAction(callData);
    }

    @Override
    public void generateEvent() throws SQLException, URISyntaxException { //Note that when this is called, currEventState = 2
        if (this.delMonth){
            int currEventState = super.getPSQL().getUserEventState(super.getChatId());
            String entryType = super.getPSQL().getUserEntryType(super.getChatId());
            if (currEventState == 2 && entryType.equals("delete")){ //checking if user trying to access the delete month button when they shouldnt access
                int numOfEntriesMonth = super.getPSQL().getAllEntriesMonthCondensed(super.getChatId(), this.targetStartDate, this.targetEndDate).size();
                if (numOfEntriesMonth > 0){
                    this.editMessage.setText("Entries of " + this.targetYM.getMonth() + " will be deleted. Press the button to confirm deletion");  
                    this.editMessage.setReplyMarkup(GetInlineKeyboardMarkup.deleteKBSecond(this.targetYM)); 
                } else {
                    this.editMessage.setText(Prompts.generateNoEntriesToDeletePrompt());  
                } 
            } else {
                this.editMessage.setText("Error. Cannot delete. Type /delete to try again.");  
            }
       
        } else if (delConfirm){
            this.editMessage.setText(super.getPSQL().getDeleteEntryByTime(super.getChatId(), this.targetStartDate, this.targetEndDate));
        } else if (delCancel) {
            genMonthPlainEntries();
            this.editMessage.setReplyMarkup(GetInlineKeyboardMarkup.deleteKB(this.targetYM.minusMonths(1), this.targetYM, this.targetYM.plusMonths(1)));
        } 
        else {
            genMonthPlainEntries();
            this.editMessage.setReplyMarkup(GetInlineKeyboardMarkup.deleteKB(this.targetYM.minusMonths(1), this.targetYM, this.targetYM.plusMonths(1)));
        }
    }

    @Override
    public void updateDatabase() throws SQLException{
        if (delConfirm){
            resetSystemToEventStateOne(super.getChatId(), true); //to ensure user is able to put other commands after completing deletion.
        }
    }

    
    public void setInlineDeleteAction(String callData){
        String[] cdArray = callData.split("_");
        //To generate new event for deleting multiple entries
        //To delete curr month as that is displayed 
        if (cdArray[1].equals("month")){
            setDeleteMonth(YearMonth.of(Integer.parseInt(cdArray[2]), Integer.parseInt(cdArray[3]))); 
            this.delMonth = true;
        } 
        else if (cdArray[1].equals("confirm")){
            setDeleteMonth(YearMonth.of(Integer.parseInt(cdArray[2]), Integer.parseInt(cdArray[3]))); 
            this.delConfirm = true;
        }
        else if (cdArray[1].equals("cancel")){
            setDeleteMonth(YearMonth.of(Integer.parseInt(cdArray[2]), Integer.parseInt(cdArray[3]))); 
            this.delCancel = true;
        }
        // To view other month's entries
        else {
            setDeleteMonth(YearMonth.of(Integer.parseInt(cdArray[1]), Integer.parseInt(cdArray[2]))); 
        }
    }

    public void setDeleteMonth(YearMonth yearMonth){
        this.targetYM = yearMonth;
        this.targetStartDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 1);
        this.targetEndDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), yearMonth.lengthOfMonth());
    }


    public void genMonthPlainEntries() throws SQLException{
        String entries = "<b>" + this.targetYM.getMonth() + " Entry List</b> \n\n";

        //SQL Query
        ArrayList<ArrayList<String>> entryList = super.getPSQL().getAllEntriesMonthCondensed(super.getChatId(), this.targetStartDate, this.targetEndDate);

        //Example of how the entrylist will look Like:
        // [ ["spend", "cost", "comment","entryNum"] , ["earn", "cost", "comment","entryNum"] ]
        for (ArrayList<String> entry : entryList) {
            String typeOfEntry = entry.get(0);
            String cost = entry.get(1);
            String comment = entry.get(2);
            String entryNum = entry.get(3);

            super.getErrorLogs().add("[Entries] Select query successful.");

            if (typeOfEntry.equals("spend")) {
                entries += "   " + entryNum + ".  - $" + cost + " : " + comment + "\n";
            } else if (typeOfEntry.equals("earn")) {
                entries += "   " + entryNum + ".  + $" + cost + " : " + comment + "\n";
            }
            
        }

        entries += "\n<em>No. of entries found: <b>" + entryList.size() + "</b></em>";
        this.editMessage.setText(entries);
    }
}