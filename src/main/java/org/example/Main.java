package org.example;

/**
 * Non-Application launcher — required so that the fat-JAR manifest points to a
 * plain main class while JavaFX is on the module path.
 */
public class Main {
    public static void main(String[] args) {
        App.launch(App.class, args);
    }
}
