package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Map;

public class GenEntriesInlineKeyboardEvent extends Event{
    private final String[] sCategory = {"Entertainment","Food","Gift","Shopping","Transport", "Utilities"}; //can be an event class attribute because it can be used for other events!!
    private final String[] eCategory = {"Allowance", "Income", "Investment"};
    private final String NO_ENTRY_STRING = "<code>---- no entry ---- </code>\n\n";
    private final String OTHERS_STRING = "<em>Others</em>\n";
    private final String SPENDING_STRING = "\n<b>-------- SPENDINGS --------</b>\n\n";
    private final String EARNING_STRING = "\n<b>-------- EARNINGS --------</b>\n\n";
    private final EditMessageText editMessage;
    private YearMonth targetYM;
    private LocalDate targetStartDate;
    private LocalDate targetEndDate;

    public GenEntriesInlineKeyboardEvent(SendMessage message, EditMessageText newMessage, ArrayList<String> errorlogs, int chatId, String callData) throws URISyntaxException, SQLException {
        super(message,errorlogs, chatId);
        this.editMessage = newMessage;
        setInlineEntriesAction(callData);
    }

    @Override
    public void generateEvent() throws SQLException {
        String entryType = "spend";
        HashMap<String,String> entryList = super.getPSQL().getMonthSortedEntries(super.getChatId(), entryType, targetStartDate, targetEndDate);
        String entries = getFormattedEntries(entryList, SPENDING_STRING, sCategory);
        super.getMessage().setText(entries);
    }

    @Override
    public void generateOtherEvents() throws SQLException {
        String entryType = "earn";
        HashMap<String,String> entryList = super.getPSQL().getMonthSortedEntries(super.getChatId(), entryType, targetStartDate, targetEndDate);
        String entries = getFormattedEntries(entryList, EARNING_STRING, eCategory);

        super.getMessage().setText(entries);
        super.getMessage().setReplyMarkup(GetInlineKeyboardMarkup.entriesKB(this.targetYM.minusMonths(1), this.targetYM, this.targetYM.plusMonths(1)));
    }

    public void setEditMonth(YearMonth yearMonth){
        this.targetYM = yearMonth;
        this.targetStartDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 1);
        this.targetEndDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), yearMonth.lengthOfMonth());
    }

    public void setInlineEntriesAction(String callData){
        String[] cdArray = callData.split("_");
        setEditMonth(YearMonth.of(Integer.parseInt(cdArray[1]) , Integer.parseInt(cdArray[2])));
    }

    private String getFormattedEntries(HashMap<String,String> hashEntry, String entryTypeString, String [] categoriesArray){
        ArrayList<String> allCategory;;

        String entries = "";

        //keeping track of total number of entries
        int totalEntrycount = Integer.parseInt(hashEntry.get("count"));
        hashEntry.remove("count");

        entries +=       entryTypeString;
        allCategory = new ArrayList<>(Arrays.asList(categoriesArray));

        //outputting for big seven categories IF DONT WANT OUTPUT ALL CATEGORIES, can just make use of iterator to go through hashmap
        for (String cat : allCategory) {
            if (hashEntry.containsKey(cat)) {
                entries += "<b>" + cat + "</b>\n";
                entries += hashEntry.get(cat);
                entries += "\n";
                hashEntry.remove(cat);
            } else {
                entries += "<b>" + cat + "</b>\n";
                entries += NO_ENTRY_STRING;
            }
        }

        entries += OTHERS_STRING ;

        if (hashEntry.size() == 0){
            entries += NO_ENTRY_STRING;
        }

        for (Map.Entry<String, String> stringStringEntry : hashEntry.entrySet()) {
            String category = ((stringStringEntry).getKey());
            entries += "<b>" + category + "</b>\n";
            entries += hashEntry.get(category);

        }

        entries += "\n<em>No. of entries found: <b>" + totalEntrycount + "</b></em>";

        return entries;
    }

}