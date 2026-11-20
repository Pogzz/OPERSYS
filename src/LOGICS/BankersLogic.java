package LOGICS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BankersLogic {

    public static class BankersResult {
        public final boolean isSafe;
        public final List<Integer> safeSequence;
        public final int[][] needMatrix;
        public final String logText;

        public BankersResult(boolean isSafe, List<Integer> safeSequence, int[][] needMatrix, String logText) {
            this.isSafe = isSafe;
            this.safeSequence = safeSequence;
            this.needMatrix = needMatrix;
            this.logText = logText;
        }
    }

    public static BankersResult analyze(int[][] allocation, int[][] max, int[] available) {
        int n = allocation.length;
        int m = available.length;

        int[][] need = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                need[i][j] = max[i][j] - allocation[i][j];
            }
        }

        int[] work = Arrays.copyOf(available, m);
        boolean[] finish = new boolean[n];
        List<Integer> safeSeq = new ArrayList<>();
        StringBuilder log = new StringBuilder();

        log.append("Initial Available (Work): ").append(Arrays.toString(work)).append("\n\n");

        int count = 0;
        while (count < n) {
            boolean found = false;
            for (int p = 0; p < n; p++) {
                if (!finish[p]) {
                    boolean canAllocate = true;
                    for (int r = 0; r < m; r++) {
                        if (need[p][r] > work[r]) {
                            canAllocate = false;
                            break;
                        }
                    }

                    if (canAllocate) {
                        int[] prevWork = Arrays.copyOf(work, m);
                        for (int r = 0; r < m; r++) {
                            work[r] += allocation[p][r];
                        }
                        finish[p] = true;
                        safeSeq.add(p);
                        found = true;
                        count++;

                        log.append(String.format("Step %d: P%d executes (Need %s <= Work %s) -> New Work: %s%n",
                                count, p + 1, Arrays.toString(need[p]), Arrays.toString(prevWork), Arrays.toString(work)));
                    }
                }
            }
            if (!found) {
                log.append("\nNo remaining process can satisfy its Need with current Work ")
                   .append(Arrays.toString(work)).append(".");
                return new BankersResult(false, safeSeq, need, log.toString());
            }
        }

        return new BankersResult(true, safeSeq, need, log.toString());
    }
}