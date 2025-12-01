package nl.vu.psy.ams.suite.tools;

import nl.vu.psy.ams.suite.data.structures.Sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataSetGenerator implements Iterator<Sample> {
    private int length;
    private final long startTime;
    private final long deltaTime;
    private final List<float[]> data;

    private int index;
    private int returned;

    public DataSetGenerator(String csvPath) throws IOException {
        this(csvPath, 0, 0);
    }

    public DataSetGenerator(String csvPath, long startTime, long deltaTime) throws IOException {
        this.startTime = startTime;
        this.deltaTime = deltaTime;
        this.data = readCsvFile(csvPath);
        this.length = this.data.size();
    }

    public List<float[]> getNativeCsvData() {
        return data;
    }

    public List<Sample> generate(int sampleNum) {
        length = sampleNum;
        return generate();
    }

    public List<Sample> generate() {
        List<Sample> result = new ArrayList<>();
        while (hasNext())
            result.add(next());

        return result;
    }

    public List<float[]> generateCsvData(int sampleNum) {
        length = sampleNum;
        return generateCsvData();
    }

    public List<float[]> generateCsvData() {
        List<float[]> result = new ArrayList<>();
        while (hasNext())
            result.add(next().data());

        return result;
    }

    @Override
    public boolean hasNext() {
        return (!data.isEmpty() && returned < length);
    }

    @Override
    public Sample next() {
        if (!hasNext())
            return null;

        if (index >= data.size())
            index = 0;

        var result = new Sample(startTime + deltaTime * returned, data.get(index));

        index++;
        returned++;

        return result;
    }

    static List<float[]> readCsvFile(String path) throws IOException {
        try (InputStream featuresStream = DataSetGenerator.class.getResourceAsStream(path)) {
            if (featuresStream == null) {
                throw new IOException("File " + path + " file not found in JAR!");
            }

            List<float[]> result = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(featuresStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split(",");
                    float[] values = new float[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        values[i] = Float.parseFloat(parts[i]);
                    }
                    result.add(values);
                }
            }
            return result;
        }
    }
}
