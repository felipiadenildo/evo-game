package game.evo.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;

public class InputManager implements KeyListener {

    private final Set<Integer> pressedKeys = new HashSet<>();

    public boolean isKeyPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // --- LOG DE DEPURAÇÃO ADICIONADO AQUI ---
//        System.out.println("[DEBUG InputManager] Key Pressed: " + KeyEvent.getKeyText(e.getKeyCode()));
        pressedKeys.add(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // --- LOG DE DEPURAÇÃO ADICIONADO AQUI ---
//        System.out.println("[DEBUG InputManager] Key Released: " + KeyEvent.getKeyText(e.getKeyCode()));
        pressedKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used.
    }
}