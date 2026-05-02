import javax.swing.*;
import java.awt.*;

public class MainMenu {

   public void showMenu() {

    JFrame frame = new JFrame("Map");
    frame.setSize(500, 800);

    frame.add(new LevelMap());   // directly open map

    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   }}
