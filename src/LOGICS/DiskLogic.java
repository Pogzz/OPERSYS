package LOGICS;

import java.util.*;

public class DiskLogic {

    // Stores the complete seek order and total head movement for an algorithm
    public static class DiskResult {
        public final String algorithm;
        public final List<Integer> sequence; // starts with initial head
        public final List<Integer> steps;    // distance traversed per step
        public final int totalMovement;
        public final double avgSeek;

        public DiskResult(String algorithm, List<Integer> sequence, List<Integer> steps, int totalMovement, int requestCount) {
            this.algorithm = algorithm;
            this.sequence = sequence;
            this.steps = steps;
            this.totalMovement = totalMovement;
            this.avgSeek = requestCount > 0 ? (double) totalMovement / requestCount : 0.0;
        }
    }

    // Runs a single scheduling algorithm and returns full step-by-step metrics
    public static DiskResult simulate(String algo, int head, List<Integer> reqs, int maxTrack) {
        List<Integer> seq = new ArrayList<>();
        List<Integer> steps = new ArrayList<>();
        seq.add(head);
        steps.add(0);

        int current = head;
        int total = 0;

        switch (algo) {
            case "FIFO (FCFS)":
                for (int r : reqs) {
                    int dist = Math.abs(current - r);
                    total += dist;
                    current = r;
                    seq.add(current);
                    steps.add(dist);
                }
                break;

            case "SSTF":
                List<Integer> pending = new ArrayList<>(reqs);
                while (!pending.isEmpty()) {
                    final int cur = current;
                    pending.sort(Comparator.comparingInt(r -> Math.abs(r - cur)));
                    int next = pending.remove(0);
                    int dist = Math.abs(current - next);
                    total += dist;
                    current = next;
                    seq.add(current);
                    steps.add(dist);
                }
                break;

            case "Elevator (SCAN)": {
                List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
                for (int r : reqs) {
                    if (r < head) left.add(r);
                    else right.add(r);
                }
                Collections.sort(left, Collections.reverseOrder());
                Collections.sort(right);

                for (int r : right) {
                    int dist = Math.abs(current - r);
                    total += dist;
                    current = r;
                    seq.add(current);
                    steps.add(dist);
                }
                // Move to the upper cylinder boundary before reversing
                if (!left.isEmpty() && current != maxTrack) {
                    int dist = Math.abs(current - maxTrack);
                    total += dist;
                    current = maxTrack;
                    seq.add(current);
                    steps.add(dist);
                }
                for (int r : left) {
                    int dist = Math.abs(current - r);
                    total += dist;
                    current = r;
                    seq.add(current);
                    steps.add(dist);
                }
                break;
            }

            case "C-SCAN": {
                List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
                for (int r : reqs) {
                    if (r < head) left.add(r);
                    else right.add(r);
                }
                Collections.sort(right);
                Collections.sort(left); // ascending from track 0

                for (int r : right) {
                    int dist = Math.abs(current - r);
                    total += dist;
                    current = r;
                    seq.add(current);
                    steps.add(dist);
                }
                if (!left.isEmpty()) {
                    if (current != maxTrack) {
                        int dist = Math.abs(current - maxTrack);
                        total += dist;
                        current = maxTrack;
                        seq.add(current);
                        steps.add(dist);
                    }
                    // Jump back to track 0
                    int jump = Math.abs(current - 0);
                    total += jump;
                    current = 0;
                    seq.add(current);
                    steps.add(jump);

                    for (int r : left) {
                        int dist = Math.abs(current - r);
                        total += dist;
                        current = r;
                        seq.add(current);
                        steps.add(dist);
                    }
                }
                break;
            }

            case "LOOK": {
                List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
                for (int r : reqs) {
                    if (r < head) left.add(r);
                    else right.add(r);
                }
                Collections.sort(left, Collections.reverseOrder());
                Collections.sort(right);

                for (int r : right) {
                    int dist = Math.abs(current - r);
                    total += dist;
                    current = r;
                    seq.add(current);
                    steps.add(dist);
                }
                for (int r : left) {
                    int dist = Math.abs(current - r);
                    total += dist;
                    current = r;
                    seq.add(current);
                    steps.add(dist);
                }
                break;
            }
        }

        return new DiskResult(algo, seq, steps, total, reqs.size());
    }

    // Legacy string methods kept intact
    public static String fcfs(int head, List<Integer> reqs) {
        DiskResult r = simulate("FIFO (FCFS)", head, reqs, 200);
        return formatLegacy(r);
    }

    public static String sstf(int head, List<Integer> reqs) {
        DiskResult r = simulate("SSTF", head, reqs, 200);
        return formatLegacy(r);
    }

    public static String scan(int head, List<Integer> reqs, int diskSize) {
        DiskResult r = simulate("Elevator (SCAN)", head, reqs, diskSize - 1);
        return formatLegacy(r);
    }

    public static String cscan(int head, List<Integer> reqs, int diskSize) {
        DiskResult r = simulate("C-SCAN", head, reqs, diskSize - 1);
        return formatLegacy(r);
    }

    private static String formatLegacy(DiskResult r) {
        StringBuilder sb = new StringBuilder("Order: ");
        for (int i = 0; i < r.sequence.size(); i++) {
            if (i > 0) sb.append(" -> ");
            sb.append(r.sequence.get(i));
        }
        sb.append("\nTotal Head Movement: ").append(r.totalMovement);
        return sb.toString();
    }
}