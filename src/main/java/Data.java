import uk.ac.bangor.meander.streams.ArffSpliterator;
import uk.ac.bangor.meander.streams.Example;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Will Faithfull
 */
public class Data {

    private final static String ARFF_SUFFIX = ".arff";
    public final static int N = 6;
    public final static int DIMENSIONALITY = 60;
    public final static Integer[] CHANGE_CLASSES = {1,2};
    public final static int SUBJECTS = 6;

    public static Stream<Example> get(int subject) {
        String file = String.format("%d%s", subject, ARFF_SUFFIX);
        try {
            Stream<Example> arffStream = StreamSupport.stream(
                    new ArffSpliterator(Data.class.getClassLoader(), file, CHANGE_CLASSES), false);
            return arffStream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
