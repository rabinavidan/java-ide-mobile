package com.javaide.mobile.compiler

data class InterviewExercise(val className: String, val source: String, val expectedOutput: String)

/**
 * A handful of classic coding-interview-style exercises used to exercise the compile/dex/run
 * pipeline end to end: loops and strings, recursion, arrays + HashMap, string manipulation,
 * arrays + binary search, and Collections + Streams + lambdas.
 *
 * Expected outputs were verified against plain javac/java and against the actual
 * ECJ compile + D8 dex + reflection-execute chain before being written here.
 */
object InterviewExercises {

    val FIZZ_BUZZ = InterviewExercise(
        className = "FizzBuzz",
        source = """
            public class FizzBuzz {
                public static void main(String[] args) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= 15; i++) {
                        if (i % 15 == 0) sb.append("FizzBuzz");
                        else if (i % 3 == 0) sb.append("Fizz");
                        else if (i % 5 == 0) sb.append("Buzz");
                        else sb.append(i);
                        sb.append(i < 15 ? "," : "");
                    }
                    System.out.println(sb);
                }
            }
        """.trimIndent(),
        expectedOutput = "1,2,Fizz,4,Buzz,Fizz,7,8,Fizz,Buzz,11,Fizz,13,14,FizzBuzz"
    )

    val FIBONACCI = InterviewExercise(
        className = "Fibonacci",
        source = """
            public class Fibonacci {
                static long fib(int n) {
                    return n <= 1 ? n : fib(n - 1) + fib(n - 2);
                }
                public static void main(String[] args) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 10; i++) {
                        sb.append(fib(i));
                        if (i < 9) sb.append(",");
                    }
                    System.out.println(sb);
                }
            }
        """.trimIndent(),
        expectedOutput = "0,1,1,2,3,5,8,13,21,34"
    )

    val TWO_SUM = InterviewExercise(
        className = "TwoSum",
        source = """
            import java.util.HashMap;
            import java.util.Map;

            public class TwoSum {
                static int[] twoSum(int[] nums, int target) {
                    Map<Integer, Integer> seen = new HashMap<>();
                    for (int i = 0; i < nums.length; i++) {
                        int complement = target - nums[i];
                        if (seen.containsKey(complement)) {
                            return new int[]{seen.get(complement), i};
                        }
                        seen.put(nums[i], i);
                    }
                    throw new IllegalArgumentException("No solution");
                }
                public static void main(String[] args) {
                    int[] result = twoSum(new int[]{2, 7, 11, 15}, 9);
                    System.out.println(result[0] + "," + result[1]);
                }
            }
        """.trimIndent(),
        expectedOutput = "0,1"
    )

    val IS_PALINDROME = InterviewExercise(
        className = "IsPalindrome",
        source = """
            public class IsPalindrome {
                static boolean isPalindrome(String s) {
                    String cleaned = s.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                    return cleaned.equals(new StringBuilder(cleaned).reverse().toString());
                }
                public static void main(String[] args) {
                    System.out.println(isPalindrome("A man, a plan, a canal: Panama"));
                    System.out.println(isPalindrome("hello"));
                }
            }
        """.trimIndent(),
        expectedOutput = "true\nfalse"
    )

    val BINARY_SEARCH = InterviewExercise(
        className = "BinarySearch",
        source = """
            public class BinarySearch {
                static int search(int[] arr, int target) {
                    int lo = 0, hi = arr.length - 1;
                    while (lo <= hi) {
                        int mid = lo + (hi - lo) / 2;
                        if (arr[mid] == target) return mid;
                        if (arr[mid] < target) lo = mid + 1; else hi = mid - 1;
                    }
                    return -1;
                }
                public static void main(String[] args) {
                    int[] arr = {1, 3, 5, 7, 9, 11, 13};
                    System.out.println(search(arr, 7));
                    System.out.println(search(arr, 4));
                }
            }
        """.trimIndent(),
        expectedOutput = "3\n-1"
    )

    val GROUP_ANAGRAMS = InterviewExercise(
        className = "GroupAnagrams",
        source = """
            import java.util.*;
            import java.util.stream.*;

            public class GroupAnagrams {
                public static void main(String[] args) {
                    List<String> words = Arrays.asList("eat", "tea", "tan", "ate", "nat", "bat");
                    Map<String, List<String>> groups = words.stream()
                        .collect(Collectors.groupingBy(w -> {
                            char[] chars = w.toCharArray();
                            Arrays.sort(chars);
                            return new String(chars);
                        }, TreeMap::new, Collectors.toList()));
                    System.out.println(groups.values());
                }
            }
        """.trimIndent(),
        expectedOutput = "[[bat], [eat, tea, ate], [tan, nat]]"
    )

    val ALL = listOf(FIZZ_BUZZ, FIBONACCI, TWO_SUM, IS_PALINDROME, BINARY_SEARCH, GROUP_ANAGRAMS)
}
