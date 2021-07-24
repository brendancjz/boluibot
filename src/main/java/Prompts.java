import java.util.ArrayList;

class Prompts {
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
        prompts.add("$" + cost + ", got it. Now, what's the story behind this? [Input Comment]");
        prompts.add("$" + cost + ", got it. Let's add a comment to this. [Input Comment]");
        prompts.add("$" + cost + ", got it. Can you elaborate on this? [Input Comment]");
        prompts.add("$" + cost + ", got it. Let's make a comment for this entry! [Input Comment]");
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
        prompts.add("Got it from " + category + "? Nice man, how did you earn? [Input Earnings]");
        prompts.add("That's wassup man! That's my man! How much did you earn from " + category + "? [Input Earnings]");

    }

    public static String generateEventThreeEarnPrompt(String cost) {
        genListOfEventThreeEarnPrompts(cost);
        int idx = (int) (Math.random() * prompts.size());
        return prompts.get(idx);
    }
    private static void genListOfEventThreeEarnPrompts(String cost) {
        prompts.clear();
        prompts.add("Nice! How do you feel earning $" + cost + "? [Input Comment]");
        prompts.add("Wow that's a lot! How do you feel earning $" + cost + "? [Input Comment]");
        prompts.add(cost + "?? Brooo, that's sick! How do you feel about earning this much? [Input Comment]");

    }

}