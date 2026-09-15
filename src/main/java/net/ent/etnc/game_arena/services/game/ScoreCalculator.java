package net.ent.etnc.game_arena.services.game;

import org.springframework.stereotype.Component;

import java.text.Normalizer;

/**
 * Calcule les points gagnés par un joueur selon la vitesse de réponse
 * et son multiplicateur de streak.
 * <p>
 * Règles métier :
 * <ul>
 *   <li>Réponse instantanée (0 ms) → {@code MAX_POINTS} (1 000 pts)</li>
 *   <li>Réponse à la dernière milliseconde → {@code MIN_POINTS} (50 pts)</li>
 *   <li>Entre les deux : interpolation linéaire décroissante</li>
 *   <li>Mauvaise réponse ou temps écoulé → 0 pt</li>
 *   <li>Streak ≥ 3 déclenche un bonus ×2 sur la question suivante</li>
 * </ul>
 */
@Component
public class ScoreCalculator {

    private static final int MAX_POINTS        = 1_000;
    private static final int MIN_POINTS        = 50;
    private static final int TIMEOUT_MS        = 15_000;
    private static final int STREAK_BONUS_MIN  = 3;
    private static final int STREAK_MULTIPLIER = 2;

    /**
     * Calcule le score de base selon le temps écoulé.
     * Indépendant du streak — le multiplicateur est appliqué séparément.
     *
     * @param elapsedMs millisecondes écoulées depuis le début de la question
     * @return points entre {@code MIN_POINTS} et {@code MAX_POINTS}, ou 0 si dépassement
     */
    public int basePoints(long elapsedMs) {
        if (elapsedMs >= TIMEOUT_MS) return 0;
        double ratio = (double) elapsedMs / TIMEOUT_MS;
        return (int) Math.round(MIN_POINTS + (MAX_POINTS - MIN_POINTS) * (1.0 - ratio));
    }

    /**
     * Applique le multiplicateur de streak.
     *
     * @param base        score de base (résultat de {@link #basePoints})
     * @param bonusActive le joueur a-t-il un bonus ×2 actif sur cette question ?
     * @return score final après multiplicateur
     */
    public int applyMultiplier(int base, boolean bonusActive) {
        return bonusActive ? base * STREAK_MULTIPLIER : base;
    }

    /**
     * Compare une réponse libre à la réponse attendue en ignorant
     * accents, casse et espaces superflus.
     * Pour les réponses numériques : seuls les chiffres sont comparés.
     *
     * @param userAnswer  texte saisi par le joueur
     * @param correctText bonne réponse attendue
     * @return {@code true} si les deux textes normalisés sont identiques
     */
    public boolean matchesFreeText(String userAnswer, String correctText) {
        return normalize(userAnswer).equals(normalize(correctText));
    }

    /**
     * Indique si le streak atteint le seuil déclenchant le bonus ×2
     * pour la prochaine question.
     */
    public boolean triggersBonus(int streak) {
        return streak >= STREAK_BONUS_MIN;
    }

    private String normalize(String value) {
        if (value == null) return "";
        String s = Normalizer.normalize(value.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ");
        if (s.matches(".*\\d.*")) s = s.replaceAll("[^0-9]", "");
        return s;
    }
}
