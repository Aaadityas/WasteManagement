import java.awt.*;
import javax.swing.border.AbstractBorder;

public class RoundedBorder extends AbstractBorder {
    private int radius;
    public RoundedBorder(int radius) {
        this.radius = radius;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        g.setColor(Color.GRAY);
        g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
    }
}
