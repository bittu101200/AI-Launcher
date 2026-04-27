package bhupendra.ai.launcher.managers.suggestions;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import it.andreuzzi.comparestring2.StringableObject;

public final class SuggestionScorer {

    public static final int MIN_SCORE = 240;
    private static final int SHORT_QUERY_MIN_SCORE = 520;

    private SuggestionScorer() {
    }

    public static int minimumScore(String query) {
        String q = normalize(query);
        return q.length() <= 2 ? SHORT_QUERY_MIN_SCORE : MIN_SCORE;
    }

    public static int score(String query, String candidate) {
        String q = normalize(query);
        String c = normalize(candidate);

        if (q.length() == 0 || c.length() == 0) return 0;

        String compactQ = compact(q);
        String compactC = compact(c);
        String[] tokens = tokens(c);

        int best = 0;

        if (c.equals(q)) best = Math.max(best, 1200);
        if (compactC.equals(compactQ)) best = Math.max(best, 1160);

        if (c.startsWith(q)) best = Math.max(best, 1040 - lengthPenalty(c, q));
        if (compactC.startsWith(compactQ)) best = Math.max(best, 1000 - lengthPenalty(compactC, compactQ));

        for (String token : tokens) {
            if (token.equals(q)) best = Math.max(best, 980 - lengthPenalty(token, q));
            if (token.startsWith(q)) best = Math.max(best, 900 - lengthPenalty(token, q));
        }

        String acronym = acronym(tokens);
        if (acronym.equals(compactQ)) best = Math.max(best, 900);
        if (acronym.startsWith(compactQ)) best = Math.max(best, 820 - lengthPenalty(acronym, compactQ));

        int contains = c.indexOf(q);
        if (contains >= 0) best = Math.max(best, 720 - contains * 12 - lengthPenalty(c, q));

        int compactContains = compactC.indexOf(compactQ);
        if (compactContains >= 0) best = Math.max(best, 700 - compactContains * 10 - lengthPenalty(compactC, compactQ));

        int subsequenceScore = subsequenceScore(compactQ, compactC);
        best = Math.max(best, subsequenceScore);

        int editScore = editScore(compactQ, compactC);
        best = Math.max(best, editScore);

        return Math.max(0, best);
    }

    public static List<String> rankStrings(String query, String[] values, int limit) {
        List<ScoredString> scored = new ArrayList<>();
        if (values == null) return Collections.emptyList();

        int minScore = minimumScore(query);
        for (String value : values) {
            int score = score(query, value);
            if (score >= minScore) scored.add(new ScoredString(value, score));
        }

        Collections.sort(scored, new Comparator<ScoredString>() {
            @Override
            public int compare(ScoredString a, ScoredString b) {
                int byScore = b.score - a.score;
                if (byScore != 0) return byScore;
                return a.value.compareToIgnoreCase(b.value);
            }
        });

        List<String> result = new ArrayList<>();
        for (int i = 0; i < scored.size() && i < limit; i++) result.add(scored.get(i).value);
        return result;
    }

    public static <T extends StringableObject> List<ScoredObject<T>> rankObjects(String query, List<T> values, int limit) {
        List<ScoredObject<T>> scored = new ArrayList<>();
        if (values == null) return scored;

        int minScore = minimumScore(query);
        for (T value : values) {
            if (value == null) continue;
            int score = score(query, value.getString());
            if (score >= minScore) scored.add(new ScoredObject<T>(value, score));
        }

        Collections.sort(scored, new Comparator<ScoredObject<T>>() {
            @Override
            public int compare(ScoredObject<T> a, ScoredObject<T> b) {
                int byScore = b.score - a.score;
                if (byScore != 0) return byScore;
                return a.value.getString().compareToIgnoreCase(b.value.getString());
            }
        });

        if (scored.size() <= limit) return scored;
        return new ArrayList<>(scored.subList(0, limit));
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return normalized;
    }

    private static String compact(String value) {
        return value.replace(" ", "");
    }

    private static String[] tokens(String normalized) {
        return normalized.length() == 0 ? new String[0] : normalized.split(" ");
    }

    private static String acronym(String[] tokens) {
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.length() > 0) builder.append(token.charAt(0));
        }
        return builder.toString();
    }

    private static int lengthPenalty(String candidate, String query) {
        return Math.min(160, Math.max(0, candidate.length() - query.length()) * 6);
    }

    private static int subsequenceScore(String query, String candidate) {
        if (query.length() == 0 || candidate.length() == 0) return 0;

        int qIndex = 0;
        int first = -1;
        int last = -1;
        for (int cIndex = 0; cIndex < candidate.length() && qIndex < query.length(); cIndex++) {
            if (query.charAt(qIndex) == candidate.charAt(cIndex)) {
                if (first == -1) first = cIndex;
                last = cIndex;
                qIndex++;
            }
        }
        if (qIndex != query.length()) return 0;

        int spread = Math.max(0, last - first + 1 - query.length());
        return Math.max(0, 560 - first * 18 - spread * 20 - lengthPenalty(candidate, query));
    }

    private static int editScore(String query, String candidate) {
        if (query.length() < 3 || candidate.length() == 0) return 0;

        int prefixLength = Math.min(candidate.length(), Math.max(query.length(), Math.min(query.length() + 2, candidate.length())));
        String comparable = candidate.substring(0, prefixLength);
        int distance = levenshtein(query, comparable);
        int maxLength = Math.max(query.length(), comparable.length());
        double similarity = 1.0 - ((double) distance / (double) maxLength);
        if (similarity < 0.58) return 0;

        return (int) Math.round(620 * similarity) - lengthPenalty(candidate, query);
    }

    private static int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) previous[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
            }
            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[b.length()];
    }

    private static final class ScoredString {
        final String value;
        final int score;

        ScoredString(String value, int score) {
            this.value = value;
            this.score = score;
        }
    }

    public static final class ScoredObject<T> {
        public final T value;
        public final int score;

        ScoredObject(T value, int score) {
            this.value = value;
            this.score = score;
        }
    }
}
