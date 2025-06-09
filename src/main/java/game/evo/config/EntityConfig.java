package game.evo.config;

import java.io.Serializable;
import java.util.Map;

// Modifique a linha de declaração da classe para adicionar "implements Serializable"
public class EntityConfig implements Serializable {
    
    // É uma boa prática adicionar um serialVersionUID para controle de versão.
    private static final long serialVersionUID = 1L;
    
    // O resto da sua classe permanece igual...
    public String type;
    public String image;
    public int row;
    public int column;
    public Map<String, Object> properties;
}