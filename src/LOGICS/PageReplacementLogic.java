package LOGICS;
import java.util.*;

public class PageReplacementLogic {
    public static String runFIFO(int[] pages, int capacity) {
        HashSet<Integer> s = new HashSet<>(capacity);
        Queue<Integer> indexes = new LinkedList<>();
        int pageFaults = 0;

        for (int page : pages) {
            if (s.size() < capacity) {
                if (!s.contains(page)) { s.add(page); pageFaults++; indexes.add(page); }
            } else if (!s.contains(page)) {
                int val = indexes.poll();
                s.remove(val); s.add(page); indexes.add(page); pageFaults++;
            }
        }
        return "FIFO Page Faults: " + pageFaults;
    }

    public static String runLRU(int[] pages, int capacity) {
        HashSet<Integer> s = new HashSet<>(capacity);
        List<Integer> indexes = new ArrayList<>();
        int pageFaults = 0;

        for (int page : pages) {
            if (s.size() < capacity) {
                if (!s.contains(page)) { s.add(page); pageFaults++; }
                indexes.add(page);
            } else if (!s.contains(page)) {
                int lru = indexes.get(0);
                indexes.remove(0); s.remove(lru); s.add(page); pageFaults++;
                indexes.add(page);
            } else {
                indexes.remove(Integer.valueOf(page)); indexes.add(page);
            }
        }
        return "LRU Page Faults: " + pageFaults;
    }

    public static String runOptimal(int[] pages, int capacity) {
        HashSet<Integer> s = new HashSet<>(capacity);
        int pageFaults = 0;

        for (int i = 0; i < pages.length; i++) {
            if (s.size() < capacity) {
                if (!s.contains(pages[i])) { s.add(pages[i]); pageFaults++; }
            } else if (!s.contains(pages[i])) {
                int farthest = i, val = -1;
                for (int temp : s) {
                    int nextUse = Integer.MAX_VALUE;
                    for (int j = i + 1; j < pages.length; j++) {
                        if (pages[j] == temp) { nextUse = j; break; }
                    }
                    if (nextUse > farthest) { farthest = nextUse; val = temp; }
                }
                if (val == -1) val = s.iterator().next();
                s.remove(val); s.add(pages[i]); pageFaults++;
            }
        }
        return "Optimal Page Faults: " + pageFaults;
    }
}
