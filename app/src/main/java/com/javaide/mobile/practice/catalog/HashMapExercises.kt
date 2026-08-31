package com.javaide.mobile.practice.catalog

import com.javaide.mobile.practice.migration.LegacyExerciseMigration
import com.javaide.mobile.practice.model.Difficulty
import com.javaide.mobile.practice.model.ExerciseExample
import com.javaide.mobile.practice.model.ExerciseTestCase
import com.javaide.mobile.practice.model.InterviewExercise

/**
 * Hash Maps and Sets topic (Milestone 6, Section A) — the first newly-authored (not migrated)
 * category. Unlike the legacy topic files, every exercise here has a real problem statement,
 * genuine starter/solution separation, and multiple structured test cases (including at least
 * one hidden one) written from scratch for the V2 model.
 *
 * Each `main()` reads its input from stdin (via `Scanner`) and prints one deterministic line of
 * output, matching how [com.javaide.mobile.practice.execution.TestCaseRunner] actually drives a
 * compiled program — this is the first content written *for* that execution model rather than
 * migrated from the legacy single-hardcoded-run style. Where an output could otherwise have more
 * than one valid ordering (e.g. [TOP_K_FREQUENT_ELEMENTS]'s ties), the problem statement and
 * reference solution both commit to one explicit, deterministic tie-break rule, so exact-string
 * comparison stays meaningful — see Milestone 20's "no challenge depends on unstable output
 * ordering" acceptance criterion.
 */
object HashMapExercises {

    private const val CATEGORY_TITLE = "Hash Maps and Sets"
    private val CATEGORY_ID = LegacyExerciseMigration.categoryIdFor(CATEGORY_TITLE)

    val FIRST_NON_REPEATING_CHARACTER = InterviewExercise(
        id = "hashmap-first-non-repeating-character",
        title = "First Non-Repeating Character",
        className = "FirstNonRepeatingChar",
        categoryId = CATEGORY_ID,
        difficulty = Difficulty.EASY,
        description = "Given a string s, return the first character that appears exactly once " +
            "in s, scanning left to right. If every character repeats, return the underscore " +
            "character '_' instead.",
        constraints = listOf(
            "1 <= s.length <= 10^4",
            "s consists only of lowercase English letters"
        ),
        examples = listOf(
            ExerciseExample(
                input = "s = \"swiss\"",
                output = "'w'",
                explanation = "'s' and 'i' each repeat; 'w' is the first character that appears exactly once."
            ),
            ExerciseExample(
                input = "s = \"aabbcc\"",
                output = "'_'",
                explanation = "Every character repeats, so there is no non-repeating character."
            )
        ),
        starterCode = """
            import java.util.Scanner;

            public class FirstNonRepeatingChar {
                // TODO: return the first character in s that appears exactly once, or '_' if
                // every character repeats.
                static char firstNonRepeatingChar(String s) {
                    return '_';
                }

                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    String s = scanner.nextLine();
                    System.out.println(firstNonRepeatingChar(s));
                }
            }
        """.trimIndent(),
        solutionCode = """
            import java.util.HashMap;
            import java.util.Map;
            import java.util.Scanner;

            public class FirstNonRepeatingChar {
                static char firstNonRepeatingChar(String s) {
                    Map<Character, Integer> counts = new HashMap<>();
                    for (char c : s.toCharArray()) {
                        counts.put(c, counts.getOrDefault(c, 0) + 1);
                    }
                    for (char c : s.toCharArray()) {
                        if (counts.get(c) == 1) return c;
                    }
                    return '_';
                }

                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    String s = scanner.nextLine();
                    System.out.println(firstNonRepeatingChar(s));
                }
            }
        """.trimIndent(),
        hints = listOf(
            "Count how many times each character appears using a HashMap (or an array of size 26 for lowercase letters).",
            "Scan the string a second time, in order, and return the first character whose count is 1."
        ),
        testCases = listOf(
            ExerciseTestCase("basic", "swiss", "w", visible = true, description = "a mix of repeating and one non-repeating character"),
            ExerciseTestCase("all-repeat", "aabbcc", "_", visible = true, description = "every character repeats"),
            ExerciseTestCase("leetcode", "leetcode", "l", visible = true, description = "the non-repeating character is at the very start"),
            ExerciseTestCase("single-char", "z", "z", visible = false, description = "a single character is trivially non-repeating")
        ),
        timeComplexity = "O(n)",
        spaceComplexity = "O(n)",
        patterns = setOf("hash-map"),
        tags = setOf("strings", "hash-map"),
        estimatedMinutes = 10
    )

    val INTERSECTION_OF_TWO_ARRAYS = InterviewExercise(
        id = "hashmap-intersection-of-two-arrays",
        title = "Intersection of Two Arrays",
        className = "IntersectionOfTwoArrays",
        categoryId = CATEGORY_ID,
        difficulty = Difficulty.EASY,
        description = "Given two integer arrays nums1 and nums2, return their intersection: " +
            "each element must appear only once in the result, even if it repeats in either " +
            "input. Print the result sorted in ascending order.",
        constraints = listOf(
            "1 <= nums1.length, nums2.length <= 1000",
            "0 <= nums1[i], nums2[i] <= 1000"
        ),
        examples = listOf(
            ExerciseExample(
                input = "nums1 = [1,2,2,1], nums2 = [2,2]",
                output = "[2]",
                explanation = "2 is the only value present in both arrays."
            ),
            ExerciseExample(
                input = "nums1 = [4,9,5], nums2 = [9,4,9,8,4]",
                output = "[4, 9]",
                explanation = "4 and 9 are present in both arrays; sorted ascending for a deterministic result."
            )
        ),
        starterCode = """
            import java.util.ArrayList;
            import java.util.Collections;
            import java.util.List;
            import java.util.Scanner;

            public class IntersectionOfTwoArrays {
                // TODO: return the unique values present in both nums1 and nums2.
                static List<Integer> intersection(int[] nums1, int[] nums2) {
                    return new ArrayList<>();
                }

                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    int[] nums1 = parseLine(scanner.nextLine());
                    int[] nums2 = parseLine(scanner.nextLine());
                    List<Integer> result = intersection(nums1, nums2);
                    Collections.sort(result);
                    System.out.println(result);
                }

                private static int[] parseLine(String line) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) return new int[0];
                    String[] parts = trimmed.split("\\s+");
                    int[] arr = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i]);
                    return arr;
                }
            }
        """.trimIndent(),
        solutionCode = """
            import java.util.ArrayList;
            import java.util.Collections;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Scanner;
            import java.util.Set;

            public class IntersectionOfTwoArrays {
                static List<Integer> intersection(int[] nums1, int[] nums2) {
                    Set<Integer> set1 = new HashSet<>();
                    for (int n : nums1) set1.add(n);
                    Set<Integer> result = new HashSet<>();
                    for (int n : nums2) {
                        if (set1.contains(n)) result.add(n);
                    }
                    return new ArrayList<>(result);
                }

                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    int[] nums1 = parseLine(scanner.nextLine());
                    int[] nums2 = parseLine(scanner.nextLine());
                    List<Integer> result = intersection(nums1, nums2);
                    Collections.sort(result);
                    System.out.println(result);
                }

                private static int[] parseLine(String line) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) return new int[0];
                    String[] parts = trimmed.split("\\s+");
                    int[] arr = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i]);
                    return arr;
                }
            }
        """.trimIndent(),
        hints = listOf(
            "Put every value from nums1 into a HashSet for O(1) lookups.",
            "Walk nums2 once, adding a value to the result set only if it's present in the first set."
        ),
        testCases = listOf(
            ExerciseTestCase("basic", "1 2 2 1\n2 2", "[2]", visible = true, description = "duplicates in the input collapse to one result"),
            ExerciseTestCase("multi", "4 9 5\n9 4 9 8 4", "[4, 9]", visible = true, description = "two common values, printed sorted"),
            ExerciseTestCase("no-overlap", "1 2 3\n4 5 6", "[]", visible = true, description = "no values in common"),
            ExerciseTestCase("identical", "5 5 5\n5 5", "[5]", visible = false, description = "both arrays contain only the same repeated value")
        ),
        timeComplexity = "O(n + m)",
        spaceComplexity = "O(n + m)",
        patterns = setOf("hash-map", "hash-set"),
        tags = setOf("arrays", "hash-set"),
        estimatedMinutes = 10
    )

    val ISOMORPHIC_STRINGS = InterviewExercise(
        id = "hashmap-isomorphic-strings",
        title = "Isomorphic Strings",
        className = "IsomorphicStrings",
        categoryId = CATEGORY_ID,
        difficulty = Difficulty.EASY,
        description = "Given two strings s and t of the same length, return true if the " +
            "characters in s can be consistently replaced to obtain t: every occurrence of a " +
            "character in s must map to the same character in t, no two different characters " +
            "in s may map to the same character in t, and a character may map to itself.",
        constraints = listOf(
            "1 <= s.length <= 5 * 10^4",
            "t.length == s.length",
            "s and t consist of any valid ASCII characters"
        ),
        examples = listOf(
            ExerciseExample(input = "s = \"egg\", t = \"add\"", output = "true", explanation = "e -> a, g -> d is a consistent, one-to-one mapping."),
            ExerciseExample(input = "s = \"foo\", t = \"bar\"", output = "false", explanation = "'o' would have to map to both 'a' and 'r'.")
        ),
        starterCode = """
            import java.util.Scanner;

            public class IsomorphicStrings {
                // TODO: return true if s and t are isomorphic (see the problem description for
                // the exact mapping rules).
                static boolean isIsomorphic(String s, String t) {
                    return false;
                }

                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    String s = scanner.nextLine();
                    String t = scanner.nextLine();
                    System.out.println(isIsomorphic(s, t));
                }
            }
        """.trimIndent(),
        solutionCode = """
            import java.util.HashMap;
            import java.util.Map;
            import java.util.Scanner;

            public class IsomorphicStrings {
                static boolean isIsomorphic(String s, String t) {
                    if (s.length() != t.length()) return false;
                    Map<Character, Character> mapST = new HashMap<>();
                    Map<Character, Character> mapTS = new HashMap<>();
                    for (int i = 0; i < s.length(); i++) {
                        char cs = s.charAt(i), ct = t.charAt(i);
                        if (mapST.containsKey(cs) && mapST.get(cs) != ct) return false;
                        if (mapTS.containsKey(ct) && mapTS.get(ct) != cs) return false;
                        mapST.put(cs, ct);
                        mapTS.put(ct, cs);
                    }
                    return true;
                }

                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    String s = scanner.nextLine();
                    String t = scanner.nextLine();
                    System.out.println(isIsomorphic(s, t));
                }
            }
        """.trimIndent(),
        hints = listOf(
            "Track a two-way mapping: which character in t does each character in s map to, and vice versa.",
            "A pair of characters (s[i], t[i]) must always agree with any mapping recorded earlier for either character."
        ),
        testCases = listOf(
            ExerciseTestCase("egg-add", "egg\nadd", "true", visible = true, description = "a valid consistent mapping"),
            ExerciseTestCase("foo-bar", "foo\nbar", "false", visible = true, description = "one source character would need two different targets"),
            ExerciseTestCase("paper-title", "paper\ntitle", "true", visible = true, description = "a longer valid mapping, including a repeated character"),
            ExerciseTestCase("two-sources-one-target", "ab\naa", "false", visible = false, description = "two different source characters cannot map to the same target character")
        ),
        timeComplexity = "O(n)",
        spaceComplexity = "O(n)",
        patterns = setOf("hash-map"),
        tags = setOf("strings", "hash-map"),
        estimatedMinutes = 15
    )

    val HAPPY_NUMBER = InterviewExercise(
        id = "hashmap-happy-number",
        title = "Happy Number",
        className = "HappyNumber",
        categoryId = CATEGORY_ID,
        difficulty = Difficulty.EASY,
        description = "A happy number is found by repeatedly replacing it with the sum of the " +
            "squares of its digits until the value equals 1 (happy), or it enters a cycle that " +
            "never reaches 1 (not happy). Given a positive integer n, return true if it is happy.",
        constraints = listOf("1 <= n <= 2^31 - 1"),
        examples = listOf(
            ExerciseExample(
                input = "n = 19",
                output = "true",
                explanation = "19 -> 82 -> 68 -> 100 -> 1, reaching 1."
            ),
            ExerciseExample(
                input = "n = 2",
                output = "false",
                explanation = "2 enters the cycle 4 -> 16 -> 37 -> 58 -> 89 -> 145 -> 42 -> 20 -> 4 -> ... and never reaches 1."
            )
        ),
        starterCode = """
            import java.util.Scanner;

            public class HappyNumber {
                // TODO: return true if n is a happy number.
                static boolean isHappy(int n) {
                    return false;
                }

                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    int n = Integer.parseInt(scanner.nextLine().trim());
                    System.out.println(isHappy(n));
                }
            }
        """.trimIndent(),
        solutionCode = """
            import java.util.HashSet;
            import java.util.Scanner;
            import java.util.Set;

            public class HappyNumber {
                static boolean isHappy(int n) {
                    Set<Integer> seen = new HashSet<>();
                    while (n != 1 && !seen.contains(n)) {
                        seen.add(n);
                        n = sumOfSquaredDigits(n);
                    }
                    return n == 1;
                }

                private static int sumOfSquaredDigits(int n) {
                    int sum = 0;
                    while (n > 0) {
                        int digit = n % 10;
                        sum += digit * digit;
                        n /= 10;
                    }
                    return sum;
                }

                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    int n = Integer.parseInt(scanner.nextLine().trim());
                    System.out.println(isHappy(n));
                }
            }
        """.trimIndent(),
        hints = listOf(
            "Compute the sum of the squares of the digits repeatedly; track which values you've already seen.",
            "If you ever see the same value twice before reaching 1, you're in a cycle and the number is not happy."
        ),
        testCases = listOf(
            ExerciseTestCase("happy", "19", "true", visible = true, description = "reaches 1 after a few steps"),
            ExerciseTestCase("not-happy", "2", "false", visible = true, description = "falls into a cycle that never includes 1"),
            ExerciseTestCase("already-one", "1", "true", visible = true, description = "the trivial base case"),
            ExerciseTestCase("seven", "7", "true", visible = false, description = "another happy number, via a longer chain")
        ),
        timeComplexity = "O(log n)",
        spaceComplexity = "O(log n)",
        patterns = setOf("hash-set", "math"),
        tags = setOf("math", "hash-set"),
        estimatedMinutes = 15
    )

    val LONGEST_CONSECUTIVE_SEQUENCE = InterviewExercise(
        id = "hashmap-longest-consecutive-sequence",
        title = "Longest Consecutive Sequence",
        className = "LongestConsecutiveSequence",
        categoryId = CATEGORY_ID,
        difficulty = Difficulty.MEDIUM,
        description = "Given an unsorted array of integers nums, return the length of the " +
            "longest run of consecutive integers present in nums (the integers do not need to " +
            "be contiguous in the array itself). Your solution should run in O(n) time.",
        constraints = listOf(
            "1 <= nums.length <= 10^5",
            "-10^9 <= nums[i] <= 10^9"
        ),
        examples = listOf(
            ExerciseExample(
                input = "nums = [100,4,200,1,3,2]",
                output = "4",
                explanation = "The longest run is 1, 2, 3, 4."
            ),
            ExerciseExample(
                input = "nums = [0,3,7,2,5,8,4,6,0,1]",
                output = "9",
                explanation = "The longest run is 0 through 8 (the repeated 0 doesn't add anything new)."
            )
        ),
        starterCode = """
            import java.util.Scanner;

            public class LongestConsecutiveSequence {
                // TODO: return the length of the longest run of consecutive integers in nums.
                static int longestConsecutive(int[] nums) {
                    return 0;
                }

                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    String line = scanner.nextLine().trim();
                    String[] parts = line.isEmpty() ? new String[0] : line.split("\\s+");
                    int[] nums = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) nums[i] = Integer.parseInt(parts[i]);
                    System.out.println(longestConsecutive(nums));
                }
            }
        """.trimIndent(),
        solutionCode = """
            import java.util.HashSet;
            import java.util.Scanner;
            import java.util.Set;

            public class LongestConsecutiveSequence {
                static int longestConsecutive(int[] nums) {
                    Set<Integer> set = new HashSet<>();
                    for (int n : nums) set.add(n);
                    int best = 0;
                    for (int n : set) {
                        if (!set.contains(n - 1)) {
                            int length = 1;
                            int cur = n;
                            while (set.contains(cur + 1)) {
                                cur++;
                                length++;
                            }
                            best = Math.max(best, length);
                        }
                    }
                    return best;
                }

                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    String line = scanner.nextLine().trim();
                    String[] parts = line.isEmpty() ? new String[0] : line.split("\\s+");
                    int[] nums = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) nums[i] = Integer.parseInt(parts[i]);
                    System.out.println(longestConsecutive(nums));
                }
            }
        """.trimIndent(),
        hints = listOf(
            "Put every number into a HashSet first so membership checks are O(1).",
            "Only start counting a run from a number n when n-1 is NOT in the set -- that guarantees n is the start of its run, so each run gets counted exactly once."
        ),
        testCases = listOf(
            ExerciseTestCase("basic", "100 4 200 1 3 2", "4", visible = true, description = "the classic example"),
            ExerciseTestCase("longer", "0 3 7 2 5 8 4 6 0 1", "9", visible = true, description = "a longer run with a duplicate in the input"),
            ExerciseTestCase("single", "5", "1", visible = true, description = "one element is a run of length 1"),
            ExerciseTestCase("no-consecutive", "10 30 20", "1", visible = false, description = "no two values are consecutive, so the longest run is length 1")
        ),
        timeComplexity = "O(n)",
        spaceComplexity = "O(n)",
        patterns = setOf("hash-set"),
        tags = setOf("arrays", "hash-set"),
        estimatedMinutes = 20
    )

    val TOP_K_FREQUENT_ELEMENTS = InterviewExercise(
        id = "hashmap-top-k-frequent-elements",
        title = "Top K Frequent Elements",
        className = "TopKFrequentElements",
        categoryId = CATEGORY_ID,
        difficulty = Difficulty.MEDIUM,
        description = "Given an integer array nums and an integer k, return the k most " +
            "frequent elements. Order the result by frequency, highest first; break ties " +
            "between equally-frequent values by ascending numeric value, so the result is " +
            "always uniquely determined.",
        constraints = listOf(
            "1 <= nums.length <= 10^5",
            "1 <= k <= number of distinct elements in nums"
        ),
        examples = listOf(
            ExerciseExample(
                input = "nums = [1,1,1,2,2,3], k = 2",
                output = "[1, 2]",
                explanation = "1 appears 3 times and 2 appears 2 times -- the two most frequent values."
            ),
            ExerciseExample(
                input = "nums = [4,4,5,5,6], k = 2",
                output = "[4, 5]",
                explanation = "4 and 5 are tied at frequency 2; the tie is broken by ascending value."
            )
        ),
        starterCode = """
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Scanner;

            public class TopKFrequentElements {
                // TODO: return the k most frequent values in nums, highest frequency first,
                // ties broken by ascending value.
                static List<Integer> topKFrequent(int[] nums, int k) {
                    return new ArrayList<>();
                }

                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    String[] parts = scanner.nextLine().trim().split("\\s+");
                    int[] nums = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) nums[i] = Integer.parseInt(parts[i]);
                    int k = Integer.parseInt(scanner.nextLine().trim());
                    System.out.println(topKFrequent(nums, k));
                }
            }
        """.trimIndent(),
        solutionCode = """
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.Scanner;

            public class TopKFrequentElements {
                static List<Integer> topKFrequent(int[] nums, int k) {
                    Map<Integer, Integer> freq = new HashMap<>();
                    for (int n : nums) freq.merge(n, 1, Integer::sum);
                    List<Integer> values = new ArrayList<>(freq.keySet());
                    values.sort((a, b) -> {
                        int cmp = freq.get(b) - freq.get(a);
                        if (cmp != 0) return cmp;
                        return a - b;
                    });
                    return values.subList(0, k);
                }

                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    String[] parts = scanner.nextLine().trim().split("\\s+");
                    int[] nums = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) nums[i] = Integer.parseInt(parts[i]);
                    int k = Integer.parseInt(scanner.nextLine().trim());
                    System.out.println(topKFrequent(nums, k));
                }
            }
        """.trimIndent(),
        hints = listOf(
            "Count each value's frequency with a HashMap first.",
            "Sort the distinct values by frequency descending; break ties by ascending value for a deterministic order."
        ),
        testCases = listOf(
            ExerciseTestCase("basic", "1 1 1 2 2 3\n2", "[1, 2]", visible = true, description = "a clear frequency ordering"),
            ExerciseTestCase("single", "1\n1", "[1]", visible = true, description = "k equals the number of distinct elements"),
            ExerciseTestCase("tie-break", "4 4 5 5 6\n2", "[4, 5]", visible = true, description = "two values tied at the cutoff, broken by ascending value"),
            ExerciseTestCase("all-unique", "7 3 9\n2", "[3, 7]", visible = false, description = "every value has frequency 1, so the whole order comes from the tie-break rule")
        ),
        timeComplexity = "O(n log n)",
        spaceComplexity = "O(n)",
        patterns = setOf("hash-map", "sorting"),
        tags = setOf("arrays", "hash-map"),
        estimatedMinutes = 20
    )

    val ALL: List<InterviewExercise> = listOf(
        FIRST_NON_REPEATING_CHARACTER,
        INTERSECTION_OF_TWO_ARRAYS,
        ISOMORPHIC_STRINGS,
        HAPPY_NUMBER,
        LONGEST_CONSECUTIVE_SEQUENCE,
        TOP_K_FREQUENT_ELEMENTS
    )
}
