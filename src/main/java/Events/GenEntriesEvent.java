package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.YearMonth;

public class GenEntriesEvent extends Event{
    private final String[] sCategory = {"Entertainment","Food","Gift","Shopping","Transport", "Utilities"}; //can be an event class attribute because it can be used for other events!!
    private final String[] eCategory = {"Allowance", "Income", "Investment"};
    private final String NO_ENTRY_STRING = "<code>---- no entry ---- </code>\n\n";
    private final String OTHERS_STRING = "<em>Others</em>\n";
    private final String SPENDING_STRING = "\n<b>-------- SPENDINGS --------</b>\n\n";
    private final String EARNING_STRING = "\n<b>-------- EARNINGS --------</b>\n\n";
    private final LocalDate dateToday;
    private final YearMonth targetYM;
    private final LocalDate targetStartDate;
    private final LocalDate targetEndDate;



    public GenEntriesEvent(SendMessage message, PSQL psql, int chatId) throws URISyntaxException, SQLException {
        super(message, psql, chatId);
        dateToday = LocalDate.now();
        this.targetYM = YearMonth.of(dateToday.getYear(),dateToday.getMonthValue());
        this.targetStartDate = dateToday.withDayOfMonth(1);
        this.targetEndDate = dateToday.withDayOfMonth(this.dateToday.lengthOfMonth());
    }

    @Override
    public void generateEvent() throws SQLException {
        String entryType = "spend";
        HashMap<String,String> entryList = super.getPSQL().getMonthSortedEntries(super.getChatId(), entryType, targetStartDate, targetEndDate);
        String entries = "<b>" + targetEndDate.getMonth() +" Entries </b>\n";
        entries += getFormattedEntries(entryList, SPENDING_STRING, sCategory);

        entryType = "earn";
        entryList = super.getPSQL().getMonthSortedEntries(super.getChatId(), entryType, targetStartDate, targetEndDate);
        entries += getFormattedEntries(entryList, EARNING_STRING, eCategory);

        super.getMessage().setText(entries);
        super.getMessage().setReplyMarkup(KeyboardMarkups.entriesKB(this.targetYM.minusMonths(1), this.targetYM.plusMonths(1)));
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

            System.out.println("[Entries] " + entryNum + " " + cost + " " + comment);

            if (typeOfEntry.equals("spend")) {
                entries += "   " + entryNum + ".  - $" + cost + " : " + comment + "\n";
            } else if (typeOfEntry.equals("earn")) {
                entries += "   " + entryNum + ".  + $" + cost + " : " + comment + "\n";
            }
            
        }

        entries += "\n<em>No. of entries found: <b>" + entryList.size() + "</b></em>";
        super.getMessage().setText(entries);
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
            }
            /* Removing the no entry categories so a cleaner look
            else {
                entries += "<b>" + cat + "</b>\n";
                entries += NO_ENTRY_STRING;
            }
             */
        }



        if (hashEntry.size() != 0){ //Printing out the other categories
            entries += OTHERS_STRING ;
            for (Map.Entry<String, String> stringStringEntry : hashEntry.entrySet()) {
                String category = ((stringStringEntry).getKey());
                entries += "<b>" + category + "</b>\n";
                entries += hashEntry.get(category);
            }
        }



        entries += "\n<em>No. of entries found: <b>" + totalEntrycount + "</b></em>\n";

        return entries;
    }



}