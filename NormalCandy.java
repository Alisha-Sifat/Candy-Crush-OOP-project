


import com.candycrush.Candy;

public class NormalCandy extends Candy {

    public NormalCandy(int type, java.awt.Image image) {
        super(type, image);
    }

    @Override
    public void onMatch() {
        // normal candy disappears (no extra behavior)
    }
}