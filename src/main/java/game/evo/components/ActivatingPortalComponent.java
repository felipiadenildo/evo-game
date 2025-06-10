package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * Componente adicionado a uma entidade quando ela está ativando um portal.
 * Contém um temporizador para o delay da transição.
 */
public class ActivatingPortalComponent implements Component, Serializable {
    private static final long serialVersionUID = 1L;

    public float activationTimer; // Tempo restante em segundos

    public ActivatingPortalComponent(float delayInSeconds) {
        this.activationTimer = delayInSeconds;
    }
}