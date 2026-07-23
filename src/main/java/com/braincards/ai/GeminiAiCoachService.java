package com.braincards.ai;

import com.braincards.ai.gemini.GeminiClient;
import com.braincards.dto.GameDto;
import com.braincards.model.Child;
import com.braincards.repository.ChildRepository;
import com.braincards.service.GameService;
import com.braincards.service.ResourceNotFoundException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;

@Service
public class GeminiAiCoachService implements AiCoachService {

  private static final String COACH_PROMPT_TEMPLATE = """
            You are a warm, practical child-development coach helping a parent during a short daily \
            play session with their young child.

            Child's age: %d months.
            Game: "%s"
            How to play: %s

            The parent's question: "%s"

            Answer in %s. Be concise (a few short paragraphs at most), concrete and encouraging. Give \
            tips the parent can use right now, and if the child resists, suggest gentle ways to spark \
            interest or an easier variation of the game. Do not give medical or diagnostic advice; if \
            the question really needs a professional, gently say so.
            """;

    private final GeminiClient geminiClient;
    private final GameService gameService;
    private final ChildRepository childRepository;

    public GeminiAiCoachService(GeminiClient geminiClient, GameService gameService, ChildRepository childRepository) {
        this.geminiClient = geminiClient;
        this.gameService = gameService;
        this.childRepository = childRepository;
    }

    @Override
    public String explainOrCoach(Long childId, Long gameId, String parentQuestion) {
        GameDto game = gameService.getGame(gameId);
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Child not found: " + childId));

        String prompt = buildPrompt(game, ageInMonths(child.getBirthDate()), parentQuestion,
                LocaleContextHolder.getLocale());
        return geminiClient.generate(prompt);
    }

    private String buildPrompt(GameDto game, int ageMonths, String parentQuestion, Locale locale) {
        return COACH_PROMPT_TEMPLATE.formatted(ageMonths, safe(game.title()), safe(game.instructions()),
                safe(parentQuestion), languageName(locale));
    }

    private String languageName(Locale locale) {
        String language = locale == null ? "" : locale.getLanguage().toLowerCase(Locale.ROOT);
        return (language.startsWith("ua") || language.startsWith("uk")) ? "Ukrainian" : "English";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int ageInMonths(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return (int) Period.between(birthDate, LocalDate.now()).toTotalMonths();
    }
}
