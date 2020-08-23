package com.github.koriel50000.prelude.reversi;

import java.util.*;

public class LineBuffer {

    private Map<Integer, List<StringBuilder>> offsetMap;

    private int currentOffset;
    private List<StringBuilder> currentBuffer;
    private StringBuilder currentLine;

    public LineBuffer() {
        offsetMap = new HashMap<>();
    }

    void print(String str) {
        currentLine.append(str);
    }


    public void print(int y) {
        print(Integer.toString(y));
    }

    public void println() {
        currentLine = new StringBuilder();
        currentBuffer.add(currentLine);
    }

    public void println(String str) {
        print(str);
        println();
    }

    public void flush() {
        List<Integer> sortedKeys = new ArrayList<>(offsetMap.keySet());
        Collections.sort(sortedKeys);

        int index = 0;
        while (true) {
            boolean eol = true;
            StringBuilder line = new StringBuilder();
            for (Integer offset : sortedKeys) {
                List<StringBuilder> buffer = offsetMap.get(offset);
                if (index < buffer.size()) {
                    StringBuilder offsetLine = buffer.get(index);
                    eol = (index == buffer.size() - 1) && (offsetLine.length() == 0);
                    if (!eol) {
                        while (line.length() < offset) {
                            line.append(' ');
                        }
                        line.append(offsetLine);
                    }
                }
            }
            if (eol) {
                break;
            }
            System.out.println(line.toString());
            index++;
        }

        offsetMap.clear();
    }

    public LineBuffer offset(int offset) {
        if (!offsetMap.containsKey(offset)) {
            currentLine = new StringBuilder();
            currentBuffer = new ArrayList<>();
            currentBuffer.add(currentLine);
            offsetMap.put(offset, currentBuffer);
            currentOffset = offset;
        } else {
            currentOffset = offset;
            currentBuffer = offsetMap.get(offset);
            currentLine = currentBuffer.get(currentBuffer.size() - 1);
        }
        return this;
    }
}
