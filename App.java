import javax.swing.*;

public class App {
    public static void main(String[] args) {

        JFrame frame = new JFrame("Sugar Rush Saga");

        frame.setSize(500, 800);
        frame.add(new PlayPage());   // 👈 THIS IS THE IMPORTANT LINE

        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}