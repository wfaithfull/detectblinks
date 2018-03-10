import lombok.extern.java.Log;
import uk.ac.bangor.meander.detectors.*;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

/**
 * @author Will Faithfull
 */
@Log
public class Experiment1 extends Experiment {

    public static void main(String[] args) throws IOException {
        new Experiment1().run();
    }

    @Override
    String getFileName() {
        return "results-1.csv";
    }

    @Override
    Map<String, Supplier<Detector<Double[]>>> getDetectors() {
        return Detectors.multivariatesF();
    }

}
