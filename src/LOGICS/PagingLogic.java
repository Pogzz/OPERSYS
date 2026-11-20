package LOGICS;

import java.util.*;

public class PagingLogic {

    public static class TranslationRow {
        public final int logical;
        public final int page;
        public final int offset;
        public final Integer frame;    // null if page fault
        public final Integer physical; // null if page fault
        public final boolean isFault;
        public final String statusNote;

        public TranslationRow(int logical, int page, int offset, Integer frame, Integer physical, boolean isFault, String statusNote) {
            this.logical = logical;
            this.page = page;
            this.offset = offset;
            this.frame = frame;
            this.physical = physical;
            this.isFault = isFault;
            this.statusNote = statusNote;
        }
    }

    public static class PagingResult {
        public final List<TranslationRow> rows;
        public final Set<Integer> translatedLogicalBytes;
        public final Set<Integer> faultedLogicalBytes;
        public final Set<Integer> translatedPhysicalBytes;
        public final Map<Integer, Integer> byteArrows; // logicalByte -> physicalByte

        public PagingResult(List<TranslationRow> rows,
                            Set<Integer> translatedLogicalBytes,
                            Set<Integer> faultedLogicalBytes,
                            Set<Integer> translatedPhysicalBytes,
                            Map<Integer, Integer> byteArrows) {
            this.rows = rows;
            this.translatedLogicalBytes = translatedLogicalBytes;
            this.faultedLogicalBytes = faultedLogicalBytes;
            this.translatedPhysicalBytes = translatedPhysicalBytes;
            this.byteArrows = byteArrows;
        }
    }

    public static PagingResult translateDetailed(int pageSize, int numFrames,
                                                 Map<Integer, Integer> pageTable, List<Integer> addresses) {
        List<TranslationRow> rows = new ArrayList<>();
        Set<Integer> transLog = new LinkedHashSet<>();
        Set<Integer> faultLog = new LinkedHashSet<>();
        Set<Integer> transPhys = new LinkedHashSet<>();
        Map<Integer, Integer> arrows = new LinkedHashMap<>();

        for (int addr : addresses) {
            int pageNum = addr / pageSize;
            int offset = addr % pageSize;

            if (!pageTable.containsKey(pageNum)) {
                faultLog.add(addr);
                rows.add(new TranslationRow(addr, pageNum, offset, null, null, true, "PAGE FAULT"));
            } else {
                int frame = pageTable.get(pageNum);
                if (frame < 0 || frame >= numFrames) {
                    faultLog.add(addr);
                    rows.add(new TranslationRow(addr, pageNum, offset, frame, null, true, "OUT OF RANGE"));
                } else {
                    int physical = (frame * pageSize) + offset;
                    transLog.add(addr);
                    transPhys.add(physical);
                    arrows.put(addr, physical);
                    rows.add(new TranslationRow(addr, pageNum, offset, frame, physical, false, String.valueOf(physical)));
                }
            }
        }

        return new PagingResult(rows, transLog, faultLog, transPhys, arrows);
    }

    // Original method kept intact
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