import Events.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

class BoLuiBot extends TelegramLongPollingBot {

    BoLuiBot() {

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
        //TODO
        // Allow user to choose language for Telebot (sassy mode (e.g. EH STOP SPENDING), cat (e.g. how much kibble did you earn)) (SUPER LOW)
        // Need more commands to spice up user experience.
        // /Bill for user to key in recurring payment (LOW)
        // /Setgoal for user to set target amount spend for each category (LOW)
        // We check if the update has a message and the message has text


        try {

            if (update.hasMessage() && update.getMessage().hasText()) {
                SendMessage message = new SendMessage(); // Create a SendMessage object with mandatory fields
                message.setChatId(update.getMessage().getChatId().toString());
                message.enableHtml(true);


                int chatId = Integer.parseInt(update.getMessage().getChatId().toString());
                PSQL psql = new PSQL();
                String text = update.getMessage().getText();
                System.out.println("OPENED CONNECTION");

                if (chatId > 0) { //Personal Chats have positive chatId while Group Chats have negative chatId
                    personalChatCode(message, psql, update, chatId);

                    //Wit is used to interact with Users. It is still in development
                    //witJavaCommand(text);
                } else {
                    groupChatCode(message, psql, update);
                }
            }
            else if (update.hasCallbackQuery()) {
                String callData = update.getCallbackQuery().getData();
                String prevAnswer = update.getCallbackQuery().getMessage().getText();
                int messageId = update.getCallbackQuery().getMessage().getMessageId();
                String chatId = update.getCallbackQuery().getMessage().getChatId().toString();

                EditMessageText newMessage = new EditMessageText();
                newMessage.setChatId(chatId);
                newMessage.setMessageId(messageId);

                SendMessage message = new SendMessage();
                message.setChatId(chatId);

                Event event = null;
                PSQL psql = new PSQL();

                if (callData.startsWith("numpad")) {
                    System.out.println("========= Number Pad Keyboard Events.Event Called ========= ");
                    event = new Events.GenNumPadInlineKeyboardEvent(message, psql, Integer.parseInt(chatId), callData, prevAnswer, newMessage);
                } else if (callData.startsWith("fin")) {
                    System.out.println("========= Generate Finance Keyboard Events.Event Called ========= ");
                    event = new Events.GenFinInlineKeyboardEvent(message, newMessage, psql, Integer.parseInt(chatId), callData);
                    newMessage.enableHtml(true);
                } else if (callData.startsWith("del")){
                    System.out.println("========= Generate Delete Keyboard Events.Event Called ========= ");
                    event = new Events.GenDelInlineKeyboardEvent(message, newMessage, psql, Integer.parseInt(chatId), callData);
                    newMessage.enableHtml(true);
                } else if (callData.startsWith("entry")){
                    System.out.println("========= Generate Entries Keyboard Events.Event Called ========= ");
                    event = new Events.GenEntriesInlineKeyboardEvent(message, newMessage, psql,  Integer.parseInt(chatId), callData);
                    message.enableHtml(true);
                }

                try {
                     assert event != null;
                    System.out.println("Executing Callback Event");
                    executeCallbackEvent(event, newMessage, message, psql);
                } catch (TelegramApiException e) {
                     e.printStackTrace();
                }
            }

        } catch (SQLException | TelegramApiException | URISyntaxException | IOException throwables) {
            throwables.printStackTrace();
        }
    }

    private void witJavaCommand(String text) {
        try {
            String encodedText = URLEncoder.encode(text, "UTF-8");
            URL url = new URL("https://api.wit.ai/message?v=20210813&q=" + encodedText);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer UNI5BQKKTCNFTDHAV7JW3JZLPUAWD5N4");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }

            conn.disconnect();

        } catch (IOException e) {

            e.printStackTrace();

        }
    }

    private void groupChatCode(SendMessage message, PSQL psql, Update update) throws TelegramApiException, URISyntaxException, SQLException {

        String text = update.getMessage().getText();
        String name = update.getMessage().getFrom().getFirstName();
        Integer messageId = update.getMessage().getMessageId();
        int chatId = Integer.parseInt(update.getMessage().getFrom().getId().toString());

        if (text.endsWith("@bo_lui_test_bot")) {
            message.setText("Hi " + name + ", I see your msg in this group chat. Thanks for having me in this cosy group. Right now, " +
                    "I am unable to facilitate finance tracking in a group setting. But, I will soon so stay tune!");
            message.setReplyToMessageId(messageId);
            execute(message);
            psql.closeConnection();
            System.out.println("CLOSED CONNECTION");
        }
    }

    private void personalChatCode(SendMessage message, PSQL psql, Update update, int chatId) throws URISyntaxException, SQLException, TelegramApiException, IOException {
        String name = update.getMessage().getChat().getFirstName();
        String text = update.getMessage().getText();

        Event event = null;

        //Universal Commands. No need to update Query and check User.
        if ("/start".equals(text)) {
            System.out.println("=== Start Events.Event Called === ");
            event = new StartEvent(message, psql, chatId, name);
            executeEvent(event, message, psql);
            return; //Code ends here
        }

        //When received a text, check the sender of this text and update text into database.
        boolean isCheckGood = checkingQueryAndUser(chatId, text, psql);
        if (!isCheckGood) {
            System.out.println("=== Not Registered Events.Event Called === ");
            event = new NotRegisteredEvent(message, psql, chatId);
            executeEvent(event, message, psql);
            return; //Code ends here
        } else {
            System.out.println("=== User and Text All Good === ");
        }

        //Retrieving Information
        boolean isInputtingEntry = psql.getIsUserInputting(chatId);
        String entryType = psql.getUserEntryType(chatId); //userType as defined in entries table

        //Conditionals
        boolean cancelCondition = text.equals("Cancel") || text.length() >= 7 && text.startsWith("/cancel");
        boolean goodCommandCondition = text.charAt(0) == '/' && !isInputtingEntry;
        boolean badCommandCondition = !cancelCondition && text.charAt(0) == '/' && isInputtingEntry;

        if (goodCommandCondition) {
            switch (text) {
                case "/entries":
                    System.out.println("========= Entries Events.Event Called ========= ");
                    event = new GenEntriesEvent(message, psql, chatId);
                    break;
                case "/spend":
                    System.out.println("========= Spend Events.Event Called ========= ");
                    event = new SpendEvent(message, psql, chatId);
                    break;
                case "/earn":
                    System.out.println("========= Earn Events.Event Called ========= ");
                    event = new EarnEvent(message, psql, chatId);
                    break;
                case "/edit":
                    System.out.println("========= Edit Events.Event Called ========= ");
                    event = new EditEvent(message, psql, chatId);
                    break;
                case "/delete":
                    System.out.println("========= Delete Events.Event Called ========= ");
                    event = new DeleteEvent(message, psql, chatId);
                    break;
                case "/s":
                case "/e":
                    System.out.println("========= Shortcut Help Events.Event Called ========= ");
                    event = new GenShortcutHelpEvent(message, psql, chatId);
                    break;
                case "/finance":
                    System.out.println("========= Generate Financials Events.Event Called ========= ");
                    event = new GenFinancialsEvent(message, psql, chatId);
                    break;
                case "/help":
                    System.out.println("========= Help Events.Event Called ========= ");
                    event = new HelpEvent(message, psql, chatId);
                    break;
                case "/feedback":
                    System.out.println("========= Feedback Events.Event Called ========= ");
                    event = new FeedbackEvent(message, psql, chatId);
                    break;
                case "/feedbacklogs":
                    System.out.println("========= Feedback Log Events.Event Called ========= ");
                    generateFeedBackFile(chatId,psql);
                    break;
                default:
                    System.out.println("========= Generate Shortcut Events.Event Called ========= ");
                    event = new GenShortcutEvent(message, psql, chatId);
                    break;
            }

        }
        else if (isInputtingEntry) {
            if (cancelCondition) { //User cancels entry.
                event = new CancelEvent(message, psql, chatId);

            } else if (badCommandCondition) {
                event = new BadCommandEvent(message, psql, chatId);

            } else {
                switch (entryType) {
                    case "spend":
                        System.out.println("========= Spend Events.Event Called ========= ");
                        event = new SpendEvent(message, psql, chatId);
                        break;
                    case "earn":
                        System.out.println("========= Earn Events.Event Called ========= ");
                        event = new EarnEvent(message, psql, chatId);
                        break;
                    case "edit":
                        System.out.println("========= Edit Events.Event Called ========= ");
                        event = new EditEvent(message, psql, chatId);
                        break;
                    case "delete":
                        System.out.println("========= Delete Events.Event Called ========= ");
                        event = new DeleteEvent(message, psql, chatId);
                        break;
                    case "feedback":
                        System.out.println("========= Feedback Events.Event Called ========= ");
                        event = new FeedbackEvent(message, psql, chatId);
                        break;
                }
            }
        }
        else {
            event = new EchoEvent(message, psql, chatId);
        }

        assert event != null;
        System.out.println("Executing Event");
        executeEvent(event, message, psql);
    }

    private void generateFeedBackFile(int chatId, PSQL psql) throws IOException, TelegramApiException, SQLException {
        //Creating SendDocuments
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(String.valueOf(chatId));
        File file = new File("this_is_test.csv");
        FileWriter fileWriter = new FileWriter(file);
        ArrayList<String> feedbackFile = psql.getAllFeedbackRows();
        for (String rowData : feedbackFile){
            fileWriter.write(rowData);
        }
        InputFile inputFile = new InputFile(file);
        inputFile.setMedia(file);
        sendDocument.setDocument(inputFile);
        fileWriter.close();
        execute(sendDocument);
    }

    private void executeCallbackEvent(Event event, EditMessageText newMessage, SendMessage message, PSQL psql) throws SQLException, TelegramApiException, URISyntaxException {
        event.generateEvent();
        String msg = message.toString();
        event.updateDatabase(); //NOTE THAT UPDATE DATABASE IS AFTER GENERATE EVENT (FOR THE DELETE EVENT)
        if (!(newMessage.getText() == null)){ //cannot execute empty newMessage
            execute(newMessage);
        }
        execute(message);

        event.generateOtherEvents();
        if (!msg.equals(message.toString())) { //Only proceed down if message changed.
            execute(message);
        }

        psql.closeConnection();
    }

    private void executeEvent(Event event, SendMessage message, PSQL psql) throws SQLException, TelegramApiException, URISyntaxException {
        event.updateDatabase();
        event.generateEvent();
        String msg = message.toString();
        execute(message);
        event.generateOtherEvents();

        if (!msg.equals(message.toString())) { //Only proceed down if message changed.
            execute(message);
        }

        psql.closeConnection();
        System.out.println("CLOSED CONNECTION");
    }

    /**
     * This method checks if the user has been registered and store the text into the database.
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param text String variable that stores the latest text sent by the user
     * @return Returns a boolean to show that the user is registered or not.
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private boolean checkingQueryAndUser(int chatId, String text, PSQL psql) throws SQLException {
        boolean userExists = psql.isUserRegistered(chatId);

        if (userExists) {
            psql.updateUserText(chatId, text);
        }

        return userExists;
    }
}


