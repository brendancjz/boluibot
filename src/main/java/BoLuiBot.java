import Events.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URISyntaxException;
import java.sql.*;
import java.util.ArrayList;
class BoLuiBot extends TelegramLongPollingBot {

    BoLuiBot() throws URISyntaxException, SQLException {
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
                String name = update.getMessage().getChat().getFirstName();
                String text = update.getMessage().getText();

                Event event = null;
                PSQL psql = new PSQL();
                System.out.println("OPENED CONNECTION");

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
                            event = new SpendEvent(message, psql, chatId);
                            break;
                        case "/earn":
                            event = new EarnEvent(message, psql, chatId);
                            break;
                        case "/edit":
                            event = new EditEvent(message, psql, chatId);
                            break;
                        case "/delete":
                            event = new DeleteEvent(message, psql, chatId);
                            break;
                        case "/s":
                        case "/e":
                            event = new GenShortcutHelpEvent(message, psql, chatId);
                            break;
                        case "/finance":
                            event = new GenFinancialsEvent(message, psql, chatId);
                            break;
                        case "/help":
                            event = new HelpEvent(message, psql, chatId);
                            break;
                        case "/errorlogs":
                            //event = new ErrorLogsEvent(message, psql, chatId);
                            break;
                        default:
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
                                event = new SpendEvent(message, psql, chatId);
                                break;
                            case "earn":
                                event = new EarnEvent(message, psql, chatId);
                                break;
                            case "edit":
                                event = new EditEvent(message, psql, chatId);
                                break;
                            case "delete":
                                event = new DeleteEvent(message, psql, chatId);
                                break;
                        }
                    }

                }
                else {
                    event = new EchoEvent(message, psql, chatId);

                }

                assert event != null;
                executeEvent(event, message, psql);
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
                    event = new Events.GenNumPadInlineKeyboardEvent(message, psql, Integer.parseInt(chatId), callData, prevAnswer, newMessage);
                } else if (callData.startsWith("fin")) {
                    event = new Events.GenFinInlineKeyboardEvent(message, newMessage, psql, Integer.parseInt(chatId), callData);
                    newMessage.enableHtml(true);
                } else if (callData.startsWith("del")){
                    event = new Events.GenDelInlineKeyboardEvent(message, newMessage, psql, Integer.parseInt(chatId), callData);
                    newMessage.enableHtml(true);
                } else if (callData.startsWith("entry")){
                    event = new Events.GenEntriesInlineKeyboardEvent(message, newMessage, psql,  Integer.parseInt(chatId), callData);
                    message.enableHtml(true);
                }

                try {
                     assert event != null;
                    executeCallbackEvent(event, newMessage, message, psql);
                } catch (TelegramApiException e) {
                     e.printStackTrace();
                }
            }

        } catch (SQLException | TelegramApiException | URISyntaxException throwables) {
            throwables.printStackTrace();
        }
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
            System.out.println("Message has changed");
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
            System.out.println("Message has changed");
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
        boolean everythingGood = false;


        //SQL Queries
        //2. Check if chatId is in database
        boolean userExists = psql.isUserRegistered(chatId);

        //3. If TRUE, Store text into database of this user.
        if (userExists) {
            psql.updateUserText(chatId, text);
            everythingGood= true;
        }

        return everythingGood;
    }
}


