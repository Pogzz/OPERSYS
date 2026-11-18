package LOGICS;
import java.util.*;

public class CPULogic {
    public static String fcfs(List<Process> procs) {
        procs.sort(Comparator.comparingInt(p -> p.arrival));
        int time = 0, totalWait = 0, totalTurn = 0;
        StringBuilder sb = new StringBuilder("Gantt Chart:\n");
        for (Process p : procs) {
            if (time < p.arrival) time = p.arrival;
            sb.append(p.pid).append(" [").append(time).append("-").append(time + p.burst).append("] ");
            int tat = (time + p.burst) - p.arrival;
            totalWait += tat - p.burst;
            totalTurn += tat;
            time += p.burst;
        }
        sb.append(String.format("\nAvg Wait: %.2f | Avg Turnaround: %.2f", (double)totalWait/procs.size(), (double)totalTurn/procs.size()));
        return sb.toString();
    }

    public static String sjf(List<Process> procs) {
        List<Process> pending = new ArrayList<>(procs);
        int time = 0, completed = 0, totalWait = 0, totalTurn = 0;
        StringBuilder sb = new StringBuilder("Gantt Chart:\n");
        while (!pending.isEmpty()) {
            List<Process> avail = new ArrayList<>();
            for (Process p : pending) if (p.arrival <= time) avail.add(p);
            if (avail.isEmpty()) {
                time = pending.stream().mapToInt(p -> p.arrival).min().getAsInt();
                continue;
            }
            avail.sort(Comparator.comparingInt(p -> p.burst));
            Process p = avail.get(0);
            pending.remove(p);
            sb.append(p.pid).append(" [").append(time).append("-").append(time + p.burst).append("] ");
            int tat = (time + p.burst) - p.arrival;
            totalWait += tat - p.burst;
            totalTurn += tat;
            time += p.burst;
            completed++;
        }
        sb.append(String.format("\nAvg Wait: %.2f | Avg Turnaround: %.2f", (double)totalWait/completed, (double)totalTurn/completed));
        return sb.toString();
    }

    public static String priority(List<Process> procs) {
        List<Process> pending = new ArrayList<>(procs);
        int time = 0, completed = 0, totalWait = 0, totalTurn = 0;
        StringBuilder sb = new StringBuilder("Gantt Chart:\n");
        while (!pending.isEmpty()) {
            List<Process> avail = new ArrayList<>();
            for (Process p : pending) if (p.arrival <= time) avail.add(p);
            if (avail.isEmpty()) {
                time = pending.stream().mapToInt(p -> p.arrival).min().getAsInt();
                continue;
            }
            avail.sort(Comparator.comparingInt(p -> p.priority));
            Process p = avail.get(0);
            pending.remove(p);
            sb.append(p.pid).append(" [").append(time).append("-").append(time + p.burst).append("] ");
            int tat = (time + p.burst) - p.arrival;
            totalWait += tat - p.burst;
            totalTurn += tat;
            time += p.burst;
            completed++;
        }
        sb.append(String.format("\nAvg Wait: %.2f | Avg Turnaround: %.2f", (double)totalWait/completed, (double)totalTurn/completed));
        return sb.toString();
    }

    public static String roundRobin(List<Process> procs, int q) {
        procs.sort(Comparator.comparingInt(p -> p.arrival));
        Queue<Process> queue = new LinkedList<>();
        Map<Process, Integer> remBurst = new HashMap<>();
        for (Process p : procs) remBurst.put(p, p.burst);

        int time = 0, completed = 0, index = 0, totalWait = 0, totalTurn = 0;
        StringBuilder sb = new StringBuilder("Gantt Chart:\n");
        if (!procs.isEmpty()) time = procs.get(0).arrival;
        while (index < procs.size() && procs.get(index).arrival <= time) queue.add(procs.get(index++));

        while (!queue.isEmpty()) {
            Process p = queue.poll();
            int execTime = Math.min(q, remBurst.get(p));
            sb.append(p.pid).append(" [").append(time).append("-").append(time + execTime).append("] ");
            time += execTime;
            remBurst.put(p, remBurst.get(p) - execTime);

            while (index < procs.size() && procs.get(index).arrival <= time) queue.add(procs.get(index++));
            if (remBurst.get(p) > 0) queue.add(p);
            else {
                int tat = time - p.arrival;
                totalWait += tat - p.burst;
                totalTurn += tat;
                completed++;
            }
            if (queue.isEmpty() && index < procs.size()) {
                time = Math.max(time, procs.get(index).arrival);
                while (index < procs.size() && procs.get(index).arrival <= time) queue.add(procs.get(index++));
            }
        }
        sb.append(String.format("\nAvg Wait: %.2f | Avg Turnaround: %.2f", (double)totalWait/completed, (double)totalTurn/completed));
        return sb.toString();
    }
}