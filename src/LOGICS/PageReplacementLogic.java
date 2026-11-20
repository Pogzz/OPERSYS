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

    public static String runNRU(int[] pages, int capacity) {
        Result r = simulate("NRU", pages, capacity);
        return "NRU Page Faults: " + r.faults;
    }

    public static class Result {
        public final int[][] frames;
        public final boolean[] fault;
        public final int faults;

        Result(int[][] frames, boolean[] fault, int faults) {
            this.frames = frames;
            this.fault = fault;
            this.faults = faults;
        }
    }

    public static Result simulate(String algo, int[] pages, int capacity) {
        int n = pages.length;
        int[][] frames = new int[n][capacity];
        boolean[] fault = new boolean[n];
        int[] mem = new int[capacity];
        int[] loadedAt = new int[capacity];
        int[] lastUsed = new int[capacity];
        int[] refBit = new int[capacity]; // R bit for NRU (0 or 1)
        int[] modBit = new int[capacity]; // M bit for NRU (0 or 1)
        Arrays.fill(mem, -1);
        int faults = 0;

        for (int i = 0; i < n; i++) {
            // Periodic clock interrupt resets R-bits every `capacity` steps
            if ("NRU".equals(algo) && i > 0 && i % capacity == 0) {
                Arrays.fill(refBit, 0);
            }

            int p = pages[i];
            int idx = -1;
            for (int f = 0; f < capacity; f++) {
                if (mem[f] == p) { idx = f; break; }
            }

            if (idx >= 0) {
                lastUsed[idx] = i; // HIT
                refBit[idx] = 1;
                // Mark as modified on repeated access
                if (i % 2 == 1) modBit[idx] = 1;
            } else { // FAULT
                faults++;
                fault[i] = true;
                int victim = -1;
                for (int f = 0; f < capacity; f++) {
                    if (mem[f] == -1) { victim = f; break; }
                }

                if (victim == -1) {
                    if ("FIFO".equals(algo)) {
                        victim = 0;
                        for (int f = 1; f < capacity; f++)
                            if (loadedAt[f] < loadedAt[victim]) victim = f;
                    } else if ("LRU".equals(algo)) {
                        victim = 0;
                        for (int f = 1; f < capacity; f++)
                            if (lastUsed[f] < lastUsed[victim]) victim = f;
                    } else if ("NRU".equals(algo)) {
                        // Class = (R * 2) + M -> pick lowest class (0..3), tie-break by oldest load
                        victim = 0;
                        int bestClass = (refBit[0] * 2) + modBit[0];
                        for (int f = 1; f < capacity; f++) {
                            int cls = (refBit[f] * 2) + modBit[f];
                            if (cls < bestClass || (cls == bestClass && loadedAt[f] < loadedAt[victim])) {
                                bestClass = cls;
                                victim = f;
                            }
                        }
                    } else { // Optimal
                        int farthest = -1;
                        for (int f = 0; f < capacity; f++) {
                            int next = n;
                            for (int j = i + 1; j < n; j++)
                                if (pages[j] == mem[f]) { next = j; break; }
                            if (next > farthest) { farthest = next; victim = f; }
                        }
                    }
                }

                mem[victim] = p;
                loadedAt[victim] = i;
                lastUsed[victim] = i;
                refBit[victim] = 1;
                modBit[victim] = (p % 2 == 1) ? 1 : 0;
            }
            frames[i] = mem.clone();
        }
        return new Result(frames, fault, faults);
    }
}