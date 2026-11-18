package LOGICS;
import java.util.*;

public class DiskLogic {
    public static String fcfs(int head, List<Integer> reqs) {
        int seekCount = 0, current = head;
        StringBuilder sb = new StringBuilder("Order: " + head);
        for (int r : reqs) {
            seekCount += Math.abs(current - r);
            current = r;
            sb.append(" -> ").append(r);
        }
        sb.append("\nTotal Head Movement: ").append(seekCount);
        return sb.toString();
    }

    public static String sstf(int head, List<Integer> reqs) {
        int seekCount = 0, current = head;
        List<Integer> order = new ArrayList<>(reqs);
        StringBuilder sb = new StringBuilder("Order: " + head);
        while (!order.isEmpty()) {
            final int finalCurrent = current;
            order.sort(Comparator.comparingInt(r -> Math.abs(r - finalCurrent)));
            int next = order.remove(0);
            seekCount += Math.abs(current - next);
            current = next;
            sb.append(" -> ").append(current);
        }
        sb.append("\nTotal Head Movement: ").append(seekCount);
        return sb.toString();
    }

    public static String scan(int head, List<Integer> reqs, int diskSize) {
        int seekCount = 0, current = head;
        List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
        for (int r : reqs) { if (r < head) left.add(r); else right.add(r); }
        Collections.sort(left, Collections.reverseOrder());
        Collections.sort(right);

        StringBuilder sb = new StringBuilder("Order: " + head);
        for (int r : right) { seekCount += Math.abs(current - r); current = r; sb.append(" -> ").append(r); }
        seekCount += Math.abs(current - (diskSize - 1)); current = diskSize - 1; sb.append(" -> ").append(current);
        for (int r : left) { seekCount += Math.abs(current - r); current = r; sb.append(" -> ").append(r); }

        sb.append("\nTotal Head Movement: ").append(seekCount);
        return sb.toString();
    }

    public static String cscan(int head, List<Integer> reqs, int diskSize) {
        int seekCount = 0, current = head;
        List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
        for (int r : reqs) { if (r < head) left.add(r); else right.add(r); }
        Collections.sort(left, Collections.reverseOrder());
        Collections.sort(right);

        StringBuilder sb = new StringBuilder("Order: " + head);
        for (int r : right) { seekCount += Math.abs(current - r); current = r; sb.append(" -> ").append(r); }
        seekCount += Math.abs(current - (diskSize - 1)); current = diskSize - 1; sb.append(" -> ").append(current);
        seekCount += Math.abs(current - 0); current = 0; sb.append(" -> ").append(current);
        for (int r : left) { seekCount += Math.abs(current - r); current = r; sb.append(" -> ").append(r); }

        sb.append("\nTotal Head Movement: ").append(seekCount);
        return sb.toString();
    }
}
