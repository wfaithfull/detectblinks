import uk.ac.bangor.meander.detectors.Detector;
import uk.ac.bangor.meander.evaluators.BasicEvaluator;
import uk.ac.bangor.meander.evaluators.Evaluation;
import uk.ac.bangor.meander.evaluators.Evaluator;

/**
 * @author Will Faithfull
 */
public class IARL {

    public static void main(String[] args) {
        Evaluator evaluator = new BasicEvaluator();

        System.out.print("iarl = [");
        for(int i = 1; i<= Data.N; i++) {
            Evaluation evaluation = evaluator.evaluate(new Detector<Double[]>() {
                @Override
                public void update(Double[] input) {

                }

                @Override
                public boolean isChangeDetected() {
                    return false;
                }

                @Override
                public void reset() {

                }
            }, Data.get(i));

            System.out.print(evaluation.getIdealARL());
            if(i != Data.N) {
                System.out.print(",\n");
            }
        }
        System.out.print("]");
    }

}
