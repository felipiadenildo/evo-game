package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * Um componente "marcador" que atua como um evento para sinalizar que o jogador
 * pressionou a tecla para iniciar o gameplay a partir da tela de introdução.
 * Não precisa de campos; sua simples presença em uma entidade é o sinal.
 */
public class StartGameplayRequestComponent implements Component, Serializable {
    // A anotação @Override não é necessária para interfaces, mas a implementação sim.
    // Garanta que "implements Component" esteja na linha acima.
    
    private static final long serialVersionUID = 1L;
}