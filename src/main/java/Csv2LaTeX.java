import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Will Faithfull
 */
public class Csv2LaTeX {

    public static void main(String[] args) throws IOException {
        System.out.println(toLatexTable(new File("results.csv").toPath(), 0, 1, 2));
    }

    public static String toLatexTable(Path csv, int idCol, int groupByCol, int... ignoreCols) throws IOException {

        List<String> lines = Files.readAllLines(csv);

        StringBuilder table = new StringBuilder();

        List<String> groups = lines.stream()
                .map(l -> l.split(","))
                .map(row -> row[groupByCol])
                .distinct()
                .collect(Collectors.toList());

        table.append("\\begin{tabular}");
        table.append("{");
        for(int i=0;i<groups.size()+1;i++) {
            table.append("c");
        }
        table.append("}\n");

        Map<String,String> distinctRows = new HashMap<>();

        lines.stream()
                .map(l -> l.split(","))
                .forEach(row -> {
                    String rowTemplate = "\t";
                    for(int i=0;i<groups.size();i++) {
                        rowTemplate += String.format("${%s}&", groups.get(i));
                    }
                    rowTemplate += "\\\\\n";

                    distinctRows.put(row[idCol], rowTemplate);

                });

        lines.stream()
                .map(l -> l.split(","))
                .forEach(row -> {
                    int col = groups.indexOf(row[groupByCol]);
                    List<String> otherColumns = new ArrayList<>(row.length-2);

                    for(int i=0;i<row.length;i++) {
                        if(i == idCol || i == groupByCol) {
                            continue;
                        } else {
                            boolean skip = false;
                            for(int ignore : ignoreCols) {
                                if(i==ignore) {
                                    skip=true;
                                }
                            }

                            if(skip) {
                                continue;
                            }
                        }

                        otherColumns.add(row[i]);
                    }

                    String code = row[idCol];
                    String template = distinctRows.get(code);

                    String data = "$\\bigl(\\begin{smallmatrix}";
                    List<Double> asDoubles = otherColumns.stream().mapToDouble(Double::parseDouble).boxed().collect(Collectors.toList());

                    for(int i=0;i<asDoubles.size();i++) {
                        data += String.format("%.2f", asDoubles.get(i));

                        if(i % 2 == 0) {
                            data += "&";
                        } else {
                            if(i < asDoubles.size()-1) {
                                data += "\\\\";
                            }
                        }
                    }
                    data += "\\end{smallmatrix}\\bigr)$";

                    String filled = template.replace(String.format("${%s}", row[groupByCol]), data);
                    distinctRows.put(code, filled);
                });

        distinctRows.values().forEach(table::append);

        table.append("\\end{tabular}");
        return table.toString();
    }

    private static boolean isSquare(int n) {
        double sqrt = Math.sqrt(n);
        int x = (int)sqrt;
        return Math.pow(sqrt,2) == Math.pow(x,2);
    }
}
