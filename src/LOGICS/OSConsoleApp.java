package LOGICS;

import java.util.*;

public class OSConsoleApp {
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n================================================");
            System.out.println("          OPERATING SYSTEM SIMULATOR (CLI)      ");
            System.out.println("================================================");
            System.out.println("1. CPU Scheduling");
            System.out.println("2. Banker's Algorithm");
            System.out.println("3. Paging");
            System.out.println("4. Page Replacement");
            System.out.println("5. Segmentation");
            System.out.println("6. Fragmentation");
            System.out.println("7. Disk Scheduling");
            System.out.println("8. Multiprogramming");
            System.out.println("0. Exit");
            System.out.print("Select a module: ");

            int choice = getInt();
            switch (choice) {
                case 1: runCPUScheduling(); break;
                case 2: runBankersAlgorithm(); break;
                case 3: runPaging(); break;
                case 4: runPageReplacement(); break;
                case 5: runSegmentation(); break;
                case 6: runFragmentation(); break;
                case 7: runDiskScheduling(); break;
                case 8: runMultiprogramming(); break;
                case 0: System.out.println("Exiting..."); return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    static void runCPUScheduling() {
        System.out.println("\n--- CPU Scheduling ---");
        List<Process> processes = new ArrayList<>();
        System.out.println("Enter processes (Type 'done' for PID to finish):");
        while (true) {
            System.out.print("PID: ");
            String pid = scanner.next();
            if (pid.equalsIgnoreCase("done")) break;
            System.out.print("Arrival Time: ");
            int arr = getInt();
            System.out.print("Burst Time: ");
            int burst = getInt();
            System.out.print("Priority (lower = higher): ");
            int pri = getInt();
            processes.add(new Process(pid, arr, burst, pri));
        }
        if (processes.isEmpty()) return;

        System.out.println("\nSelect Algorithm:\n1. FCFS\n2. SJF\n3. Priority\n4. Round Robin");
        int algo = getInt();

        if (algo == 1) System.out.println(CPULogic.fcfs(processes));
        else if (algo == 2) System.out.println(CPULogic.sjf(processes));
        else if (algo == 3) System.out.println(CPULogic.priority(processes));
        else if (algo == 4) {
            System.out.print("Enter Time Quantum: ");
            int q = getInt();
            System.out.println(CPULogic.roundRobin(processes, q));
        }
    }

    static void runBankersAlgorithm() {
        System.out.println("\n--- Banker's Algorithm ---");
        System.out.print("Number of processes: ");
        int n = getInt();
        System.out.print("Number of resource types: ");
        int m = getInt();

        int[][] alloc = new int[n][m];
        int[][] max = new int[n][m];
        int[] avail = new int[m];

        for (int i = 0; i < n; i++) {
            System.out.println("Process P" + i + " Allocation:");
            for (int j = 0; j < m; j++) alloc[i][j] = getInt();
            System.out.println("Process P" + i + " Max:");
            for (int j = 0; j < m; j++) max[i][j] = getInt();
        }
        System.out.println("Available resources:");
        for (int j = 0; j < m; j++) avail[j] = getInt();

        System.out.println(BankersLogic.checkSafety(alloc, max, avail));
    }

    static void runPaging() {
        System.out.println("\n--- Paging ---");
        System.out.print("Page size: ");
        int pageSize = getInt();
        System.out.print("Number of frames: ");
        int numFrames = getInt();

        Map<Integer, Integer> pageTable = new HashMap<>();
        System.out.println("Enter Page -> Frame mappings (Type -1 to finish):");
        while (true) {
            System.out.print("Page: ");
            int page = getInt();
            if (page == -1) break;
            System.out.print("Frame: ");
            int frame = getInt();
            pageTable.put(page, frame);
        }

        System.out.println("Enter logical addresses (Type -1 to finish):");
        List<Integer> addrs = new ArrayList<>();
        while (true) {
            System.out.print("Logical Address: ");
            int addr = getInt();
            if (addr == -1) break;
            addrs.add(addr);
        }

        System.out.println(PagingLogic.translate(pageSize, numFrames, pageTable, addrs));
    }

    static void runPageReplacement() {
        System.out.println("\n--- Page Replacement ---");
        System.out.print("Reference string (space separated): ");
        scanner.nextLine();
        String[] parts = scanner.nextLine().trim().split("\\s+");
        int[] pages = new int[parts.length];
        for (int i = 0; i < parts.length; i++) pages[i] = Integer.parseInt(parts[i]);

        System.out.print("Number of frames: ");
        int capacity = getInt();

        System.out.println(PageReplacementLogic.runFIFO(pages, capacity));
        System.out.println(PageReplacementLogic.runLRU(pages, capacity));
        System.out.println(PageReplacementLogic.runOptimal(pages, capacity));
    }

    static void runSegmentation() {
        System.out.println("\n--- Segmentation ---");
        Map<Integer, int[]> segmentTable = new HashMap<>();
        System.out.println("Enter Segment mappings (Type -1 to finish):");
        while (true) {
            System.out.print("Segment: ");
            int seg = getInt();
            if (seg == -1) break;
            System.out.print("Base: ");
            int base = getInt();
            System.out.print("Limit: ");
            int limit = getInt();
            segmentTable.put(seg, new int[]{base, limit});
        }

        System.out.println("Enter addresses (Segment Offset) (Type -1 to finish):");
        List<int[]> queries = new ArrayList<>();
        while (true) {
            System.out.print("Segment: ");
            int seg = getInt();
            if (seg == -1) break;
            System.out.print("Offset: ");
            int offset = getInt();
            queries.add(new int[]{seg, offset});
        }

        System.out.println(SegmentationLogic.translate(segmentTable, queries));
    }

    static void runFragmentation() {
        System.out.println("\n--- Fragmentation ---");
        System.out.println("1. Fixed\n2. Dynamic");
        System.out.print("Choice: ");
        int choice = getInt();

        if (choice == 1) {
            System.out.print("Block size: ");
            int blockSize = getInt();
            System.out.print("Number of blocks: ");
            int numBlocks = getInt();
            System.out.print("Process sizes (space separated): ");
            scanner.nextLine();
            String[] parts = scanner.nextLine().trim().split("\\s+");
            List<Integer> procs = new ArrayList<>();
            for (String part : parts) procs.add(Integer.parseInt(part));

            System.out.println(FragmentationLogic.runFixed(blockSize, numBlocks, procs));
        } else {
            System.out.print("Memory blocks (space separated): ");
            scanner.nextLine();
            String[] bParts = scanner.nextLine().trim().split("\\s+");
            List<Integer> blocks = new ArrayList<>();
            for (String b : bParts) blocks.add(Integer.parseInt(b));

            System.out.print("Process sizes (space separated): ");
            String[] pParts = scanner.nextLine().trim().split("\\s+");
            List<Integer> procs = new ArrayList<>();
            for (String p : pParts) procs.add(Integer.parseInt(p));

            System.out.println(FragmentationLogic.runDynamic(blocks, procs));
        }
    }

    static void runDiskScheduling() {
        System.out.println("\n--- Disk Scheduling ---");
        System.out.print("Initial head: ");
        int head = getInt();
        System.out.print("Disk size: ");
        int diskSize = getInt();
        System.out.print("Requests (space separated): ");
        scanner.nextLine();
        String[] parts = scanner.nextLine().trim().split("\\s+");
        List<Integer> reqs = new ArrayList<>();
        for (String p : parts) reqs.add(Integer.parseInt(p));

        System.out.println("Select Algorithm:\n1. FCFS\n2. SSTF\n3. SCAN\n4. C-SCAN");
        int algo = getInt();

        if (algo == 1) System.out.println(DiskLogic.fcfs(head, reqs));
        else if (algo == 2) System.out.println(DiskLogic.sstf(head, reqs));
        else if (algo == 3) System.out.println(DiskLogic.scan(head, reqs, diskSize));
        else if (algo == 4) System.out.println(DiskLogic.cscan(head, reqs, diskSize));
    }

    static void runMultiprogramming() {
        System.out.println("\n--- Multiprogramming ---");
        System.out.print("Total Memory: ");
        int totalMem = getInt();
        System.out.print("Time Quantum: ");
        int q = getInt();

        List<Process> procs = new ArrayList<>();
        Map<Process, Integer> memNeeds = new HashMap<>();
        System.out.println("Enter processes (Type 'done' for PID to finish):");
        while (true) {
            System.out.print("PID: ");
            String pid = scanner.next();
            if (pid.equalsIgnoreCase("done")) break;
            System.out.print("Arrival: ");
            int arr = getInt();
            System.out.print("Burst: ");
            int burst = getInt();
            System.out.print("Memory: ");
            int mem = getInt();

            Process p = new Process(pid, arr, burst, 0);
            procs.add(p);
            memNeeds.put(p, mem);
        }

        System.out.println(MultiprogrammingLogic.simulate(totalMem, q, procs, memNeeds));
    }

    static int getInt() {
        while (true) {
            try { return Integer.parseInt(scanner.next()); }
            catch (NumberFormatException e) { System.out.print("Invalid input! Enter a number: "); }
        }
    }
}