package LOGICS;
import java.util.*;

public class PagingLogic {
    public static String translate(int pageSize, int numFrames, Map<Integer, Integer> pageTable, List<Integer> addresses) {
        StringBuilder sb = new StringBuilder();
        for (int addr : addresses) {
            int pageNum = addr / pageSize;
            int offset = addr % pageSize;

            if (!pageTable.containsKey(pageNum)) {
                sb.append("Logical ").append(addr).append(" -> PAGE FAULT (Page ").append(pageNum).append(" not mapped)\n");
            } else {
                int frame = pageTable.get(pageNum);
                int physical = (frame * pageSize) + offset;
                sb.append("Logical ").append(addr).append(" -> Physical Address: ").append(physical).append("\n");
            }
        }
        return sb.toString();
    }
}