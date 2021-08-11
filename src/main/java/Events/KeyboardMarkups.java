package Events;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

class KeyboardMarkups {
        
    public KeyboardMarkups(){
    }

    //KeyboardMarkUps
    public static InlineKeyboardMarkup deleteKBSecond(YearMonth currYearMonth) {

        List<InlineKeyboardButton> row = new ArrayList<>();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("Confirm");
        button1.setCallbackData("del_confirm_" + currYearMonth.getYear() + "_" + currYearMonth.getMonthValue());
        row.add(button1);

        keyboard.add(row);

        row = new ArrayList<>();

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("Back");
        button2.setCallbackData("del_cancel_" + currYearMonth.getYear() + "_" + currYearMonth.getMonthValue()); //direct back
        row.add(button2);

        keyboard.add(row);
        
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        inlineKeyboard.setKeyboard(keyboard);
        return inlineKeyboard;
    }

    //KeyboardMarkUps
    public static InlineKeyboardMarkup entriesKB(YearMonth prevMonth, YearMonth nextMonth) {
        List<InlineKeyboardButton> row = new ArrayList<>();

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("<");
        button1.setCallbackData("entry_" + prevMonth.getYear() + "_" + prevMonth.getMonthValue()); //e.g. fin_2021_7
        row.add(button1);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(">");
        button2.setCallbackData("entry_" + nextMonth.getYear() + "_" + nextMonth.getMonthValue());
        row.add(button2);

        keyboard.add(row);
        row = new ArrayList<>();

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        inlineKeyboard.setKeyboard(keyboard);
        return inlineKeyboard;
    }

    //KeyboardMarkUps
    public static InlineKeyboardMarkup numpadKB() {

        List<InlineKeyboardButton> row = new ArrayList<>();
        Stack<InlineKeyboardButton> stack = new Stack<>();

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (int i = 9; i >= 1; i--) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(Integer.valueOf(i).toString());
            button.setCallbackData("numpad_" + i);
            stack.push(button);

            if (i == 7 ||  i == 4 || i == 1) {
                while (!stack.isEmpty()) {
                    row.add(stack.pop());
                }
                keyboard.add(row);
                row = new ArrayList<>();
                stack = new Stack<>();
            }

        }

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("0");
        button1.setCallbackData("numpad_0");
        row.add(button1);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(".");
        button2.setCallbackData("numpad_dot");
        row.add(button2);

        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText("Del");
        button3.setCallbackData("numpad_del");
        row.add(button3);

        keyboard.add(row);

        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText("Done");
        button4.setCallbackData("numpad_done");
        row = new ArrayList<>();
        row.add(button4);

        keyboard.add(row);

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        inlineKeyboard.setKeyboard(keyboard);
        return inlineKeyboard;
    }

    //KeyboardMarkUps
    public static InlineKeyboardMarkup financeKB(YearMonth prevMonth, YearMonth currMonth, YearMonth nextMonth) {

        List<InlineKeyboardButton> row = new ArrayList<>();

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("<");
        button1.setCallbackData("fin_" + prevMonth.getYear() + "_" + prevMonth.getMonthValue()); //e.g. fin_2021_7
        row.add(button1);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(">");
        button2.setCallbackData("fin_" + nextMonth.getYear() + "_" + nextMonth.getMonthValue());
        row.add(button2);

        keyboard.add(row);
        row = new ArrayList<>();

        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText(String.valueOf(LocalDate.now().getMonth()));
        button3.setCallbackData("fin_revert");
        row.add(button3);

        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText("Refresh");
        button4.setCallbackData("fin_refresh_" + currMonth.getYear() + "_" + currMonth.getMonthValue());
        row.add(button4);

        keyboard.add(row);

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        inlineKeyboard.setKeyboard(keyboard);
        return inlineKeyboard;
    }

    //KeyboardMarkUps
    public static InlineKeyboardMarkup deleteKB(YearMonth prevMonth, YearMonth currMonth, YearMonth nextMonth) {

        List<InlineKeyboardButton> row = new ArrayList<>();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("<");
        button1.setCallbackData("del_" + prevMonth.getYear() + "_" + prevMonth.getMonthValue()); //e.g. fin_2021_7
        row.add(button1);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(">");
        button2.setCallbackData("del_" + nextMonth.getYear() + "_" + nextMonth.getMonthValue());
        row.add(button2);

        keyboard.add(row);
        row = new ArrayList<>();

        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText("Delete " + LocalDate.now().getMonth());
        button3.setCallbackData("del_month_" + currMonth.getYear() + "_" + currMonth.getMonthValue());
        row.add(button3);

        keyboard.add(row);

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        inlineKeyboard.setKeyboard(keyboard);
        return inlineKeyboard;
    }

    public static ReplyKeyboardMarkup getEarnReplyKeyboardMarkup() {
        String[] eCategory = {"Allowance", "Income", "Investment", "Cancel"};


        KeyboardRow row = new KeyboardRow();

        List<KeyboardRow> keyboard = new ArrayList<>();
        int count = 1;
        for (String category : eCategory) {
            if (count > 1 && count % 2 == 1) {
                row = new KeyboardRow();
            }

            KeyboardButton button = new KeyboardButton();
            button.setText(category);
            row.add(button);
            if (count % 2 == 0) {
                keyboard.add(row);
            } else if (count == eCategory.length) {
                keyboard.add(row);
            }
            count++;
        }

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);
        return markup;
    }

    public static ReplyKeyboardMarkup getSpendReplyKeyboardMarkup() {
        String[] sCategory = {"Entertainment","Food","Gift","Shopping","Transport", "Utilities", "Cancel"};


        KeyboardRow row = new KeyboardRow();

        List<KeyboardRow> keyboard = new ArrayList<>();
        int count = 1;
        for (String category : sCategory) {
            if (count > 1 && count % 2 == 1) {
                row = new KeyboardRow();
            }

            KeyboardButton button = new KeyboardButton();
            button.setText(category);
            row.add(button);
            if (count % 2 == 0) {
                keyboard.add(row);
            } else if (count == sCategory.length) {
                keyboard.add(row);
            }
            count++;
        }

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);
        return markup;
    }
    
}
