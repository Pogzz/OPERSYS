package LOGICS;

public class Process {
    public String pid;
    public int arrival, burst, priority;

    public Process(String pid, int arrival, int burst, int priority) {
        this.pid = pid;
        this.arrival = arrival;
        this.burst = burst;
        this.priority = priority;
    }
}
