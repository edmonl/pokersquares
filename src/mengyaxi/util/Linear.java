package mengyaxi.util;

import java.util.function.UnaryOperator;

/**
 *
 * @author Meng
 */
public final class Linear implements UnaryOperator<Double> {

    public final double a, b;

    public Linear(final double x0, final double y0, final double x1, final double y1) {
        a = (y1 - y0) / (x1 - x0);
        b = y0 - a * x0;
    }

    @Override
    public Double apply(Double x) {
        return a * x + b;
    }
}
