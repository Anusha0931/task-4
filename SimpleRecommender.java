import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleRecommender {

    public static void main(String[] args) throws Exception {
        String dataFile = "data/user_prefs.csv";
        int targetUser = (args.length > 0) ? Integer.parseInt(args[0]) : 1;
        int topN = (args.length > 1) ? Integer.parseInt(args[1]) : 3;

        // Load data
        Map<Integer, Map<Integer, Double>> userItemRatings = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dataFile))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first && line.toLowerCase().startsWith("userid")) { first = false; continue; }
                String[] p = line.split(",");
                if (p.length < 3) continue;
                int user = Integer.parseInt(p[0].trim());
                int item = Integer.parseInt(p[1].trim());
                double rating = Double.parseDouble(p[2].trim());
                userItemRatings.computeIfAbsent(user, k -> new HashMap<>()).put(item, rating);
            }
        }

        if (!userItemRatings.containsKey(targetUser)) {
            System.out.println("Target user " + targetUser + " not found in data.");
            return;
        }

        Map<Integer, Double> targetRatings = userItemRatings.get(targetUser);

        // Precompute norms for cosine similarity
        Map<Integer, Double> norms = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Double>> e : userItemRatings.entrySet()) {
            double sumSq = 0.0;
            for (double r : e.getValue().values()) sumSq += r * r;
            norms.put(e.getKey(), Math.sqrt(sumSq));
        }

        // Compute similarity between target user and others (cosine)
        Map<Integer, Double> similarity = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Double>> e : userItemRatings.entrySet()) {
            int other = e.getKey();
            if (other == targetUser) continue;
            double dot = 0.0;
            for (Map.Entry<Integer, Double> it : targetRatings.entrySet()) {
                Double rOther = e.getValue().get(it.getKey());
                if (rOther != null) dot += it.getValue() * rOther;
            }
            double denom = norms.get(targetUser) * norms.getOrDefault(other, 0.0);
            double sim = (denom == 0.0) ? 0.0 : dot / denom;
            if (sim > 0) similarity.put(other, sim); // keep only positive similarity
        }

        if (similarity.isEmpty()) {
            System.out.println("No similar users found for user " + targetUser);
            return;
        }

        // For all items not rated by target, compute predicted score:
        Map<Integer, Double> scoreNumerator = new HashMap<>();
        Map<Integer, Double> scoreDenominator = new HashMap<>();

        for (Map.Entry<Integer, Double> simEntry : similarity.entrySet()) {
            int other = simEntry.getKey();
            double sim = simEntry.getValue();
            Map<Integer, Double> otherRatings = userItemRatings.get(other);
            for (Map.Entry<Integer, Double> itemRating : otherRatings.entrySet()) {
                int item = itemRating.getKey();
                if (targetRatings.containsKey(item)) continue; // skip already rated by target
                double r = itemRating.getValue();
                scoreNumerator.merge(item, sim * r, Double::sum);
                scoreDenominator.merge(item, Math.abs(sim), Double::sum);
            }
        }

        Map<Integer, Double> predicted = new HashMap<>();
        for (int item : scoreNumerator.keySet()) {
            double denom = scoreDenominator.getOrDefault(item, 0.0);
            if (denom == 0.0) continue;
            predicted.put(item, scoreNumerator.get(item) / denom);
        }

        if (predicted.isEmpty()) {
            System.out.println("No candidate items to recommend for user " + targetUser);
            return;
        }

        // Sort and take top N
        List<Map.Entry<Integer, Double>> recs = predicted.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .collect(Collectors.toList());

        System.out.println("Recommendations for User " + targetUser + ":");
        for (Map.Entry<Integer, Double> r : recs) {
            System.out.printf("Item: %d | Predicted score: %.3f%n", r.getKey(), r.getValue());
        }
    }
}
