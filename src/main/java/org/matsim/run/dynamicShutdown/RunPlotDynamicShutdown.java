package org.matsim.run.dynamicShutdown;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;



public class RunPlotDynamicShutdown {

    // Dynamic Shutdown Config Group
    private static final int MINIMUM_ITERATION = 0; // 500 TODO: Revert
    private static final int ITERATION_TO_START_FINDING_SLOPES = 50;
    private static final int MINIMUM_WINDOW_SIZE = 50;
    private static final boolean EXPANDING_WINDOW = true;
    private static final double EXPANDING_WINDOW_PCT_RETENTION = 0.25;
    private int ITERATIONS_IN_ZONE_TO_CONVERGE = 50;

    private static Map<String, Map<Integer,Double>> slopesMode = new HashMap<>();

    public static void main(String[] args) {

        RunPlotDynamicShutdown plot = new RunPlotDynamicShutdown();
        Map<String, Map<Integer, Double>> modeHistories = generateModeHistories();

        for (int i = 0; i < modeHistories.get("car").size(); i++) {
            plot.bestFitLineMode(i, modeHistories);

        }



        produceGraphs(modeHistories);

    }

    private static void produceGraphs(Map<String, Map<Integer, Double>> modeHistories) {

        for (String mode : modeHistories.keySet()) {
            XYLineChartDualYAxis chart = new XYLineChartDualYAxis("Mode Stats", "iteration", "mode", "slope");
            Map<Integer, Double> metric = modeHistories.get(mode);
            chart.addSeries(mode, metric);
            chart.addSeries2("d/dx(" + mode + ")", slopesMode.get(mode));

            chart.addMatsimLogo();
            chart.addVerticalRange(-0.00003,0.00003);
            String path = "C:\\Users\\jakob\\projects\\matsim-berlin\\src\\main\\java\\org\\matsim\\run\\dynamicShutdown\\OutputGraphs/";
            chart.saveAsPng(path + mode + "_dynamicShutdown.png", 800, 600);
        }

    }

    void bestFitLineMode(int iteration, Map<String, Map<Integer, Double>> modeHist) { //TODO: Make private

        if (iteration < ITERATION_TO_START_FINDING_SLOPES) {
            return;
        }


        for (Map.Entry<String, Map<Integer, Double>> entry : modeHist.entrySet()) {
            String mode = entry.getKey();
//            log.info("Mode checked for " + mode);

            double slope = computeLineSlope(entry.getValue(), iteration);

            Map<Integer,Double> slopesForMode = slopesMode.computeIfAbsent(mode, v -> new HashMap<>());
            slopesForMode.put(iteration,slope);
        }
    }

    private double computeLineSlope(Map<Integer,Double> inputMap, int iteration ) {


        int currentIter = iteration;
        int startIteration = currentIter - MINIMUM_WINDOW_SIZE;
        if (EXPANDING_WINDOW && (int) EXPANDING_WINDOW_PCT_RETENTION * currentIter > MINIMUM_WINDOW_SIZE) {
            startIteration = (int) (1 - EXPANDING_WINDOW_PCT_RETENTION) * currentIter;
        }

        ArrayList<Integer> x = new ArrayList<>();
        ArrayList<Double> y = new ArrayList<>();
        for (Integer it : inputMap.keySet()) {
            if (it >= startIteration && it <= currentIter) {
                x.add(it);
                y.add(inputMap.get(it));
            }
        }

        if (x.size() != y.size()) {
            throw new IllegalArgumentException("array lengths are not equal");
        }
        int n = x.size();

        // first pass
        double sumx = 0.0, sumy = 0.0;
        for (int i = 0; i < n; i++) {
            sumx  += x.get(i);
            sumy  += y.get(i);
        }
        double xbar = sumx / n;
        double ybar = sumy / n;

        // second pass: compute summary statistics
        double xxbar = 0.0, xybar = 0.0;
        for (int i = 0; i < n; i++) {
            xxbar += (x.get(i) - xbar) * (x.get(i) - xbar);
            xybar += (x.get(i) - xbar) * (y.get(i) - ybar);
        }
        return xybar / xxbar;


    }

    private static Map<String, Map<Integer, Double>> generateModeHistories() {
        Map<String, Map<Integer, Double>> modeHistories = new HashMap<>();
        String csvFile = "D:\\runs\\dynamicShutdown\\07-2500-noZoomer\\scenarios\\berlin-v5.5-1pct\\output-berlin-v5.5-1pct\\berlin-v5.5-1pct.modestats.txt";
        BufferedReader br = null;
        String line = "";
        String[] header;
        String cvsSplitBy = "\t";

        try {

            br = new BufferedReader(new FileReader(csvFile));
            header = br.readLine().split(cvsSplitBy);
            for (String x : header) {
                modeHistories.put(x, new HashMap<>());
            }

            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] modeStatLine = line.split(cvsSplitBy);

                double[] doubleValues = Arrays.stream(modeStatLine)
                        .mapToDouble(Double::parseDouble)
                        .toArray();
                System.out.println("\n");
                Integer iter = (int) doubleValues[0];
                for (int i = 1; i < doubleValues.length; i++) {
                    String name = header[i];
                    Double x = doubleValues[i];
                    Map<Integer, Double> integerDoubleMap = modeHistories.get(name);
                    integerDoubleMap.put(iter, x);

//                    System.out.println(x + "\t");
                }

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        for (String name : modeHistories.keySet()) {
            System.out.println(name);
            for (Integer iter : modeHistories.get(name).keySet()) {
                System.out.println(iter + " - " + modeHistories.get(name).get(iter));

            }
        }
        return modeHistories;
    }
}
