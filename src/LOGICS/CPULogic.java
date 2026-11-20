package LOGICS;

import java.util.*;

public class CPULogic {

    public static class SimResult {
        public final List<int[]> segments; // {procIndex, start, end}
        public final double awt;
        public final double atat;
        public final int maxTime;
        public final String logText;

        public SimResult(List<int[]> segments, double awt, double atat, int maxTime, String logText) {
            this.segments = segments;
            this.awt = awt;
            this.atat = atat;
            this.maxTime = maxTime;
            this.logText = logText;
        }
    }

    public static SimResult run(List<Process> procs, String algo, int quantum) {
        for (Process p : procs) {
            p.rem = p.burst;
            p.ct = 0;
        }

        List<int[]> segs = new ArrayList<>();
        if ("Round Robin".equals(algo)) {
            simulateRR(procs, quantum, segs);
        } else {
            simulate(procs, algo, segs);
        }

        double totalWt = 0, totalTat = 0;
        int maxTime = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("Algorithm : ").append(algo);
        if ("Round Robin".equals(algo)) sb.append(" (q=").append(quantum).append(")");
        sb.append("\n\nGantt Chart:\n  ");

        for (int k = 0; k < segs.size(); k++) {
            int[] s = segs.get(k);
            sb.append(procs.get(s[0]).pid).append(" [").append(s[1]).append("-").append(s[2]).append("]");
            if (k < segs.size() - 1) sb.append(" => ");
            maxTime = Math.max(maxTime, s[2]);
        }

        sb.append("\n\n").append(String.format("%-5s %4s %4s %4s %5s %4s%n", "Job", "AT", "BT", "CT", "TAT", "WT"));
        for (Process p : procs) {
            int tat = p.ct - p.arrival;
            int wt = tat - p.burst;
            totalTat += tat;
            totalWt += wt;
            sb.append(String.format("%-5s %4d %4d %4d %5d %4d%n", p.pid, p.arrival, p.burst, p.ct, tat, wt));
        }

        double awt = totalWt / procs.size();
        double atat = totalTat / procs.size();
        return new SimResult(segs, awt, atat, maxTime, sb.toString());
    }

    private static boolean better(Process a, Process b, String algo) {
        int ka, kb;
        if (algo.startsWith("SJF")) { ka = a.burst; kb = b.burst; }
        else if (algo.startsWith("SRTF")) { ka = a.rem; kb = b.rem; }
        else if (algo.startsWith("Priority")) { ka = a.priority; kb = b.priority; }
        else { ka = a.arrival; kb = b.arrival; }
        if (ka != kb) return ka < kb;
        return a.arrival < b.arrival;
    }

    private static void simulate(List<Process> ps, String algo, List<int[]> segs) {
        boolean preemptive = algo.startsWith("SRTF") || algo.equals("Priority (Preemptive)");
        int n = ps.size(), done = 0, t = 0, cur = -1;

        while (done < n) {
            if (cur == -1 || preemptive) {
                int best = -1;
                for (int i = 0; i < n; i++) {
                    Process p = ps.get(i);
                    if (p.rem > 0 && p.arrival <= t && (best == -1 || better(p, ps.get(best), algo))) {
                        best = i;
                    }
                }
                cur = best;
            }
            if (cur == -1) { t++; continue; }

            Process p = ps.get(cur);
            addSeg(segs, cur, t, t + 1);
            p.rem--;
            t++;
            if (p.rem == 0) {
                p.ct = t;
                done++;
                cur = -1;
            }
        }
    }

    private static void simulateRR(List<Process> ps, int quantum, List<int[]> segs) {
        int n = ps.size(), done = 0, t = 0, next = 0;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingInt(x -> ps.get(x).arrival));

        Deque<Integer> q = new ArrayDeque<>();
        while (done < n) {
            while (next < n && ps.get(order[next]).arrival <= t) q.add(order[next++]);
            if (q.isEmpty()) { t = ps.get(order[next]).arrival; continue; }

            int i = q.poll();
            Process p = ps.get(i);
            int run = Math.min(quantum, p.rem);
            addSeg(segs, i, t, t + run);
            t += run;
            p.rem -= run;

            while (next < n && ps.get(order[next]).arrival <= t) q.add(order[next++]);
            if (p.rem > 0) q.add(i);
            else { p.ct = t; done++; }
        }
    }

    private static void addSeg(List<int[]> segs, int idx, int start, int end) {
        if (!segs.isEmpty()) {
            int[] last = segs.get(segs.size() - 1);
            if (last[0] == idx && last[2] == start) {
                last[2] = end;
                return;
            }
        }
        segs.add(new int[]{idx, start, end});
    }
}