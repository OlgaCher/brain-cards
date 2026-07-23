package com.braincards.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"child_id", "game_id", "swapped_on"}),
        indexes = @Index(name = "idx_swap_log_child_date", columnList = "child_id, swapped_on")
)
@Getter
@Setter
@NoArgsConstructor
public class SwapLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private LocalDate swappedOn;
}
