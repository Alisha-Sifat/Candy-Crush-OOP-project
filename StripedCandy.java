import com.candycrush.Candy;
import java.awt.Image;

public class StripedCandy extends Candy {

    private boolean horizontal;

    public StripedCandy(int type, Image image, boolean horizontal) {
        super(type, image);
        this.horizontal = horizontal;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    @Override
    public void onMatch() {
        // handled in Board
    }
}