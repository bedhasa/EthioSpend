package com.example.ethiospand;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {

    private static final List<String> CBE_SENDERS = Arrays.asList(
            "CBE", "CBEBIRR", "CBEBirr", "Commercial Bank", "CBE-Bank", "telebirr", "889", "127", "827", "830"
    );

    private static final List<String> BANK_KEYWORDS = Arrays.asList(
            "ETB", "debited", "credited", "transferred", "withdrawn", "Account No", "Ref No", "Balance", "received", "paid"
    );

    public static boolean isBankSms(String sender, String message) {
        if (sender == null || message == null) return false;
        
        boolean senderMatch = false;
        String upperSender = sender.toUpperCase();
        for (String s : CBE_SENDERS) {
            if (upperSender.contains(s.toUpperCase())) {
                senderMatch = true;
                break;
            }
        }

        boolean keywordMatch = false;
        String lowerMsg = message.toLowerCase();
        for (String k : BANK_KEYWORDS) {
            if (lowerMsg.contains(k.toLowerCase())) {
                keywordMatch = true;
                break;
            }
        }

        return senderMatch && keywordMatch;
    }

    public static Transaction parseSms(String message) {
        String cleanMessage = message.replace("\n", " ").replaceAll("\\s+", " ");
        String lower = cleanMessage.toLowerCase();

        String type = "Transfer";
        if (lower.contains("debited") || lower.contains("withdrawn") || lower.contains("paid") || lower.contains("debit transaction")) {
            type = "Expense";
        } else if (lower.contains("credited") || lower.contains("received")) {
            type = "Income";
        }

        String amount = "0.00";
        Matcher amountMatcher = Pattern.compile("ETB\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE).matcher(cleanMessage);
        if (amountMatcher.find()) {
            amount = amountMatcher.group(1);
            if (amount.endsWith(".")) amount = amount.substring(0, amount.length() - 1);
        }

        String person = "Unknown";
        if (lower.contains("transferred") && lower.contains("to account")) {
            Matcher m = Pattern.compile("to\\s+account\\s+[\\d\\*]+\\s+\\((.*?)\\)", Pattern.CASE_INSENSITIVE).matcher(cleanMessage);
            if (m.find()) person = m.group(1);
            else {
                Matcher m2 = Pattern.compile("to\\s+account\\s+[\\d\\*]+\\s+([A-Z][a-zA-Z\\s]+)(?:\\.|\\s+on)", Pattern.CASE_INSENSITIVE).matcher(cleanMessage);
                if (m2.find()) person = m2.group(1).trim();
            }
        } else if (lower.contains("credited") && lower.contains("from")) {
            Matcher m = Pattern.compile("from\\s+(.*?)(?:,\\s*on|\\s+on)", Pattern.CASE_INSENSITIVE).matcher(cleanMessage);
            if (m.find()) person = m.group(1).trim();
        } else if (lower.contains("received") && lower.contains("from")) {
            Matcher m = Pattern.compile("from\\s+(.*?)\\s+to\\s+your", Pattern.CASE_INSENSITIVE).matcher(cleanMessage);
            if (m.find()) person = m.group(1).trim();
        } else if (lower.contains("paid")) {
            if (lower.contains("for package")) {
                Matcher m = Pattern.compile("for\\s+package\\s+(.*?)\\s+purchase", Pattern.CASE_INSENSITIVE).matcher(cleanMessage);
                if (m.find()) person = m.group(1).trim();
            } else {
                Matcher m = Pattern.compile("to\\s+(.*?)\\s+on", Pattern.CASE_INSENSITIVE).matcher(cleanMessage);
                if (m.find()) person = m.group(1).trim();
            }
        }

        String refNo = "N/A";
        Matcher refMatcher = Pattern.compile("Ref(?:\\s*No)?[:\\s]+([A-Z0-9]+)|transaction number is\\s+([A-Z0-9]+)|by transaction number\\s+([A-Z0-9]+)", Pattern.CASE_INSENSITIVE).matcher(cleanMessage);
        if (refMatcher.find()) {
            if (refMatcher.group(1) != null) refNo = refMatcher.group(1);
            else if (refMatcher.group(2) != null) refNo = refMatcher.group(2);
            else if (refMatcher.group(3) != null) refNo = refMatcher.group(3);
        }

        String balance = "0.00";
        Matcher balanceMatcher = Pattern.compile("(?:balance is|Available\\s*Bal|Balance is)\\s*ETB\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE).matcher(cleanMessage);
        if (balanceMatcher.find()) balance = balanceMatcher.group(1);

        String date = "N/A", time = "";
        Matcher dateMatcher = Pattern.compile("(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})").matcher(cleanMessage);
        if (dateMatcher.find()) date = dateMatcher.group(1);
        Matcher timeMatcher = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}|\\d{2}:\\d{2})").matcher(cleanMessage);
        if (timeMatcher.find()) time = timeMatcher.group(1);

        if (date.equals("N/A")) date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        if (time.isEmpty()) time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        return new Transaction(type, amount, person, date, time, balance, refNo);
    }
}
