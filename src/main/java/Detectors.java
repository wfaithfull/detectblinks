import uk.ac.bangor.meander.MeanderException;
import uk.ac.bangor.meander.detectors.*;
import uk.ac.bangor.meander.detectors.controlchart.MR;
import uk.ac.bangor.meander.detectors.controlchart.WindowDeviationChart;
import uk.ac.bangor.meander.detectors.ensemble.SubspaceEnsemble;
import uk.ac.bangor.meander.detectors.ensemble.SubspaceVoteDecayEnsemble;
import uk.ac.bangor.meander.detectors.windowing.WindowPair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Will Faithfull
 */
public class Detectors {

    public static Map<String, Detector<Double[]>> multivariates() {
        Map<String,Detector<Double[]>> detectorMap = new LinkedHashMap<>();

        int[] wsz = {5, 10, 15, 20, 25, 30, 35, 40, 45, 50};

        for(int sz : wsz) {
            addMultivariates(detectorMap, sz);
        }

        return detectorMap;
    }

    public static Map<String, Supplier<Detector<Double[]>>> multivariatesF() {
        Map<String,Supplier<Detector<Double[]>>> detectorMap = new LinkedHashMap<>();

        int[] wsz = {5, 10, 15, 20, 25, 30, 35, 40, 45, 50};

        for(int sz : wsz) {
            addMultivariatesF(detectorMap, sz);
        }

        return detectorMap;
    }

    public static Map<String, Supplier<Detector<Double[]>>> multivariatesRethresholdedF() {
        Map<String,Supplier<Detector<Double[]>>> newMap = new LinkedHashMap<>();

        int[] wsz = {5, 10, 15, 20, 25, 30, 35, 40, 45, 50};
        for(int sz : wsz) {
            String spllKey = String.format("%s-W%d", "SPLL", sz);
            String klKey = String.format("%s-W%d", "KL", sz);
            String hotellingKey = String.format("%s-W%d", "Hotelling", sz);

            addRethresholdedF(newMap, spllKey, () -> spll(sz));
            addRethresholdedF(newMap, klKey, () -> kl(sz));
            addRethresholdedF(newMap, hotellingKey, () -> hotelling(sz));
        }

        return newMap;
    }

    public static Map<String, Detector<Double[]>> multivariatesRethresholded() {
        Map<String,Detector<Double[]>> newMap = new LinkedHashMap<>();

        int[] wsz = {5, 10, 15, 20, 25, 30, 35, 40, 45, 50};
        for(int sz : wsz) {
            String spllKey = String.format("%s-W%d", "SPLL", sz);
            String klKey = String.format("%s-W%d", "KL", sz);
            String hotellingKey = String.format("%s-W%d", "Hotelling", sz);

            addRethresholded(newMap, spllKey, () -> spll(sz));
            addRethresholded(newMap, klKey, () -> kl(sz));
            addRethresholded(newMap, hotellingKey, () -> hotelling(sz));
        }

        return newMap;
    }

    private static void addRethresholdedF(Map<String,Supplier<Detector<Double[]>>> map, String mvKey, Supplier<Detector<Double[]>> detector) {
        for (Map.Entry<String, Detector<Double>> uv : univariates().entrySet()) {
            map.put(String.format("%s-%s", mvKey, uv.getKey()), () -> rethreshold(detector.get(), uv.getValue()));
        }
    }

    private static void addRethresholded(Map<String,Detector<Double[]>> map, String mvKey, Supplier<Detector<Double[]>> detector) {
        for (Map.Entry<String, Detector<Double>> uv : univariates().entrySet()) {
            map.put(String.format("%s-%s", mvKey, uv.getKey()), rethreshold(detector.get(), uv.getValue()));
        }
    }

    public static Detector<Double[]> rethreshold(Detector<Double[]> detector, Detector<Double> threshold) {
        if(!ReductionFunction.class.isAssignableFrom(detector.getClass()))
            throw new MeanderException("Cannot continue. MV detector is not a reduction function.");

        if(!DecisionFunction.class.isAssignableFrom(threshold.getClass()))
            throw new MeanderException("Cannot continue. UV detector is not a decision function.");

        return new FunctionalDetector((ReductionFunction)detector, (DecisionFunction) threshold, x -> x > 50);
    }

    public static Map<String, Detector<Double>> univariates() {
        Map<String,Detector<Double>> detectorMap = new LinkedHashMap<>();

        Method[] methods = MoaDetectorAdapter.class.getMethods();

        for(Method method : methods) {

            // skip seq2
            if(method.getName().equals("seq2")) {
                continue;
            }

            if(method.getReturnType().equals(MoaDetectorAdapter.class)) {
                try {
                    detectorMap.put(String.format("%s", method.getName()), (Detector<Double>) method.invoke(null));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }

        MR mr = new MR();
        mr.setResetOnChangeDetected(true);

        detectorMap.put("MR", mr);

        WindowDeviationChart wdc = new WindowDeviationChart(50);
        detectorMap.put("WDC", wdc);

        return detectorMap;
    }

    public static Map<String,Supplier<Detector<Double[]>>> ensembles() {
        Map<String,Supplier<Detector<Double[]>>> detectorMap = new LinkedHashMap<>();
        double[] agreements = {.01, .05, .1, .15, .2, .25};

        Method[] methods = MoaDetectorAdapter.class.getMethods();

        for(Method method : methods) {
            if(method.getReturnType().equals(MoaDetectorAdapter.class)) {
                for(double agreement : agreements) {
                    detectorMap.put(String.format("%s-%d", method.getName(), (int)(agreement*100)), () ->
                            buildEnsemble(Data.DIMENSIONALITY, agreement, () -> {
                        try {
                            return (Detector<Double>) method.invoke(null);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    ));

                    detectorMap.put(String.format("%sDecay-%d", method.getName(), (int)(agreement*100)), () ->
                            buildDecayEnsemble(Data.DIMENSIONALITY, agreement, () -> {
                                        try {
                                            return (Detector<Double>) method.invoke(null);
                                        } catch (IllegalAccessException | InvocationTargetException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                            ));
                }
            }
        }

        for(double agreement : agreements) {
            detectorMap.put(String.format("MR-%d", (int)(agreement*100)), () ->
                    buildEnsemble(Data.DIMENSIONALITY, agreement, () -> {
                MR mr = new MR();
                mr.setResetOnChangeDetected(false);
                return mr;
            }));
            detectorMap.put(String.format("MRDecay-%d", (int)(agreement*100)), () ->
                    buildDecayEnsemble(Data.DIMENSIONALITY, agreement, () -> {
                        MR mr = new MR();
                        mr.setResetOnChangeDetected(false);
                        return mr;
                    }));
        }

        return detectorMap;
    }

    private static void addMultivariates(Map<String, Detector<Double[]>> detectorMap, int wsz) {
        SPLL spll = spll(wsz);
        detectorMap.put(String.format("SPLL-W%d", wsz), spll);

        KL kl = kl(wsz);
        detectorMap.put(String.format("KL-W%d", wsz), new FunctionalDetector(kl, kl, x -> x > wsz*2));

        Hotelling hotelling = hotelling(wsz);
        detectorMap.put(String.format("Hotelling-W%d", wsz), hotelling);

    }

    private static void addMultivariatesF(Map<String, Supplier<Detector<Double[]>>> detectorMap, int wsz) {
        detectorMap.put(String.format("SPLL-W%d", wsz), () -> spll(wsz));
        detectorMap.put(String.format("KL-W%d", wsz), () -> {
            KL kl = kl(wsz);
            return new FunctionalDetector(kl, kl, x -> x > wsz*2);
        });
        detectorMap.put(String.format("Hotelling-W%d", wsz), () -> hotelling(wsz));
    }

    public static SPLL spll(int wsz) {
        return new SPLL(new WindowPair<>(wsz, wsz, double[].class), 3);
    }

    public static KL kl(int wsz) {
        return new KL(wsz, 3);
    }

    public static Hotelling hotelling(int wsz) {
        return new Hotelling(new WindowPair<>(wsz, wsz, double[].class));
    }

    public static SubspaceEnsemble buildEnsemble(int features, double threshold, Supplier<Detector<Double>> supplier) {
        Detector<Double>[] detectors = new Detector[features];
        for(int i=0;i<Data.DIMENSIONALITY;i++) {
            detectors[i] = supplier.get();
        }

        return new SubspaceEnsemble(threshold, detectors);
    }

    public static SubspaceVoteDecayEnsemble buildDecayEnsemble(int features, double threshold, Supplier<Detector<Double>> supplier) {
        Detector<Double>[] detectors = new Detector[features];
        for(int i=0;i<Data.DIMENSIONALITY;i++) {
            detectors[i] = supplier.get();
        }

        return new SubspaceVoteDecayEnsemble(threshold, detectors);
    }


}
