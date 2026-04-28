package org.example.service.tests;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ExamTimeGuardService {

    private static final double SEUIL_SUSPECT = 0.20;
    private static final double SEUIL_LEGER   = 1.00;
    private static final double SEUIL_MODERE  = 1.20;
    private static final double SEUIL_GRAVE   = 1.50;

    public enum Statut {
        SUSPECT, NORMAL, DEPASSE_LEGER, DEPASSE_MODERE, DEPASSE_GRAVE
    }

    public static class RapportTemporel {
        public final int     dureeAutoriseeMin;
        public final long    dureeUtiliseeMin;
        public final long    dureeUtiliseeSec;
        public final double  ratioTemps;
        public final Statut  statut;
        public final double  penaliteCoeff;
        public final float   scoreAvantPenalite;
        public final float   scoreFinal;
        public final boolean soumissionAuto;

        public RapportTemporel(int dureeAutoriseeMin, long dureeUtiliseeSec,
                               Statut statut, double penaliteCoeff,
                               float scoreAvantPenalite, boolean soumissionAuto) {
            this.dureeAutoriseeMin  = dureeAutoriseeMin;
            this.dureeUtiliseeSec   = dureeUtiliseeSec;
            this.dureeUtiliseeMin   = dureeUtiliseeSec / 60;
            this.ratioTemps         = (double) dureeUtiliseeSec / (dureeAutoriseeMin * 60.0);
            this.statut             = statut;
            this.penaliteCoeff      = penaliteCoeff;
            this.scoreAvantPenalite = scoreAvantPenalite;
            this.scoreFinal         = Math.max(0f, (float)(scoreAvantPenalite * (1.0 + penaliteCoeff)));
            this.soumissionAuto     = soumissionAuto;
        }

        public String toRapportTexte() {
            String statutLabel = switch (statut) {
                case SUSPECT        -> "SUSPECT (soumission trop rapide)";
                case NORMAL         -> "NORMAL";
                case DEPASSE_LEGER  -> "DEPASSEMENT LEGER";
                case DEPASSE_MODERE -> "DEPASSEMENT MODERE";
                case DEPASSE_GRAVE  -> "DEPASSEMENT GRAVE" + (soumissionAuto ? " - Soumission automatique" : "");
            };
            String penLabel = penaliteCoeff == 0.0 ? "Aucune"
                    : String.format("%.0f%%", penaliteCoeff * 100);
            return String.format(
                    "[RAPPORT TEMPOREL]\n" +
                            "Temps autorise : %d min\n" +
                            "Temps utilise  : %d min %d sec\n" +
                            "Statut         : %s\n" +
                            "Penalite       : %s\n" +
                            "Score avant    : %.1f%%\n" +
                            "Score final    : %.1f%%",
                    dureeAutoriseeMin,
                    dureeUtiliseeMin, dureeUtiliseeSec % 60,
                    statutLabel, penLabel,
                    scoreAvantPenalite, scoreFinal);
        }

        public String getCouleurStatut() {
            return switch (statut) {
                case SUSPECT        -> "#e65100";
                case NORMAL         -> "#2e7d32";
                case DEPASSE_LEGER  -> "#f59f00";
                case DEPASSE_MODERE -> "#d63939";
                case DEPASSE_GRAVE  -> "#7b1fa2";
            };
        }
    }

    public RapportTemporel analyser(LocalDateTime startTime, int dureeAutoriseeMin,
                                    float scoreAvantPenalite) {
        long secondesEcoulees   = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        long secondesAutorisees = (long) dureeAutoriseeMin * 60;
        double ratio = (double) secondesEcoulees / secondesAutorisees;

        Statut  statut;
        double  penalite;
        boolean auto = false;

        if      (ratio < SEUIL_SUSPECT) { statut = Statut.SUSPECT;        penalite = -0.30; }
        else if (ratio <= SEUIL_LEGER)  { statut = Statut.NORMAL;         penalite =  0.0;  }
        else if (ratio <= SEUIL_MODERE) { statut = Statut.DEPASSE_LEGER;  penalite = -0.05; }
        else if (ratio <= SEUIL_GRAVE)  { statut = Statut.DEPASSE_MODERE; penalite = -0.20; }
        else                            { statut = Statut.DEPASSE_GRAVE;  penalite = -0.50; auto = true; }

        return new RapportTemporel(dureeAutoriseeMin, secondesEcoulees,
                statut, penalite, scoreAvantPenalite, auto);
    }

    public static String formatChrono(long secondesRestantes) {
        if (secondesRestantes < 0) secondesRestantes = 0;
        return String.format("%02d:%02d", secondesRestantes / 60, secondesRestantes % 60);
    }

    public static String getCouleurChrono(long secondesRestantes, long secondesTotales) {
        if (secondesTotales <= 0) return "#2e7d32";
        double ratio = (double) secondesRestantes / secondesTotales;
        if (ratio > 0.50) return "#2e7d32";
        if (ratio > 0.20) return "#f59f00";
        return "#d63939";
    }

    public static boolean doitSoumettreAuto(long secondesEcoulees, int dureeAutoriseeMin) {
        return secondesEcoulees > (long) dureeAutoriseeMin * 60 * SEUIL_GRAVE;
    }
}