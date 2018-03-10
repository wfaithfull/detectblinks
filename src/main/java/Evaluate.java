import uk.ac.bangor.meander.detectors.*;
import uk.ac.bangor.meander.detectors.controlchart.MR;
import uk.ac.bangor.meander.detectors.controlchart.WindowDeviationChart;
import uk.ac.bangor.meander.detectors.ensemble.LogisticDecayFunction;
import uk.ac.bangor.meander.detectors.ensemble.MultivariateVoteDecayEnsemble;
import uk.ac.bangor.meander.detectors.ensemble.SubspaceVoteDecayEnsemble;
import uk.ac.bangor.meander.detectors.windowing.WindowPair;
import uk.ac.bangor.meander.evaluators.Evaluation;
import uk.ac.bangor.meander.evaluators.PrintStreamProgressBar;
import uk.ac.bangor.meander.evaluators.ShortConceptsEvaluator;
import uk.ac.bangor.meander.streams.ArffSpliterator;
import uk.ac.bangor.meander.streams.Example;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Will Faithfull
 */
public class Evaluate {

    public static void main(String[] args) throws IOException {

        SPLL spll = new SPLL(new WindowPair<double[]>(25,25,double[].class), 3);
        KL kl = new KL(50, 3);

        MR threshold = new MR();
        threshold.setResetOnChangeDetected(true);
        Detector<Double[]> mv = new FunctionalDetector(spll, threshold, x -> x >= 50);

        Stream<Example> arffStream = StreamSupport.stream(new ArffSpliterator(Evaluate.class.getClassLoader(), "1.arff", 1, 2), false);

        /*
        arffStream.forEach(example -> {
            StreamContext context = example.getContext();
            System.out.println(String.format("%d)\t %d [%s] %s", context.getIndex(), context.getLabel(),
                    context.getCurrentTransition().toString(), context.isChanging() ? "Changing..." : ""));
        });*/


        Supplier<Detector<Double>> mr = () -> {
            MR chart = new MR();
            chart.setResetOnChangeDetected(false);
            return chart;
        };
        AbstractMultivariateDetector ensemble = createEnsemble(60, .25, mr);

        ensemble = Detectors.buildDecayEnsemble(Data.DIMENSIONALITY, .25, () -> {
            return new WindowDeviationChart(50);
        });


        MultivariateVoteDecayEnsemble mve = new MultivariateVoteDecayEnsemble(.4, new LogisticDecayFunction(.05),
                mv,
                createEnsemble(60, .15, mr),
                createEnsemble(60, .15, () -> MoaDetectorAdapter.cusum()));

        Hotelling hotelling = new Hotelling(new WindowPair<double[]>(10,10,double[].class));


        FunctionalDetector detector = new FunctionalDetector(hotelling, threshold, x -> x > 100);

        ShortConceptsEvaluator evaluator = new ShortConceptsEvaluator();
        evaluator.setProgressReporter(new PrintStreamProgressBar('-',20));
        evaluator.setAllowEarly(5);
        Evaluation evaluation = evaluator.evaluate(ensemble, arffStream);
        System.out.println(evaluation.toString());

        System.out.println("Detections: " + Arrays.toString(evaluation.getDetections().toArray()));
        System.out.println("Blinks: " + Arrays.toString(evaluation.getTransitions().stream().map(x -> x.getStart()).toArray()));
    }

    static SubspaceVoteDecayEnsemble createEnsemble(int features, double threshold, Supplier<Detector<Double>> detectorSupplier) {
        Detector<Double>[] detectors = new Detector[features];

        for(int i=0;i<features;i++) {
            detectors[i] = detectorSupplier.get();
        }

        return new SubspaceVoteDecayEnsemble(threshold, detectors);
    }

}
