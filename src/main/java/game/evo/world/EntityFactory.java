package game.evo.world;

import game.evo.config.EntityConfig;
import game.evo.config.PlayerConfig;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.components.*;
import game.evo.utils.GameConstants;

import java.awt.Color;
import java.util.Map;
import java.util.Random;

/**
 * Factory class to create game entities from configuration objects.
 * It now uses behavioral archetypes to construct NPCs.
 */
public class EntityFactory {

    private final World world;
    private final Random random = new Random();

    public EntityFactory(World world) {
        this.world = world;
    }

    /**
     * Creates the player entity based on its configuration.
     */
    public Entity createPlayerCharacter(PlayerConfig config) {
        Entity playerEntity = world.createEntity();
        
        world.addComponent(playerEntity, new PositionComponent(config.row, config.column));
        world.addComponent(playerEntity, new PlayerControlledComponent());
        world.addComponent(playerEntity, new DirectionComponent());
        world.addComponent(playerEntity, new CollisionComponent());

        int playerSize = getIntFromProperties(config.properties, "size", 5);
        world.addComponent(playerEntity, new SizeComponent(playerSize));
        
        ProceduralSpriteComponent.BodyType playerBodyType = getBodyTypeFromProperties(config.properties, "bodyType", ProceduralSpriteComponent.BodyType.BIPED_TERRESTRIAL);
        
        // Define the player's ecology
        EcologyComponent.DietaryType diet = getDietaryTypeFromProperties(config.properties, "diet", EcologyComponent.DietaryType.OMNIVORE);
        world.addComponent(playerEntity, new EcologyComponent(diet, EcologyComponent.Temperament.NEUTRAL)); // Player is Neutral by default

        // Generate stats using the Pokémon-inspired formula
        world.addComponent(playerEntity, generateStats(playerSize, playerBodyType, random.nextLong(), config.lives));
        
        // Create the procedural visual
        world.addComponent(playerEntity, new ProceduralSpriteComponent(
            random.nextLong(), playerSize, Color.CYAN, Color.WHITE, playerBodyType
        ));
        
        world.addComponent(playerEntity, new TraitComponent()); // Gives the player a component to hold passive skills

        System.out.println("[INFO EntityFactory] PlayerCharacter created from config: " + playerEntity);
        return playerEntity;
    }

    /**
     * Creates a generic game entity based on its configuration 'type', which now represents temperament/behavior.
     */
    public Entity createGameEntity(EntityConfig config) {
        if (config == null || config.type == null || config.type.isEmpty()) return null;

        // The switch now uses generic behavioral types
        switch (config.type) {
            case "SkittishNPC":
                return createBehavioralNPC(config, EcologyComponent.Temperament.SKITTISH);
            case "NeutralNPC":
                return createBehavioralNPC(config, EcologyComponent.Temperament.NEUTRAL);
            case "AggressiveNPC":
                return createBehavioralNPC(config, EcologyComponent.Temperament.AGGRESSIVE);
            case "StaticObject":
                return createStaticObject(config);
            case "FoodItem":
                return createFoodItem(config);
            case "Portal":
                return createPortal(config);
            default:
                System.err.println("[ERROR EntityFactory] Unknown entity type in config: '" + config.type + "'");
                return null;
        }
    }

    // --- Private Helper Method to build NPCs ---

    /**
     * Creates a generic NPC with a defined temperament. All other properties,
     * including diet, are read from the config.
     */
    private Entity createBehavioralNPC(EntityConfig config, EcologyComponent.Temperament temperament) {
        Entity npcEntity = world.createEntity();
        
        world.addComponent(npcEntity, new PositionComponent(config.row, config.column));
        world.addComponent(npcEntity, new NpcComponent());
        world.addComponent(npcEntity, new DirectionComponent());
        world.addComponent(npcEntity, new CollisionComponent());
        
        // Diet is now a configurable property, with a default
        EcologyComponent.DietaryType diet = getDietaryTypeFromProperties(config.properties, "diet", EcologyComponent.DietaryType.HERBIVORE);
        world.addComponent(npcEntity, new EcologyComponent(diet, temperament));

        int npcSize = getIntFromProperties(config.properties, "size", 1);
        world.addComponent(npcEntity, new SizeComponent(npcSize));
        
        ProceduralSpriteComponent.BodyType npcBodyType = getBodyTypeFromProperties(config.properties, "bodyType", ProceduralSpriteComponent.BodyType.BIPED_TERRESTRIAL);
        
        StatusComponent status = generateStats(npcSize, npcBodyType, random.nextLong(), 1);
        world.addComponent(npcEntity, status);
        world.addComponent(npcEntity, new TraitComponent());

        // Movement speed is derived from the generated 'speed' stat
        long moveDelay = 25000 / (long)Math.max(1, status.speed);
        
        world.addComponent(npcEntity, new ProceduralSpriteComponent(
            random.nextLong(), npcSize, Color.ORANGE, Color.BLACK, npcBodyType
        ));
        
        // The AiComponent always starts with a default behavior; the AISystem will decide the action based on temperament.
        AiComponent ai = new AiComponent(AiComponent.BehaviorType.WANDER_RANDOM, moveDelay);
        world.addComponent(npcEntity, ai);

        System.out.println("[INFO EntityFactory] Created " + temperament + " " + diet + " NPC: " + npcEntity);
        return npcEntity;
    }
    
    /**
     * Creates a Portal entity using a procedurally generated sprite.
     */
    private Entity createPortal(EntityConfig config) {
        Entity portalEntity = world.createEntity();
        
        world.addComponent(portalEntity, new PositionComponent(config.row, config.column));
        world.addComponent(portalEntity, new PortalComponent());
        
        world.addComponent(portalEntity, new ProceduralSpriteComponent(
            random.nextLong(), GameConstants.CELL_SIZE, Color.BLACK, Color.WHITE,
            ProceduralSpriteComponent.BodyType.PORTAL_SPIRAL
        ));
        
        System.out.println("[INFO EntityFactory] Procedural Portal created: " + portalEntity);
        return portalEntity;
    }
    
    /**
     * Creates a Food Item entity.
     */
    private Entity createFoodItem(EntityConfig config) {
        Entity foodEntity = world.createEntity();
        
        world.addComponent(foodEntity, new PositionComponent(config.row, config.column));
        
        int nutrition = getIntFromProperties(config.properties, "nutrition", 10);
        int size = getIntFromProperties(config.properties, "size", 1);
        world.addComponent(foodEntity, new FoodComponent(nutrition));
        
        world.addComponent(foodEntity, new ProceduralSpriteComponent(
            random.nextLong(), size, Color.RED, Color.WHITE, 
            ProceduralSpriteComponent.BodyType.MEAT_CHUNK
        ));
        
        System.out.println("[INFO EntityFactory] Procedural Food Item created: " + foodEntity);
        return foodEntity;
    }

    /**
     * Creates a static object, like a tree or a rock.
     */
    private Entity createStaticObject(EntityConfig config) {
        Entity staticEntity = world.createEntity();
        world.addComponent(staticEntity, new PositionComponent(config.row, config.column));
        world.addComponent(staticEntity, new RenderableComponent(config.image, GameConstants.LAYER_ENVIRONMENT));
        
        boolean isWalkable = getBooleanFromProperties(config.properties, "isWalkable", false);
        world.addComponent(staticEntity, new TileComponent(null, isWalkable, true));

        if (!isWalkable) {
            world.addComponent(staticEntity, new CollisionComponent());
        }

        System.out.println("[INFO EntityFactory] Static Object with image '" + config.image + "' created: " + staticEntity);
        return staticEntity;
    }

    /**
     * Generates a balanced StatusComponent for a creature using Pokémon Gen 1 formulas.
     */
    private StatusComponent generateStats(int size, ProceduralSpriteComponent.BodyType bodyType, long ivSeed, int lives) {
        Random ivRandom = new Random(ivSeed);
        
        int baseHealth, baseAttack, baseDefense, baseSpeed, baseSpecial;
        switch (bodyType) {
            case FINNED_AQUATIC:
                baseHealth = 40; baseAttack = 45; baseDefense = 30; baseSpeed = 70; baseSpecial = 50;
                break;
            case BIPED_TERRESTRIAL:
            default:
                baseHealth = 55; baseAttack = 60; baseDefense = 50; baseSpeed = 50; baseSpecial = 45;
                break;
        }

        int attackIV = ivRandom.nextInt(16);
        int defenseIV = ivRandom.nextInt(16);
        int speedIV = ivRandom.nextInt(16);
        int specialIV = ivRandom.nextInt(16);
        int hpIV = ((attackIV % 2) * 8) + ((defenseIV % 2) * 4) + ((speedIV % 2) * 2) + ((specialIV % 2) * 1);

        int evTerm = 0; // EVs are 0 for newly generated creatures

        int maxHealth = ((((baseHealth + hpIV) * 2 + evTerm) * size) / 100) + size + 10;
        int attack = ((((baseAttack + attackIV) * 2 + evTerm) * size) / 100) + 5;
        int defense = ((((baseDefense + defenseIV) * 2 + evTerm) * size) / 100) + 5;
        int speed = ((((baseSpeed + speedIV) * 2 + evTerm) * size) / 100) + 5;
        int special = ((((baseSpecial + specialIV) * 2 + evTerm) * size) / 100) + 5;

        return new StatusComponent(maxHealth, attack, defense, speed, special, lives,
                                   hpIV, attackIV, defenseIV, speedIV, specialIV);
    }
    
    // --- Utility functions to safely read from the 'properties' map ---
    
    private int getIntFromProperties(Map<String, Object> props, String key, int defaultValue) {
        if (props != null && props.get(key) instanceof Number) {
            return ((Number) props.get(key)).intValue();
        }
        return defaultValue;
    }

    private double getDoubleFromProperties(Map<String, Object> props, String key, double defaultValue) {
        if (props != null && props.get(key) instanceof Number) {
            return ((Number) props.get(key)).doubleValue();
        }
        return defaultValue;
    }

    private long getLongFromProperties(Map<String, Object> props, String key, long defaultValue) {
        if (props != null && props.get(key) instanceof Number) {
            return ((Number) props.get(key)).longValue();
        }
        return defaultValue;
    }

    private boolean getBooleanFromProperties(Map<String, Object> props, String key, boolean defaultValue) {
        if (props != null && props.get(key) instanceof Boolean) {
            return (Boolean) props.get(key);
        }
        return defaultValue;
    }
    
    private ProceduralSpriteComponent.BodyType getBodyTypeFromProperties(Map<String, Object> props, String key, ProceduralSpriteComponent.BodyType defaultValue) {
        if (props != null && props.get(key) instanceof String) {
            try {
                return ProceduralSpriteComponent.BodyType.valueOf(((String) props.get(key)).toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("[WARN EntityFactory] Invalid BodyType '" + props.get(key) + "'. Using default.");
            }
        }
        return defaultValue;
    }

    private EcologyComponent.DietaryType getDietaryTypeFromProperties(Map<String, Object> props, String key, EcologyComponent.DietaryType defaultValue) {
        if (props != null && props.get(key) instanceof String) {
            try {
                return EcologyComponent.DietaryType.valueOf(((String) props.get(key)).toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("[WARN EntityFactory] Invalid DietaryType '" + props.get(key) + "'. Using default.");
            }
        }
        return defaultValue;
    }
}
