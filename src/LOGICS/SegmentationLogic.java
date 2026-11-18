package LOGICS;
import java.util.*;

public class SegmentationLogic {
    public static String translate(Map<Integer, int[]> segmentTable, List<int[]> queries) {
        StringBuilder sb = new StringBuilder();
        for (int[] query : queries) {
            int seg = query[0], offset = query[1];
            if (!segmentTable.containsKey(seg)) {
                sb.append("Seg ").append(seg).append(", Offset ").append(offset).append(" -> SEGMENTATION FAULT\n");
            } else {
                int[] data = segmentTable.get(seg);
                if (offset >= data[1]) {
                    sb.append("Seg ").append(seg).append(", Offset ").append(offset).append(" -> OUT OF BOUNDS\n");
                } else {
                    sb.append("Seg ").append(seg).append(", Offset ").append(offset).append(" -> Physical Address: ").append(data[0] + offset).append("\n");
                }
            }
        }
        return sb.toString();
    }
}