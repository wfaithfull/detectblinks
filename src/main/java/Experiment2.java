import uk.ac.bangor.meander.detectors.Detector;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Will Faithfull
 */
public class Experiment2 extends Experiment {

    public static void main(String[] args) throws IOException {
        new Experiment2().run();
    }

    @Override
    String getFileName() {
        return "results-cf-2.csv";
    }

    @Override
    Map<String, Supplier<Detector<Double[]>>> getDetectors() {
        return Detectors.multivariatesRethresholdedF();
    }
}
