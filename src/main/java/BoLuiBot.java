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
    private final PSQL psql;
    private final ArrayList<String> errorLogs;

    BoLuiBot() throws URISyntaxException, SQLException {
        this.psql = new PSQL();
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

                //Universal Commands. No need to update Query and check User.
                if ("/start".equals(text)) {
                    event = new StartEvent(message, errorLogs, chatId, name);
                    executeEvent(event, message);
                    return; //Code ends here
                }

                //When received a text, check the sender of this text and update text into database.
                boolean isCheckGood = checkingQueryAndUser(chatId, text);
                if (!checkingQueryAndUser(chatId, text)) {
                    event = new NotRegisteredEvent(message, errorLogs, chatId);
                    executeEvent(event, message);
                    return; //Code ends here
                }
                errorLogs.add("Received text and Checking user... isCheckGood " + isCheckGood);

                //Retrieving Information
                boolean isInputtingEntry = psql.getIsUserInputting(chatId);
                String entryType = psql.getUserEntryType(chatId); //userType as defined in entries table

                //Conditionals
                boolean cancelCondition = text.length() >= 7 && text.startsWith("/cancel");
                boolean goodCommandCondition = text.charAt(0) == '/' && !isInputtingEntry;
                boolean badCommandCondition = !cancelCondition && text.charAt(0) == '/' && isInputtingEntry;

                if (goodCommandCondition) {
                    switch (text) {
                        case "/entries":
                            event = new GenEntriesEvent(message, errorLogs, chatId);
                            break;
                        case "/spend":
                            event = new SpendEvent(message, errorLogs, chatId);
                            break;
                        case "/earn":
                            event = new EarnEvent(message, errorLogs, chatId);
                            break;
                        case "/edit":
                            event = new EditEvent(message, errorLogs, chatId);
                            break;
                        case "/delete":
                            event = new DeleteEvent(message, errorLogs, chatId);
                            break;
                        case "/s":
                        case "/e":
                            event = new GenShortcutHelpEvent(message, errorLogs, chatId);
                            break;
                        case "/finance":
                            event = new GenFinancialsEvent(message, errorLogs, chatId);
                            break;
                        case "/help":
                            event = new HelpEvent(message, errorLogs, chatId);
                            break;
                        case "/errorlogs":
                            event = new ErrorLogsEvent(message, errorLogs, chatId);
                            break;
                        default:
                            event = new GenShortcutEvent(message, errorLogs, chatId);
                            break;
                    }

                }
                else if (isInputtingEntry) {
                    if (cancelCondition) { //User cancels entry.
                        event = new CancelEvent(message, errorLogs, chatId);

                    } else if (badCommandCondition) {
                        event = new BadCommandEvent(message, errorLogs, chatId);

                    } else {
                        switch (entryType) {
                            case "spend":
                                event = new SpendEvent(message, errorLogs, chatId);
                                break;
                            case "earn":
                                event = new EarnEvent(message, errorLogs, chatId);
                                break;
                            case "edit":
                                event = new EditEvent(message, errorLogs, chatId);
                                break;
                            case "delete":
                                event = new DeleteEvent(message, errorLogs, chatId);
                                break;
                        }
                    }

                }
                else {
                    event = new EchoEvent(message, errorLogs, chatId);

                }

                assert event != null;
                executeEvent(event, message);
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

                errorLogs.add(callData + " this is calldata");

                if (callData.startsWith("numpad")) {
                    event = new Events.GenNumPadInlineKeyboardEvent(message, errorLogs, Integer.parseInt(chatId), callData, prevAnswer, newMessage);
                } else if (callData.startsWith("fin")) {
                    event = new Events.GenFinInlineKeyboardEvent(message, newMessage, errorLogs, Integer.parseInt(chatId), callData);
                    newMessage.enableHtml(true);
                } else if (callData.startsWith("del")){
                    event = new Events.GenDelInlineKeyboardEvent(message, newMessage, errorLogs, Integer.parseInt(chatId), callData);
                    newMessage.enableHtml(true);
                } else if (callData.startsWith("entry")){
                    event = new Events.GenEntriesInlineKeyboardEvent(message, newMessage, errorLogs,  Integer.parseInt(chatId), callData);
                    message.enableHtml(true);
                }

                try {
                     assert event != null;
                    executeCallbackEvent(event, newMessage, message);
                    //execute(newMessage);
                } catch (TelegramApiException e) {
                     e.printStackTrace();
                }
            }

        } catch (SQLException | TelegramApiException | URISyntaxException throwables) {
            throwables.printStackTrace();
        }
    }

    private void executeCallbackEvent(Event event, EditMessageText newMessage, SendMessage message) throws SQLException, TelegramApiException, URISyntaxException {
        event.generateEvent();
        event.generateOtherEvents();
        event.updateDatabase(); //NOTE THAT UPDATE DATABASE IS AFTER GENERATE EVENT (FOR THE DELETE EVENT)
        if (!(newMessage.getText() == null)){ //cannot execute empty newMessage
            execute(newMessage);
        }
        execute(message);
    }

    private void executeEvent(Event event, SendMessage message) throws SQLException, TelegramApiException, URISyntaxException {
        event.updateDatabase();
        event.generateEvent();
        String msg = message.toString();
        execute(message);
        event.generateOtherEvents();

        if (!msg.equals(message.toString())) { //Only proceed down if message changed.
            errorLogs.add("Message has changed");
            execute(message);
        }
    }

    /**
     * This method checks if the user has been registered and store the text into the database.
     * @param chatId int variable that stores the chatId. ChatId is unique
     * @param text String variable that stores the latest text sent by the user
     * @return Returns a boolean to show that the user is registered or not.
     * @throws SQLException Throws an exception when query is unsuccessful
     */
    private boolean checkingQueryAndUser(int chatId, String text) throws SQLException {
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


