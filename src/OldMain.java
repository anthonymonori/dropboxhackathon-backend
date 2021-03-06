import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;

//
// we are team 25
//

public class OldMain {

    public static void main(String[] args) {

        IDropboxStats fakeStats = new IDropboxStats() {
            @Override
            public long totalSizeInBytes() {
                return 10 * 1024 /* kb */ * 1024 /* mb */ * 1024L;
            }

            @Override
            public long spaceAvailableInBytes() {
                return 2 * 1024 /* kb */ * 1024 /* mb */ * 1024L;
            }

            @Override
            public long spaceUsedInBytes() {
                return 8 * 1024 /* kb */ * 1024 /* mb */ * 1024L;
            }

            @Override
            public long spaceUsedSharedInBytes() {
                return 1 * 1024 /* kb */ * 1024 /* mb */ * 1024L;
            }
        };


        List<IDropboxFile> all = new ArrayList<IDropboxFile>();
        for (int i = 0; i < 700; i++) {
            all.add(new TempFile());
        }

        // create some dupes
        for (int i = 0; i < 1; i++) {
            all.add(((TempFile) all.get(22)).fakeDupe());
            all.add(((TempFile) all.get(23)).fakeDupe());
            all.add(((TempFile) all.get(23)).fakeDupe());
            all.add(((TempFile) all.get(23)).fakeDupe());
        }

        String json = makeJson(all, fakeStats);
        System.out.println(json);
    }

    public static String makeJson(List<IDropboxFile> all, IDropboxStats stats) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object> outer = new LinkedHashMap<String, Object>();

        outer.put("generated-at-timestamp", new Date().getTime());
        outer.put("stats-bytes-used-shared", stats.spaceUsedSharedInBytes());
        outer.put("stats-bytes-available", stats.spaceAvailableInBytes());
        outer.put("stats-bytes-used", stats.spaceUsedInBytes());
        outer.put("stats-bytes-total", stats.totalSizeInBytes());

        outer.put("total-file-count", (long) all.size());
        outer.put("file-extensions", extensions(all));
        outer.put("media-types-count", mediaTypes(extensions(all)));
        outer.put("media-types-percentages", percentagify(mediaTypes(extensionsByFileSize(all))));
        outer.put("media-types-totalfilesize", mediaTypes(extensionsByFileSize(all)));
        outer.put("biggest-files", biggestFiles(all));
        outer.put("duplicate-files", DupeFinder.find(all));

        outer.put("old-big-files", OldBigFiles.find(all));

        // last 24 hours
        // last week
        return gson.toJson(outer);
    }

    private static Object percentagify(Map<String, Long> counts) {
        long total = 0;

        for (Long l : counts.values()) {
            total += l;
        }

        double onePercent = total / 100d;

        Map<String, Long> percentages = new LinkedHashMap<String, Long>();

        for (Map.Entry<String, Long> e : counts.entrySet()) {
            percentages.put(e.getKey(),
                    onePercent != 0 ?
                        roundOff((double) (e.getValue()) / onePercent) :
                        0);
        }
        return percentages;
    }

    private static long roundOff(double v) {
        return Math.round(v);
    }

    private static Map<String, Long> extensions(List<IDropboxFile> all) {
        Map<String, Long> extensions = new HashMap<String, Long>();
        for (IDropboxFile f : all) {
            String fn = f.filename();
            String[] parts = fn.split("\\.");
            String ext = "(none)";
            if (parts.length > 1) {
                ext = parts[parts.length - 1];

                if (ext.length() > 4) {
                    ext = "(none)";
                }
            }

            if (!extensions.containsKey(ext)) {
                extensions.put(ext, 0L);
            }
            long count = extensions.get(ext);
            count++;
            extensions.put(ext, count);
        }

        return extensions;
    }

    private static Map<String, Long> extensionsByFileSize(List<IDropboxFile> all) {
        Map<String, Long> extensions = new HashMap<String, Long>();
        for (IDropboxFile f : all) {
            String fn = f.filename();
            String[] parts = fn.split("\\.");
            String ext = "(none)";
            if (parts.length > 1) {
                ext = parts[parts.length - 1];

                if (ext.length() > 4) {
                    ext = "(none)";
                }
            }

            if (!extensions.containsKey(ext)) {
                extensions.put(ext, 0L);
            }
            long count = extensions.get(ext);
            count += f.size();
            extensions.put(ext, count);
        }

        return extensions;
    }



    private static Map<String, Long> mediaTypes(Map<String, Long> extensions) {
        CounterThing c = new CounterThing();
        c.ensurePresent(picture, movie, text, audio, "(unknown)");
        for (Map.Entry<String, Long> e : extensions.entrySet()) {
            c.inc(extToMediaType(e.getKey()), e.getValue());
        }
        return c.get();
    }

    static String picture = "picture";
    static String movie = "movie";
    static String text = "text";
    static String audio = "audio";

    private static String extToMediaType(String ext) {

        if ("png".equals(ext)) return picture;
        if ("txt".equals(ext)) return text;
        if ("mkv".equals(ext)) return movie;
        if ("jpg".equals(ext)) return picture;
        if ("mp4".equals(ext)) return movie;
        if ("mp3".equals(ext)) return audio;
        if ("avi".equals(ext)) return movie;
        if ("gif".equals(ext)) return picture;
        if ("bmp".equals(ext)) return picture;

        return "(unknown)";
    }

    static class BigFile {
        String name;
        String path;
        long size;
    }

    private static Object biggestFiles(List<IDropboxFile> all) {
        Collections.sort(all, new Comparator<IDropboxFile>() {
            @Override
            public int compare(IDropboxFile o1, IDropboxFile o2) {
                return -new Long(o1.size()).compareTo(o2.size());
            }
        });
        List<IDropboxFile> biggest = all.subList(0, Math.min(5, all.size()));
        List<BigFile> out = new ArrayList<BigFile>();
        for (IDropboxFile f : biggest) {
            BigFile bf = new BigFile();
            bf.path = f.path();
            bf.name = f.filename();
            bf.size = f.size();
            out.add(bf);
        }
        return out;
    }
}
