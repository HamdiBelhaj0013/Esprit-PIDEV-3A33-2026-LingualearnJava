package org.example.controllers.backoffice;

public final class BackofficeContext {
    private static Integer selectedPublicationId;
    private static Integer selectedQuestionId;
    private static Integer selectedReponseId;

    private BackofficeContext() {
    }

    public static Integer getSelectedQuestionId() {
        return selectedQuestionId;
    }

    public static Integer getSelectedPublicationId() {
        return selectedPublicationId;
    }

    public static void setSelectedPublicationId(Integer selectedPublicationId) {
        BackofficeContext.selectedPublicationId = selectedPublicationId;
    }

    public static void setSelectedQuestionId(Integer selectedQuestionId) {
        BackofficeContext.selectedQuestionId = selectedQuestionId;
    }

    public static Integer getSelectedReponseId() {
        return selectedReponseId;
    }

    public static void setSelectedReponseId(Integer selectedReponseId) {
        BackofficeContext.selectedReponseId = selectedReponseId;
    }
}

