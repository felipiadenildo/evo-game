package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * Stores the core gameplay statistics for an entity, inspired by Pokémon Gen 1 mechanics.
 * This includes final calculated stats and the underlying "genes" (IVs).
 */
public class StatusComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 

    // --- Final Calculated Stats ---
    public int health;
    public int maxHealth;
    public int attack;
    public int defense;
    public int speed;
    public int special; // Special Attack and Special Defense combined

    // --- Genetic Makeup (Individual Values) ---
    public int hpIV;
    public int attackIV;
    public int defenseIV;
    public int speedIV;
    public int specialIV;

    // --- Game Progression ---
    public int lives;
    public int evolutionPoints;
    
    public int stamina;       // << CAMPO ADICIONADO
    public int maxStamina;    // << CAMPO ADICIONADO

    /**
     * Constructor used by the EntityFactory after all stats and IVs have been calculated.
     */
    public StatusComponent(int maxHealth, int attack, int defense, int speed, int special, int lives,
                           int hpIV, int attackIV, int defenseIV, int speedIV, int specialIV) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.special = special;
        this.lives = lives;
        
        this.hpIV = hpIV;
        this.attackIV = attackIV;
        this.defenseIV = defenseIV;
        this.speedIV = speedIV;
        this.specialIV = specialIV;
        
        this.maxStamina = 50 + (speed / 2); // Exemplo de fórmula: stamina baseada na velocidade
        this.stamina = this.maxStamina;
        
        this.evolutionPoints = 0;
    }
}