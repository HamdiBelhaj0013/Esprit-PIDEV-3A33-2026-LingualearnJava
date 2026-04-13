package org.example.console;

import org.example.entity.User;
import org.example.service.*;
import org.example.util.Menu;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;


public class AdminConsole {

    private final UserService svc;
    private final Scanner     sc;

    public AdminConsole(UserService svc, Scanner sc) {
        this.svc = svc;
        this.sc  = sc;
    }

    public void run(User admin) {
        boolean on = true;
        while (on) {
            Menu.title("ADMIN — " + admin.getFullName());
            System.out.println("  1  List all users       7  Grant premium");
            System.out.println("  2  Search users         8  Revoke premium");
            System.out.println("  3  Show user detail     9  Change roles");
            System.out.println("  4  Create user         10  Reset password");
            System.out.println("  5  Edit name           11  Statistics");
            System.out.println("  6  Activate/Suspend/Delete");
            System.out.println("  0  Logout");
            Menu.blank();

            switch (Menu.ask(sc, "Choice")) {
                case "1"  -> list();
                case "2"  -> search();
                case "3"  -> detail();
                case "4"  -> create();
                case "5"  -> editName();
                case "6"  -> statusMenu();
                case "7"  -> grantPremium();
                case "8"  -> revokePremium();
                case "9"  -> changeRoles();
                case "10" -> resetPassword();
                case "11" -> stats();
                case "0"  -> on = false;
                default   -> Menu.warn("Unknown option.");
            }
        }
    }

    // ── 1 List ────────────────────────────────────────────────────────────────

    private void list() {
        Menu.title("All users");
        Menu.printTable(svc.findAll());
        Menu.enter(sc);
    }

    // ── 2 Search ──────────────────────────────────────────────────────────────

    private void search() {
        Menu.title("Search");
        String term = Menu.ask(sc, "Name or email");
        if (term.isBlank()) return;
        Menu.printTable(svc.search(term));
        Menu.enter(sc);
    }

    // ── 3 Detail ──────────────────────────────────────────────────────────────

    private void detail() {
        Menu.title("User detail");
        User u = pick();
        if (u == null) return;
        Menu.printDetail(u);
        Menu.enter(sc);
    }

    // ── 4 Create ──────────────────────────────────────────────────────────────

    private void create() {
        Menu.title("Create user");
        try {
            String email   = Menu.ask(sc, "Email");
            String fname   = Menu.ask(sc, "First name");
            String lname   = Menu.ask(sc, "Last name");
            String pass    = Menu.ask(sc, "Password (min 6)");
            String confirm = Menu.ask(sc, "Confirm password");
            System.out.println("  Role: 1=User  2=Teacher  3=Admin");
            List<String> roles = switch (Menu.ask(sc, "Role (default 1)")) {
                case "2" -> List.of("ROLE_USER", "ROLE_TEACHER");
                case "3" -> List.of("ROLE_USER", "ROLE_ADMIN");
                default  -> List.of("ROLE_USER");
            };
            User u = svc.createUser(email, pass, confirm, fname, lname, roles);
            Menu.ok("Created — ID " + u.getId() + "  " + u.getFullName());
        } catch (Exception e) {
            Menu.err(e.getMessage());
        }
        Menu.enter(sc);
    }

    // ── 5 Edit name ───────────────────────────────────────────────────────────

    private void editName() {
        Menu.title("Edit name");
        User u = pick();
        if (u == null) return;
        try {
            String fn = Menu.ask(sc, "First name [" + u.getFirstName() + "]");
            String ln = Menu.ask(sc, "Last name  [" + u.getLastName()  + "]");
            if (fn.isBlank()) fn = u.getFirstName();
            if (ln.isBlank()) ln = u.getLastName();
            svc.updateName(u, fn, ln);
            Menu.ok("Name updated.");
        } catch (Exception e) {
            Menu.err(e.getMessage());
        }
        Menu.enter(sc);
    }

    // ── 6 Status ──────────────────────────────────────────────────────────────

    private void statusMenu() {
        Menu.title("Activate / Suspend / Delete");
        User u = pick();
        if (u == null) return;
        Menu.info("User: " + u.getFullName() + " — current status: " + u.getStatus());
        System.out.println("  1  Activate   2  Suspend   3  Delete   0  Cancel");
        switch (Menu.ask(sc, "Action")) {
            case "1" -> { svc.activate(u); Menu.ok("Activated."); }
            case "2" -> {
                if (Menu.confirm(sc, "Suspend " + u.getFullName() + "?"))
                    { svc.suspend(u); Menu.warn("Suspended."); }
            }
            case "3" -> {
                if (u.hasRole("ROLE_ADMIN")) { Menu.err("Cannot delete an admin."); break; }
                if (Menu.confirm(sc, "DELETE " + u.getFullName() + "? Cannot be undone"))
                    { svc.delete(u); Menu.warn("Deleted."); }
            }
        }
        Menu.enter(sc);
    }

    // ── 7 Grant premium ───────────────────────────────────────────────────────

    private void grantPremium() {
        Menu.title("Grant premium");
        User u = pick();
        if (u == null) return;
        if (u.isPremium()) { Menu.warn(u.getFullName() + " already has " + u.getSubscriptionPlan() + "."); Menu.enter(sc); return; }

        System.out.println("  Plan: 1=MONTHLY  2=YEARLY");
        String plan = "2".equals(Menu.ask(sc, "Plan")) ? "YEARLY" : "MONTHLY";

        System.out.println("  Duration: 1=1 month  2=1 year  3=custom days");
        LocalDateTime expiry = switch (Menu.ask(sc, "Duration")) {
            case "2" -> LocalDateTime.now().plusYears(1);
            case "3" -> LocalDateTime.now().plusDays(Math.max(1, Menu.askInt(sc, "Days")));
            default  -> LocalDateTime.now().plusMonths(1);
        };

        try {
            svc.grantPremium(u, plan, expiry);
            Menu.ok(u.getFullName() + " → " + plan + " until " + expiry.format(Menu.FMT));
        } catch (Exception e) { Menu.err(e.getMessage()); }
        Menu.enter(sc);
    }

    // ── 8 Revoke premium ──────────────────────────────────────────────────────

    private void revokePremium() {
        Menu.title("Revoke premium");
        User u = pick();
        if (u == null) return;
        if (!u.isPremium()) { Menu.info(u.getFullName() + " is already FREE."); Menu.enter(sc); return; }
        if (!Menu.confirm(sc, "Revoke premium for " + u.getFullName() + "?")) return;
        svc.revokePremium(u);
        Menu.warn("Revoked — moved to FREE.");
        Menu.enter(sc);
    }

    // ── 9 Change roles ────────────────────────────────────────────────────────

    private void changeRoles() {
        Menu.title("Change roles");
        User u = pick();
        if (u == null) return;
        Menu.info("Current: " + u.getRoles());
        System.out.println("  1=User only  2=Teacher  3=Admin  4=Admin+Teacher");
        List<String> roles = switch (Menu.ask(sc, "New roles")) {
            case "2" -> List.of("ROLE_USER", "ROLE_TEACHER");
            case "3" -> List.of("ROLE_USER", "ROLE_ADMIN");
            case "4" -> List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_TEACHER");
            default  -> List.of("ROLE_USER");
        };
        svc.changeRoles(u, roles);
        Menu.ok("Roles → " + u.getRoles());
        Menu.enter(sc);
    }

    // ── 10 Reset password ─────────────────────────────────────────────────────

    private void resetPassword() {
        Menu.title("Reset password");
        User u = pick();
        if (u == null) return;
        String pass = Menu.ask(sc, "New password (min 6)");
        try { svc.resetPassword(u, pass); Menu.ok("Password reset."); }
        catch (Exception e) { Menu.err(e.getMessage()); }
        Menu.enter(sc);
    }

    // ── 11 Statistics ─────────────────────────────────────────────────────────

    private void stats() {
        Menu.title("Statistics");
        svc.printStats();
        Menu.blank();
        int n = svc.downgradeExpired();
        if (n > 0) Menu.warn(n + " expired subscription(s) downgraded to FREE.");
        else       Menu.ok("No expired subscriptions.");
        Menu.enter(sc);
    }

    // ── Helper: pick a user by ID ─────────────────────────────────────────────

    private User pick() {
        long id = Menu.askLong(sc, "User ID");
        if (id <= 0) { Menu.err("Invalid ID."); return null; }
        return svc.findById(id).orElseGet(() -> { Menu.err("User #" + id + " not found."); return null; });
    }
}
