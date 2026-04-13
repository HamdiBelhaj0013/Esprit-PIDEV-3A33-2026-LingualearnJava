package org.example.console;

import org.example.entity.User;
import org.example.service.UserService;
import org.example.util.Menu;
import java.util.Scanner;

/** Regular-user dashboard. */
public class UserConsole {

    private final UserService svc;
    private final Scanner     sc;

    public UserConsole(UserService svc, Scanner sc) {
        this.svc = svc;
        this.sc  = sc;
    }

    public void run(User user) {
        boolean on = true;
        while (on) {
            Menu.title("DASHBOARD — " + user.getFullName());
            System.out.println("  1  View my profile");
            System.out.println("  2  Update name");
            System.out.println("  3  Change password");
            System.out.println("  0  Logout");
            Menu.blank();

            switch (Menu.ask(sc, "Choice")) {
                case "1" -> {
                    svc.findById(user.getId()).ifPresent(Menu::printDetail);
                    Menu.enter(sc);
                }
                case "2" -> {
                    try {
                        String fn = Menu.ask(sc, "First name [" + user.getFirstName() + "]");
                        String ln = Menu.ask(sc, "Last name  [" + user.getLastName()  + "]");
                        if (fn.isBlank()) fn = user.getFirstName();
                        if (ln.isBlank()) ln = user.getLastName();
                        svc.updateName(user, fn, ln);
                        Menu.ok("Name updated.");
                    } catch (Exception e) { Menu.err(e.getMessage()); }
                    Menu.enter(sc);
                }
                case "3" -> {
                    try {
                        String cur = Menu.ask(sc, "Current password");
                        if (!svc.verifyPassword(cur, user.getPassword())) {
                            Menu.err("Wrong password."); Menu.enter(sc); break;
                        }
                        String np  = Menu.ask(sc, "New password (min 6)");
                        String cnf = Menu.ask(sc, "Confirm");
                        if (!np.equals(cnf)) { Menu.err("Passwords do not match."); Menu.enter(sc); break; }
                        svc.resetPassword(user, np);
                        Menu.ok("Password changed.");
                    } catch (Exception e) { Menu.err(e.getMessage()); }
                    Menu.enter(sc);
                }
                case "0" -> on = false;
                default  -> Menu.warn("Unknown option.");
            }
        }
    }
}
