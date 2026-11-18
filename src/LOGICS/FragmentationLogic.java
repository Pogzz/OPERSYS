package LOGICS;
import java.util.*;

public class FragmentationLogic {
    public static String runFixed(int blockSize, int numBlocks, List<Integer> processes) {
        int internalFrag = 0, allocated = 0;
        StringBuilder sb = new StringBuilder();
        for (int p : processes) {
            if (p > blockSize) sb.append("Process ").append(p).append(" too large.\n");
            else { internalFrag += (blockSize - p); allocated++; }
        }
        sb.append("Allocated processes: ").append(allocated).append("\n");
        sb.append("Total Internal Fragmentation: ").append(internalFrag).append("\n");
        sb.append("Free Blocks: ").append(numBlocks - allocated);
        return sb.toString();
    }

    public static String runDynamic(List<Integer> blocks, List<Integer> processes) {
        List<Integer> memory = new ArrayList<>(blocks);
        StringBuilder sb = new StringBuilder();
        for (int p : processes) {
            boolean placed = false;
            for (int i = 0; i < memory.size(); i++) {
                if (memory.get(i) >= p) {
                    memory.set(i, memory.get(i) - p);
                    placed = true;
                    break;
                }
            }
            if (!placed) sb.append("Process ").append(p).append(" could not be allocated (External Fragmentation).\n");
        }
        int extFrag = 0;
        for (int b : memory) extFrag += b;
        sb.append("Total External Fragmentation (Free Space): ").append(extFrag);
        return sb.toString();
    }
}
