package bhupendra.ai.launcher.managers;

import bhupendra.ai.launcher.managers.TextProcessor;


import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;

import android.util.Log;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import bhupendra.ai.launcher.tuils.Tuils;


public class TextProcessor {
    public static final Pattern unnecessarySpaces = Pattern.compile("\\s{2,}");
    public static final Pattern calculusPattern = Pattern.compile("([\\+\\-\\*\\/\\^])(\\d+\\.?\\d*)");
    public static final String SPACE_REGEXP = "\\s";


    private interface RegexAction {
        void apply(SpannableStringBuilder builder, int start, int end);
    }

    private static void applyRegex(SpannableStringBuilder builder, String regex, RegexAction action) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(builder);
        int offset = 0;
        while (matcher.find()) {
            int start = matcher.start() + offset;
            int end = matcher.end() + offset;
            int oldLen = builder.length();
            action.apply(builder, start, end);
            offset += builder.length() - oldLen;
            matcher = pattern.matcher(builder);
            matcher.region(start + (builder.length() - oldLen), builder.length());
        }
    }
    public static void addSeparator(List<String> list, String separator) {
            for (int count = 0; count < list.size(); count++)
                list.set(count, list.get(count).concat(separator));
        }

    public static void addPrefix(List<String> list, String prefix) {
            for (int count = 0; count < list.size(); count++) {
                list.set(count, prefix.concat(list.get(count)));
            }
        }

    public static String[] toString(Enum[] enums) {
            String[] arr = new String[enums.length];
            for(int count = 0; count < enums.length; count++) arr[count] = enums[count].name();
            return arr;
        }

    public static char firstNonDigit(String s) {
            if(s == null) {
                return 0;
            }
    
            char[] chars = s.toCharArray();
    
            for (char c : chars) {
                if (!Character.isDigit(c)) {
                    return c;
                }
            }
    
            return 0;
        }

    public static boolean isNumber(String s) {
            if(s == null || s.length() == 0) {
                return false;
            }
    
            char[] chars = s.toCharArray();
    
            for (char c : chars) {
                if (!Character.isDigit(c)) {
                    return false;
                }
            }
    
            return true;
        }

    public static boolean isPhoneNumber(String s) {
            if(s == null) {
                return false;
            }
            char[] chars = s.toCharArray();
    
            for (char c : chars) {
                if (Character.isLetter(c)) {
                    return false;
                }
            }
    
            return true;
        }

    public static boolean isAlpha(String s) {
            if(s == null) {
                return false;
            }
            char[] chars = s.toCharArray();
    
            for (char c : chars)
                if (!Character.isLetter(c))
                    return false;
    
            return true;
        }

    public static int alphabeticCompare(String s1, String s2) {
            String cmd1 = removeSpaces(s1).toLowerCase();
            String cmd2 = removeSpaces(s2).toLowerCase();
    
            for (int count = 0; count < cmd1.length() && count < cmd2.length(); count++) {
                char c1 = cmd1.charAt(count);
                char c2 = cmd2.charAt(count);
    
                if (c1 < c2) {
                    return -1;
                } else if (c1 > c2) {
                    return 1;
                }
            }
    
            if (s1.length() > s2.length()) {
                return 1;
            } else if (s1.length() < s2.length()) {
                return -1;
            }
            return 0;
        }

    public static String filesToPlanString(List<File> files, String separator) {
            if(files == null || files.size() == 0) {
                return null;
            }
    
            StringBuilder builder = new StringBuilder();
            int limit = files.size() - 1;
            for (int count = 0; count < files.size(); count++) {
                builder.append(files.get(count).getName());
                if (count < limit) {
                    builder.append(separator);
                }
            }
            return builder.toString();
        }

    public static String toPlanString(String[] strings, String separator) {
            if(strings == null) {
                return Tuils.EMPTYSTRING;
            }
    
            String output = Tuils.EMPTYSTRING;
            for (int count = 0; count < strings.length; count++) {
                output = output.concat(strings[count]);
                if (count < strings.length - 1) output = output.concat(separator);
            }
            return output;
        }

    public static String toPlanString(String[] strings) {
            if (strings != null) {
                return TextProcessor.toPlanString(strings, Tuils.NEWLINE);
            }
            return Tuils.EMPTYSTRING;
        }

    public static String toPlanString(String separator, List strings) {
            if(strings == null) {
                return Tuils.EMPTYSTRING;
            }
    
            String output = Tuils.EMPTYSTRING;
            for (int count = 0; count < strings.size(); count++) {
                output = output.concat(strings.get(count).toString());
                if (count < strings.size() - 1) output = output.concat(separator);
            }
            return output;
        }

    public static String toPlanString(List<String> strings, String separator) {
            if(strings != null) {
                String[] object = new String[strings.size()];
                return TextProcessor.toPlanString(strings.toArray(object), separator);
            }
            return Tuils.EMPTYSTRING;
        }

    public static String toPlanString(List<String> strings) {
            return TextProcessor.toPlanString(strings, Tuils.NEWLINE);
        }

    public static String toPlanString(Object[] objs, String separator) {
            if(objs == null) {
                return Tuils.EMPTYSTRING;
            }
    
            StringBuilder output = new StringBuilder();
            for(int count = 0; count < objs.length; count++) {
                output.append(objs[count]);
                if(count < objs.length - 1) {
                    output.append(separator);
                }
            }
            return output.toString();
        }

    public static String removeUnncesarySpaces(String string) {
            return unnecessarySpaces.matcher(string).replaceAll(Tuils.SPACE);
        }

    public static String removeSpaces(String string) {
            return string.replaceAll(SPACE_REGEXP, Tuils.EMPTYSTRING);
        }

    public static double eval(final String str) {
            return new Object() {
                int pos = -1, ch;
    
                void nextChar() {
                    ch = (++pos < str.length()) ? str.charAt(pos) : -1;
                }
    
                boolean eat(int charToEat) {
                    while (ch == ' ') nextChar();
                    if (ch == charToEat) {
                        nextChar();
                        return true;
                    }
                    return false;
                }
    
                double parse() {
                    nextChar();
                    double x = parseExpression();
                    if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                    return x;
                }
    
                double parseExpression() {
                    double x = parseTerm();
                    for (;;) {
                        if      (eat('+')) x += parseTerm(); // addition
                        else if (eat('-')) x -= parseTerm(); // subtraction
                        else return x;
                    }
                }
    
                double parseTerm() {
                    double x = parseFactor();
                    for (;;) {
                        if      (eat('*')) x *= parseFactor(); // multiplication
                        else if (eat('/')) x /= parseFactor(); // division
                        else return x;
                    }
                }
    
                double parseFactor() {
                    if (eat('+')) return parseFactor(); // unary plus
                    if (eat('-')) return -parseFactor(); // unary minus
    
                    double x;
                    int startPos = this.pos;
                    if (eat('(')) { // parentheses
                        x = parseExpression();
                        eat(')');
                    } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                        x = Double.parseDouble(str.substring(startPos, this.pos));
                    } else if (ch >= 'a' && ch <= 'z') { // functions
                        while (ch >= 'a' && ch <= 'z') nextChar();
                        String func = str.substring(startPos, this.pos);
                        x = parseFactor();
                        if (func.equals("sqrt")) x = Math.sqrt(x);
                        else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                        else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                        else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                        else throw new RuntimeException("Unknown function: " + func);
                    } else {
                        throw new RuntimeException("Unexpected: " + (char)ch);
                    }
    
                    if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation
    
                    return x;
                }
            }.parse();
        }

    public static double textCalculus(double input, String text) {
            Matcher m = calculusPattern.matcher(text);
            while(m.find()) {
                char operator = m.group(1).charAt(0);
                double value = Double.parseDouble(m.group(2));
    
                switch (operator) {
                    case '+':
                        input += value;
                        break;
                    case '-':
                        input -= value;
                        break;
                    case '*':
                        input *= value;
                        break;
                    case '/':
                        input = input / value;
                        break;
                    case '^':
                        input = Math.pow(input, value);
                        break;
                }
    
                Tuils.log("now im", input);
            }
    
            return input;
        }

    public static CharSequence parseMarkdown(CharSequence text) {
            if (text == null) return null;
            SpannableStringBuilder ssb = new SpannableStringBuilder(text);
    
            // 1. Code Blocks (``` ... ```)
            applyRegex(ssb, "```[\\s\\S]*?```", (builder, start, end) -> {
                builder.setSpan(new ForegroundColorSpan(0xFF888888), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new android.text.style.TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.delete(end - 3, end);
                builder.delete(start, start + 3);
            });
    
            // 2. Bold (**text**)
            applyRegex(ssb, "\\*\\*(.*?)\\*\\*", (builder, start, end) -> {
                builder.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.delete(end - 2, end);
                builder.delete(start, start + 2);
            });
    
            // 3. Italic (*text*)
            applyRegex(ssb, "\\*(.*?)\\*", (builder, start, end) -> {
                builder.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.delete(end - 1, end);
                builder.delete(start, start + 1);
            });
    
            // 4. Inline Code (`text`)
            applyRegex(ssb, "`(.*?)`", (builder, start, end) -> {
                builder.setSpan(new ForegroundColorSpan(0xFF888888), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new android.text.style.TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.delete(end - 1, end);
                builder.delete(start, start + 1);
            });
    
            // 5. Headers (### text)
            applyRegex(ssb, "(?m)^#{1,6}\\s+(.*)$", (builder, start, end) -> {
                builder.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new android.text.style.RelativeSizeSpan(1.1f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                int hashEnd = start;
                while(hashEnd < end && builder.charAt(hashEnd) == '#') hashEnd++;
                while(hashEnd < end && Character.isWhitespace(builder.charAt(hashEnd))) hashEnd++;
                builder.delete(start, hashEnd);
            });
    
            // 6. Lists (* text or - text)
            applyRegex(ssb, "(?m)^\\s*[\\*\\-]\\s+", (builder, start, end) -> {
                builder.replace(start, end, " • ");
            });
    
            // 7. Final Cleanup: If any ** or ### still exist, strip them (fallback)
            String finalStr = ssb.toString();
            if (finalStr.contains("**") || finalStr.contains("###")) {
                 applyRegex(ssb, "\\*\\*", (builder, start, end) -> builder.delete(start, end));
                 applyRegex(ssb, "###", (builder, start, end) -> builder.delete(start, end));
                 applyRegex(ssb, "##", (builder, start, end) -> builder.delete(start, end));
            }
    
            return ssb;
        }

    public static SpannableString span(CharSequence text, int color) {
            return span(null, text, color, Integer.MAX_VALUE);
        }

    public static SpannableString span(Context context, int size, CharSequence text) {
            return span(context, text, Integer.MAX_VALUE, size);
        }

    public static SpannableString span(Context context, CharSequence text, int color, int size) {
            return span(context, Integer.MAX_VALUE, color, text, size);
        }

    public static SpannableString span(int bgColor, int foreColor, CharSequence text) {
            return span(null, bgColor, foreColor, text, Integer.MAX_VALUE);
        }

    public static SpannableString span(Context context, int bgColor, int foreColor, CharSequence text, int size) {
            if(text == null) {
                text = Tuils.EMPTYSTRING;
            }
    
            SpannableString spannableString;
            if(text instanceof SpannableString) spannableString = (SpannableString) text;
            else spannableString = new SpannableString(text);
    
            if(size != Integer.MAX_VALUE && context != null) spannableString.setSpan(new AbsoluteSizeSpan(Tuils.convertSpToPixels(size, context)), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if(foreColor != Integer.MAX_VALUE) spannableString.setSpan(new ForegroundColorSpan(foreColor), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if(bgColor != Integer.MAX_VALUE) spannableString.setSpan(new BackgroundColorSpan(bgColor), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    
            return spannableString;
        }

    public static int span(int bgColor, SpannableString text, String section, int fromIndex) {
            int index = text.toString().indexOf(section, fromIndex);
            if(index == -1) return index;
    
            text.setSpan(new BackgroundColorSpan(bgColor), index, index + section.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    
            return index + section.length();
        }

}
