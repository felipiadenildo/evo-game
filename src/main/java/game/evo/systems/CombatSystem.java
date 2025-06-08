package game.evo.systems;

import game.evo.config.EntityConfig;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.world.EntityFactory;
import game.evo.components.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles all combat-related logic, including processing attacks,
 * applying damage, and handling entity deaths.
 */
public class CombatSystem extends GameSystem {

    private final EntityFactory entityFactory;

    public CombatSystem(World world, EntityFactory entityFactory) {
        super(world);
        if (entityFactory == null) {
            throw new IllegalArgumentException("CombatSystem requires a non-null EntityFactory.");
        }
        this.entityFactory = entityFactory;
    }

    @Override
    public void update() {
        Set<Entity> attackers = new HashSet<>(world.getEntitiesWithComponent(WantsToAttackComponent.class));

        for (Entity attacker : attackers) {
            processAttack(attacker);
            world.removeComponent(attacker, WantsToAttackComponent.class);
        }
    }

    private void processAttack(Entity attacker) {
        PositionComponent attackerPos = world.getComponent(attacker, PositionComponent.class);
        DirectionComponent attackerDir = world.getComponent(attacker, DirectionComponent.class);
        StatusComponent attackerStatus = world.getComponent(attacker, StatusComponent.class);
        SizeComponent attackerSize = world.getComponent(attacker, SizeComponent.class);
        
        if (attackerPos == null || attackerDir == null || attackerStatus == null || attackerSize == null) {
            return;
        }

        int targetRow = attackerPos.row;
        int targetCol = attackerPos.column;
        switch (attackerDir.facing) {
            case UP:    targetRow--; break;
            case DOWN:  targetRow++; break;
            case LEFT:  targetCol--; break;
            case RIGHT: targetCol++; break;
        }

        Entity target = findAttackableEntityAt(targetRow, targetCol); // << A CHAMADA AGORA FUNCIONARÁ
        if (target == null || target.equals(attacker)) {
            return;
        }

        StatusComponent targetStatus = world.getComponent(target, StatusComponent.class);
        SizeComponent targetSize = world.getComponent(target, SizeComponent.class);
        
        System.out.println("[INFO COMBAT] " + attacker + " attacks " + target + "!");

        int attackPower = attackerStatus.attack;
        int defensePower = targetStatus.defense;

        if (attackerSize.size > targetSize.size) {
            attackPower *= 1.5;
            System.out.println("  > Size advantage! Attack power is boosted.");
        }
        
        int damage = Math.max(1, attackPower - (defensePower / 2));
        
        System.out.println("  > Dealt " + damage + " damage!");
        targetStatus.health -= damage;

        if (targetStatus.health <= 0) {
            System.out.println("  > Target " + target + " was defeated!");
            handleDeath(target);
        } else {
            System.out.println("  > Target " + target + " remaining health: " + targetStatus.health);
        }
    }

    private void handleDeath(Entity killedEntity) {
        PositionComponent pos = world.getComponent(killedEntity, PositionComponent.class);
        SizeComponent size = world.getComponent(killedEntity, SizeComponent.class);
        if (pos == null || size == null) return;
        
        world.destroyEntity(killedEntity);

        EntityConfig foodConfig = new EntityConfig();
        foodConfig.type = "FoodItem";
        foodConfig.image = "objects/meat.png";
        foodConfig.row = pos.row;
        foodConfig.column = pos.column;
        foodConfig.properties = Map.of("nutrition", size.size * 5, "size", size.size);
        
        entityFactory.createGameEntity(foodConfig);
        System.out.println("[INFO COMBAT] A food item appeared at (" + pos.row + "," + pos.column + ")");
    }

    /**
     * Finds an attackable entity (one with Status and Size) at a specific location.
     * @param row The target row.
     * @param col The target column.
     * @return The found Entity, or null.
     */
    private Entity findAttackableEntityAt(int row, int col) {
        Set<Entity> attackableEntities = world.getEntitiesWithComponents(
                PositionComponent.class,
                StatusComponent.class,
                SizeComponent.class
        );

        for (Entity entity : attackableEntities) {
            PositionComponent pos = world.getComponent(entity, PositionComponent.class);
            if (pos.row == row && pos.column == col) {
                return entity;
            }
        }
        return null;
    }
}