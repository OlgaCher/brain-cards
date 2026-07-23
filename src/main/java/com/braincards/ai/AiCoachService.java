package com.braincards.ai;

public interface AiCoachService {

    /**
     * Real-time coaching hook: a parent asks about a game they don't understand or can't get the
     * child interested in, and gets a short, practical, age-appropriate answer.
     *
     * @param childId        the (owned) child the session is for - drives age-appropriate advice
     * @param gameId         the game the question is about
     * @param parentQuestion the parent's free-text question
     * @return the coach's answer, phrased in the current request locale's language
     */
    String explainOrCoach(Long childId, Long gameId, String parentQuestion);
}
