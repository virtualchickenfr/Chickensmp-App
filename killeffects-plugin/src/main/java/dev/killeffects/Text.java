/*
 * Decompiled with CFR 0.152.
 */
package dev.killeffects;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {
    private static final char SECTION = '\u00a7';
    private static final Pattern GRADIENT = Pattern.compile("<gradient:#([0-9a-fA-F]{6}):#([0-9a-fA-F]{6})>(.*?)</gradient>");
    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final String LEGACY_CODES = "0123456789abcdefklmnorABCDEFKLMNOR";

    private Text() {
    }

    public static String color(String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        String string2 = Text.applyGradients(string);
        string2 = Text.applyHex(string2);
        char[] cArray = string2.toCharArray();
        for (int i = 0; i < cArray.length - 1; ++i) {
            if (cArray[i] != '&' || LEGACY_CODES.indexOf(cArray[i + 1]) <= -1) continue;
            cArray[i] = 167;
            cArray[i + 1] = Character.toLowerCase(cArray[i + 1]);
        }
        return new String(cArray);
    }

    public static String strip(String string) {
        if (string == null) {
            return "";
        }
        return Text.color(string).replaceAll("\u00a7[0-9a-fk-orx]", "");
    }

    public static String apply(String string, Map<String, String> map) {
        if (string == null) {
            return "";
        }
        String string2 = string;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            string2 = string2.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : (CharSequence)entry.getValue());
        }
        return string2;
    }

    private static String applyHex(String string) {
        Matcher matcher = HEX.matcher(string);
        StringBuilder stringBuilder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(stringBuilder, Matcher.quoteReplacement(Text.hex(matcher.group(1))));
        }
        matcher.appendTail(stringBuilder);
        return stringBuilder.toString();
    }

    private static String applyGradients(String string) {
        Matcher matcher = GRADIENT.matcher(string);
        StringBuilder stringBuilder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(stringBuilder, Matcher.quoteReplacement(Text.gradient(matcher.group(3), matcher.group(1), matcher.group(2))));
        }
        matcher.appendTail(stringBuilder);
        return stringBuilder.toString();
    }

    private static String gradient(String string, String string2, String string3) {
        StringBuilder stringBuilder = new StringBuilder();
        String string4 = string;
        Matcher matcher = Pattern.compile("^((?:&[k-orK-OR])+)").matcher(string);
        if (matcher.find()) {
            stringBuilder.append(matcher.group(1));
            string4 = string.substring(matcher.end());
        }
        int n = Integer.parseInt(string2.substring(0, 2), 16);
        int n2 = Integer.parseInt(string2.substring(2, 4), 16);
        int n3 = Integer.parseInt(string2.substring(4, 6), 16);
        int n4 = Integer.parseInt(string3.substring(0, 2), 16);
        int n5 = Integer.parseInt(string3.substring(2, 4), 16);
        int n6 = Integer.parseInt(string3.substring(4, 6), 16);
        int n7 = string4.codePointCount(0, string4.length());
        StringBuilder stringBuilder2 = new StringBuilder();
        int n8 = 0;
        int n9 = 0;
        while (n9 < string4.length()) {
            int n10 = string4.codePointAt(n9);
            double d = n7 <= 1 ? 0.0 : (double)n8 / (double)(n7 - 1);
            int n11 = (int)Math.round((double)n + (double)(n4 - n) * d);
            int n12 = (int)Math.round((double)n2 + (double)(n5 - n2) * d);
            int n13 = (int)Math.round((double)n3 + (double)(n6 - n3) * d);
            stringBuilder2.append(Text.hex(String.format("%02x%02x%02x", n11, n12, n13))).append((CharSequence)stringBuilder).appendCodePoint(n10);
            n9 += Character.charCount(n10);
            ++n8;
        }
        return stringBuilder2.toString();
    }

    private static String hex(String string) {
        StringBuilder stringBuilder = new StringBuilder().append('\u00a7').append('x');
        for (char c : string.toLowerCase().toCharArray()) {
            stringBuilder.append('\u00a7').append(c);
        }
        return stringBuilder.toString();
    }
}

