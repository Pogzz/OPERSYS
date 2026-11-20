package LOGICS;

import java.util.*;

public class MultiprogrammingLogic {

    // Represents a single row in the event-driven Simulation table
    public static class EventRow {
        public final double time;
        public final String event;
        public final int jobs;
        public final String cpuPerJob;
        public final String elapsed;
        public final String headwayPerJob;
        public final String activeJobs;
        public final String remainingWork;

        public EventRow(double time, String event, int jobs, String cpuPerJob,
                        String elapsed, String headwayPerJob, String activeJobs, String remainingWork) {
            this.time = time;
            this.event = event;
            this.jobs = jobs;
            this.cpuPerJob = cpuPerJob;
            this.elapsed = elapsed;
            this.headwayPerJob = headwayPerJob;
            this.activeJobs = activeJobs;
            this.remainingWork = remainingWork;
        }
    }

    // Complete simulation output including event table, timeline notes, and summary metrics
    public static class SimulationResult {
        public final List<EventRow> events;
        public final List<String> timelineLogs;
        public final int totalJobs;
        public final double finishTime;
        public final double avgTurnaround;
        public final double avgWaiting;

        public SimulationResult(List<EventRow> events, List<String> timelineLogs,
                                int totalJobs, double finishTime, double avgTurnaround, double avgWaiting) {
            this.events = events;
            this.timelineLogs = timelineLogs;
            this.totalJobs = totalJobs;
            this.finishTime = finishTime;
            this.avgTurnaround = avgTurnaround;
            this.avgWaiting = avgWaiting;
        }
    }

    private static class JobState {
        final String id;
        final double arrival;
        final double burst;
        double remaining;
        double finishTime;

        JobState(String id, double arrival, double burst) {
            this.id = id;
            this.arrival = arrival;
            this.burst = burst;
            this.remaining = burst;
        }
    }

    // Runs continuous processor-sharing multiprogramming simulation
    public static SimulationResult simulateProcessorSharing(List<String> ids, List<Double> arrivals, List<Double> bursts) {
        int n = ids.size();
        List<JobState> allJobs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            allJobs.add(new JobState(ids.get(i), arrivals.get(i), bursts.get(i)));
        }

        // Preserve arrival order while tracking unarrived jobs
        List<JobState> pending = new ArrayList<>(allJobs);
        pending.sort(Comparator.comparingDouble((JobState j) -> j.arrival).thenComparing(j -> j.id));

        List<JobState> active = new ArrayList<>();
        List<EventRow> eventRows = new ArrayList<>();
        List<String> timelineLogs = new ArrayList<>();

        if (pending.isEmpty()) {
            return new SimulationResult(eventRows, timelineLogs, 0, 0.0, 0.0, 0.0);
        }

        double currentTime = pending.get(0).arrival;
        final double EPS = 1e-9;

        while (!pending.isEmpty() || !active.isEmpty()) {
            // 1. Admit all jobs arriving at currentTime
            List<JobState> arrivedNow = new ArrayList<>();
            while (!pending.isEmpty() && Math.abs(pending.get(0).arrival - currentTime) <= EPS) {
                JobState j = pending.remove(0);
                active.add(j);
                arrivedNow.add(j);
            }

            if (!arrivedNow.isEmpty()) {
                for (JobState j : arrivedNow) {
                    String evLabel = j.id + " Arrived/Started";
                    timelineLogs.add(String.format("%s arrived/started at %.3f", j.id, currentTime));

                    // First event has no prior elapsed interval
                    if (eventRows.isEmpty()) {
                        eventRows.add(new EventRow(
                            currentTime,
                            evLabel,
                            0,
                            "",
                            "",
                            "",
                            formatActiveIds(active),
                            formatRemaining(active)
                        ));
                    } else {
                        int prevCount = active.size();
                        eventRows.add(new EventRow(
                            currentTime,
                            evLabel,
                            prevCount,
                            String.format("%.2f%%", 100.0 / prevCount),
                            String.format("%.3f", 0.0),
                            String.format("%.3f", 0.0),
                            formatActiveIds(active),
                            formatRemaining(active)
                        ));
                    }
                }
            }

            if (active.isEmpty()) {
                currentTime = pending.get(0).arrival;
                continue;
            }

            // 2. Determine next event time (either next arrival or earliest job completion)
            double nextArrival = pending.isEmpty() ? Double.POSITIVE_INFINITY : pending.get(0).arrival;

            double minRemaining = Double.POSITIVE_INFINITY;
            for (JobState j : active) {
                if (j.remaining < minRemaining) {
                    minRemaining = j.remaining;
                }
            }
            double nextCompletion = currentTime + (minRemaining * active.size());
            double nextTime = Math.min(nextArrival, nextCompletion);

            double elapsed = nextTime - currentTime;
            int runningCount = active.size();
            double cpuShare = 100.0 / runningCount;
            double headway = elapsed / runningCount;

            // Advance work for all active jobs equally
            for (JobState j : active) {
                j.remaining = Math.max(0.0, j.remaining - headway);
            }

            currentTime = nextTime;

            // Check if any job(s) finished at this instant
            List<JobState> finishedNow = new ArrayList<>();
            Iterator<JobState> it = active.iterator();
            while (it.hasNext()) {
                JobState j = it.next();
                if (j.remaining <= EPS) {
                    j.remaining = 0.0;
                    j.finishTime = currentTime;
                    finishedNow.add(j);
                    it.remove();
                }
            }

            // Check if any job(s) also arrived at this exact instant
            List<JobState> simultaneousArrivals = new ArrayList<>();
            while (!pending.isEmpty() && Math.abs(pending.get(0).arrival - currentTime) <= EPS) {
                JobState j = pending.remove(0);
                active.add(j);
                simultaneousArrivals.add(j);
            }

            for (JobState j : finishedNow) {
                String evLabel = j.id + " Terminated";
                timelineLogs.add(String.format("%s terminated at %.3f", j.id, currentTime));
                eventRows.add(new EventRow(
                    currentTime,
                    evLabel,
                    runningCount,
                    String.format("%.2f%%", cpuShare),
                    String.format("%.3f", elapsed),
                    String.format("%.3f", headway),
                    formatActiveIds(active),
                    formatRemaining(active)
                ));
                // Avoid double-counting elapsed if multiple events share this timestamp
                elapsed = 0.0;
                headway = 0.0;
            }

            for (JobState j : simultaneousArrivals) {
                String evLabel = j.id + " Arrived/Started";
                timelineLogs.add(String.format("%s arrived/started at %.3f", j.id, currentTime));
                eventRows.add(new EventRow(
                    currentTime,
                    evLabel,
                    runningCount,
                    String.format("%.2f%%", cpuShare),
                    String.format("%.3f", elapsed),
                    String.format("%.3f", headway),
                    formatActiveIds(active),
                    formatRemaining(active)
                ));
                elapsed = 0.0;
                headway = 0.0;
            }
        }

        double sumTat = 0.0, sumWt = 0.0, maxFinish = 0.0;
        for (JobState j : allJobs) {
            double tat = j.finishTime - j.arrival;
            double wt = Math.max(0.0, tat - j.burst);
            sumTat += tat;
            sumWt += wt;
            if (j.finishTime > maxFinish) maxFinish = j.finishTime;
        }

        double avgTat = n > 0 ? sumTat / n : 0.0;
        double avgWt  = n > 0 ? sumWt / n : 0.0;

        return new SimulationResult(eventRows, timelineLogs, n, maxFinish, avgTat, avgWt);
    }

    private static String formatActiveIds(List<JobState> active) {
        if (active.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < active.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(active.get(i).id);
        }
        return sb.toString();
    }

    private static String formatRemaining(List<JobState> active) {
        if (active.isEmpty()) return "0.000";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < active.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.3f", active.get(i).remaining));
        }
        return sb.toString();
    }

    // Legacy method kept intact for compatibility
    public static String simulate(int totalMem, int q, List<Process> procs, Map<Process, Integer> memNeeds) {
        List<String> ids = new ArrayList<>();
        List<Double> arr = new ArrayList<>();
        List<Double> bst = new ArrayList<>();
        for (Process p : procs) {
            ids.add(p.pid);
            arr.add((double) p.arrival);
            bst.add((double) p.burst);
        }
        SimulationResult res = simulateProcessorSharing(ids, arr, bst);
        return String.join("\n", res.timelineLogs);
    }
}