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

public class BoLuiBot extends TelegramLongPollingBot {
    private boolean isInputtingEntry;
    private String typeOfEntry;
    private ArrayList<String> entryList;
    private ArrayList<ArrayList<String>> entriesList;
    private int currEventState;
    private Connection connection;

    BoLuiBot() {
        this.isInputtingEntry = false;
        this.typeOfEntry = "";
        this.entryList = new ArrayList<>();
        this.entriesList = new ArrayList<>();
        this.currEventState = 1;
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
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {

            SendMessage message = new SendMessage(); // Create a SendMessage object with mandatory fields
            message.setChatId(update.getMessage().getChatId().toString());
            String text = update.getMessage().getText();

            boolean cancelCondition = text.length() >= 7 && text.substring(0, 7).equals("/cancel");

            //Text is a command and isInputtingEntry is false.
            if (text.charAt(0) == '/' && !isInputtingEntry) {
                if (text.equals("/start")) {
                    try {
                        generateStartEvent(update, message);
                    } catch (URISyntaxException | SQLException e) {
                        e.printStackTrace();
                    }
                } else if (text.equals("/entries")) {
                    generateEntriesEvent(message);
                } else if (text.equals("/help")) {
                    generateHelpEvent(message);
                } else {
                    generateEventStateOne(text, message);
                }

            } else if (isInputtingEntry && currEventState == 2) {
                if (cancelCondition) { //User cancels entry.
                    cancelEntry(message);
                } else {
                    generateEventStateTwo(text, message);
                }
            } else if (isInputtingEntry && currEventState == 3) {
                if (cancelCondition) { //User cancels entry.
                    cancelEntry(message);
                } else {
                    generateEventStateThree(text, message);
                }
            } else if (isInputtingEntry && currEventState == 4) {
                if (cancelCondition) { //User cancels entry.
                    cancelEntry(message);
                } else {
                    try {
                        generateEventStateFour(text, message, update);
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }

                    System.out.println(entriesList.toString());
                }

            } else {
                message.setText(text + " - Bo Lui"); //Echo text
            }


            // Call method to send the message
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

    }

    public void generateHelpEvent(SendMessage message) {
        //TODO
        message.setText("Currently developing on this command... Try another command.");
    }

    public void generateStartEvent(Update update, SendMessage message) throws URISyntaxException, SQLException {
        System.out.println("========= Start Event Called ========= ");
        connection = getConnection();
        boolean isConnected = !connection.isClosed();

        //================================= [Model]
        //Thinking in terms of SQL, we need to create a row in users
        String chatId = update.getMessage().getChatId().toString();
        String name = update.getMessage().getChat().getFirstName();

        //Check if user already in database
        boolean userExists = false;
        String sql = "SELECT * FROM users WHERE chat_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, chatId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            userExists = true;
            String selectedChatId = resultSet.getString("chat_id");
            String selectedName = resultSet.getString("name");
            System.out.println("[" + selectedChatId + " " + selectedName + "] has been selected.");
        }
        statement.close();
        resultSet.close();

        //Insert into table users
        if (!userExists) {
            sql = "INSERT INTO users (chat_id, name) VALUES (?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, chatId);
            preparedStatement.setString(2, name);
            int rowsInserted = preparedStatement.executeUpdate();
            userExists = rowsInserted > 0;
            preparedStatement.close();
            System.out.println("[" + chatId + " " + name + "] has been registered.");
        }


        //================================= [View]
        message.setText(generateIntro(update, userExists));
    }


    public void generateEntriesEvent(SendMessage message) {
        System.out.println("========= Entries Event Called ========= ");

        String entries = "ENTRIES\n";
        entries += "|---------------- \n";
        for (ArrayList<String> entry : entriesList) {

            if (entry.get(0) == "spend") {
                entries += "-> SPENT | " + entry.get(2) + " | " + entry.get(1) + " | " + entry.get(3) + "\n";
            } else if (entry.get(0) == "earn") {
                entries += "-> EARN | " + entry.get(2) + " | " + entry.get(1) + " | " + entry.get(3) + "\n";
            }

            entries += "|---------------- \n";
        }

        if (entriesList.isEmpty()) {
            message.setText("No Entries. Let's add one.");
        } else {
            message.setText(entries);
        }

    }
    public void generateEventStateOne(String command, SendMessage message) {
        System.out.println("========= Event State One Called ========= ");

        if (command.equals("/spend")) {
            message.setText("Alright, what did you spend on? [Input Category]");
            isInputtingEntry = true;
            typeOfEntry = "spend";

        } else if (command.equals("/earn")) {
            message.setText("Swee! Where did you earn this moolah? [Input Category]");
            isInputtingEntry = true;
            typeOfEntry = "earn";
        }

        entryList.add(typeOfEntry);
        currEventState++; //Update Event State
    }

    public void generateEventStateTwo(String text, SendMessage message) {
        System.out.println("========= Event State Two Called ========= ");

        if (typeOfEntry.equals("spend")) {
            entryList.add(text); //Getting the category
            message.setText("Okay, how much did you spend on " + text + "? [Input Cost]");
        } else if (typeOfEntry.equals("earn")) {
            entryList.add(text); //Getting the category
            message.setText("Okay, how much did you earn from " + text + "? [Input Earnings]");
        } else {
            message.setText("Uh oh.. Something broke.");
        }

        currEventState++; //Update Event State
    }

    public void generateEventStateThree(String text, SendMessage message) {
        System.out.println("========= Event State Three Called ========= ");

        if (typeOfEntry.equals("spend")) {
            entryList.add(text); //Getting the cost
            message.setText("$" + text + ", got it. Now, what's the story behind this? [Input Description]");
        } else if (typeOfEntry.equals("earn")) {
            entryList.add(text); //Getting the earnings
            message.setText("$" + text +"! Nice! How do you feel earning $" + text + "? [Input Description]");
        } else {
            message.setText("Uh oh.. Something broke.");
        }

        currEventState++; //Update Event State
    }

    public void generateEventStateFour(String text, SendMessage message, Update update) throws SQLException {
        System.out.println("========= Event State Four Called ========= ");
        boolean success = updateEntriesList(update);
        if (success) {
            if (typeOfEntry.equals("spend")) {
                entryList.add(text); //Getting the description
                message.setText("Thanks! You have added a new entry: \nSpent $" + entryList.get(2) + " on " + entryList.get(1) + " - \"" + entryList.get(3) + "\"");
            } else if (typeOfEntry.equals("earn")) {
                entryList.add(text); //Getting the description
                message.setText("Thanks! You have added a new entry: \nEarned $" + entryList.get(2) + " from " + entryList.get(1) + " - Feeling: \"" + entryList.get(3) + "\"");
            } else {
                message.setText("Uh oh.. Something broke.");
            }
        } else {
            message.setText("Uh oh.. Entry failed.");
        }

        resetEntry();
    }

    public String generateIntro(Update update, boolean userExists) {
        String intro = "";

        intro += "Hi " + update.getMessage().getChat().getFirstName() +
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

    public boolean updateEntriesList(Update update) throws SQLException {
        System.out.println("Updating Entries");
        //================================= [Model]
        //Thinking in terms of SQL, we need to create a row in entries table.
        String chatId = update.getMessage().getChatId().toString();
        int selectedUserId = 0;
        String category = entryList.get(1);
        Double cost = Double.parseDouble(entryList.get(2));
        String description = entryList.get(3);

        //Check if user already in database
        boolean userExists = false;
        String sql = "SELECT * FROM users WHERE chat_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, chatId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) { //Should only loop once
            userExists = true;
            selectedUserId = resultSet.getInt("user_id");
        }
        statement.close();
        resultSet.close();

        System.out.println("Inserting " + typeOfEntry + " " + category + " " + cost + " " + description + " " + selectedUserId);

        //Insert the entry into entries table.
        boolean successfulInsertion = false;
        if (userExists) {
            sql = "INSERT INTO entries (typeOfEntry, category, cost, description, user_id) VALUES " +
                    "(?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, typeOfEntry);
            preparedStatement.setString(2, category);
            preparedStatement.setDouble(3, cost);
            preparedStatement.setString(4, description);
            preparedStatement.setInt(5, selectedUserId);

            int rowsInserted = preparedStatement.executeUpdate();
            successfulInsertion = rowsInserted > 0;
            preparedStatement.close();

        }

        entriesList.add(entryList);
        return successfulInsertion;
    }

    public void cancelEntry(SendMessage message) {
        message.setText("Entry cancelled.");
        resetEntry();
    }

    public void resetEntry() {
        currEventState = 1;
        isInputtingEntry = false;
        entryList = new ArrayList<String>();
    }

    private static Connection getConnection() throws URISyntaxException, SQLException {
        URI dbUri = new URI(System.getenv("DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

        return DriverManager.getConnection(dbUrl, username, password);
    }
}


