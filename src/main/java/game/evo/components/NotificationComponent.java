package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * Componente que exibe uma mensagem temporária na tela.
 * REFATORADO: Agora suporta tipos de notificação, ícones e animação.
 */
public class NotificationComponent implements Component, Serializable {
    private static final long serialVersionUID = 1L;

    // Enum para os diferentes tipos de notificação
    public enum NotificationType {
        INFO,      // Para informações gerais (entrou no portal)
        SUCCESS,   // Para ações bem-sucedidas (comeu, salvou)
        WARNING,   // Para alertas ou dano recebido
        COMBAT     // Para ações de combate (dano causado)
    }

    public String message;
    public NotificationType type;
    public float initialDuration; // Duração total para cálculo da animação
    public float remainingDuration; // Duração atual que será decrementada

    public NotificationComponent(String message, NotificationType type, float durationInSeconds) {
        this.message = message;
        this.type = type;
        this.initialDuration = durationInSeconds;
        this.remainingDuration = durationInSeconds;
    }
}