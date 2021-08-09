package Events;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;

public class GenEntriesEvent extends Event{
    private final String[] sCategory = {"Entertainment","Food","Gift","Shopping","Transport", "Utilities"}; //can be an event class attribute because it can be used for other events!!
    private final String[] eCategory = {"Allowance", "Income", "Investment"};
    private final LocalDate dateToday;
    private YearMonth targetYM;
    private LocalDate targetStartDate;
    private LocalDate targetEndDate;


    public GenEntriesEvent(SendMessage message, ArrayList<String> errorlogs, int chatId) throws URISyntaxException, SQLException {
        super(message, errorlogs, chatId);
        dateToday = LocalDate.now();
        this.targetYM = YearMonth.of(dateToday.getYear(),dateToday.getMonthValue());
        this.targetStartDate = dateToday.withDayOfMonth(1);
        this.targetEndDate = dateToday.withDayOfMonth(this.dateToday.lengthOfMonth());
    }

    @Override
    public void generateEvent() throws SQLException {
        ArrayList<String> allCategory;
        int totalEntrycount = 0;

        //SQL Query
        // entryList[0] : All spending entries sorted by Categories stored in Hashmap
        // entryList[1] : All earning entries sorted by Categories stored in Hashmap
        ArrayList<HashMap<String,String>> entryList = super.getPSQL().getAllEntriesMonth(super.getChatId(), targetStartDate, targetEndDate);

        String entries = "<b>" + this.dateToday.getMonth() + " Entries</b> \n";

        HashMap<String,String> hashEntry = entryList.get(0);

        //keeping track of total number of entries
        totalEntrycount += Integer.parseInt(hashEntry.get("count"));
        hashEntry.remove("count");

        entries +=       "\n<b>-------- SPENDINGS --------</b>\n\n";
        allCategory = new ArrayList<>(Arrays.asList(sCategory));

        //outputting for big seven categories IF DONT WANT OUTPUT ALL CATEGORIES, can just make use of iterator to go through hashmap
        for (String cat : allCategory) {
            if (hashEntry.containsKey(cat)) {
                entries += "<b>" + cat + "</b>\n";
                entries += hashEntry.get(cat);
                entries += "\n";
                hashEntry.remove(cat);
            } else {
                entries += "<b>" + cat + "</b>\n";
                entries += "<code>---- no entry ---- </code>\n\n";
            }
        }

            entries += "<em>Others</em>\n";


            if (hashEntry.size() == 0){
                entries += "<code>---- no entry ---- </code>\n\n";
            }

        for (Map.Entry<String, String> stringStringEntry : hashEntry.entrySet()) {
            String category = (stringStringEntry).getKey();
            entries += "<b>" + category + "</b>\n";
            entries += hashEntry.get(category);

        }

        entries += "\n<em>No. of entries found: <b>" + totalEntrycount + "</b></em>";

        super.getMessage().setText(entries);
    }

    @Override
    public void generateOtherEvents() throws SQLException {
        ArrayList<String> allCategory;
        int totalEntrycount = 0;

        //SQL Query
        // entryList[0] : All spending entries sorted by Categories stored in Hashmap
        // entryList[1] : All earning entries sorted by Categories stored in Hashmap
        ArrayList<HashMap<String,String>> entryList = super.getPSQL().getAllEntriesMonth(super.getChatId(), targetStartDate, targetEndDate);

        String entries = "";

        HashMap<String,String> hashEntry = entryList.get(1);

        //keeping track of total number of entries
        totalEntrycount += Integer.parseInt(hashEntry.get("count"));
        hashEntry.remove("count");

        entries +=       "\n<b>-------- EARNINGS --------</b>\n\n";
        allCategory = new ArrayList<>(Arrays.asList(eCategory));

        //outputting for big seven categories IF DONT WANT OUTPUT ALL CATEGORIES, can just make use of iterator to go through hashmap
        for (String cat : allCategory) {
            if (hashEntry.containsKey(cat)) {
                entries += "<b>" + cat + "</b>\n";
                entries += hashEntry.get(cat);
                entries += "\n";
                hashEntry.remove(cat);
            } else {
                entries += "<b>" + cat + "</b>\n";
                entries += "<code>---- no entry ---- </code>\n\n";
            }
        }

        entries += "<em>Others</em>\n";

        if (hashEntry.size() == 0){
            entries += "<code>---- no entry ---- </code>\n\n";
        }

        for (Map.Entry<String, String> stringStringEntry : hashEntry.entrySet()) {
            String category = ((stringStringEntry).getKey());
            entries += "<b>" + category + "</b>\n";
            entries += hashEntry.get(category);

        }

        entries += "\n<em>No. of entries found: <b>" + totalEntrycount + "</b></em>";
        super.getMessage().setText(entries);
    }

    public void genPlainEntries() throws SQLException{
        super.getErrorLogs().add("========= Entries Events.Event Called ========= ");

        String entries = "<b>Complete Entry List</b> \n\n";

        //SQL Query
        ArrayList<ArrayList<String>> entryList = super.getPSQL().getAllEntries(super.getChatId());

        //Example of how the entrylist will look Like:
        // [ ["spend", "cost", "comment"] , ["earn", "cost", "comment"] ]

        int count = 0;
        for (ArrayList<String> entry : entryList) {
            String typeOfEntry = entry.get(0);
            String cost = entry.get(1);
            String comment = entry.get(2);

            super.getErrorLogs().add("[Entries] Select query successful.");

            if (typeOfEntry.equals("spend")) {
                entries += "   " + ++count + ".  - $" + cost + " : " + comment + "\n";
            } else if (typeOfEntry.equals("earn")) {
                entries += "   " + ++count + ".  + $" + cost + " : " + comment + "\n";
            }
        }

        entries += "\n<em>No. of entries found: <b>" + entryList.size() + "</b></em>";
        super.getMessage().setText(entries);
    }

    public void genMonthPlainEntries() throws SQLException{
        super.getErrorLogs().add("========= Entries Events.Event Called ========= ");

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
        super.getMessage().setText(entries);
    }

    public void setTargetYM(YearMonth yearMonth){
        this.targetYM = yearMonth;
        this.targetStartDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 1);
        this.targetEndDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), yearMonth.lengthOfMonth());
    }

}