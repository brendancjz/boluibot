package Events;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;

public class PSQL {
    private final Connection connection;
    private static final int INITIAL_EVENT_STATE = 1;
    private static final boolean INITIAL_IS_INPUTTING = false;
    private static final String[] INITIAL_ENTRY_LIST = new String[3];
    private static final String INITIAL_ENTRY_TYPE = "null";
    private static final int INITIAL_FINANCIAL_VALUE = 0; //for variables like total_spending/earning/entry_count
    private final ArrayList<String> errorLogs;

    public PSQL() throws URISyntaxException, SQLException {
        this.connection = getConnection();
        this.errorLogs = new ArrayList<>();
    }

    //Users table Queries
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    /**
     * This method ensures that user_id is inserted into the database
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param text String variable that stores the latest text of the user
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public void addNewUser(int chatId, String text, int currentYear, int currentMonth) throws SQLException {
        //================================= [Model]
        //Check if user already in database
        boolean userExists = isUserRegistered(chatId);

        if (!userExists) {
            errorLogs.add("This user is not registered yet.");

            int userId = 0;

            addNewUserRow(chatId, text, Arrays.toString(INITIAL_ENTRY_LIST));

            ResultSet resultSet = getUsersDataResultSet(chatId);
            while (resultSet.next()) {
                userId = resultSet.getInt("user_id");
            }

            addNewFinancials(userId, currentYear, currentMonth);
        }

    }

    /**
     * The method adds a new row in the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param text String variable that stores the latest text of the user
     * @param entryList String variable that stores the inputs of the user
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private void addNewUserRow(int chatId, String text, String entryList) throws SQLException{
        //Insert into table users
        String sql = "INSERT INTO users (chat_id, event_state, is_inputting, text, entry_count, entry_type, entry_list) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, chatId);
        preparedStatement.setInt(2, PSQL.INITIAL_EVENT_STATE);
        preparedStatement.setBoolean(3, PSQL.INITIAL_IS_INPUTTING);
        preparedStatement.setString(4, text);
        preparedStatement.setInt(5, PSQL.INITIAL_FINANCIAL_VALUE);
        preparedStatement.setString(6, PSQL.INITIAL_ENTRY_TYPE);
        preparedStatement.setString(7, entryList);

        int rowsInserted = preparedStatement.executeUpdate();
        if (rowsInserted > 0) {
            errorLogs.add("Successful registration.");
            errorLogs.add("[" + chatId + "] has been registered in users.");
        } else {
            errorLogs.add("Unsuccessful registration in users.");
        }
    }

    /**
     * This method add an entry list in the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param text String variable that stores the latest text of the user
     * @param currEventState int variable that stores the current event state of the user
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public void addEntryListItem(int chatId, String text, int currEventState) throws SQLException {
        errorLogs.add("Adding entry list item at event state: " + currEventState);
        //Get all the entry list first
        String entryList = "";

        ResultSet resultSet = getUsersDataResultSet(chatId);
        while (resultSet.next()) {
            entryList = resultSet.getString("entry_list");
        }

        //Clean the entry list string to be an array
        String[] tempArr = entryList.substring(1, entryList.length() - 1).split(", ");
        //Update tempArr with new list item
        tempArr[currEventState - 2] = text;
        updateEntryListItem(chatId,  Arrays.toString(tempArr));


    }

    /**
     * This method checks the range of entry count in the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param desiredEntryNum int variable that stores the desired entry number
     * @return Returns the boolean if entry count is within range
     */
    public boolean checkEntryCountRange(int chatId, int desiredEntryNum) throws SQLException {
        boolean inRange = false;

        ResultSet resultSet = getUsersDataResultSet(chatId);
        while (resultSet.next()){
            if (resultSet.getInt("entry_count") >= desiredEntryNum){
                inRange = true;
            }
        }
        return inRange;

    }

    /**
     * This method gets all user data from the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @return Returns the ResultSet object to use the user data
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private ResultSet getUsersDataResultSet(int chatId) throws SQLException{
        // Obtaining user information from USERS
        String sql = "SELECT * FROM users WHERE chat_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, chatId);

        return statement.executeQuery();
    }

    /**
     * This method gets entry list from the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @return Returns the String arr of the entry list
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public String[] getUsersEntryList(int chatId) throws SQLException {
        //Get entry list items from database
        String entryList = "";

        ResultSet resultSet = getUsersDataResultSet(chatId);
        while (resultSet.next()) { //Should only loop once
            entryList = resultSet.getString("entry_list");
        }

        //Clean up the entryList string into array and store the list items
        return entryList.substring(1, entryList.length() - 1).split(", ");
    }

    /**
     * This method gets the event state from the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @return Returns the current event state of the user
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public int getUserEventState(int chatId) throws SQLException {
        errorLogs.add("-- Getting User Events.Event State --");

        int eventState = 0;

        //Selecting User from Users table.
        ResultSet resultSet = getUsersDataResultSet(chatId);
        while (resultSet.next()) {
            eventState = resultSet.getInt("event_state");
            errorLogs.add("Current Events.Event State: " + eventState);
        }


        return eventState;
    }

    /**
     * This method gets the  is inputting from the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @return Returns a boolean whether the user is inputting or not
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public boolean getIsUserInputting(int chatId) throws SQLException {
        boolean isInputting = false;

        //Selecting User from Users table.
        ResultSet resultSet = getUsersDataResultSet(chatId);
        while (resultSet.next()) {
            isInputting = resultSet.getBoolean("is_inputting");
            errorLogs.add("[" + chatId + "] Current is_inputting: " + isInputting);
        }

        return isInputting;
    }


    /**
     * This method gets the user entry type from the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @return Returns the String of the entry type of the user
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public String getUserEntryType(int chatId) throws SQLException {
        String entryType = "";

        //Selecting User from Users table.
        ResultSet resultSet = getUsersDataResultSet(chatId);
        while (resultSet.next()) {
            entryType = resultSet.getString("entry_type");
            errorLogs.add("Current entry_type: " + entryType);
        }

        return entryType;
    }

    /**
     * This method gets the user text from the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @return Returns the String of the latest text
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public String getUserText(int chatId) throws SQLException {
        String text = "";

        //Selecting User from Users table.
        ResultSet resultSet = getUsersDataResultSet(chatId);
        while (resultSet.next()) {
            text = resultSet.getString("text");
        }


        return text;
    }

    /**
     * This method gets the boolean whether or not the user is registered in Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @return Returns the boolean whether user is registered or not
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public boolean isUserRegistered(int chatId) throws SQLException {

        boolean userExists = false;
        ResultSet resultSet = getUsersDataResultSet(chatId);
        while (resultSet.next()) {
            userExists = true;
            String selectedChatId = resultSet.getString("chat_id");
            errorLogs.add("[" + selectedChatId + "] has been selected.");
        }

        return userExists;


    }

    /**
     * This method resets the entry list in Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public void resetEntryList(int chatId) throws SQLException {
        errorLogs.add("Resetting Entry List. This is because entry has been completed or cancelled.");

        String sql = "UPDATE users SET entry_list=? WHERE chat_id=? ";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, Arrays.toString(new String[3]));
        statement.setInt(2, chatId);
        int rowsInserted = statement.executeUpdate();
        if ((rowsInserted > 0)) {
            errorLogs.add("[Reset Entry List] Update query successful.");

        } else {
            errorLogs.add("[Reset Entry List] Update query failed.");
        }
    }

    /**
     * The method updates the is_inputting in Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param isInputting boolean variable that shows if the user is inputting or not
     */
    public void updateIsUserInputting(int chatId, boolean isInputting) {
        try {
            isInputting = !isInputting;

            errorLogs.add("After updating, isInputting should now be: " + isInputting);

            String sql = "UPDATE users SET is_inputting=? WHERE chat_id=? ";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setBoolean(1, isInputting);
            statement.setInt(2, chatId);
            int rowsInserted = statement.executeUpdate();
            if ((rowsInserted > 0)) {
                errorLogs.add("[is_inputting] Update query successful.");

            } else {
                errorLogs.add("[is_inputting] Update query failed.");
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * This method updates the entry list to have an updated item in its list
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param entryList String variable that stores the inputs of the user
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public void updateEntryListItem(int chatId, String entryList) throws SQLException{
        //Update entry_list
        String sql = "UPDATE users SET entry_list=? WHERE chat_id=? ";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, entryList);
        statement.setInt(2, chatId);
        int rowsInserted = statement.executeUpdate();
        if ((rowsInserted > 0)) {
            errorLogs.add("[Entry List] Update query successful.");

        } else {
            errorLogs.add("[Entry List] Update query failed.");
        }
    }

    /**
     * This method updates the event state in the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param currEventState int variable that stores the current event state of the user
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public void updateUserEventState(int chatId, int currEventState) throws SQLException {
        //Increment because new event state
        if (currEventState == 5) {
            currEventState = 1;
        } else {
            currEventState++;
        }
        errorLogs.add("-- Updating User Events.Event State -- It is now: " + currEventState);

        String sql = "UPDATE users SET event_state=? WHERE chat_id=? ";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, currEventState);
        statement.setInt(2, chatId);
        int rowsInserted = statement.executeUpdate();
        if ((rowsInserted > 0)) {
            errorLogs.add("[Events.Event State] Update query successful.");

        } else {
            errorLogs.add("[Events.Event State] Update query failed.");
        }
    }

    /**
     * This method updates the entry type in the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param command String variable that stores the command made by the user
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public void updateUserEntryType(int chatId, String command) throws SQLException {
        errorLogs.add("---------------- Updating User Entry Type");

        //Example: command: /spend , entryType: spend.
        String entryType = command.substring(1);
        errorLogs.add("entry_type is now: " + entryType);

        String sql = "UPDATE users SET entry_type=? WHERE chat_id=? ";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, entryType);
        statement.setInt(2, chatId);
        int rowsInserted = statement.executeUpdate();
        if ((rowsInserted > 0)) {
            errorLogs.add("[entry_type] Update query successful.");

        } else {
            errorLogs.add("[entry_type] Update query failed.");
        }
    }

    /**
     * This method updates the text in the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param text String variable that stores the latest text of the user
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public void updateUserText(int chatId, String text) throws SQLException {
        String sql = "UPDATE users SET text=? WHERE chat_id=? ";
        PreparedStatement statement= connection.prepareStatement(sql);
        statement.setString(1, text);
        statement.setInt(2, chatId);
        int rowsInserted = statement.executeUpdate();
        if ((rowsInserted > 0)) {
            errorLogs.add("[Text] Update query successful.");
        } else {
            errorLogs.add("[Text] Update query failed.");
        }
    }

    /**
     * This method updates the entry count in the Users table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param updateEntryNumBy int variable that stores the number that is to be updated, +1 or -1
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private void updateUserEntryCount (int chatId, int updateEntryNumBy) throws SQLException {
        String sql = "UPDATE users SET entry_count = entry_count + ? WHERE chat_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, updateEntryNumBy);
        statement.setInt(2, chatId);
        statement.executeUpdate();
    }

    //Entries table Queries
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    /**
     * This method adds a new entry in the Entries table
     * @param entryType String variable that stores the entry type of the entry
     * @param entryNum int variable that stores the number that the entry is added
     * @param category String variable that stores the category input of the entry
     * @param costEarning double variable that stores the amount of cost or earning
     * @param comment String variable that stores the comment input of the entry
     * @param userId int variable that stores the user_id to link the entry to its user
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private void addNewEntryRow(String entryType, int entryNum, String category, double costEarning, String comment, int userId) throws SQLException{
        //Insert the entry into entries table.
        String sql = "INSERT INTO entries (entry_type, entry_num, category, cost_earning, comment, user_id) VALUES " +
                "(?, ?, ?, ?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, entryType);
        preparedStatement.setInt(2, entryNum);
        preparedStatement.setString(3, category);
        preparedStatement.setDouble(4, costEarning);
        preparedStatement.setString(5, comment);
        preparedStatement.setInt(6, userId);

        int rowsInserted = preparedStatement.executeUpdate();
        if (rowsInserted > 0) {
            errorLogs.add("New entry row added.");
        } else {
            errorLogs.add("Failed to add new entry row");
        }
        preparedStatement.close();
    }

    /**
     * This method deletes an entry row in the Entries table
     * @param entriesId int variable that stores the position of entry in the entries table
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private void deleteEntryRow(int entriesId) throws SQLException {
        //Deleting entry from entries table using entries_id
        String sql = "DELETE FROM entries WHERE entries_id = ?";
        PreparedStatement delstatement = connection.prepareStatement(sql);
        delstatement.setInt(1, entriesId);

        int rowsDeleted = delstatement.executeUpdate(); //problem happens when executeQuery is called... -> changed to executeUpdate (for edit/delete)

        if (rowsDeleted > 0) {
            errorLogs.add("Delete query successful");
        } else {
            errorLogs.add("Delete query unsuccessful");
        }
    }

    private void deleteEntryByTime(int userId, LocalDate startDate, LocalDate endDate) throws SQLException{
        java.sql.Date sDate = java.sql.Date.valueOf(startDate);
        java.sql.Date eDate = java.sql.Date.valueOf(endDate);

        String sql = "DELETE FROM entries WHERE user_id = ? and entry_date >= ? and entry_date <= ?";
        PreparedStatement delstatement = connection.prepareStatement(sql);
        delstatement.setInt(1, userId);
        delstatement.setDate(2, sDate);
        delstatement.setDate(3, eDate);
        int rowsDeleted = delstatement.executeUpdate(); 

        if (rowsDeleted > 0) {
            errorLogs.add("Delete Month successful");
        } else {
            errorLogs.add("Delete Month unsuccessful");
        }

    }

    private String formatCategory(String word){
        String newWord = "";
        newWord += String.valueOf(word.charAt(0)).toUpperCase() + word.substring(1);
        return newWord;
    }


    //sorts by category and returns in hashmap
    public HashMap<String,String> getSortedEntries (int chatId, String entryType, LocalDate startDate, LocalDate endDate) throws SQLException {
        String[] sCategory = {"Entertainment","Food","Gift","Shopping","Transport", "Utilities"}; //can be an event class attribute
        String[] eCategory = {"Income", "Allowance", "Investment"};
        ArrayList<String> allCategory = entryType.equals("spend") ? new ArrayList<>(Arrays.asList(sCategory)) : new ArrayList<>(Arrays.asList(eCategory));

        String sign = entryType.equals("spend") ? "-" : "+";

        HashMap<String,String> hashEntry = new HashMap<>();
        ResultSet resultSet = getEntrybyTypeResultSet(chatId, entryType, startDate, endDate);
        errorLogs.add("Query successful!");
        int count = 0;
        while (resultSet.next()){
            
            String entryCategory = resultSet.getString("category");
            String fCategory = formatCategory(entryCategory);
            String entryNum = String.valueOf(resultSet.getInt("entry_num"));
            String cost = String.valueOf(resultSet.getDouble("cost_earning"));
            String comment = String.valueOf(resultSet.getString("comment"));

            //The function of this if loop is just to format category(obtained) if they belong to the big six, else leave them as they are
            if (allCategory.contains(fCategory))  {
                hashEntry.putIfAbsent(fCategory, "");
                String existingEntries = hashEntry.get(fCategory);
                existingEntries += "   " + entryNum+ ".  " + sign + " $" + cost + " : " + comment + "\n";
                hashEntry.put(fCategory,existingEntries);
            } else {  //Misc -- not sure if should sort misc further, counting occurences of repeated misc category
                hashEntry.putIfAbsent(entryCategory, "");
                String existingEntries = hashEntry.get(entryCategory);
                existingEntries += "   " + entryNum+ ".  " + sign + " $" + cost + " : " + comment + "\n";
                hashEntry.put(entryCategory,existingEntries);
            }
            count += 1;
        }
        hashEntry.put("count",String.valueOf(count));
        return hashEntry;
    }

    /**
     * Alternative to getAllEntries
     * @param chatId
     * @param startDate
     * @param endDate
     * @return
     * @throws SQLException
     */
    public ArrayList<ArrayList<String>> getAllEntriesMonthCondensed(int chatId, LocalDate startDate, LocalDate endDate) throws SQLException {
        ArrayList<ArrayList<String>> entryList = new ArrayList<>();
        ResultSet resultSet = getMonthEntryResultSet(chatId, startDate, endDate);
        while (resultSet.next()) {
            ArrayList<String> anEntry = new ArrayList<>();
            String entryType = resultSet.getString("entry_type");
            double cost = resultSet.getDouble("cost_earning");
            String comment = resultSet.getString("comment");
            int entryNum = resultSet.getInt("entry_num");

            anEntry.add(entryType);
            anEntry.add(Double.toString(cost));
            anEntry.add(comment);
            anEntry.add(String.valueOf(entryNum));

            entryList.add(anEntry);
        }

        if (entryList.size() > 0) {
            errorLogs.add("[Entries] Select query successful.");
        } else {
            errorLogs.add("[Entries] Select query unsuccessful.");
        }

        return entryList;
    }

/**
     * This method gets all entries from the Entries table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @return Returns the ArrayList<ArrayList<String>> of all entries in the Entries table
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public ArrayList<ArrayList<String>> getAllEntries(int chatId) throws SQLException {
        ArrayList<ArrayList<String>> entryList = new ArrayList<>();

        //Selecting User from Users table.
        ResultSet resultSet = getAllEntriesFromChatIdResultSet(chatId);

        while (resultSet.next()) {
            ArrayList<String> anEntry = new ArrayList<>();
            String entryType = resultSet.getString("entry_type");
            double cost = resultSet.getDouble("cost_earning");
            String comment = resultSet.getString("comment");

            anEntry.add(entryType);
            anEntry.add(Double.toString(cost));
            anEntry.add(comment);

            entryList.add(anEntry);
        }

        if (entryList.size() > 0) {
            errorLogs.add("[Entries] Select query successful.");
        } else {
            errorLogs.add("[Entries] Select query unsuccessful.");
        }

        return entryList;
    }

    public ArrayList<HashMap<String,String>> getAllEntriesMonth(int chatId, LocalDate startDate, LocalDate endDate) throws SQLException {

        ArrayList<HashMap<String,String>> entryList = new ArrayList<>();
        String spend = "spend";
        String earn = "earn";
        entryList.add(getSortedEntries(chatId, spend, startDate, endDate));
        entryList.add(getSortedEntries(chatId, earn, startDate, endDate));

        if (entryList.size() > 0) {
            errorLogs.add("[Entries] Select query successful.");
        } else {
            errorLogs.add("[Entries] Select query unsuccessful.");
        }

        return entryList;
    }
    

    private ResultSet getEntrybyTypeResultSet(int chatId, String entryType, LocalDate startDate, LocalDate endDate) throws SQLException {
        errorLogs.add("Doing query...");
        ResultSet resultSet = getUsersDataResultSet(chatId);
        int userId = 0;
        java.sql.Date sDate = java.sql.Date.valueOf(startDate);
        java.sql.Date eDate = java.sql.Date.valueOf(endDate); //toInstance always gives some error.
        while (resultSet.next()){
            userId = resultSet.getInt("user_id");
        }

        String sql = "SELECT * FROM entries WHERE user_id = ? and entry_type = ? and entry_date >= ? and entry_date <= ? ORDER BY category, entry_num";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, userId);
        statement.setString(2, entryType);
        statement.setDate(3, sDate);
        statement.setDate(4, eDate);
        
        return statement.executeQuery();
    }

    public String[] getSpecificEntry(int chatId, int userEntryNum) throws SQLException{
        int userId = 0;
        String[] entry = new String[4];
        ResultSet resultSet = getUsersDataResultSet(chatId);
        while (resultSet.next()){
            userId = resultSet.getInt("user_id");
        }

        resultSet = getSpecificEntryResultSet(userId, userEntryNum);
        while (resultSet.next()){
            entry[0] = resultSet.getString("entry_type");
            entry[1] = resultSet.getString("category");
            entry[2] = String.valueOf(resultSet.getDouble("cost_earning"));
            entry[3] = resultSet.getString("comment");
        }

        return entry;
    }

    /**
     * This method gets all entries in the Entries table
     * @param userId int variable that stores the position of user in Users table
     * @return Returns the ResultSet object to be used to obtain data
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private ResultSet getAllEntriesFromUserIdResultSet(int userId) throws SQLException {
        //Using user_id and its entry_num to get the desired entry from entries table

        // Query to be sorted by entry_type and category for /entries event
        // and if entries table has jumbled entry_num, the displayed entry_num will not be equals to its actual entry_num
        String sql = "SELECT * FROM entries WHERE user_id=? ORDER BY entry_num";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, userId);

        return statement.executeQuery();
    }


    /**
     * This method gets all entries in the Entries table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @return Returns the ResultSet object to be used to obtain data
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private ResultSet getAllEntriesFromChatIdResultSet (int chatId) throws SQLException {
        ResultSet resultSet = getUsersDataResultSet(chatId);
        int userId = 0;
        while (resultSet.next()){
            userId = resultSet.getInt("user_id");
        }
        return getAllEntriesFromUserIdResultSet(userId);
    }

    /**
     * Gives entry of a specified month sorted by entry_num
     * @param chatId
     * @param startDate - defines the start date of the month
     * @param endDate - defines the end date of the month
     * @return
     * @throws SQLException
     */
    private ResultSet getMonthEntryResultSet(int chatId, LocalDate startDate, LocalDate endDate) throws SQLException {
        ResultSet resultSet = getUsersDataResultSet(chatId);
        int userId = 0;
        java.sql.Date sDate = java.sql.Date.valueOf(startDate);
        java.sql.Date eDate = java.sql.Date.valueOf(endDate); //toInstance always gives some error.
        while (resultSet.next()){
            userId = resultSet.getInt("user_id");
        }

        String sql = "SELECT * FROM entries WHERE user_id = ? and entry_date >= ? and entry_date <= ? ORDER BY entry_num";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, userId);
        statement.setDate(2, sDate);
        statement.setDate(3, eDate);
        
        return statement.executeQuery();
    }

    /**
     * Thie method gets the specific entry from the Entries table
     * @param userId int variable that stores the position of user in Users table
     * @param userEntryNum int variable that stores the user entry number
     * @return Returns the ResultSet object to be used to obtain data
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private ResultSet getSpecificEntryResultSet(int userId, int userEntryNum) throws SQLException {
        String sql = "SELECT * from ENTRIES where user_id = ? and entry_num = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, userId);
        statement.setInt(2, userEntryNum);

        return statement.executeQuery();
    }


    /**
     * This method updates entry number in the Entries table
     * @param userId int variable that stores the position of user in Users table
     * @param affectedEntryNum int variable that stores the affected entry number
     * @param updateEntryNumBy int variable that stores the number that is to be updated, +1 or -1
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private void updateEntriesEntryNum (int userId, int affectedEntryNum, int updateEntryNumBy) throws SQLException {
        String sql  = "UPDATE entries SET entry_num = entry_num + ? WHERE user_id = ? AND entry_num > ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, updateEntryNumBy);
        statement.setInt(2, userId);
        statement.setInt(3, affectedEntryNum);
        statement.executeUpdate();
        statement.close();
    }

    /**
     * This method updates the cost or earning in the Entries table
     * @param userId int variable that stores the position of user in Users table
     * @param entryNum int variable that stores the number that the entry is added
     * @param newCostEarning double variable that stores the new cost or earning
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public void updateEntriesCostEarning(int userId, int entryNum, double newCostEarning) throws SQLException {
        String sql = "UPDATE entries SET cost_earning = ? where user_id = ? AND entry_num = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setDouble(1, newCostEarning);
        statement.setInt(2, userId);
        statement.setInt(3, entryNum);
        statement.executeUpdate();
    }

    /**
     * This method updates the category in the Entries table
     * @param userId int variable that stores the position of user in Users table
     * @param entryNum int variable that stores the number that the entry is added
     * @param newCategory String variable that stores the new category input of the entry
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public void updateEntriesCategory(int userId, int entryNum, String newCategory) throws SQLException {
        String sql = "UPDATE entries SET category = ? where user_id = ? AND entry_num = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, newCategory);
        statement.setInt(2, userId);
        statement.setInt(3, entryNum);
        statement.executeUpdate();
        statement.close();
    }

    /**
     * This method updates the comment in the Entries table
     * @param userId int variable that stores the position of user in Users table
     * @param entryNum int variable that stores the number that the entry is added
     * @param newComment String variable that stores the new comment input of the entry
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public void updateEntriesComment(int userId, int entryNum, String newComment) throws SQLException {
        String sql = "UPDATE entries SET comment = ? where user_id = ? AND entry_num = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, newComment);
        statement.setInt(2, userId);
        statement.setInt(3, entryNum);
        statement.executeUpdate();
        statement.close();
    }

    /**
     * This method updates the entry type of the Entries table
     * @param userId int variable that stores the position of user in Users table
     * @param entryNum int variable that stores the number that the entry is added
     * @param newEntryType String variable that stores the new entry type of the entry
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public void updateEntriesEntryType(int userId, int entryNum, String newEntryType) throws SQLException {
        String sql = "UPDATE entries SET entry_type = ? where user_id = ? AND entry_num = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, newEntryType);
        statement.setInt(2, userId);
        statement.setInt(3, entryNum);
        statement.executeUpdate();
        statement.close();
    }



    //Financials table Queries
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    
    // yearMonth favoured over integer because the .getMonth() gives us a Month type
    // yearMonth favoured over LocalDate because yearMonth can be crafted with integer year and date values 
    // -- ease of viewing other months data.
    public String getMonthFinancials(int chatId, YearMonth yearMonth) throws SQLException {
        String fEntry = "<b>" + yearMonth.getMonth()+ " Money Flow</b>\n\n";
        ResultSet resultSet = getUsersDataResultSet(chatId);

        int userId = 0;
        while (resultSet.next()){
            userId = resultSet.getInt("user_id");
        }

        double totalSpending = 0, totalEarning = 0;
        resultSet = getMonthSpendEarnResultSet(userId, yearMonth.getYear(), yearMonth.getMonthValue());
        
        while (resultSet.next()){
            totalSpending = resultSet.getDouble("total_spending");
            totalEarning = resultSet.getDouble("total_earning");
        }

        fEntry += "<b>Total Spending\n   $</b>" + totalSpending + "\n\n";
        fEntry += "<b>Total Earning\n   $</b>" + totalEarning + "\n\n";

        DecimalFormat df = new DecimalFormat("0.00");
        if (totalEarning >= totalSpending) {
            fEntry += "<em>Balance:</em> $" + df.format( totalEarning - totalSpending);
        } else {
            fEntry += "<em>Balance:</em> - $" + df.format(totalSpending - totalEarning);
        }

        return fEntry;

    }

    /**
     * The use has been depreciated.
     * This method gets the financial row from the Financials table
     * @param userId int variable that stores the position of the financial row in Financials
     * @return Returns the ResultSet object to be used to obtain data
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private ResultSet getTotalSpendEarnResultSet(int userId) throws SQLException {
        //Retrieve current total spending and earning
        String sql = "SELECT * FROM financials WHERE user_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, userId);

        return statement.executeQuery();
    }

    private ResultSet getMonthSpendEarnResultSet(int userId, int year, int month) throws SQLException {

        //Retrieve current total spending and earning
        String sql = "SELECT * FROM financials WHERE user_id = ? and year = ? and month = ?"; 
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, userId);
        statement.setInt(2, year);
        statement.setInt(3, month);
        ResultSet resultSet = statement.executeQuery();

        if (!resultSet.isBeforeFirst()) {
            addNewFinancials(userId, year, month);
            return getMonthSpendEarnResultSet(userId, year, month);
        } else {
            return resultSet;
        }

    }

    /**
     * This methods adds a new financial row in the Financials table
     * @return Returns the ResultSet object to be used to obtain data
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private void addNewFinancials(int userId, int year, int month) throws SQLException {
        YearMonth insertYearMonth = YearMonth.of(year, month);
        //inserting a year worth of data
        for (int i = 0; i < 12; i++){
            String sql = "INSERT INTO financials (total_spending, total_earning, user_id, year, month) VALUES (? ,?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setDouble(1, INITIAL_FINANCIAL_VALUE);
            preparedStatement.setDouble(2, INITIAL_FINANCIAL_VALUE);
            preparedStatement.setInt(3, userId);
            preparedStatement.setInt(4, insertYearMonth.getYear());
            preparedStatement.setInt(5, insertYearMonth.getMonthValue());
            preparedStatement.executeQuery();
            insertYearMonth = insertYearMonth.plusMonths(1);
        }

    }


    /**
     * This method updates both spending and earning in the Financials table after user adds/deletes an entry
     * @param userId int variable that stores the position of the spending in the Financials table
     * @param cost_earning double variable that stores the cost or earning of the entry
     * @param updateMultiplier int variable that determines if the cost_earning is added/subtracted from the current spending
     * @param entryType String variable that stores teh entry type of the entry
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private void updateFinancial (int userId, int entryYear, int entryMonth, double cost_earning, int updateMultiplier, String entryType) throws SQLException {
        double totalEarning = 0, totalSpending = 0;
        ResultSet resultSet = getMonthSpendEarnResultSet(userId,entryYear, entryMonth);

        while (resultSet.next()) {
            totalSpending = resultSet.getDouble("total_spending");
            totalEarning = resultSet.getDouble("total_earning");
        }

        // Updating total spending and earning according to updateMultiplier (+: adding new entry, -: deleting entry)
        // Potential to update net flow(?) here as well
        if (entryType.equals("spend")) {
            updateFinancialSpending(userId, totalSpending + updateMultiplier*cost_earning, entryYear, entryMonth);
        } else if (entryType.equals("earn")) {
            updateFinancialEarning(userId, totalEarning + updateMultiplier*cost_earning, entryYear, entryMonth);
        }
    }

    /**
     * This method updates the spending of the user in the Financials table
     * @param userId int variable that stores the position of the spending in the Financials table
     * @param inputtedExpenditure double variable that stores the total spending thus far
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private void updateFinancialSpending (int userId, double inputtedExpenditure, int entryYear, int entryMonth) throws SQLException {
        String sql = "UPDATE financials SET total_spending = ? WHERE user_id = ? and year = ? and month = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setDouble(1,inputtedExpenditure);
        statement.setInt(2,userId);
        statement.setInt(3, entryYear);
        statement.setInt(4, entryMonth);
        statement.executeUpdate();
    }

    /**
     * This method updates the earning in the Financials table
     * @param userId int variable that stores the position of the spending in the Financials table
     * @param inputtedExpenditure double variable that stores the total earning thus far
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private void updateFinancialEarning (int userId, double inputtedExpenditure, int entryYear, int entryMonth) throws SQLException {
        String sql = "UPDATE financials SET total_earning = ? WHERE user_id = ? and year = ? and month = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setDouble(1,inputtedExpenditure);
        statement.setInt(2, userId);
        statement.setInt(3, entryYear);
        statement.setInt(4, entryMonth);
        statement.executeUpdate();
    }


    //Users and Entries table Queries
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    /**
     * This method gets the edited entry from Entries table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param editEntryArr ArrayList<String> variable that stores the edited entry.
     * @return Returns the string of the edit statement to be executed to the user.
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public String getEditedEntry(int chatId, ArrayList<String> editEntryArr) throws SQLException {

        String editedEntry = "";


        //Decoding editEntryArr
        /*
        String newEntryType = editEntryArr.get(0);
        String newCategory = editEntryArr.get(1);
        String newCostEarning = editEntryArr.get(2);
        String newComment = editEntryArr.get(3); 
        String userEntryNum = editEntryArr.get(4);
        */

        int userEntryNum = Integer.parseInt(editEntryArr.get(4));

        //Get old entry from Entries table
        String[] oldEntry = getSpecificEntry(chatId, userEntryNum);
        errorLogs.add("oldEntry " + Arrays.toString(oldEntry));
        errorLogs.add("edited entry " + editEntryArr.toString());

        if (oldEntry[0].equals("spend")) {
            editedEntry += userEntryNum + ".  - " + oldEntry[1] + " $" + oldEntry[2] + " : " + oldEntry[3] + "\n";
        } else if (oldEntry[0].equals("earn")) {
            editedEntry += userEntryNum + ".  + " + oldEntry[1] + " $" + oldEntry[2] + " : " + oldEntry[3] + "\n";
        }

        editedEntry += "      Edited to:\n";

        //Edit entry using editEntryRow 
        editEntryRow(chatId, editEntryArr, oldEntry);

        if (editEntryArr.get(0).equals("spend")) {
            editedEntry += userEntryNum + ".  - " + editEntryArr.get(1) + " $" + editEntryArr.get(2) + " : " + editEntryArr.get(3);
        } else if (editEntryArr.get(0).equals("earn")) {
            editedEntry += userEntryNum + ".  + " + editEntryArr.get(1) + " $" + editEntryArr.get(2) + " : " + editEntryArr.get(3);
        }

        return editedEntry;
    }
    /**
     * This method gets the deleted entry from the Entries table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param userEntryNum int variable that stores the user entry number
     * @return Returns the String statement of the deleted entry.
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public String getDeleteEntry (int chatId, int[] delEntryNumArr) throws SQLException{
        String entry = "<b>Selected Entry</b>\n";
        int userId = 0, entriesId = 0, delAmount = -1, delMultiplier = -1;
        double cost = 0;
        String entryType = "", comment = "";
        Date entryDate = new Date();

        ResultSet resultSet = getUsersDataResultSet(chatId);
        errorLogs.add("DELETE: run getUSERS");
        while (resultSet.next()){
            userId = resultSet.getInt("user_id");
        }

        for (int i = 0; i < delEntryNumArr.length; i++){
            int userEntryNum = delEntryNumArr[i];
            resultSet = getSpecificEntryResultSet(userId, userEntryNum);
            errorLogs.add("DELETE: run getSPECIFICENTRY"); 

            while (resultSet.next()) {
                entriesId = resultSet.getInt("entries_id");
                entryType = resultSet.getString("entry_type");
                cost = resultSet.getDouble("cost_earning");
                comment = resultSet.getString("comment");
                entryDate = resultSet.getDate("entry_date");
            }

            //Obtain date of insertion
            //errorLogs.add("entryDate: " +entryDate.toString());

            // //Have to convert Date (from sql) data type to LocalDate (which is easier to use)
            LocalDate localDate = LocalDate.parse(entryDate.toString());
            int entryYear = localDate.getYear();
            int entryMonth = localDate.getMonthValue();

            //errorLogs.add("LocalDate: " + localDate.toString());
            //errorLogs.add("entryMonth: " + entryMonth );

            if (entryType.equals("spend")) {
                entry += "   " + userEntryNum + ".  - $" + cost + " : " + comment + "\n";
            } else if (entryType.equals("earn")) {
                entry += "   " + userEntryNum + ".  + $" + cost + " : " + comment + "\n";
            }

            updateEntryCountAndNum(chatId, userEntryNum, delAmount);
            errorLogs.add("DELETE: update EntryCount");
            updateFinancial(userId, entryYear, entryMonth, cost, delMultiplier, entryType);
            errorLogs.add("DELETE: update Financials");
            deleteEntryRow(entriesId);
            errorLogs.add("DELETE: Deleted item");

        }

        entry+= "<em>has/have been deleted.</em>";
        return entry;
    }

    public String getDeleteEntryByTime (int chatId, LocalDate startDate, LocalDate endDate) throws SQLException{
        
        int userId = 0, delMultiplier = -1;
        int firstEntryNum = 0 , lastEntryNum = 0, counter = 0;
        double totalSpending = 0, totalEarning = 0;
        String entryType = "";

        ResultSet resultSet = getUsersDataResultSet(chatId);
        while (resultSet.next()){
            userId = resultSet.getInt("user_id");
        }

        //To update entry count and entry num, get start and end entry num, to update financials, we sum cost_earning
        resultSet = getMonthEntryResultSet(chatId, startDate, endDate);
        
        while (resultSet.next()) {
            if (counter == 0){
                firstEntryNum = resultSet.getInt("entry_num");
            }
            
            entryType = resultSet.getString("entry_type");
            if (entryType.equals("spend")){
                totalSpending += resultSet.getDouble("cost_earning");
            } else {
                totalEarning += resultSet.getDouble("cost_earning");
            }

            counter += 1;
        }

        deleteEntryByTime(userId, startDate, endDate);
        errorLogs.add("DELETE: Deleted item");

        lastEntryNum = firstEntryNum + counter;
        updateEntryCountAndNum(chatId, lastEntryNum, -(counter));
        errorLogs.add("DELETE: update EntryCount");

        int entryYear = startDate.getYear();
        int entryMonth = startDate.getMonthValue();
        updateFinancial(userId, entryYear, entryMonth, totalSpending, delMultiplier, "spend");
        updateFinancial(userId, entryYear, entryMonth, totalEarning, delMultiplier, "earn");
        errorLogs.add("DELETE: update Financials");

        String entry = counter + " entries from " + startDate.getMonth() + " have been deleted.";

        return entry;
    }

    /**
     * This method updates the entry count and entry number in the Users and Entries tables
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param affectedEntryNum numbers > affectedEntryNum will face decrease in entry num 
     * @param updateEntryNumBy int variable that stores the number that is to be updated, +1 or -1
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private void updateEntryCountAndNum(int chatId, int affectedEntryNum, int updateEntryNumBy) throws SQLException {
        int userId = 0;

        ResultSet resultSetUsers = getUsersDataResultSet(chatId);
        while (resultSetUsers.next()){
            userId = resultSetUsers.getInt("user_id");
        }

        updateUserEntryCount(chatId, updateEntryNumBy);
        updateEntriesEntryNum(userId, affectedEntryNum, updateEntryNumBy);
    }


    //Entries and Financials table Queries
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    /**
     * This method edits the entry row in the Entries table and the Financials table
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    public void editEntryRow(int chatId, ArrayList<String> editEntryArr, String[] oldEntry) throws SQLException {
        errorLogs.add("Editing Entries");

        int userEntryNum = Integer.parseInt(editEntryArr.get(4));

        //Get entry list items from database
        int selectedUserId = 0;
        Date entryDate = new Date();

        ResultSet resultSet = getUsersDataResultSet(chatId);
        while (resultSet.next()) { //Should only loop once
            selectedUserId = resultSet.getInt("user_id");
        }

        resultSet = getSpecificEntryResultSet(selectedUserId, userEntryNum);
        while (resultSet.next()){
            entryDate = resultSet.getDate("entry_date");
        }

        //Have to convert Date (from sql) data type to LocalDate (which is easier to use)
        LocalDate localDate = LocalDate.parse(entryDate.toString());
        int entryYear = localDate.getYear();    
        int entryMonth = localDate.getMonthValue();
    

        //Determine what has been changed (could be a combination)
        if (!editEntryArr.get(0).equals(oldEntry[0])) {  //EntryType has changed
            errorLogs.add("EDIT: ENTRYTYPE");
            errorLogs.add("Old Entry [0] " + oldEntry[0]);
            errorLogs.add("New Entry [0] " + editEntryArr.get(0));

            //updating Entries table
            updateEntriesEntryType(selectedUserId, userEntryNum, editEntryArr.get(0));
            
            int addRecord = 1, delRecord = -1;

            //updating Financials table
            double existingValue = Double.parseDouble(oldEntry[2]);
            if (editEntryArr.get(0).equals("spend")) { //if new entry is spend
                //Add record of the existing value into "spend"
                updateFinancial(selectedUserId, entryYear, entryMonth, existingValue, addRecord, "spend");
                //Del record of the existing vaue in "earn"
                updateFinancial(selectedUserId, entryYear, entryMonth, existingValue, delRecord, "earn");
            } else {
                updateFinancial(selectedUserId, entryYear, entryMonth, existingValue, addRecord, "earn");
                updateFinancial(selectedUserId, entryYear, entryMonth, existingValue, delRecord, "spend");
            }
        } 

        if (!editEntryArr.get(1).equals(oldEntry[1])) { //Category has changed
            errorLogs.add("EDIT: CATEGORY");
            updateEntriesCategory(selectedUserId, userEntryNum, editEntryArr.get(1));
        }

        if (!editEntryArr.get(2).equals(oldEntry[2])) { //amount has changed
            errorLogs.add("EDIT: AMOUNT");
            int updateMultiplier = 1;
            double oldValue = Double.parseDouble(oldEntry[2]);
            double newValue = Double.parseDouble(editEntryArr.get(2));
            double difference = newValue-oldValue;
            errorLogs.add("EDIT Difference: " + difference);

            //updating Entries table
            updateEntriesCostEarning(selectedUserId, userEntryNum, newValue);

            //updating Financials table
            updateFinancial(selectedUserId, entryYear, entryMonth, difference, updateMultiplier, editEntryArr.get(0));
        }

        if (!editEntryArr.get(3).equals(oldEntry[3])){
            errorLogs.add("EDIT: COMMENT");

            updateEntriesComment(selectedUserId, userEntryNum, editEntryArr.get(3));
        }

        //updateFinancial(userId, selectedUserId);
        
    }

    public void updateEntries(int chatId) throws SQLException {
        errorLogs.add("Updating Entries");
        //================================= [Model]
        //Thinking in terms of SQL, we need to create a row in entries table.

        //Get entry list items from database
        String entryList = "", entryType = "";
        int selectedUserId = 0, entryCount = 0, addRecord = 1;


        ResultSet resultSet = getUsersDataResultSet(chatId);
        while (resultSet.next()) { //Should only loop once
            entryList = resultSet.getString("entry_list");
            entryType = resultSet.getString("entry_type");
            selectedUserId = resultSet.getInt("user_id");
            entryCount = resultSet.getInt("entry_count");
        }

        //Clean up the entryList string into array and store the list items
        String[] entryListArr = entryList.substring(1, entryList.length() - 1).split(", ");

        //Insert entry into entries table
        String category = entryListArr[0];
        double cost = Double.parseDouble(entryListArr[1]);
        String comment = entryListArr[2];
        addNewEntryRow(entryType, entryCount+1, category, cost, comment, selectedUserId);
        errorLogs.add("Inserted " + entryType + " " + category + " " + cost + " " + comment + " " + selectedUserId);

        Date entryDate = new Date();
        //Obtain date of insertion
        resultSet = getSpecificEntryResultSet(selectedUserId, entryCount+1);
        while (resultSet.next()){
            entryDate = resultSet.getDate("entry_date");
        }

        errorLogs.add("entryDate: " +entryDate.toString());

        // //Have to convert Date (from sql) data type to LocalDate (which is easier to use)
        LocalDate localDate = LocalDate.parse(entryDate.toString());
        int entryYear = localDate.getYear();
        int entryMonth = localDate.getMonthValue();

        //Update entry_count by 1 in users table
        updateUserEntryCount(chatId, 1);
        //Update total spending/earning in financial table
        updateFinancial(selectedUserId, entryYear, entryMonth, cost, addRecord, entryType);

        resultSet.close();

    }



    //Miscellaneous Code
    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------

    /**
     * This method gets the error logs
     * @return Returns the ArrayList<String> of the error logs
     */
    public ArrayList<String> getErrorLogs() {
        ArrayList<String> logs = (ArrayList<String>) errorLogs.clone();
        errorLogs.clear();

        return logs;
    }

    /**
     * This method gets a connection with the POSTGRESQL database through JDBC
     * @return Returns the connection necessary to power the queries
     * @throws URISyntaxException Throws an exception when query is unsuccessful
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private static Connection getConnection() throws URISyntaxException, SQLException {
        URI dbUri = new URI(System.getenv("DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

        return DriverManager.getConnection(dbUrl, username, password);
    }



}
    
