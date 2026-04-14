package controllers.backoffice;

public final class BackofficeContext {
    private static Integer selectedQuestionId;
    private static Integer selectedReponseId;

    private BackofficeContext() {
    }

    public static Integer getSelectedQuestionId() {
        return selectedQuestionId;
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

