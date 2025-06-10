package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * Componente marcador que indica que o jogo está em um estado de pausa
 * (como a tela de introdução) e aguardando um input específico do jogador para continuar.
 */
public class AwaitingInputComponent implements Component, Serializable {
    private static final long serialVersionUID = 1L;
}