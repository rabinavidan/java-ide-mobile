package com.javaide.mobile.practice.migration

/** Starter-code stub and hints for one legacy exercise, keyed by [com.javaide.mobile.compiler.InterviewExercise.className] in [LegacyStarterCode.BY_CLASS_NAME]. */
data class LegacyStarterContent(val starterCode: String, val hints: List<String>)

/**
 * Hand-authored starter-code stubs for the 30 legacy exercises (Milestone 5), used by
 * [LegacyExerciseMigration] as the V2 model's `starterCode` instead of reusing the reference
 * solution. Each stub keeps the exercise's required class(es), the method signature(s) to
 * implement, and the unmodified `main()` that exercises them — only the algorithm's body is
 * replaced with a `// TODO` and a compiles-cleanly default return, so a fresh starter always
 * compiles (it just won't produce the right answer yet, by design: "solution does not appear
 * automatically").
 *
 * Two hints are included per exercise. Keep this in sync with
 * `app/src/testShared/java/com/javaide/mobile/compiler/InterviewExercises.kt` if a legacy
 * exercise's method signature ever changes there.
 */
object LegacyStarterCode {

    val BY_CLASS_NAME: Map<String, LegacyStarterContent> = mapOf(
        "FizzBuzz" to LegacyStarterContent(
            starterCode = """
                public class FizzBuzz {
                    // TODO: build a comma-joined string for i = 1..15 where multiples of 3 use
                    // "Fizz", multiples of 5 use "Buzz", multiples of both use "FizzBuzz", and
                    // everything else is just the number itself.
                    static String fizzBuzz() {
                        return "";
                    }

                    public static void main(String[] args) {
                        System.out.println(fizzBuzz());
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Use the modulo operator (%) to check divisibility by 3 and 5.",
                "Check divisibility by 15 (or both 3 and 5) before checking 3 or 5 alone."
            )
        ),
        "Fibonacci" to LegacyStarterContent(
            starterCode = """
                public class Fibonacci {
                    // TODO: return the nth Fibonacci number (fib(0) = 0, fib(1) = 1).
                    static long fib(int n) {
                        return 0;
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
            hints = listOf(
                "fib(n) = fib(n-1) + fib(n-2), with fib(0) = 0 and fib(1) = 1.",
                "A loop building up from the base cases is faster than naive recursion."
            )
        ),
        "TwoSum" to LegacyStarterContent(
            starterCode = """
                import java.util.HashMap;
                import java.util.Map;

                public class TwoSum {
                    // TODO: return the indices of the two numbers in nums that add up to target.
                    static int[] twoSum(int[] nums, int target) {
                        return new int[0];
                    }

                    public static void main(String[] args) {
                        int[] result = twoSum(new int[]{2, 7, 11, 15}, 9);
                        System.out.println(result[0] + "," + result[1]);
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "A HashMap from value to index lets you check for the complement in O(1).",
                "For each number, look up target - number before adding the current number to the map."
            )
        ),
        "IsPalindrome" to LegacyStarterContent(
            starterCode = """
                public class IsPalindrome {
                    // TODO: return true if s is a palindrome, ignoring non-alphanumeric
                    // characters and case.
                    static boolean isPalindrome(String s) {
                        return false;
                    }

                    public static void main(String[] args) {
                        System.out.println(isPalindrome("A man, a plan, a canal: Panama"));
                        System.out.println(isPalindrome("hello"));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Strip out anything that isn't a letter or digit first, and lowercase the rest.",
                "Compare the cleaned string to its own reverse."
            )
        ),
        "BinarySearch" to LegacyStarterContent(
            starterCode = """
                public class BinarySearch {
                    // TODO: return the index of target in the sorted array arr, or -1 if not found.
                    static int search(int[] arr, int target) {
                        return -1;
                    }

                    public static void main(String[] args) {
                        int[] arr = {1, 3, 5, 7, 9, 11, 13};
                        System.out.println(search(arr, 7));
                        System.out.println(search(arr, 4));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Track lo/hi bounds and repeatedly check the middle element.",
                "If arr[mid] is too small, search the right half; otherwise search the left half."
            )
        ),
        "GroupAnagrams" to LegacyStarterContent(
            starterCode = """
                import java.util.Arrays;
                import java.util.List;
                import java.util.Map;
                import java.util.TreeMap;

                public class GroupAnagrams {
                    // TODO: group words that are anagrams of each other; key by sorted
                    // characters, using a TreeMap so groups come out in a deterministic order.
                    static Map<String, List<String>> groupAnagrams(List<String> words) {
                        return new TreeMap<>();
                    }

                    public static void main(String[] args) {
                        List<String> words = Arrays.asList("eat", "tea", "tan", "ate", "nat", "bat");
                        System.out.println(groupAnagrams(words).values());
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Two words are anagrams if their sorted characters are identical.",
                "Use the sorted-character string as a map key, and collect words with a matching key into the same list."
            )
        ),
        "MaxSubArray" to LegacyStarterContent(
            starterCode = """
                public class MaxSubArray {
                    // TODO: return the maximum sum of a contiguous subarray of nums (Kadane's algorithm).
                    static int maxSubArray(int[] nums) {
                        return 0;
                    }

                    public static void main(String[] args) {
                        System.out.println(maxSubArray(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Kadane's algorithm: track the best sum ending at the current index.",
                "At each step, either extend the previous subarray or start a new one at the current element."
            )
        ),
        "ReverseString" to LegacyStarterContent(
            starterCode = """
                public class ReverseString {
                    // TODO: reverse the character array s in place.
                    static void reverse(char[] s) {
                    }

                    public static void main(String[] args) {
                        char[] s = "hello".toCharArray();
                        reverse(s);
                        System.out.println(new String(s));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Swap characters from both ends moving toward the middle.",
                "Use two index pointers, one from each end, and stop when they meet."
            )
        ),
        "ValidAnagram" to LegacyStarterContent(
            starterCode = """
                public class ValidAnagram {
                    // TODO: return true if s and t are anagrams of each other.
                    static boolean isAnagram(String s, String t) {
                        return false;
                    }

                    public static void main(String[] args) {
                        System.out.println(isAnagram("anagram", "nagaram"));
                        System.out.println(isAnagram("rat", "car"));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Two strings are anagrams only if they're the same length.",
                "Sort both strings' characters and compare, or count character frequencies."
            )
        ),
        "ContainsDuplicate" to LegacyStarterContent(
            starterCode = """
                public class ContainsDuplicate {
                    // TODO: return true if any value appears more than once in nums.
                    static boolean containsDuplicate(int[] nums) {
                        return false;
                    }

                    public static void main(String[] args) {
                        System.out.println(containsDuplicate(new int[]{1, 2, 3, 1}));
                        System.out.println(containsDuplicate(new int[]{1, 2, 3, 4}));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "A HashSet lets you check \"have I seen this before?\" in O(1).",
                "Return true as soon as adding a value to the set fails (it was already there)."
            )
        ),
        "MoveZeroes" to LegacyStarterContent(
            starterCode = """
                import java.util.Arrays;

                public class MoveZeroes {
                    // TODO: move all zeroes in nums to the end, in place, keeping the relative
                    // order of the non-zero elements.
                    static void moveZeroes(int[] nums) {
                    }

                    public static void main(String[] args) {
                        int[] nums = {0, 1, 0, 3, 12};
                        moveZeroes(nums);
                        System.out.println(Arrays.toString(nums));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Track a \"write\" index for the next non-zero value's position.",
                "After moving all non-zero values forward, fill the rest of the array with zeroes."
            )
        ),
        "ReverseLinkedList" to LegacyStarterContent(
            starterCode = """
                class ListNode {
                    int val;
                    ListNode next;
                    ListNode(int val) { this.val = val; }
                }

                public class ReverseLinkedList {
                    // TODO: reverse the linked list starting at head and return the new head.
                    static ListNode reverse(ListNode head) {
                        return head;
                    }

                    static ListNode fromArray(int[] arr) {
                        ListNode dummy = new ListNode(0);
                        ListNode cur = dummy;
                        for (int v : arr) { cur.next = new ListNode(v); cur = cur.next; }
                        return dummy.next;
                    }

                    static String toStringList(ListNode head) {
                        StringBuilder sb = new StringBuilder();
                        while (head != null) {
                            sb.append(head.val);
                            if (head.next != null) sb.append(",");
                            head = head.next;
                        }
                        return sb.toString();
                    }

                    public static void main(String[] args) {
                        ListNode head = fromArray(new int[]{1, 2, 3, 4, 5});
                        System.out.println(toStringList(reverse(head)));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Track a \"previous\" pointer, starting at null.",
                "For each node, save its next pointer before overwriting it to point backward."
            )
        ),
        "MergeTwoSortedLists" to LegacyStarterContent(
            starterCode = """
                class MNode {
                    int val;
                    MNode next;
                    MNode(int val) { this.val = val; }
                }

                public class MergeTwoSortedLists {
                    // TODO: merge two sorted linked lists a and b into one sorted list and
                    // return its head.
                    static MNode merge(MNode a, MNode b) {
                        return null;
                    }

                    static MNode fromArray(int[] arr) {
                        MNode dummy = new MNode(0);
                        MNode cur = dummy;
                        for (int v : arr) { cur.next = new MNode(v); cur = cur.next; }
                        return dummy.next;
                    }

                    public static void main(String[] args) {
                        MNode a = fromArray(new int[]{1, 2, 4});
                        MNode b = fromArray(new int[]{1, 3, 4});
                        MNode result = merge(a, b);
                        StringBuilder sb = new StringBuilder();
                        while (result != null) {
                            sb.append(result.val);
                            if (result.next != null) sb.append(",");
                            result = result.next;
                        }
                        System.out.println(sb);
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Use a dummy head node to simplify building the result list.",
                "At each step, attach whichever of a or b has the smaller value, then advance that list."
            )
        ),
        "LinkedListHasCycle" to LegacyStarterContent(
            starterCode = """
                class CNode {
                    int val;
                    CNode next;
                    CNode(int val) { this.val = val; }
                }

                public class LinkedListHasCycle {
                    // TODO: return true if the linked list starting at head has a cycle.
                    static boolean hasCycle(CNode head) {
                        return false;
                    }

                    public static void main(String[] args) {
                        CNode a = new CNode(1);
                        CNode b = new CNode(2);
                        CNode c = new CNode(3);
                        a.next = b; b.next = c; c.next = a;
                        System.out.println(hasCycle(a));

                        CNode x = new CNode(1);
                        CNode y = new CNode(2);
                        x.next = y;
                        System.out.println(hasCycle(x));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Use two pointers moving at different speeds (Floyd's cycle detection).",
                "If a fast pointer (2 steps) ever equals a slow pointer (1 step), there's a cycle."
            )
        ),
        "ValidParentheses" to LegacyStarterContent(
            starterCode = """
                public class ValidParentheses {
                    // TODO: return true if s is a valid, correctly-matched sequence of brackets.
                    static boolean isValid(String s) {
                        return false;
                    }

                    public static void main(String[] args) {
                        System.out.println(isValid("()[]{}"));
                        System.out.println(isValid("(]"));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "A stack naturally matches the most recent unclosed bracket.",
                "Push opening brackets; on a closing bracket, check it matches the top of the stack."
            )
        ),
        "MinStack" to LegacyStarterContent(
            starterCode = """
                public class MinStack {
                    // TODO: implement a stack that supports push/pop/top and retrieving the
                    // minimum element, all in O(1).
                    void push(int val) {
                    }
                    void pop() {
                    }
                    int top() {
                        return 0;
                    }
                    int getMin() {
                        return 0;
                    }

                    public static void main(String[] args) {
                        MinStack ms = new MinStack();
                        ms.push(-2); ms.push(0); ms.push(-3);
                        System.out.println(ms.getMin());
                        ms.pop();
                        System.out.println(ms.top());
                        System.out.println(ms.getMin());
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Keep a second stack that tracks the minimum seen so far at each push.",
                "Push the min of the new value and the current min onto the min-stack alongside every push."
            )
        ),
        "TreeInorderTraversal" to LegacyStarterContent(
            starterCode = """
                import java.util.ArrayList;
                import java.util.List;

                class TNode1 {
                    int val;
                    TNode1 left, right;
                    TNode1(int val) { this.val = val; }
                }

                public class TreeInorderTraversal {
                    // TODO: append node's values to out in inorder (left, node, right).
                    static void inorder(TNode1 node, List<Integer> out) {
                    }

                    public static void main(String[] args) {
                        TNode1 root = new TNode1(1);
                        root.right = new TNode1(2);
                        root.right.left = new TNode1(3);
                        List<Integer> out = new ArrayList<>();
                        inorder(root, out);
                        System.out.println(out);
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Inorder means: left subtree, then this node, then right subtree.",
                "Recursion mirrors the definition directly -- recurse left, add the value, recurse right."
            )
        ),
        "TreeMaxDepth" to LegacyStarterContent(
            starterCode = """
                class TNode2 {
                    int val;
                    TNode2 left, right;
                    TNode2(int val) { this.val = val; }
                }

                public class TreeMaxDepth {
                    // TODO: return the maximum depth (number of nodes on the longest
                    // root-to-leaf path).
                    static int maxDepth(TNode2 node) {
                        return 0;
                    }

                    public static void main(String[] args) {
                        TNode2 root = new TNode2(3);
                        root.left = new TNode2(9);
                        root.right = new TNode2(20);
                        root.right.left = new TNode2(15);
                        root.right.right = new TNode2(7);
                        System.out.println(maxDepth(root));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "The depth of a null node is 0.",
                "The depth of a node is 1 plus the larger of its two children's depths."
            )
        ),
        "IsSameTree" to LegacyStarterContent(
            starterCode = """
                class TNode3 {
                    int val;
                    TNode3 left, right;
                    TNode3(int val) { this.val = val; }
                }

                public class IsSameTree {
                    // TODO: return true if trees p and q are structurally identical with the
                    // same node values.
                    static boolean isSameTree(TNode3 p, TNode3 q) {
                        return false;
                    }

                    public static void main(String[] args) {
                        TNode3 p = new TNode3(1); p.left = new TNode3(2); p.right = new TNode3(3);
                        TNode3 q = new TNode3(1); q.left = new TNode3(2); q.right = new TNode3(3);
                        System.out.println(isSameTree(p, q));

                        TNode3 r = new TNode3(1); r.left = new TNode3(2);
                        System.out.println(isSameTree(p, r));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Two null trees are the same; a null and a non-null tree are not.",
                "Compare the current nodes' values, then recursively compare both left and right subtrees."
            )
        ),
        "TreeLevelOrder" to LegacyStarterContent(
            starterCode = """
                import java.util.ArrayList;
                import java.util.List;

                class TNode4 {
                    int val;
                    TNode4 left, right;
                    TNode4(int val) { this.val = val; }
                }

                public class TreeLevelOrder {
                    // TODO: return the tree's node values grouped level by level, top to bottom.
                    static List<List<Integer>> levelOrder(TNode4 root) {
                        return new ArrayList<>();
                    }

                    public static void main(String[] args) {
                        TNode4 root = new TNode4(3);
                        root.left = new TNode4(9);
                        root.right = new TNode4(20);
                        root.right.left = new TNode4(15);
                        root.right.right = new TNode4(7);
                        System.out.println(levelOrder(root));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "A queue-based breadth-first traversal naturally processes one level at a time.",
                "Before processing a level, record how many nodes are currently in the queue -- that's the level's size."
            )
        ),
        "GraphBFS" to LegacyStarterContent(
            starterCode = """
                import java.util.ArrayList;
                import java.util.Arrays;
                import java.util.Collections;
                import java.util.HashMap;
                import java.util.List;
                import java.util.Map;

                public class GraphBFS {
                    // TODO: return the nodes reachable from start, in breadth-first order.
                    static List<Integer> bfs(Map<Integer, List<Integer>> graph, int start) {
                        return new ArrayList<>();
                    }

                    public static void main(String[] args) {
                        Map<Integer, List<Integer>> graph = new HashMap<>();
                        graph.put(1, Arrays.asList(2, 3));
                        graph.put(2, Arrays.asList(4));
                        graph.put(3, Arrays.asList(4));
                        graph.put(4, Collections.emptyList());
                        System.out.println(bfs(graph, 1));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Use a queue and a visited set to avoid revisiting nodes.",
                "Add a neighbor to the queue only the first time it's seen."
            )
        ),
        "GraphDFS" to LegacyStarterContent(
            starterCode = """
                import java.util.ArrayList;
                import java.util.Arrays;
                import java.util.Collections;
                import java.util.HashMap;
                import java.util.HashSet;
                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                public class GraphDFS {
                    // TODO: visit start's reachable nodes depth-first, appending each
                    // newly-visited node to order.
                    static void dfs(Map<Integer, List<Integer>> graph, int node, Set<Integer> visited, List<Integer> order) {
                    }

                    public static void main(String[] args) {
                        Map<Integer, List<Integer>> graph = new HashMap<>();
                        graph.put(1, Arrays.asList(2, 3));
                        graph.put(2, Arrays.asList(4));
                        graph.put(3, Arrays.asList(4));
                        graph.put(4, Collections.emptyList());
                        List<Integer> order = new ArrayList<>();
                        dfs(graph, 1, new HashSet<>(), order);
                        System.out.println(order);
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Recursion naturally follows one path as deep as possible before backtracking.",
                "Mark a node visited before recursing into its neighbors to avoid infinite loops on cycles."
            )
        ),
        "MergeSort" to LegacyStarterContent(
            starterCode = """
                import java.util.Arrays;

                public class MergeSort {
                    // TODO: sort arr[lo, hi) in place, ascending.
                    static void sort(int[] arr, int lo, int hi) {
                    }

                    public static void main(String[] args) {
                        int[] arr = {5, 2, 9, 1, 5, 6};
                        sort(arr, 0, arr.length);
                        System.out.println(Arrays.toString(arr));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Split the range in half, sort each half recursively, then merge the two sorted halves.",
                "The merge step needs a temporary array to combine two sorted ranges in order."
            )
        ),
        "SearchInsertPosition" to LegacyStarterContent(
            starterCode = """
                public class SearchInsertPosition {
                    // TODO: return the index where target is found, or where it would be
                    // inserted to keep nums sorted.
                    static int searchInsert(int[] nums, int target) {
                        return 0;
                    }

                    public static void main(String[] args) {
                        int[] arr = {1, 3, 5, 6};
                        System.out.println(searchInsert(arr, 5));
                        System.out.println(searchInsert(arr, 2));
                        System.out.println(searchInsert(arr, 7));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "This is binary search, but it doesn't need target to actually be in the array.",
                "Narrow lo/hi until they converge -- that final position is the answer either way."
            )
        ),
        "Factorial" to LegacyStarterContent(
            starterCode = """
                public class Factorial {
                    // TODO: return n! (n factorial).
                    static long factorial(int n) {
                        return 0;
                    }

                    public static void main(String[] args) {
                        System.out.println(factorial(0));
                        System.out.println(factorial(5));
                        System.out.println(factorial(10));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "n! = n * (n-1)!, with 0! = 1.",
                "A loop from 1 to n multiplying as you go avoids recursion overhead."
            )
        ),
        "Permutations" to LegacyStarterContent(
            starterCode = """
                import java.util.ArrayList;
                import java.util.List;

                public class Permutations {
                    // TODO: append every permutation of nums to result.
                    static void backtrack(List<List<Integer>> result, List<Integer> current, int[] nums, boolean[] used) {
                    }

                    public static void main(String[] args) {
                        int[] nums = {1, 2, 3};
                        List<List<Integer>> result = new ArrayList<>();
                        backtrack(result, new ArrayList<>(), nums, new boolean[nums.length]);
                        System.out.println(result);
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Backtracking: pick an unused number, recurse, then un-pick it before trying the next.",
                "A \"used\" array tracks which numbers are already part of the current permutation."
            )
        ),
        "Subsets" to LegacyStarterContent(
            starterCode = """
                import java.util.ArrayList;
                import java.util.List;

                public class Subsets {
                    // TODO: append every subset of nums[start:] to result.
                    static void backtrack(List<List<Integer>> result, List<Integer> current, int[] nums, int start) {
                    }

                    public static void main(String[] args) {
                        int[] nums = {1, 2, 3};
                        List<List<Integer>> result = new ArrayList<>();
                        backtrack(result, new ArrayList<>(), nums, 0);
                        System.out.println(result);
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Every prefix of the recursion (including the empty one) is itself a valid subset.",
                "At each recursive call, add the current partial list to the result before trying to extend it further."
            )
        ),
        "ClimbingStairs" to LegacyStarterContent(
            starterCode = """
                public class ClimbingStairs {
                    // TODO: return the number of distinct ways to climb n stairs, taking 1 or 2
                    // steps at a time.
                    static int climbStairs(int n) {
                        return 0;
                    }

                    public static void main(String[] args) {
                        System.out.println(climbStairs(2));
                        System.out.println(climbStairs(5));
                        System.out.println(climbStairs(10));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "This is the Fibonacci sequence in disguise: ways(n) = ways(n-1) + ways(n-2).",
                "You can compute it iteratively with two running variables instead of recursion."
            )
        ),
        "CoinChange" to LegacyStarterContent(
            starterCode = """
                public class CoinChange {
                    // TODO: return the fewest coins needed to make amount, or -1 if it's not possible.
                    static int coinChange(int[] coins, int amount) {
                        return -1;
                    }

                    public static void main(String[] args) {
                        System.out.println(coinChange(new int[]{1, 2, 5}, 11));
                        System.out.println(coinChange(new int[]{2}, 3));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "Build up a table where dp[i] is the fewest coins needed to make amount i.",
                "For each amount, try every coin and take the best (fewest-coin) result among them."
            )
        ),
        "SingleNumber" to LegacyStarterContent(
            starterCode = """
                public class SingleNumber {
                    // TODO: return the element that appears exactly once (every other element
                    // appears exactly twice).
                    static int singleNumber(int[] nums) {
                        return 0;
                    }

                    public static void main(String[] args) {
                        System.out.println(singleNumber(new int[]{2, 2, 1}));
                        System.out.println(singleNumber(new int[]{4, 1, 2, 1, 2}));
                    }
                }
            """.trimIndent(),
            hints = listOf(
                "XOR-ing a number with itself gives 0, and XOR-ing with 0 leaves it unchanged.",
                "XOR every element together -- every pair cancels out, leaving only the single number."
            )
        )
    )
}
