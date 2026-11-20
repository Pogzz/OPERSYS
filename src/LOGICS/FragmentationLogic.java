package LOGICS;

import java.util.*;

public class FragmentationLogic {

    public static final int MAX_ITEMS = 20;
    public static final int MAX_VALUE = 100_000;
    private static final Random RND = new Random();

    public enum Strategy {
        FIRST_FIT("First Fit"), BEST_FIT("Best Fit"), WORST_FIT("Worst Fit");
        public final String label;
        Strategy(String label) { this.label = label; }
    }

    public static class BlockState {
        public final int no, size;
        public final boolean fixed;                              
        public final List<int[]> procs = new ArrayList<>();    

        BlockState(int no, int size, boolean fixed) { this.no = no; this.size = size; this.fixed = fixed; }

        public int used() { int u = 0; for (int[] p : procs) u += p[1]; return u; }

        public int internalFrag() { return fixed && !procs.isEmpty() ? size - used() : 0; }

        public int freeSpace() { return fixed ? (procs.isEmpty() ? size : 0) : size - used(); }
    }

    public static class Placement {
        public final int process, size, block, wasted;  
        public final String status;                   
        Placement(int process, int size, int block, int wasted, String status) {
            this.process = process; this.size = size; this.block = block;
            this.wasted = wasted; this.status = status;
        }
        public boolean isAllocated() { return block > 0; }
    }

    public static class Report {
        public final Strategy strategy;
        public final boolean dynamic;
        public final List<BlockState> blocks;
        public final List<Placement> placements;
        public final int internal, external, placed, total;
        Report(Strategy s, boolean dynamic, List<BlockState> blocks, List<Placement> pl, int placed) {
            this.strategy = s; this.dynamic = dynamic; this.blocks = blocks; this.placements = pl;
            this.placed = placed; this.total = pl.size();
            int in = 0, ex = 0;
            for (BlockState b : blocks) { in += b.internalFrag(); ex += b.freeSpace(); }
            this.internal = in; this.external = ex;
        }
    }

    /**
     * @param dynamic false = Fixed partitions (isang process kada block, may internal fragmentation)
     *                true  = Dynamic (hinahati ang block, may external fragmentation)
     */
    public static Report run(Strategy st, boolean dynamic, List<Integer> blockSizes, List<Integer> processes) {
        List<BlockState> blocks = new ArrayList<>();
        for (int i = 0; i < blockSizes.size(); i++)
            blocks.add(new BlockState(i + 1, blockSizes.get(i), !dynamic));

        List<Placement> result = new ArrayList<>();
        int placed = 0;

        for (int i = 0; i < processes.size(); i++) {
            int pid = i + 1, p = processes.get(i);
            int idx = pick(st, blocks, p);
            if (idx >= 0) {
                BlockState b = blocks.get(idx);
                int wasted = dynamic ? 0 : b.size - p;
                b.procs.add(new int[]{pid, p});
                result.add(new Placement(pid, p, b.no, wasted, "Allocated"));
                placed++;
            } else {
                int free = 0;
                for (BlockState b : blocks) free += b.freeSpace();
                result.add(new Placement(pid, p, -1, 0, free >= p ? "Ext. Fragmentation" : "No Space"));
            }
        }
        return new Report(st, dynamic, blocks, result, placed);
    }

    private static int pick(Strategy st, List<BlockState> blocks, int p) {
        int best = -1;
        for (int i = 0; i < blocks.size(); i++) {
            int cap = blocks.get(i).freeSpace();
            if (cap < p) continue;
            switch (st) {
                case FIRST_FIT: return i;
                case BEST_FIT:  if (best == -1 || cap < blocks.get(best).freeSpace()) best = i; break;
                case WORST_FIT: if (best == -1 || cap > blocks.get(best).freeSpace()) best = i; break;
            }
        }
        return best;
    }
    public static List<Integer> parseList(String text, String label) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(label + " is empty. Example: 100, 500, 200");
        List<Integer> out = new ArrayList<>();
        for (String s : t.split("[,\\s]+")) {
            if (s.isEmpty()) continue;
            try {
                int v = Integer.parseInt(s);
                if (v <= 0 || v > MAX_VALUE) throw new NumberFormatException();
                out.add(v);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(label + ": \"" + s + "\" must be a number from 1 to " + MAX_VALUE + ".");
            }
        }
        if (out.size() > MAX_ITEMS)
            throw new IllegalArgumentException(label + ": maximum of " + MAX_ITEMS + " items.");
        return out;
    }

    public static List<Integer> randomBlocks(int n) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add((2 + RND.nextInt(11)) * 50);
        return l;
    }

    public static List<Integer> randomProcesses(int n) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add((3 + RND.nextInt(43)) * 10);  
        return l;
    }
}