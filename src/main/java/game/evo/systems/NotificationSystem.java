package game.evo.systems;

import game.evo.components.NotificationComponent;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.utils.GameConstants;

import java.util.Set;

/**
 * Gerencia o ciclo de vida das notificações na tela.
 * Ele decrementa seus temporizadores e as remove quando expiram.
 * ATUALIZADO: Funciona com o novo NotificationComponent aprimorado.
 */
public class NotificationSystem extends GameSystem {

    public NotificationSystem(World world) {
        super(world);
    }

    @Override
    public void update() {
        // Encontra todas as entidades que atualmente têm uma notificação 
        Set<Entity> entitiesWithNotification = world.getEntitiesWithComponent(NotificationComponent.class);
        if (entitiesWithNotification.isEmpty()) {
            return;
        }

        // Calcula o tempo passado desde o último frame em segundos 
        float deltaTimeSeconds = GameConstants.GAME_LOOP_DELAY_MS / 1000.0f;

        // Itera sobre uma cópia do conjunto para evitar problemas de modificação concorrente 
        for (Entity entity : Set.copyOf(entitiesWithNotification)) {
            NotificationComponent notification = world.getComponent(entity, NotificationComponent.class);
            if (notification != null) {
                // MODIFICADO: Usa o novo nome do campo 'remainingDuration'
                notification.remainingDuration -= deltaTimeSeconds;

                // Se o tempo da notificação acabou, remove o componente 
                if (notification.remainingDuration <= 0) {
                    world.removeComponent(entity, NotificationComponent.class);
                }
            }
        }
    }
}