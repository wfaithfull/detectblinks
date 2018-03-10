import uk.ac.bangor.meander.detectors.Detector;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Will Faithfull
 */
public class Experiment3 extends Experiment {

    public static void main(String[] args) throws IOException {
        new Experiment3().run();
    }

    @Override
    String getFileName() {
        return "results-3.csv";
    }

    @Override
    Map<String, Supplier<Detector<Double[]>>> getDetectors() {
        return Detectors.ensembles();
    }
}
