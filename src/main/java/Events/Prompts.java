package Events;

import java.util.ArrayList;

public class Prompts {
    private static final ArrayList<String> prompts = new ArrayList<>();

    public static String generateEventOneSpendPrompt() {
        genListOfEventOneSpendPrompts();
        int idx = (int) (Math.random() * prompts.size());
        return prompts.get(idx);
    }
    private static void genListOfEventOneSpendPrompts() {
        prompts.clear();
        prompts.add("Alright, what did you spend on? [Input Category]");
        prompts.add("Okay, what did you spend on? [Input Category]");
        prompts.add("Let's begin, what did you spend on? [Input Category]");
        prompts.add("Let's start, what did you spend on? [Input Category]");
        prompts.add("Must be food again, right? What did you spend on? [Input Category]");
        prompts.add("Oo did you buy something for me? What did you spend on? [Input Category]");
        prompts.add("Wah ballin' sia! What did you spend on? [Input Category]");
        prompts.add("Eh.. Seems like you're buying a lot of things ah. What did you spend on? [Input Category]");
        prompts.add("What did you spend on? Worth not? [Input Category]");
        prompts.add("What did you spend on?  [Input Category]");
    }
    public static String generateEventTwoSpendPrompt(String category) {
        genListOfEventTwoSpendPrompts(category);
        int idx = (int) (Math.random() * prompts.size());
        return prompts.get(idx);
    }
    private static void genListOfEventTwoSpendPrompts(String category) {
        prompts.clear();
        prompts.add("Okay, how much did you spend on " + category + "? [Input Cost]");
        prompts.add("Alright and how much did you spend on " + category + "? [Input Cost]");
        prompts.add(category + "? Nice! How much did you spend on it? [Input Cost]");
        prompts.add(category + "? Okay okay! How much did you pay? [Input Cost]");

    }
    public static String generateEventThreeSpendPrompt(String cost) {
        genListOfEventThreeSpendPrompts(cost);
        int idx = (int) (Math.random() * prompts.size());
        return prompts.get(idx);
    }
    private static void genListOfEventThreeSpendPrompts(String cost) {
        prompts.clear();
        prompts.add("$" + cost + ", got it. Now, what's the story behind this? If no comment, enter 'NA'. [Input Comment]");
        prompts.add("$" + cost + ", got it. Let's add a comment to this. If no comment, enter 'NA'. [Input Comment]");
        prompts.add("$" + cost + ", got it. Can you elaborate on this? If no comment, enter 'NA'. [Input Comment]");
        prompts.add("$" + cost + ", got it. Let's make a comment for this entry! If no comment, enter 'NA'. [Input Comment]");
    }

    public static String generateEventOneDeletePrompt() {
        return "Which entry(entries) would you like to delete? Separate entry number(s) with commas as such: \n[Entry Num],[Entry Num],[EntryNum].";
    }
    public static String generateEventTwoDeletePrompt(String entryNum) {
        return "Entry " + entryNum + ". will be deleted. " +
                "If you wish to cancel deletion, enter /cancel, else enter anything to continue.";
    }

    public static String generateEventOneEarnPrompt() {
        genListOfEventOneEarnPrompts();
        int idx = (int) (Math.random() * prompts.size());
        return prompts.get(idx);
    }
    private static void genListOfEventOneEarnPrompts() {
        prompts.clear();
        prompts.add("Swee! Where did you earn this money from? [Input Category]");
        prompts.add("Nice! Where did you earn this money from? [Input Category]");
        prompts.add("Awesome! Where did you earn this money from? [Input Category]");
        prompts.add("Wow! Where did you earn this money from? [Input Category]");
        prompts.add("Aight aight, where did you get this bread from? [Input Category]");
        prompts.add("Someone's gotta bring in the dough! Nice. Where did you earn this from? [Input Category]");
        prompts.add("Brotherrr, nicely done! Where did you earn this money from? [Input Category]");
        prompts.add("Storing this is gonna take up a whole lotta space, money's too much! Where did you earn this money from? [Input Category]");
        prompts.add("Winner winner chicken dinner! Where did you earn this money from? [Input Category]");
        prompts.add("Would you like to donate to me? Where did you earn this money from? [Input Category]");
        prompts.add("Yooo this is mad! Where did you earn this money from, dawg? [Input Category]");
        prompts.add("You're finessing life dude! Where did you earn this money from? [Input Category]");
        prompts.add("I hope you didn't steal this! Where did you get this loot from? [Input Category]");

    }
    public static String generateEventTwoEarnPrompt(String category) {
        genListOfEventTwoEarnPrompts(category);
        int idx = (int) (Math.random() * prompts.size());
        return prompts.get(idx);
    }
    private static void genListOfEventTwoEarnPrompts(String category) {
        prompts.clear();
        prompts.add("Okay, how much did you earn from " + category + "? [Input Earnings]");
        prompts.add("Awesome! How much did you earn from " + category + "? [Input Earnings]");
        prompts.add("Got it from " + category + "? Nice man, how much did you earn? [Input Earnings]");
        prompts.add("That's wassup man! That's my man! How much did you earn from " + category + "? [Input Earnings]");

    }
    public static String generateEventThreeEarnPrompt(String cost) {
        genListOfEventThreeEarnPrompts(cost);
        int idx = (int) (Math.random() * prompts.size());
        return prompts.get(idx);
    }
    private static void genListOfEventThreeEarnPrompts(String cost) {
        prompts.clear();
        prompts.add("Nice! How do you feel earning $" + cost + "? If no comment, enter 'NA'. [Input Comment]");
        prompts.add("Wow that's a lot! How do you feel earning $" + cost + "? If no comment, enter 'NA'. [Input Comment]");
        prompts.add(cost + "?? Brooo, that's sick! How do you feel about earning this much? If no comment, enter 'NA'. [Input Comment]");

    }

    public static String generateEventOneEditPrompt() {
        return "Which entry would you like to edit? [Input Entry Number]";
    }
    public static String generateEventTwoEditPrompt() {
        return "Copy the statement below. After editing your new entry, send it. " +
                generateEditInputPrompt();
    }
    public static String generateEventThreeEditErrorPrompt() {
        return "You have entered a wrong edit input. Please review and submit again. " +
                generateEditInputPrompt();
    }
    private static String generateEditInputPrompt() {
        return "Ensure that your input is as follows: \ncommand : category : cost/earning : comment";
    }

    public static String generateHelpPrompt() {
        return "<b>Help</b> \n\n" +
                "Bo keeps track of your spendings and earnings in a straight forward way. You can enter entries with commands /spend or /earn.\n\n" +
                "To <em>view</em> entries, type /entries. \n" +
                "To <em>edit</em> entries, type /edit. \n" +
                "To <em>delete</em> entries, type /delete. \n" +
                "To <em>view</em> financials, type /finance.\n\n" +
                "Made a mistake while inputting an entry? Type /cancel to reset it. \n\n" +
                "<b><u>Inputs Types</u></b> \n - Category e.g Food, Clothes <b>[Single word]</b>\n - Cost/Earning e.g $12.34, $1234 <b>[Numeric]</b>\n - Comment e.g Bought pizza for lunch <b>[Phrase]</b>\n\n"+
                "Got the hang of inputting entries? Use these shortcuts, /s or /e, for an <em>effortless</em> entry. \n\n" +
                "<em>Last thing, Bo will sometimes echo your latest message.. </em>";
    }

    public static String generateShortcutHelpPrompt() {
        return "<b>Shortcut Help</b> \n\nSo, you have gotten the hang of inputting entries? That's great! " +
                "Sometimes, we are in a rush and we do not have time to wait for Bo to reply to our inputs. " +
                "Here's a more <em>efficient</em> way.\n\n" +
                "Here are some examples of using this shortcut:\n " +
                "- /s <b>[Category] [Cost] [Comment]</b>\n " +
                "- /s Food 4.50 Salted egg chicken rice\n " +
                "- /e Income 1000 Finally! Internship pay yo!\n\n\n" +
                "<em>These shortcuts work as per normal, they can be deleted or edited.</em>";
    }

    /**
     * This method generates the intro text
     * @param name String variable that stores the first name of the user
     * @return Returns the introduction text
     */
    public static String generateIntro(String name) {
        String intro = "<b>Start</b> \n\n";

        intro += "Hi " + name +
                "! I am Bo Lui and I welcome you to Sir Brendan's financial tracker to track how deep your pockets are! Sir Brendan is my creator.\n\n";
        intro += "I am your personal finance bot! I store your spendings and earnings in a simple way. You can view, edit, delete and add entries.\n\n";
        intro += "For now, I am in the beta stages and so, I have limited functionalities. I may crash on you. I probably will crash on you... " +
                "But! Your opinion and feedback to the creator will surely improve my system, so thank you for using me! \n\n";
        intro += "Type /help to see what I can do.\n\n";
        intro += "<em>You have established a connection with the server. This connection is 24/7. All your data is saved into the database securely.</em>\n";

        return intro;
    }
    public static String generateCancelPrompt() {
        return "Entry cancelled.";
    }
    public static String generateNotRegisteredPrompt() { return "Seems like you are not registered. Enter /start to register!";}
    public static String generateBadCommandPrompt() {return "Sorry, something's wrong. You're in the midst of inputting an entry. Type /cancel to cancel the entry and try that command again.";}

    public static String generateEchoPrompt(String text) {return text + " <code>-Bo</code>";}

    public static String generateInputtingCostErrorPrompt() {
        return "Uh oh.. Input was not recognised. Did you keep it 2 d.p, numeric " +
                "and it is a positive value? Try it again or enter /cancel to cancel entry.";
    }

    public static String generateInputtingCategoryErrorPrompt() {
        return "Uh oh.. Input was not recognised. Ensure that Category input has only one word." +
                " Try it again or enter /cancel to cancel entry.";
    }

    public static String generateSuccessfulEarnEntryPrompt(String[] entryList) {
        return "Thanks! You have added a new entry: \nEarned $" + entryList[1] +
                " from " + entryList[0] + " - \"" + entryList[2] + "\"";
    }

    public static String generateInputtingEntryNumErrorPrompt(String entryNum) {
        return "Uh oh.. Entry " + entryNum + ". doesn't exist! Try it again or enter /cancel to cancel entry.";
    }

    public static String generateNoEntriesToDeletePrompt() {
        return "You do not have any entries to delete. Let's add one! Enter /spend or /earn to start inputting.";
    }

    public static String generateNoEntriesToEditPrompt() {
        return "You do not have any entries to edit. Let's add one! Enter /spend or /earn to start inputting.";
    }
}

