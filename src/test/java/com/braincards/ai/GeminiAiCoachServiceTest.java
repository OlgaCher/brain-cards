package com.braincards.ai;

import com.braincards.ai.gemini.GeminiClient;
import com.braincards.dto.GameDto;
import com.braincards.model.Child;
import com.braincards.repository.ChildRepository;
import com.braincards.service.GameService;
import com.braincards.service.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiAiCoachServiceTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private GameService gameService;

    @Mock
    private ChildRepository childRepository;

    private GeminiAiCoachService service;

    @BeforeEach
    void setup() {
        service = new GeminiAiCoachService(geminiClient, gameService, childRepository);
    }

    private GameDto game(Long id, String title, String instructions) {
        return new GameDto(id, 1L, "Self-regulation", 36, 72, true, null, title, instructions);
    }

    private Child child(Long id, LocalDate birthDate) {
        Child child = new Child();
        child.setId(id);
        child.setBirthDate(birthDate);
        return child;
    }

    @Test
    void explainOrCoach_buildsPromptFromGameAndQuestion_andReturnsClientAnswer() {
        when(gameService.getGame(5L)).thenReturn(game(5L, "Shark", "Freeze on the signal."));
        when(childRepository.findById(10L)).thenReturn(Optional.of(child(10L, LocalDate.now().minusMonths(24))));
        when(geminiClient.generate(anyString())).thenReturn("Make it playful and start with short freezes.");

        String answer = service.explainOrCoach(10L, 5L, "How do I get my child interested?");

        assertThat(answer).isEqualTo("Make it playful and start with short freezes.");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt)
                .contains("Shark")
                .contains("Freeze on the signal.")
                .contains("How do I get my child interested?")
                .contains("24 months");
    }

    @Test
    void explainOrCoach_throwsWhenChildMissing_andNeverCallsAi() {
        when(gameService.getGame(5L)).thenReturn(game(5L, "Shark", "Freeze on the signal."));
        when(childRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.explainOrCoach(10L, 5L, "Any tips?"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(geminiClient, never()).generate(anyString());
    }

    @Test
    void explainOrCoach_propagatesWhenGameMissing() {
        when(gameService.getGame(999L)).thenThrow(new ResourceNotFoundException("Game not found: 999"));

        assertThatThrownBy(() -> service.explainOrCoach(10L, 999L, "Any tips?"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(geminiClient, never()).generate(anyString());
    }
}
