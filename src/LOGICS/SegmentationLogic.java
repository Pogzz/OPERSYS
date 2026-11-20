package LOGICS;
import java.util.*;

public class SegmentationLogic {

    public static final int MEMORY_SIZE = 12204; 

    public static class Segment {
        public final int id, limit, base;
        public Segment(int id, int limit, int base) { this.id = id; this.limit = limit; this.base = base; }
        public int end() { return base + limit; }
    }

    public static class Result {
        public final int seg, offset;
        public final Integer limit, base, physical;   // null kapag fault
        public final String status;                   // "VALID", "OUT OF BOUNDS", "SEGMENTATION FAULT"
        Result(int seg, int offset, Integer limit, Integer base, Integer physical, String status) {
            this.seg = seg; this.offset = offset; this.limit = limit;
            this.base = base; this.physical = physical; this.status = status;
        }
        public boolean isValid() { return status.equals("VALID"); }
    }

    // One translation = one row in translation page
    public static Result translate(Map<Integer, Segment> table, int seg, int offset) {
        Segment s = table.get(seg);
        if (s == null)
            return new Result(seg, offset, null, null, null, "SEGMENTATION FAULT");
        if (offset < 0 || offset >= s.limit)
            return new Result(seg, offset, s.limit, s.base, null, "OUT OF BOUNDS");
        return new Result(seg, offset, s.limit, s.base, s.base + offset, "VALID");
    }

    // Input format: "0,100" o multiple: "0,100; 1,50; 2,999"
    public static List<int[]> parseQueries(String input) {
        List<int[]> list = new ArrayList<>();
        for (String part : input.split(";")) {
            String[] p = part.replaceAll("[()\\s]", "").split(",");
            if (p.length != 2) throw new IllegalArgumentException("Use format: segment, offset (e.g. 1, 250)");
            list.add(new int[]{ Integer.parseInt(p[0]), Integer.parseInt(p[1]) });
        }
        return list;
    }

    // For the add Button
    public static String validate(Map<Integer, Segment> table, Segment n) {
        if (table.containsKey(n.id)) return "Segment " + n.id + " already exists.";
        if (n.limit <= 0 || n.base < 0) return "Limit must be > 0 and base >= 0.";
        if (n.end() > MEMORY_SIZE) return "Segment exceeds physical memory (" + MEMORY_SIZE + ").";
        for (Segment s : table.values())
            if (n.base < s.end() && s.base < n.end())
                return "Overlaps with Segment " + s.id + " (" + s.base + "-" + s.end() + ").";
        return null; // OK
    }

    // For Randomize Button
    public static Map<Integer, Segment> randomize(int count) {
        Map<Integer, Segment> t = new LinkedHashMap<>();
        Random r = new Random();
        int cursor = r.nextInt(200);
        for (int i = 0; i < count; i++) {
            int base = cursor + r.nextInt(150);
            int limit = 200 + r.nextInt(2300);
            if (base + limit > MEMORY_SIZE) break;
            t.put(i, new Segment(i, limit, base));
            cursor = base + limit;
        }
        return t;
    }
}