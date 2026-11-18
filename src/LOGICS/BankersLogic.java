package LOGICS;
import java.util.*;

public class BankersLogic {
    public static String checkSafety(int[][] alloc, int[][] max, int[] avail) {
        int n = alloc.length, m = avail.length;
        int[][] need = new int[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                need[i][j] = max[i][j] - alloc[i][j];

        int[] work = avail.clone();
        boolean[] finish = new boolean[n];
        List<Integer> safeSeq = new ArrayList<>();
        int count = 0;

        while (count < n) {
            boolean found = false;
            for (int i = 0; i < n; i++) {
                if (!finish[i]) {
                    boolean canAllocate = true;
                    for (int j = 0; j < m; j++) {
                        if (need[i][j] > work[j]) { canAllocate = false; break; }
                    }
                    if (canAllocate) {
                        for (int j = 0; j < m; j++) work[j] += alloc[i][j];
                        safeSeq.add(i);
                        finish[i] = true;
                        found = true;
                        count++;
                    }
                }
            }
            if (!found) break;
        }

        if (count == n) return "System is SAFE. Sequence: " + safeSeq;
        else return "System is UNSAFE (Deadlock possible).";
    }
}
