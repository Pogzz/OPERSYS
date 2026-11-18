package LOGICS;
import java.util.*;

public class MultiprogrammingLogic {
    public static String simulate(int totalMem, int q, List<Process> procs, Map<Process, Integer> memNeeds) {
        procs.sort(Comparator.comparingInt(p -> p.arrival));
        Queue<Process> queue = new LinkedList<>();
        Map<Process, Integer> remBurst = new HashMap<>();
        for (Process p : procs) remBurst.put(p, p.burst);

        int time = 0, completed = 0, index = 0, currentMem = 0;
        StringBuilder sb = new StringBuilder();

        while (completed < procs.size()) {
            // Load memory
            while (index < procs.size() && procs.get(index).arrival <= time) {
                Process p = procs.get(index);
                int need = memNeeds.get(p);
                if (currentMem + need <= totalMem) {
                    queue.add(p);
                    currentMem += need;
                    index++;
                    sb.append("t=").append(time).append(" Loaded ").append(p.pid).append(" (Mem used: ").append(currentMem).append("/").append(totalMem).append(")\n");
                } else break;
            }

            if (queue.isEmpty()) {
                if (index < procs.size()) time = procs.get(index).arrival;
                continue;
            }

            Process p = queue.poll();
            int execTime = Math.min(q, remBurst.get(p));
            time += execTime;
            remBurst.put(p, remBurst.get(p) - execTime);

            sb.append("t=").append(time).append(" Executed ").append(p.pid).append(" for ").append(execTime).append("ms\n");

            // Load newly arrived
            while (index < procs.size() && procs.get(index).arrival <= time) {
                Process newP = procs.get(index);
                int need = memNeeds.get(newP);
                if (currentMem + need <= totalMem) {
                    queue.add(newP);
                    currentMem += need;
                    index++;
                } else break;
            }

            if (remBurst.get(p) > 0) queue.add(p);
            else {
                completed++;
                currentMem -= memNeeds.get(p);
                sb.append("t=").append(time).append(" Finished ").append(p.pid).append(" (Mem used: ").append(currentMem).append("/").append(totalMem).append(")\n");
            }
        }
        return sb.toString();
    }
}