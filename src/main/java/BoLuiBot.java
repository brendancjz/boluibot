import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

public class BoLuiBot extends TelegramLongPollingBot {
    private static final int INITIAL_EVENT_STATE = 1;
    private static final int FINAL_EVENT_STATE = 4;
    private static final boolean INITIAL_IS_INPUTTING = false;
    private static final String[] INITIAL_ENTRY_LIST = new String[3];
    private static final String INITIAL_ENTRY_TYPE = "null";
    private static final String RESET_ENTRY_TYPE = "/reset";
    private final Connection connection;
    private final ArrayList<String> errorLogs;

    BoLuiBot() throws URISyntaxException, SQLException {
        this.connection = getConnection();
        this.errorLogs = new ArrayList<>();
    }



    @Override
    public String getBotUsername() {
        return "bolui_testbot";
    }

    @Override
    public String getBotToken() {
        return "1928314499:AAGZ9EiwAKlh-QmtfgifV6F7YxWY7Q3kkF0";
    }

    @Override
    public void onUpdateReceived(Update update) {
        //TODO
        // For now, all commands are working. can Create, Add and Update. Need to do EDIT and DELETE. (HIGH)
        // Analysis related: (/entries)
        // Need to create a TOTAL Spendings and Earnings (HIGH)
        // Need to create spending/earnings categorised by Categories (LOW)
        // Need to create spending/earnings categorised by Date (e.g. weekly, monthly) (LOW)
        // Need to create spending/earnings categorised by amts (LOW)
        // UIUX : Need to find out if we can see how long the text will be. With that, we can format the text such that it will look good. (MED)
        // Need to add more restrictions and helpers for the inputs. e.g Cost cannot have alphabets or symbols, Irregardless of upper/lower case for categories (HIGH)
        // Need to create Categories for users to select. e.g Food, Clothes, Gifts, Transport. (Either buttons or auto detect) (MED)
        // Allow user to choose language for Telebot (sassy mode (e.g. EH STOP SPENDING), cat (e.g. how much kibble did you earn)) (SUPER LOW)
        // Need more commands to spice up user experience.
        // Need to refactor the updating processes. SQL queries and program code must run separate. (HIGH)
        // /Bill for user to key in recurring payment (LOW)
        // /Setgoal for user to set target amount spend for each category (LOW)
        
        // We check if the update has a message and the message has text
        try {

            if (update.hasMessage() && update.getMessage().hasText()) {
                SendMessage message = new SendMessage(); // Create a SendMessage object with mandatory fields
                //This returns the message to the specific user that is using the bot
                message.setChatId(update.getMessage().getChatId().toString());

                String text = update.getMessage().getText();

                String chatId = update.getMessage().getChatId().toString();
                String name = update.getMessage().getChat().getFirstName();

                //Universal Commands. No need to update Query and check User.
                switch (text) {
                    case "/start":
                        generateStartEvent(chatId, name, text, message);
                        execute(message);
                        return; //Code ends here
                    case "/errorlogs":
                        generateErrorLogsEvent(message);
                        execute(message);
                        return; //Code ends here
                    case "/help":
                        generateHelpEvent(message);
                        execute(message);
                        return; //Code ends here
                    default:
                        break;
                }

                //When received a text, check the sender of this text and update text into database.
                boolean isCheckGood = checkingQueryAndUser(message, chatId, text);
                errorLogs.add("Received text and Checking user... isCheckGood " + isCheckGood);
                //Get text from database. Just in case... Prevent hiccups from multiple users update at the same time.
                text = getUserText(chatId);
                //Get info from user *VERY IMPORTANT*
                int currEventState = getUserEventState(chatId);
                boolean isInputtingEntry = getIsUserInputting(chatId);
                String entryType = getUserEntryType(chatId);

                //Not impt stuff
                boolean cancelCondition = text.length() >= 7 && text.startsWith("/cancel");

                //Text is a command and isInputtingEntry is false.
                if (text.charAt(0) == '/' && !isInputtingEntry) {
                    switch (text) {
                        case "/entries":
                            generateEntriesEvent(chatId, message);
                            break;
                        case "/spend":
                        case "/earn":
                            //SQL Queries
                            updateUserEntryType(chatId, text); //Update entry type
                            updateUserEventState(chatId, currEventState);
                            updateIsUserInputting(chatId, isInputtingEntry);

                            //Program Code
                            generateEventStateOne(text, message);
                            break;

                        default:
                            //unknown command
                            message.setText("Oops, that doesn't seem right. Let's try another.");
                            break;
                    }

                } else if (!cancelCondition && text.charAt(0) == '/' && isInputtingEntry) { //User types a command while inputting
                    message.setText("Sorry, something's wrong. Type /cancel to cancel entry and try that command again.");

                } else if (isInputtingEntry && currEventState == 2) {
                    if (cancelCondition) { //User cancels entry.
                        //SQL Queries
                        resetSystemToEventStateOne(chatId, isInputtingEntry);

                        //Program Code
                        cancelEntry(message);
                    } else {
                        //SQL Queries
                        addEntryListItem(chatId, text, currEventState);
                        updateUserEventState(chatId, currEventState);

                        //Program Code
                        generateEventStateTwo(text, message, entryType);
                    }
                } else if (isInputtingEntry && currEventState == 3) {
                    if (cancelCondition) { //User cancels entry.
                        //SQL Queries
                        resetSystemToEventStateOne(chatId, isInputtingEntry);

                        //Program Code
                        cancelEntry(message);
                    } else {
                        //Verifying...
                        String cost = removingDollarSign(text);
                        boolean isNum = isNumeric(cost);
                        errorLogs.add("isNumeric: " + cost + " " + isNum);

                        //SQL Queries
                        addEntryListItem(chatId, cost, currEventState);
                        String[] entryListArr = getEntryList(chatId);

                        if (isNum) {
                            //SQL Queries
                            updateUserEventState(chatId, currEventState);
                        }

                        //Program Code
                        generateEventStateThree(message, entryType, isNum, entryListArr);
                    }
                } else if (isInputtingEntry && currEventState == 4) {
                    if (cancelCondition) { //User cancels entry.
                        //SQL Queries
                        resetSystemToEventStateOne(chatId, isInputtingEntry);

                        //Program Code
                        cancelEntry(message);
                    } else {
                        //SQL Queries
                        addEntryListItem(chatId, text, currEventState);
                        String[] entryListArr = getEntryList(chatId);
                        updateEntriesList(chatId);
                        resetSystemToEventStateOne(chatId, isInputtingEntry);

                        //Program Code
                        generateEventStateFour(message, entryType, entryListArr);
                    }

                } else {
                    message.setText(text + " - Bo"); //Echo text
                }


                // Call method to send the message
                execute(message);
            }
        } catch (SQLException | TelegramApiException | URISyntaxException throwables) {
            throwables.printStackTrace();
        }
    }

    private void resetSystemToEventStateOne(String chatId, boolean isInputtingEntry) {
        //SQL Queries
        updateUserEventState(chatId, FINAL_EVENT_STATE);
        updateIsUserInputting(chatId, isInputtingEntry);
        updateUserEntryType(chatId, RESET_ENTRY_TYPE);
        resetEntryList(chatId);
    }

    private void generateHelpEvent(SendMessage message) {

        message.setText("Hello! Here's a quick intro to what I can do. \n\n" +
                "I keep track of your spendings and earnings in a very straight forward way. You can enter your entries with commands /spend or /earn. " +
                "To view your inputted entries, type /entries. \n\n" +
                "Made a mistake while inputting an entry? Type /cancel to reset it. \n\n" +
                "Inputs Types \n - Category e.g Food, Gifts, Clothes \n - Cost/Earnings e.g $12.34, $1234\n - Comment e.g Bought pizza for lunch\n\n"+
                "For now, I cannot edit your created entries or even delete them! Don't worry though, I will soon :)\n\n\n" +
                "Last thing, I will sometimes echo your latest message...");
    }

    private void generateErrorLogsEvent(SendMessage message) {
        String log = "------ Generating Error Logs ------ \n";
        for (String error : errorLogs) {
            log += error + "\n";
        }
        message.setText(log);
        errorLogs.clear();
    }

    private void generateStartEvent(String chatId, String name, String text, SendMessage message) throws URISyntaxException, SQLException {
        errorLogs.add("=== Start Event Called === ");

        //================================= [Model]
        //Check if user already in database
        boolean userExists = false;
        String sql = "SELECT * FROM users WHERE chat_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, chatId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            userExists = true;
            String selectedChatId = resultSet.getString("chat_id");
            errorLogs.add("[" + selectedChatId + "] has been selected.");
        }
        statement.close();
        resultSet.close();

        //Insert into table users
        if (!userExists) {
            errorLogs.add("This user is not registered yet.");

            sql = "INSERT INTO users (chat_id, event_state, is_inputting, text, entry_type, entry_list) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, chatId);
            preparedStatement.setInt(2, INITIAL_EVENT_STATE);
            preparedStatement.setBoolean(3, INITIAL_IS_INPUTTING);
            preparedStatement.setString(4, text);
            preparedStatement.setString(5, INITIAL_ENTRY_TYPE);
            preparedStatement.setString(6, Arrays.toString(INITIAL_ENTRY_LIST));

            int rowsInserted = preparedStatement.executeUpdate();
            if (rowsInserted > 0) {
                errorLogs.add("Successful registration.");
                errorLogs.add("[" + name + "] has been registered.");
                userExists = true;
            } else {
                errorLogs.add("Unsuccessful registration.");
            }
            preparedStatement.close();
        }

        message.setText(generateIntro(name, userExists));
    }


    private void generateEntriesEvent(String chatId, SendMessage message) {
        try {
            errorLogs.add("========= Entries Event Called ========= ");
            //Get user_id key from users table
            int userId = 0;

            //Entries
            String entries = "Here are your entries: \n";

            //Selecting User from Users table.
            String sql = "SELECT * FROM users WHERE chat_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, chatId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                userId = resultSet.getInt("user_id");
            }

            errorLogs.add("The user_id is : " + userId);

            //Using user_id to get entries from entries table
            sql = "SELECT * FROM entries WHERE user_id=?";
            statement = connection.prepareStatement(sql);
            statement.setInt(1, userId);
            resultSet = statement.executeQuery();
            int count = 0;
            while (resultSet.next()) {
                count++;
                String typeOfEntry = resultSet.getString("typeofentry");
                String cost = resultSet.getString("cost");
                String comment = resultSet.getString("comment");

                if (typeOfEntry.equals("spend")) {
                    entries += "   <" + count + ">  - $" + cost + " : " + comment + "\n";
                } else if (typeOfEntry.equals("earn")) {
                    entries += "   <" + count + ">  + $" + cost + " : " + comment + "\n";
                }
            }

            if (count > 0) {
                errorLogs.add("[Entries] Select query successful.");
            } else {
                errorLogs.add("[Entries] Select query unsuccessful.");
            }

            entries += "Number of entries found: " + count;
            message.setText(entries);

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


    }

    private void generateEventStateOne(String command, SendMessage message) {
        errorLogs.add(" === Event State One Called === ");

        if (command.equals("/spend")) {
            String prompt = Prompts.generateEventOneSpendPrompt();
            message.setText(prompt);

        } else if (command.equals("/earn")) {
            String prompt = Prompts.generateEventOneEarnPrompt();
            message.setText(prompt);
        }

    }

    private void generateEventStateTwo(String category, SendMessage message, String typeOfEntry) {
        errorLogs.add("========= Event State Two Called ========= ");

        if (typeOfEntry.equals("spend")) {
            String prompt = Prompts.generateEventTwoSpendPrompt(category);
            message.setText(prompt);
        } else if (typeOfEntry.equals("earn")) {
            String prompt = Prompts.generateEventTwoEarnPrompt(category);
            message.setText(prompt);
        } else {
            message.setText("Uh oh.. Something broke.");
        }
    }

    private void generateEventStateThree(SendMessage message, String typeOfEntry, boolean isNum, String[] entryListArr) throws TelegramApiException {
        errorLogs.add("========= Event State Three Called ========= ");

        if (typeOfEntry.equals("spend") && isNum) {
            String prompt = Prompts.generateEventThreeSpendPrompt(entryListArr[1]);
            message.setText(prompt);
        } else if (typeOfEntry.equals("earn") && isNum) {
            String prompt = Prompts.generateEventThreeEarnPrompt(entryListArr[1]);
            message.setText(prompt);
        } else {
            message.setText("Uh oh.. Input was not recognised. Did you keep it numeric? Try it again.");
            execute(message);
            generateEventStateTwo(entryListArr[0], message, typeOfEntry);
        }
    }

    private String removingDollarSign(String text) {
        if (text.charAt(0) == '$') {
            return text.substring(1);
        } else {
            return text;
        }
    }

    private static boolean isNumeric(String strNum) {

        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private void generateEventStateFour(SendMessage message, String typeOfEntry, String[] entryListArr) throws SQLException {
        errorLogs.add("========= Event State Four Called ========= ");

        if (typeOfEntry.equals("spend")) {
            message.setText("Thanks! You have added a new entry: \nSpent $" + entryListArr[1] + " on " + entryListArr[0] + " - \"" + entryListArr[2] + "\"");
        } else if (typeOfEntry.equals("earn")) {
            message.setText("Thanks! You have added a new entry: \nEarned $" + entryListArr[1] + " from " + entryListArr[0] + " - Feeling: \"" + entryListArr[2] + "\"");
        } else {
            message.setText("Uh oh.. Something broke.");
        }

    }

    private String generateIntro(String name, boolean userExists) {
        errorLogs.add("Generating Intro... ");
        String intro = "";

        intro += "Hi " + name +
                "! I am Bo Lui and I welcome you to Sir Brendan's financial tracker to track how deep your pockets are! Sir Brendan is my creator.\n\n";
        intro += "I am your personal finance bot! I store your spendings and earnings in a simple way.\n\n";
        intro += "For now, I am in the beta stages and so, I have very limited functionalities. I may crash on you. I probably will crash on you... " +
                "But! Your opinion and feedback to the creator will surely improve my system, so thank you for using me! \n\n";
        intro += "Enter: /help to see what I can do...\n\n";

        if (userExists) {
            intro += "Yes! You have established a connection with the server. This connection is 24/7. All your data is saved into the database.\n";
        } else {
            intro += "Sorry! You have not established a connection with the server. Your data is not saved into the database. Try again later.\n ";
        }

        return intro;
    }

    private String[] getEntryList(String chatId) throws SQLException {
        //Get entry list items from database
        String entryList = "";

        String sql = "SELECT * FROM users WHERE chat_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, chatId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) { //Should only loop once
            entryList = resultSet.getString("entry_list");
        }

        //Clean up the entryList string into array and store the list items
        return entryList.substring(1, entryList.length() - 1).split(", ");

    }

    private void updateEntriesList(String chatId) throws SQLException {
        errorLogs.add("Updating Entries");
        //================================= [Model]
        //Thinking in terms of SQL, we need to create a row in entries table.

        //Get entry list items from database
        String entryList = "";
        String entryType = "";
        int selectedUserId = 0;

        String sql = "SELECT * FROM users WHERE chat_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, chatId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) { //Should only loop once
            entryList = resultSet.getString("entry_list");
            entryType = resultSet.getString("entry_type");
            selectedUserId = resultSet.getInt("user_id");
        }

        //Clean up the entryList string into array and store the list items
        String[] entryListArr = entryList.substring(1, entryList.length() - 1).split(", ");

        //Insert entry into entries table

        String category = entryListArr[0];
        double cost = Double.parseDouble(entryListArr[1]);
        String comment = entryListArr[2];

        errorLogs.add("Inserting " + entryType + " " + category + " " + cost + " " + comment + " " + selectedUserId);

        //Insert the entry into entries table.

        sql = "INSERT INTO entries (typeOfEntry, category, cost, comment, user_id) VALUES " +
                "(?, ?, ?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, entryType);
        preparedStatement.setString(2, category);
        preparedStatement.setDouble(3, cost);
        preparedStatement.setString(4, comment);
        preparedStatement.setInt(5, selectedUserId);

        int rowsInserted = preparedStatement.executeUpdate();
        if( rowsInserted > 0 ) {
            errorLogs.add("[Entry List Insert] Insert query successful");
        } else {
            errorLogs.add("[Entry List Insert] Insert query unsuccessful");
        }
        preparedStatement.close();

    }

    private void cancelEntry(SendMessage message) {
        message.setText("Entry cancelled.");
    }

    private void resetEntryList(String chatId) {
        try {
            errorLogs.add("Resetting Entry List. This is because entry has been completed or cancelled.");
            String[] resetEntryListArr = new String[3];

            String sql = "UPDATE users SET entry_list=? WHERE chat_id=? ";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, Arrays.toString(resetEntryListArr));
            statement.setString(2, chatId);
            int rowsInserted = statement.executeUpdate();
            if ((rowsInserted > 0)) {
                errorLogs.add("[Reset Entry List] Update query successful.");

            } else {
                errorLogs.add("[Reset Entry List] Update query failed.");
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    private void addEntryListItem(String chatId, String text, int currEventState) {
        try {
            errorLogs.add("Adding entry list item at event state: " + currEventState);
            //Get all the entry list first
            String entryList = "";

            String sql = "SELECT * FROM users WHERE chat_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, chatId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                entryList = resultSet.getString("entry_list");
            }

            //Clean the entry list string to be an array
            String[] tempArr = entryList.substring(1, entryList.length() - 1).split(", ");
            //Update tempArr with new list item
            tempArr[currEventState - 2] = text;


            //Update entry_list
            sql = "UPDATE users SET entry_list=? WHERE chat_id=? ";
            statement = connection.prepareStatement(sql);
            statement.setString(1, Arrays.toString(tempArr));
            statement.setString(2, chatId);
            int rowsInserted = statement.executeUpdate();
            if ((rowsInserted > 0)) {
                errorLogs.add("[Entry List] Update query successful.");

            } else {
                errorLogs.add("[Entry List] Update query failed.");
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void updateUserEventState(String chatId, int eventState) {
        try {
            //Increment because new event state
            if (eventState == 4) {
                eventState = 1;
            } else {
                eventState++;
            }
            errorLogs.add("-- Updating User Event State -- It is now: " + eventState);

            String sql = "UPDATE users SET event_state=? WHERE chat_id=? ";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, eventState);
            statement.setString(2, chatId);
            int rowsInserted = statement.executeUpdate();
            if ((rowsInserted > 0)) {
                errorLogs.add("[Event State] Update query successful.");

            } else {
                errorLogs.add("[Event State] Update query failed.");
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private int getUserEventState(String chatId) {
        errorLogs.add("-- Getting User Event State --");

        int eventState = 0;

        try {
            //Selecting User from Users table.
            String sql = "SELECT * FROM users WHERE chat_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, chatId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                eventState = resultSet.getInt("event_state");
                errorLogs.add("Current Event State: " + eventState);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return eventState;
    }

    private void updateIsUserInputting(String chatId, Boolean isInputting) {
        try {
            isInputting = !isInputting;

            errorLogs.add("After updating, isInputting should now be: " + isInputting);

            String sql = "UPDATE users SET is_inputting=? WHERE chat_id=? ";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setBoolean(1, isInputting);
            statement.setString(2, chatId);
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

    private String getUserText(String chatId) {
        String text = "";
        try {
            //Selecting User from Users table.
            String sql = "SELECT * FROM users WHERE chat_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, chatId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                text = resultSet.getString("text");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return text;
    }

    private boolean getIsUserInputting(String chatId) {
        boolean isInputting = false;
        try {
            //Selecting User from Users table.
            String sql = "SELECT * FROM users WHERE chat_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, chatId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                isInputting = resultSet.getBoolean("is_inputting");
                errorLogs.add("[" + chatId + "] Current is_inputting: " + isInputting);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return isInputting;
    }

    private void updateUserEntryType(String chatId, String command) {
        try {
            errorLogs.add("---------------- Updating User Entry Type");
            String entryType;
            switch (command) {
                case "/spend":
                    entryType = "spend";
                    break;
                case "/earn":
                    entryType = "earn";
                    break;
                case "/reset":
                    entryType = "null";
                    break;
                default:
                    entryType = "error";
                    break;
            }


            errorLogs.add("entry_type is now: " + entryType);

            String sql = "UPDATE users SET entry_type=? WHERE chat_id=? ";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, entryType);
            statement.setString(2, chatId);
            int rowsInserted = statement.executeUpdate();
            if ((rowsInserted > 0)) {
                errorLogs.add("[entry_type] Update query successful.");

            } else {
                errorLogs.add("[entry_type] Update query failed.");
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private String getUserEntryType(String chatId) {
        String entryType = "";
        try {
            //Selecting User from Users table.
            String sql = "SELECT * FROM users WHERE chat_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, chatId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                entryType = resultSet.getString("entry_type");
                errorLogs.add("Current entry_type: " + entryType);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return entryType;
    }

    private boolean checkingQueryAndUser(SendMessage message, String chatId, String text) {
        boolean everythingGood = false;

        try {

            //2. Check if chatId is in database
            boolean userExists = false;
            String sql = "SELECT * FROM users WHERE chat_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, chatId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                userExists = true;
                String selectedChatId = resultSet.getString("chat_id");
                errorLogs.add("[" + selectedChatId + "] has been selected.");
            }

            //3. If TRUE, Store text into database of this user.
            if (userExists) {
                sql = "UPDATE users SET text=? WHERE chat_id=? ";
                statement= connection.prepareStatement(sql);
                statement.setString(1, text);
                statement.setString(2, chatId);
                int rowsInserted = statement.executeUpdate();
                if ((rowsInserted > 0)) {
                    errorLogs.add("[Text] Update query successful.");
                    everythingGood = true;
                } else {
                    errorLogs.add("[Text] Update query failed.");
                }
            } else {
                //4 If false, reject text and get them to type /start
                message.setText("You have keyed in an entry without starting the bot. Type /start.");

                // Call method to send the message
                execute(message);

            }

        } catch (SQLException | TelegramApiException throwables) {
            throwables.printStackTrace();
        }

        return everythingGood;
    }
    
    private static Connection getConnection() throws URISyntaxException, SQLException {
        URI dbUri = new URI(System.getenv("DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

        return DriverManager.getConnection(dbUrl, username, password);
    }
}


