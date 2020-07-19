package com.github.koriel50000.prelude.reversi;

import java.util.*;

public class LineBuffer {

    private Map<Integer, List<StringBuilder>> offsetBuffer;

    private int currentOffset;
    private List<StringBuilder> currentBuffer;
    private StringBuilder currentLine;

    public LineBuffer() {
        offsetBuffer = new HashMap<>();
    }

    void print(String str) {
        currentLine.append(str);
    }


    public void print(int y) {
        print(Integer.toString(y));
    }

    void println() {
        currentLine = new StringBuilder();
        currentBuffer.add(currentLine);
    }

    void println(String str) {
        print(str);
        println();
    }

    public void flush() {
        List<Integer> sortedKeys = new ArrayList<>(offsetBuffer.keySet());
        Collections.sort(sortedKeys);

        boolean hasNext = true;
        for (int index = 0; hasNext; index++) {
            StringBuilder line = new StringBuilder();
            for (Integer offset : sortedKeys) {
                List<StringBuilder> buffer = offsetBuffer.get(offset);
                if (index < buffer.size()) {
                    while (line.length() < offset) {
                        line.append(' ');
                    }
                    line.append(buffer.get(index));
                }
            }
            hasNext = line.length() > 0;
            if (hasNext) {
                System.out.println(line.toString());
            }
        }

        offsetBuffer.clear();
    }

    public LineBuffer offset(int offset) {
        if (!offsetBuffer.containsKey(offset)) {
            currentLine = new StringBuilder();
            currentBuffer = new ArrayList<>();
            currentBuffer.add(currentLine);
            offsetBuffer.put(offset, currentBuffer);
            currentOffset = offset;
        } else {
            currentOffset = offset;
            currentBuffer = offsetBuffer.get(offset);
            currentLine = currentBuffer.get(currentBuffer.size() - 1);
        }
        return this;
    }
}
