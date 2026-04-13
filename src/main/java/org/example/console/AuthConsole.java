package org.example.console;

import org.example.service.UserService;
import org.example.util.Menu;
import java.util.List;
import java.util.Scanner;

/** Login / register entry point. Routes to Admin or User console. */
public class AuthConsole {

    private final UserService svc;
    private final Scanner     sc;

    public AuthConsole(UserService svc, Scanner sc) {
        this.svc = svc;
        this.sc  = sc;
    }

    public void run() {
        boolean on = true;
        while (on) {
            Menu.title("LinguaLearn");
            System.out.println("  1  Login");
            System.out.println("  2  Register");
            System.out.println("  0  Exit");
            Menu.blank();

            switch (Menu.ask(sc, "Choice")) {
                case "1" -> login();
                case "2" -> register();
                case "0" -> on = false;
                default  -> Menu.warn("Unknown option.");
            }
        }
    }

    private void login() {
        Menu.title("Login");
        String email = Menu.ask(sc, "Email");
        String pass  = Menu.ask(sc, "Password");

        var result = svc.authenticate(email, pass);
        if (result.isEmpty()) {
            Menu.err("Wrong email/password, or account is not active.");
            Menu.enter(sc);
            return;
        }

        var user = result.get();
        Menu.ok("Welcome, " + user.getFullName() + "!");

        if (user.hasRole("ROLE_ADMIN"))
            new AdminConsole(svc, sc).run(user);
        else
            new UserConsole(svc, sc).run(user);
    }

    private void register() {
        Menu.title("Register");
        try {
            String email = Menu.ask(sc, "Email");
            String fname = Menu.ask(sc, "First name");
            String lname = Menu.ask(sc, "Last name");
            String pass  = Menu.ask(sc, "Password (min 6)");
            String cnf   = Menu.ask(sc, "Confirm password");
            var u = svc.createUser(email, pass, cnf, fname, lname, List.of("ROLE_USER"));
            Menu.ok("Account created — ID " + u.getId() + ". You can now log in.");
        } catch (Exception e) {
            Menu.err(e.getMessage());
        }
        Menu.enter(sc);
    }
}
