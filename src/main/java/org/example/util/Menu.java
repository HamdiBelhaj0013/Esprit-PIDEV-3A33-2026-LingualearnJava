package org.example.util;

import org.example.entity.LearningStats;
import org.example.entity.User;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;


public class Menu {

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── Prompts ───────────────────────────────────────────────────────────────

    public static String ask(Scanner sc, String label) {
        System.out.print("  " + label + ": ");
        return sc.nextLine().trim();
    }

    public static int askInt(Scanner sc, String label) {
        System.out.print("  " + label + ": ");
        try { return Integer.parseInt(sc.nextLine().trim()); }
        catch (Exception e) { return -1; }
    }

    public static long askLong(Scanner sc, String label) {
        System.out.print("  " + label + ": ");
        try { return Long.parseLong(sc.nextLine().trim()); }
        catch (Exception e) { return -1L; }
    }

    public static boolean confirm(Scanner sc, String question) {
        System.out.print("  " + question + " [y/N]: ");
        return sc.nextLine().trim().equalsIgnoreCase("y");
    }

    public static void enter(Scanner sc) {
        System.out.print("  Press Enter to continue...");
        sc.nextLine();
    }

    // ── Output ────────────────────────────────────────────────────────────────

    public static void title(String t) {
        System.out.println();
        System.out.println("══════════════════════════════════════════");
        System.out.println("  " + t);
        System.out.println("══════════════════════════════════════════");
    }

    public static void line()          { System.out.println("  ──────────────────────────────────────────"); }
    public static void ok(String m)    { System.out.println("  [OK]  " + m); }
    public static void warn(String m)  { System.out.println("  [!!]  " + m); }
    public static void err(String m)   { System.out.println("  [ERR] " + m); }
    public static void info(String m)  { System.out.println("  [i]   " + m); }
    public static void blank()         { System.out.println(); }

    // ── User table ────────────────────────────────────────────────────────────

    public static void printTable(List<User> users) {
        if (users.isEmpty()) { info("No users found."); return; }
        System.out.printf("  %-5s %-20s %-25s %-10s %-8s %-6s%n",
            "ID","Name","Email","Status","Plan","Prem?");
        line();
        for (User u : users) {
            System.out.printf("  %-5s %-20s %-25s %-10s %-8s %-6s%n",
                u.getId(),
                cap(u.getFullName(), 19),
                cap(u.getEmail(), 24),
                u.getStatus(),
                u.getSubscriptionPlan(),
                u.isPremium() ? "YES" : "no");
        }
        line();
        System.out.println("  " + users.size() + " user(s)");
    }

    public static void printDetail(User u) {
        line();
        row("ID",       "" + u.getId());
        row("Name",     u.getFullName());
        row("Email",    u.getEmail());
        row("Status",   u.getStatus());
        row("Roles",    u.getRoles().toString());
        row("Plan",     u.getSubscriptionPlan());
        row("Premium",  u.isPremium() ? "YES" : "no");
        row("Expiry",   u.getSubscriptionExpiry() != null ? u.getSubscriptionExpiry().format(FMT) : "—");
        row("Verified", u.isVerified() ? "yes" : "no");
        row("Created",  u.getCreatedAt() != null ? u.getCreatedAt().format(FMT) : "—");
        LearningStats s = u.getLearningStats();
        if (s != null) row("Stats", "XP=" + s.getTotalXP() + "  words=" + s.getWordsLearned() + "  min=" + s.getTotalMinutesStudied());
        line();
    }

    private static void row(String label, String value) {
        System.out.printf("  %-12s %s%n", label + ":", value);
    }

    private static String cap(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
