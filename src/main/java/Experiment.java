import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import uk.ac.bangor.meander.MeanderException;
import uk.ac.bangor.meander.detectors.Detector;
import uk.ac.bangor.meander.evaluators.Evaluation;
import uk.ac.bangor.meander.evaluators.PrintStreamProgressBar;
import uk.ac.bangor.meander.evaluators.ProgressReporter;
import uk.ac.bangor.meander.evaluators.ShortConceptsEvaluator;
import uk.ac.bangor.meander.streams.Example;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Will Faithfull
 */
@Log
public abstract class Experiment {

    abstract String getFileName();
    abstract Map<String, Supplier<Detector<Double[]>>> getDetectors();

    ExecutorService executorService = Executors.newFixedThreadPool(6);
    CompletionService<EvalTuple> completionService = new ExecutorCompletionService<>(executorService);
    private              Set<String> inProgress = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final static Object      LOCK       = new Object();

    @Getter @Setter @AllArgsConstructor
    private static class EvalTuple {
        String name;
        Evaluation evaluation;
        int subject;
    }

    private int received;

    public synchronized int getCompleted() {
        return received;
    }

    private class AllSubjectsReporter implements ProgressReporter {

        Map<Long, Long> progressByThread = new ConcurrentHashMap<>();
        private String lastMessage = "";

        @Override
        public synchronized void update(long progress) {
            update(progress, lastMessage);
        }

        @Override
        public synchronized void update(long progress, String message) {
            update(progress, -1, message);
        }

        @Override
        public synchronized void update(long progress, long total) {
            update(progress, total, lastMessage);
        }

        @Override
        public synchronized void update(long progress, long total, String message) {
            long threadId = Thread.currentThread().getId();
            progressByThread.put(threadId, progress);

            long all = progressByThread.values().stream().mapToLong(Long::longValue).sum();

            StringBuilder out = new StringBuilder(progressByThread.size() + " threads ( ");

            for(Map.Entry<Long,Long> entry: progressByThread.entrySet()) {
                out.append(String.format("%d=[%d] ", entry.getKey(), entry.getValue()));
            }

            out.append(")");

            System.out.printf("\r%s %s", out.toString(), message);

            lastMessage = message;
        }

        public void reset() {
            lastMessage = "";
            progressByThread.clear();
            System.out.print("\n");
        }
    }

    void run() throws IOException {

        String fn = getFileName();
        Map<String, Supplier<Detector<Double[]>>> detectorMap = getDetectors();

        //evaluator.setProgressReporter(new PrintStreamProgressBar('-',20));

        //ProgressReporter progressReporter = new PrintStreamProgressBar('=', 50);
        final AllSubjectsReporter reporter = new AllSubjectsReporter();

        boolean writeHeader = true;
        File results = new File(fn);
        if(results.exists()) {
            List<String> done = Files.readAllLines(results.toPath());

            if(done.size() > 2) {
                done.remove(0);

                List<String> detectors = done.stream()
                        .map(s -> s.split(",")[0])
                        .collect(Collectors.toList());

                log.info(String.format("Resuming from %s..", detectors.get(detectors.size()-1)));

                detectors.stream().forEach(detectorMap::remove);

                writeHeader = false;
            }
        }

        if(writeHeader) {
            PrintWriter out = new PrintWriter(String.format(fn));
            out.println("Detector,Subject,N,ARL,TTD,FAR,MDR");
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(String.format(fn), true)));
        for(Map.Entry<String, Supplier<Detector<Double[]>>> entry : detectorMap.entrySet()) {

            log.info("Starting " + entry.getKey() + "...");

            boolean async = isThreadSafe(entry.getKey());

            if(async) {
                for (int subject = 1; subject <= Data.SUBJECTS; subject++) {

                    int finalSubject = subject;
                    completionService.submit(() -> {
                        inProgress.add(entry.getKey());
                        Stream<Example> stream = Data.get(finalSubject);
                        ShortConceptsEvaluator evaluator = new ShortConceptsEvaluator();
                        evaluator.setAllowEarly(5);
                        evaluator.setProgressReporter(reporter);
                        Evaluation evaluation = evaluator.evaluate(entry.getValue().get(), stream);
                        inProgress.remove(entry.getKey());
                        return new EvalTuple(entry.getKey(), evaluation, finalSubject);
                    });

                }

                boolean error = false;

                //progressReporter.update(received, detectorMap.size()*Data.SUBJECTS, "Working on " + entry.getKey());

                int rcv = 0;

                while (rcv < Data.SUBJECTS && !error) {
                    Future<EvalTuple> resultFuture = null; //blocks if none available
                    try {
                        resultFuture = completionService.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        EvalTuple tuple = resultFuture.get();
                        Evaluation evaluation = tuple.getEvaluation();

                        rcv++;
                        received++;

                        writeEvaluation(tuple.getName(), tuple.getSubject(), evaluation, out);
                    } catch (Exception e) {
                        //log
                        error = true;
                        log.info(e.getMessage());
                    }
                }
            } else {
                log.info(entry.getKey() + " has been designated as not thread safe. Evaluating synchronously.");

                Evaluation[] evaluations = new Evaluation[Data.SUBJECTS];
                for (int subject = 1; subject <= Data.SUBJECTS; subject++) {
                    Stream<Example> stream = Data.get(subject);
                    ShortConceptsEvaluator evaluator = new ShortConceptsEvaluator();
                    evaluator.setAllowEarly(5);
                    evaluator.setProgressReporter(reporter);
                    evaluations[subject-1] = evaluator.evaluate(entry.getValue().get(), stream);
                }

                for (int subject = 1; subject <= Data.SUBJECTS; subject++){
                    writeEvaluation(entry.getKey(), subject, evaluations[subject-1], out);
                }
            }

            reporter.reset();
        }
    }

    static final List<String> UNSAFE = Arrays.asList("adwin","seq1","seq2");
    private static boolean isThreadSafe(String key) {
        boolean unsafe = UNSAFE.stream().anyMatch(x -> key.contains(x));
        return !unsafe;
    }

    private synchronized void writeEvaluation(String name, int subject, Evaluation evaluation, PrintWriter out) {
        out.print(name + ",");
        out.print(subject + ",");
        out.print(evaluation.getN() + ",");
        out.print(evaluation.getArl() + ",");
        out.print(evaluation.getTtd() + ",");
        out.print(evaluation.getFar() + ",");
        out.print(evaluation.getMdr() + "\n");
        out.flush();
    }

}
