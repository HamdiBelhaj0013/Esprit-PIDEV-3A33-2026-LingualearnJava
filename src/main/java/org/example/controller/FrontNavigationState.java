package org.example.controller;

public class FrontNavigationState {

    private static int selectedLanguageId;
    private static String selectedLanguageName;

    private static int selectedCourseId;
    private static String selectedCourseTitle;

    public static int getSelectedLanguageId() {
        return selectedLanguageId;
    }

    public static void setSelectedLanguageId(int selectedLanguageId) {
        FrontNavigationState.selectedLanguageId = selectedLanguageId;
    }

    public static String getSelectedLanguageName() {
        return selectedLanguageName;
    }

    public static void setSelectedLanguageName(String selectedLanguageName) {
        FrontNavigationState.selectedLanguageName = selectedLanguageName;
    }

    public static int getSelectedCourseId() {
        return selectedCourseId;
    }

    public static void setSelectedCourseId(int selectedCourseId) {
        FrontNavigationState.selectedCourseId = selectedCourseId;
    }

    public static String getSelectedCourseTitle() {
        return selectedCourseTitle;
    }

    public static void setSelectedCourseTitle(String selectedCourseTitle) {
        FrontNavigationState.selectedCourseTitle = selectedCourseTitle;
    }
}