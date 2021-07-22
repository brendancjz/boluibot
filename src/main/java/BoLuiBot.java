import org.postgresql.util.PGobject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.swing.plaf.nimbus.State;
import javax.xml.transform.Result;
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
    private ArrayList<String> entryList;
    private final Connection connection;
    private final ArrayList<String> errorLogs;

    BoLuiBot() throws URISyntaxException, SQLException {
        this.entryList = new ArrayList<>();
        this.connection = getConnection();
        this.errorLogs = new ArrayList<>();
    }



    @Override
    public String getBotUsername() {
        return "bo_lui_bot";
    }

    @Override
    public String getBotToken() {
        return "1763203814:AAFbCkdniUC5EJpiLMb3Uq5GUIP7xj60Mpw";
    }

    @Override
    public void onUpdateReceived(Update update) {
        //TODO Finalise the idea for the JSON entry_list.
        // For now, all commands are working. can Create, Add and Update. Need to do EDIT and DELETE.
        // Need more functionalities for /cancel
        // Need to create a TOTAL Spendings and Earnings
        // Need to create Categories for users to select. e.g Food, Clothes, Gifts.
        // Need more commands to spice up user experience.
        // Fix /entries command
        // Make sure the text is always taken from SQL

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
                    case "/help":
                        generateHelpEvent(message);
                        execute(message);
                        return; //Code ends here
                    default:
                        break;
                }

                //EVENT STATE ONE ---------------
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
                boolean cancelCondition = text.length() >= 7 && text.substring(0, 7).equals("/cancel");

                //Text is a command and isInputtingEntry is false.
                if (text.charAt(0) == '/' && !isInputtingEntry) {
                    switch (text) {
                        case "/entries":
                            generateEntriesEvent(chatId, message);
                            break;
                        case "/spend":
                        case "/earn":
                            updateUserEntryType(chatId, text);
                            generateEventStateOne(text, message, getUserEntryType(chatId));
                            updateUserEventState(chatId, currEventState);
                            updateIsUserInputting(chatId, isInputtingEntry);
                            break;

                        default:

                            //unknown command
                            message.setText("Oops, unknown command. Let's try another.");
                            break;
                    }

                } else if (isInputtingEntry && currEventState == 2) {
                    if (cancelCondition) { //User cancels entry.
                        cancelEntry(message);
                        updateUserEventState(chatId, FINAL_EVENT_STATE);
                        updateIsUserInputting(chatId, isInputtingEntry);
                        updateUserEntryType(chatId, RESET_ENTRY_TYPE);
                        resetEntryList(chatId);
                    } else {
                        generateEventStateTwo(text, message, entryType);
                        addEntryListItem(chatId, text, currEventState);
                        updateUserEventState(chatId, currEventState);
                    }
                } else if (isInputtingEntry && currEventState == 3) {
                    if (cancelCondition) { //User cancels entry.
                        cancelEntry(message);
                        updateUserEventState(chatId, FINAL_EVENT_STATE);
                        updateIsUserInputting(chatId, isInputtingEntry);
                        updateUserEntryType(chatId, RESET_ENTRY_TYPE);
                        resetEntryList(chatId);
                    } else {
                        generateEventStateThree(text, message, entryType);
                        addEntryListItem(chatId, text, currEventState);
                        updateUserEventState(chatId, currEventState);
                    }
                } else if (isInputtingEntry && currEventState == 4) {
                    if (cancelCondition) { //User cancels entry.
                        cancelEntry(message);
                        updateUserEventState(chatId, FINAL_EVENT_STATE);
                        updateIsUserInputting(chatId, isInputtingEntry);
                        updateUserEntryType(chatId, RESET_ENTRY_TYPE);
                        resetEntryList(chatId);
                    } else {
                        generateEventStateFour(text, message, entryType);
                        addEntryListItem(chatId, text, currEventState);
                        updateEntriesList(chatId);
                        updateUserEventState(chatId, currEventState);
                        updateIsUserInputting(chatId, isInputtingEntry);
                        updateUserEntryType(chatId, RESET_ENTRY_TYPE);
                        resetEntryList(chatId);
                        resetEntry();
                    }

                } else {
                    message.setText(text + " - BoLui"); //Echo text
                }


                // Call method to send the message
                execute(message);
            }
        } catch (SQLException | TelegramApiException | URISyntaxException throwables) {
            throwables.printStackTrace();
        }
    }

    private void generateHelpEvent(SendMessage message) {
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
            String selectedName = resultSet.getString("name");
            errorLogs.add("[" + selectedName + "] has been selected.");
        }
        statement.close();
        resultSet.close();

        //Insert into table users
        if (!userExists) {
            errorLogs.add("This user is not registered yet.");

            sql = "INSERT INTO users (chat_id, name, event_state, is_inputting, text, entry_type, entry_list) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, chatId);
            preparedStatement.setString(2, name);
            preparedStatement.setInt(3, INITIAL_EVENT_STATE);
            preparedStatement.setBoolean(4, INITIAL_IS_INPUTTING);
            preparedStatement.setString(5, text);
            preparedStatement.setString(6, INITIAL_ENTRY_TYPE);
            preparedStatement.setString(7, Arrays.toString(INITIAL_ENTRY_LIST));

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
                errorLogs.add("Found an entry!");
                count++;
                String typeOfEntry = resultSet.getString("typeofentry");
                String category = resultSet.getString("category");
                String cost = resultSet.getString("cost");
                String description = resultSet.getString("description");

                if (typeOfEntry.equals("spend")) {
                    entries += "<>  - $" + cost + " on " + category + "\n";
                } else if (typeOfEntry.equals("earn")) {
                    entries += "<>  + $" + cost + " from " + category + "\n";
                }
            }

            entries += "Number of entries found: " + count;
            message.setText(entries);

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


    }
    private void generateEventStateOne(String command, SendMessage message, String typeOfEntry) {

        errorLogs.add(" === Event State One Called === ");

        if (command.equals("/spend")) {
            message.setText("Alright, what did you spend on? [Input Category]");
            typeOfEntry = "spend";

        } else if (command.equals("/earn")) {
            message.setText("Swee! Where did you earn this moolah? [Input Category]");
            typeOfEntry = "earn";
        }

        entryList.add(typeOfEntry);

    }

    private void generateEventStateTwo(String text, SendMessage message, String typeOfEntry) {
        errorLogs.add("========= Event State Two Called ========= ");

        if (typeOfEntry.equals("spend")) {
            entryList.add(text); //Getting the category
            message.setText("Okay, how much did you spend on " + text + "? [Input Cost]");
        } else if (typeOfEntry.equals("earn")) {
            entryList.add(text); //Getting the category
            message.setText("Okay, how much did you earn from " + text + "? [Input Earnings]");
        } else {
            message.setText("Uh oh.. Something broke.");
        }
    }

    private void generateEventStateThree(String text, SendMessage message, String typeOfEntry) {
        errorLogs.add("========= Event State Three Called ========= ");

        if (typeOfEntry.equals("spend")) {
            entryList.add(text); //Getting the cost
            message.setText("$" + text + ", got it. Now, what's the story behind this? [Input Description]");
        } else if (typeOfEntry.equals("earn")) {
            entryList.add(text); //Getting the earnings
            message.setText("$" + text +"! Nice! How do you feel earning $" + text + "? [Input Description]");
        } else {
            message.setText("Uh oh.. Something broke.");
        }
    }

    private void generateEventStateFour(String text, SendMessage message, String typeOfEntry) {
        errorLogs.add("========= Event State Four Called ========= ");
        entryList.add(text); //Getting the description

        if (typeOfEntry.equals("spend")) {
            message.setText("Thanks! You have added a new entry: \nSpent $" + entryList.get(2) + " on " + entryList.get(1) + " - \"" + entryList.get(3) + "\"");
        } else if (typeOfEntry.equals("earn")) {
            message.setText("Thanks! You have added a new entry: \nEarned $" + entryList.get(2) + " from " + entryList.get(1) + " - Feeling: \"" + entryList.get(3) + "\"");
        } else {
            message.setText("Uh oh.. Something broke.");
        }

    }

    private String generateIntro(String name, boolean userExists) {
        errorLogs.add("Generating Intro... ");
        String intro = "";

        intro += "Hi " + name +
                "! I am Bo Lui and I welcome you to Sir Brendan's financial tracker to track how deep your pockets are! Sir Brendan is my creator.\n\n";
        intro += "For now, I am in the beta stages and so, I have very limited functionalities. I may crash on you. I probably will crash on you... " +
                "But! Your opinion and feedback to the creator will surely improve my system, so thank you for using me! \n\n";
        intro += "Enter: \"/\" to see what I can do...\n\n";

        if (userExists) {
            intro += "Yes! You have established a connection with the server. This connection is 24/7. All your data is saved into the database.\n";
        } else {
            intro += "Sorry! You have not established a connection with the server. Your data is not saved into the database. Try again later.\n ";
        }

        return intro;
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
        String description = entryListArr[2];

        errorLogs.add("Inserting " + entryType + " " + category + " " + cost + " " + description + " " + selectedUserId);

        //Insert the entry into entries table.

        sql = "INSERT INTO entries (typeOfEntry, category, cost, description, user_id) VALUES " +
                "(?, ?, ?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, entryType);
        preparedStatement.setString(2, category);
        preparedStatement.setDouble(3, cost);
        preparedStatement.setString(4, description);
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
        resetEntry();
    }

    private void resetEntry() {
        entryList = new ArrayList<String>();
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
            errorLogs.add("Currently, event State is " + eventState);
            if (eventState == 4) {
                eventState = 1;
            } else {
                eventState++;
            }

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
        //errorLogs.add(" === getUserEventState called === ");

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
            if (isInputting) {
                isInputting = false;
            } else {
                isInputting = true;
            }

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
            String entryType = "";
            if (command.equals("/spend")) {
                entryType = "spend";
            } else if (command.equals("/earn")) {
                entryType = "earn";
            } else if (command.equals("/reset")) {
                entryType = "null";
            } else {
                entryType = "error";
            }


            errorLogs.add("entry_type should now be: " + entryType);

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
                String selectedName = resultSet.getString("name");
                errorLogs.add("[" + selectedChatId + " " + selectedName + "] has been selected.");
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


