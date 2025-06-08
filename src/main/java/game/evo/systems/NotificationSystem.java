package game.evo.systems;

import game.evo.components.NotificationComponent;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.utils.GameConstants;

import java.util.Set;

/**
 * Manages the lifecycle of on-screen notifications.
 * It decrements their timers and removes them when they expire.
 */
public class NotificationSystem extends GameSystem {

    public NotificationSystem(World world) {
        super(world);
    }

    @Override
    public void update() {
        // Find all entities that currently have a notification
        Set<Entity> entitiesWithNotification = world.getEntitiesWithComponent(NotificationComponent.class);
        if (entitiesWithNotification.isEmpty()) {
            return;
        }

        // Calculate the time passed since the last frame in seconds
        float deltaTimeSeconds = GameConstants.GAME_LOOP_DELAY_MS / 1000.0f;

        // Using a copy of the set to avoid modification issues while iterating
        for (Entity entity : Set.copyOf(entitiesWithNotification)) {
            NotificationComponent notification = world.getComponent(entity, NotificationComponent.class);
            if (notification != null) {
                notification.remainingTimeSeconds -= deltaTimeSeconds;

                // If the notification's time has run out, remove it
                if (notification.remainingTimeSeconds <= 0) {
                    world.removeComponent(entity, NotificationComponent.class);
                }
            }
        }
    }
}
