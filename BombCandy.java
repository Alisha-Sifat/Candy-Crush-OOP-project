import com.candycrush.Candy;
import java.awt.Image;

public class BombCandy extends Candy {

    public BombCandy(Image image) {
        super(-1, image); // special type
    }

    @Override
    public void onMatch() {
        // handled in Board
    }
}