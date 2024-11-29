package sk.upjs.kopr.file_copy.client;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class Props {
    private double down = 0;
    private double total = 0;

    public Props(double down, double total) {
        this.total = total;
        this.down = down;
    }

    public double getDown() {
        return down;
    }

    public void setDown(double down) {
        this.down = down;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
