package wizicl.mre.client.gui.controller;

import org.apache.commons.lang3.StringUtils;

public class SearchHelper {
    public static boolean isFuzzyMatch(String itemName, String search) {
        if (search.isEmpty()) return true;

        itemName = itemName.toLowerCase();
        search = search.toLowerCase();

        if (itemName.contains(search)) return true;

        String[] words = itemName.split(" ");

        for (String word : words) {
            if (word.startsWith(search)) return true;

            if (Math.abs(word.length() - search.length()) <= 2) {
                int distance = StringUtils.getLevenshteinDistance(word, search);
                int maxTypos = word.length() <= 4 ? 1 : 2;
                if (distance <= maxTypos) {
                    return true;
                }
            }
        }
        return false;
    }

}
