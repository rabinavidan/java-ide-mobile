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
            import java.util.ArrayList;
            import java.util.Arrays;
            import java.util.List;
            import java.util.Map;
            import java.util.TreeMap;

            public class GroupAnagrams {
                public static void main(String[] args) {
                    String[] words = {"eat", "tea", "tan", "ate", "nat", "bat"};
                    Map<String, List<String>> groups = new TreeMap<>();
                    for (String w : words) {
                        char[] chars = w.toCharArray();
                        Arrays.sort(chars);
                        String key = new String(chars);
                        List<String> list = groups.get(key);
                        if (list == null) {
                            list = new ArrayList<>();
                            groups.put(key, list);
                        }
                        list.add(w);
                    }
                    System.out.println(groups.values());
                }
            }
        """.trimIndent(),
        expectedOutput = "[[bat], [eat, tea, ate], [tan, nat]]"
    )

    // --- Arrays & strings ---

    val MAX_SUB_ARRAY = InterviewExercise(
        className = "MaxSubArray",
        source = """
            public class MaxSubArray {
                static int maxSubArray(int[] nums) {
                    int best = nums[0], cur = nums[0];
                    for (int i = 1; i < nums.length; i++) {
                        cur = Math.max(nums[i], cur + nums[i]);
                        best = Math.max(best, cur);
                    }
                    return best;
                }
                public static void main(String[] args) {
                    System.out.println(maxSubArray(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));
                }
            }
        """.trimIndent(),
        expectedOutput = "6"
    )

    val REVERSE_STRING = InterviewExercise(
        className = "ReverseString",
        source = """
            public class ReverseString {
                static void reverse(char[] s) {
                    int i = 0, j = s.length - 1;
                    while (i < j) {
                        char t = s[i]; s[i] = s[j]; s[j] = t;
                        i++; j--;
                    }
                }
                public static void main(String[] args) {
                    char[] s = "hello".toCharArray();
                    reverse(s);
                    System.out.println(new String(s));
                }
            }
        """.trimIndent(),
        expectedOutput = "olleh"
    )

    val VALID_ANAGRAM = InterviewExercise(
        className = "ValidAnagram",
        source = """
            import java.util.Arrays;

            public class ValidAnagram {
                static boolean isAnagram(String s, String t) {
                    if (s.length() != t.length()) return false;
                    char[] a = s.toCharArray(), b = t.toCharArray();
                    Arrays.sort(a); Arrays.sort(b);
                    return Arrays.equals(a, b);
                }
                public static void main(String[] args) {
                    System.out.println(isAnagram("anagram", "nagaram"));
                    System.out.println(isAnagram("rat", "car"));
                }
            }
        """.trimIndent(),
        expectedOutput = "true\nfalse"
    )

    val CONTAINS_DUPLICATE = InterviewExercise(
        className = "ContainsDuplicate",
        source = """
            import java.util.HashSet;
            import java.util.Set;

            public class ContainsDuplicate {
                static boolean containsDuplicate(int[] nums) {
                    Set<Integer> seen = new HashSet<>();
                    for (int n : nums) {
                        if (!seen.add(n)) return true;
                    }
                    return false;
                }
                public static void main(String[] args) {
                    System.out.println(containsDuplicate(new int[]{1, 2, 3, 1}));
                    System.out.println(containsDuplicate(new int[]{1, 2, 3, 4}));
                }
            }
        """.trimIndent(),
        expectedOutput = "true\nfalse"
    )

    val MOVE_ZEROES = InterviewExercise(
        className = "MoveZeroes",
        source = """
            import java.util.Arrays;

            public class MoveZeroes {
                static void moveZeroes(int[] nums) {
                    int insertPos = 0;
                    for (int n : nums) {
                        if (n != 0) nums[insertPos++] = n;
                    }
                    while (insertPos < nums.length) nums[insertPos++] = 0;
                }
                public static void main(String[] args) {
                    int[] nums = {0, 1, 0, 3, 12};
                    moveZeroes(nums);
                    System.out.println(Arrays.toString(nums));
                }
            }
        """.trimIndent(),
        expectedOutput = "[1, 3, 12, 0, 0]"
    )

    // --- Linked lists ---

    val REVERSE_LINKED_LIST = InterviewExercise(
        className = "ReverseLinkedList",
        source = """
            class ListNode {
                int val;
                ListNode next;
                ListNode(int val) { this.val = val; }
            }

            public class ReverseLinkedList {
                static ListNode reverse(ListNode head) {
                    ListNode prev = null;
                    while (head != null) {
                        ListNode next = head.next;
                        head.next = prev;
                        prev = head;
                        head = next;
                    }
                    return prev;
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
        expectedOutput = "5,4,3,2,1"
    )

    val MERGE_TWO_SORTED_LISTS = InterviewExercise(
        className = "MergeTwoSortedLists",
        source = """
            class MNode {
                int val;
                MNode next;
                MNode(int val) { this.val = val; }
            }

            public class MergeTwoSortedLists {
                static MNode merge(MNode a, MNode b) {
                    MNode dummy = new MNode(0);
                    MNode cur = dummy;
                    while (a != null && b != null) {
                        if (a.val <= b.val) { cur.next = a; a = a.next; }
                        else { cur.next = b; b = b.next; }
                        cur = cur.next;
                    }
                    cur.next = (a != null) ? a : b;
                    return dummy.next;
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
        expectedOutput = "1,1,2,3,4,4"
    )

    val LINKED_LIST_HAS_CYCLE = InterviewExercise(
        className = "LinkedListHasCycle",
        source = """
            class CNode {
                int val;
                CNode next;
                CNode(int val) { this.val = val; }
            }

            public class LinkedListHasCycle {
                static boolean hasCycle(CNode head) {
                    CNode slow = head, fast = head;
                    while (fast != null && fast.next != null) {
                        slow = slow.next;
                        fast = fast.next.next;
                        if (slow == fast) return true;
                    }
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
        expectedOutput = "true\nfalse"
    )

    // --- Stacks & queues ---

    val VALID_PARENTHESES = InterviewExercise(
        className = "ValidParentheses",
        source = """
            import java.util.ArrayDeque;
            import java.util.Deque;

            public class ValidParentheses {
                static boolean isValid(String s) {
                    Deque<Character> stack = new ArrayDeque<>();
                    for (char c : s.toCharArray()) {
                        if (c == '(' || c == '[' || c == '{') {
                            stack.push(c);
                        } else {
                            if (stack.isEmpty()) return false;
                            char top = stack.pop();
                            if (c == ')' && top != '(') return false;
                            if (c == ']' && top != '[') return false;
                            if (c == '}' && top != '{') return false;
                        }
                    }
                    return stack.isEmpty();
                }
                public static void main(String[] args) {
                    System.out.println(isValid("()[]{}"));
                    System.out.println(isValid("(]"));
                }
            }
        """.trimIndent(),
        expectedOutput = "true\nfalse"
    )

    val MIN_STACK = InterviewExercise(
        className = "MinStack",
        source = """
            import java.util.ArrayDeque;
            import java.util.Deque;

            public class MinStack {
                private final Deque<Integer> stack = new ArrayDeque<>();
                private final Deque<Integer> minStack = new ArrayDeque<>();

                void push(int val) {
                    stack.push(val);
                    minStack.push(minStack.isEmpty() ? val : Math.min(val, minStack.peek()));
                }
                void pop() {
                    stack.pop();
                    minStack.pop();
                }
                int top() { return stack.peek(); }
                int getMin() { return minStack.peek(); }

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
        expectedOutput = "-3\n0\n-2"
    )

    // --- Trees ---

    val TREE_INORDER_TRAVERSAL = InterviewExercise(
        className = "TreeInorderTraversal",
        source = """
            import java.util.ArrayList;
            import java.util.List;

            class TNode1 {
                int val;
                TNode1 left, right;
                TNode1(int val) { this.val = val; }
            }

            public class TreeInorderTraversal {
                static void inorder(TNode1 node, List<Integer> out) {
                    if (node == null) return;
                    inorder(node.left, out);
                    out.add(node.val);
                    inorder(node.right, out);
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
        expectedOutput = "[1, 3, 2]"
    )

    val TREE_MAX_DEPTH = InterviewExercise(
        className = "TreeMaxDepth",
        source = """
            class TNode2 {
                int val;
                TNode2 left, right;
                TNode2(int val) { this.val = val; }
            }

            public class TreeMaxDepth {
                static int maxDepth(TNode2 node) {
                    if (node == null) return 0;
                    return 1 + Math.max(maxDepth(node.left), maxDepth(node.right));
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
        expectedOutput = "3"
    )

    val IS_SAME_TREE = InterviewExercise(
        className = "IsSameTree",
        source = """
            class TNode3 {
                int val;
                TNode3 left, right;
                TNode3(int val) { this.val = val; }
            }

            public class IsSameTree {
                static boolean isSameTree(TNode3 p, TNode3 q) {
                    if (p == null && q == null) return true;
                    if (p == null || q == null) return false;
                    return p.val == q.val && isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
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
        expectedOutput = "true\nfalse"
    )

    val TREE_LEVEL_ORDER = InterviewExercise(
        className = "TreeLevelOrder",
        source = """
            import java.util.ArrayDeque;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Queue;

            class TNode4 {
                int val;
                TNode4 left, right;
                TNode4(int val) { this.val = val; }
            }

            public class TreeLevelOrder {
                static List<List<Integer>> levelOrder(TNode4 root) {
                    List<List<Integer>> result = new ArrayList<>();
                    if (root == null) return result;
                    Queue<TNode4> queue = new ArrayDeque<>();
                    queue.add(root);
                    while (!queue.isEmpty()) {
                        int size = queue.size();
                        List<Integer> level = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            TNode4 node = queue.poll();
                            level.add(node.val);
                            if (node.left != null) queue.add(node.left);
                            if (node.right != null) queue.add(node.right);
                        }
                        result.add(level);
                    }
                    return result;
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
        expectedOutput = "[[3], [9, 20], [15, 7]]"
    )

    // --- Graphs ---

    val GRAPH_BFS = InterviewExercise(
        className = "GraphBFS",
        source = """
            import java.util.*;

            public class GraphBFS {
                static List<Integer> bfs(Map<Integer, List<Integer>> graph, int start) {
                    List<Integer> order = new ArrayList<>();
                    Set<Integer> visited = new HashSet<>();
                    Queue<Integer> queue = new ArrayDeque<>();
                    queue.add(start);
                    visited.add(start);
                    while (!queue.isEmpty()) {
                        int node = queue.poll();
                        order.add(node);
                        for (int next : graph.getOrDefault(node, Collections.emptyList())) {
                            if (visited.add(next)) queue.add(next);
                        }
                    }
                    return order;
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
        expectedOutput = "[1, 2, 3, 4]"
    )

    val GRAPH_DFS = InterviewExercise(
        className = "GraphDFS",
        source = """
            import java.util.*;

            public class GraphDFS {
                static void dfs(Map<Integer, List<Integer>> graph, int node, Set<Integer> visited, List<Integer> order) {
                    if (!visited.add(node)) return;
                    order.add(node);
                    for (int next : graph.getOrDefault(node, Collections.emptyList())) {
                        dfs(graph, next, visited, order);
                    }
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
        expectedOutput = "[1, 2, 4, 3]"
    )

    // --- Sorting & searching ---

    val MERGE_SORT = InterviewExercise(
        className = "MergeSort",
        source = """
            import java.util.Arrays;

            public class MergeSort {
                static void sort(int[] arr, int lo, int hi) {
                    if (hi - lo <= 1) return;
                    int mid = (lo + hi) / 2;
                    sort(arr, lo, mid);
                    sort(arr, mid, hi);
                    int[] merged = new int[hi - lo];
                    int i = lo, j = mid, k = 0;
                    while (i < mid && j < hi) merged[k++] = (arr[i] <= arr[j]) ? arr[i++] : arr[j++];
                    while (i < mid) merged[k++] = arr[i++];
                    while (j < hi) merged[k++] = arr[j++];
                    System.arraycopy(merged, 0, arr, lo, merged.length);
                }
                public static void main(String[] args) {
                    int[] arr = {5, 2, 9, 1, 5, 6};
                    sort(arr, 0, arr.length);
                    System.out.println(Arrays.toString(arr));
                }
            }
        """.trimIndent(),
        expectedOutput = "[1, 2, 5, 5, 6, 9]"
    )

    val SEARCH_INSERT_POSITION = InterviewExercise(
        className = "SearchInsertPosition",
        source = """
            public class SearchInsertPosition {
                static int searchInsert(int[] nums, int target) {
                    int lo = 0, hi = nums.length;
                    while (lo < hi) {
                        int mid = (lo + hi) / 2;
                        if (nums[mid] < target) lo = mid + 1; else hi = mid;
                    }
                    return lo;
                }
                public static void main(String[] args) {
                    int[] arr = {1, 3, 5, 6};
                    System.out.println(searchInsert(arr, 5));
                    System.out.println(searchInsert(arr, 2));
                    System.out.println(searchInsert(arr, 7));
                }
            }
        """.trimIndent(),
        expectedOutput = "2\n1\n4"
    )

    // --- Recursion & backtracking ---

    val FACTORIAL = InterviewExercise(
        className = "Factorial",
        source = """
            public class Factorial {
                static long factorial(int n) {
                    return n <= 1 ? 1 : n * factorial(n - 1);
                }
                public static void main(String[] args) {
                    System.out.println(factorial(0));
                    System.out.println(factorial(5));
                    System.out.println(factorial(10));
                }
            }
        """.trimIndent(),
        expectedOutput = "1\n120\n3628800"
    )

    val PERMUTATIONS = InterviewExercise(
        className = "Permutations",
        source = """
            import java.util.ArrayList;
            import java.util.Arrays;
            import java.util.List;

            public class Permutations {
                static void backtrack(List<List<Integer>> result, List<Integer> current, int[] nums, boolean[] used) {
                    if (current.size() == nums.length) {
                        result.add(new ArrayList<>(current));
                        return;
                    }
                    for (int i = 0; i < nums.length; i++) {
                        if (used[i]) continue;
                        used[i] = true;
                        current.add(nums[i]);
                        backtrack(result, current, nums, used);
                        current.remove(current.size() - 1);
                        used[i] = false;
                    }
                }
                public static void main(String[] args) {
                    int[] nums = {1, 2, 3};
                    List<List<Integer>> result = new ArrayList<>();
                    backtrack(result, new ArrayList<>(), nums, new boolean[nums.length]);
                    System.out.println(result);
                }
            }
        """.trimIndent(),
        expectedOutput = "[[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]]"
    )

    val SUBSETS = InterviewExercise(
        className = "Subsets",
        source = """
            import java.util.ArrayList;
            import java.util.List;

            public class Subsets {
                static void backtrack(List<List<Integer>> result, List<Integer> current, int[] nums, int start) {
                    result.add(new ArrayList<>(current));
                    for (int i = start; i < nums.length; i++) {
                        current.add(nums[i]);
                        backtrack(result, current, nums, i + 1);
                        current.remove(current.size() - 1);
                    }
                }
                public static void main(String[] args) {
                    int[] nums = {1, 2, 3};
                    List<List<Integer>> result = new ArrayList<>();
                    backtrack(result, new ArrayList<>(), nums, 0);
                    System.out.println(result);
                }
            }
        """.trimIndent(),
        expectedOutput = "[[], [1], [1, 2], [1, 2, 3], [1, 3], [2], [2, 3], [3]]"
    )

    // --- Dynamic programming ---

    val CLIMBING_STAIRS = InterviewExercise(
        className = "ClimbingStairs",
        source = """
            public class ClimbingStairs {
                static int climbStairs(int n) {
                    if (n <= 2) return n;
                    int a = 1, b = 2;
                    for (int i = 3; i <= n; i++) {
                        int c = a + b;
                        a = b;
                        b = c;
                    }
                    return b;
                }
                public static void main(String[] args) {
                    System.out.println(climbStairs(2));
                    System.out.println(climbStairs(5));
                    System.out.println(climbStairs(10));
                }
            }
        """.trimIndent(),
        expectedOutput = "2\n8\n89"
    )

    val COIN_CHANGE = InterviewExercise(
        className = "CoinChange",
        source = """
            import java.util.Arrays;

            public class CoinChange {
                static int coinChange(int[] coins, int amount) {
                    int[] dp = new int[amount + 1];
                    Arrays.fill(dp, amount + 1);
                    dp[0] = 0;
                    for (int i = 1; i <= amount; i++) {
                        for (int coin : coins) {
                            if (coin <= i) dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                        }
                    }
                    return dp[amount] > amount ? -1 : dp[amount];
                }
                public static void main(String[] args) {
                    System.out.println(coinChange(new int[]{1, 2, 5}, 11));
                    System.out.println(coinChange(new int[]{2}, 3));
                }
            }
        """.trimIndent(),
        expectedOutput = "3\n-1"
    )

    // --- Bit manipulation ---

    val SINGLE_NUMBER = InterviewExercise(
        className = "SingleNumber",
        source = """
            public class SingleNumber {
                static int singleNumber(int[] nums) {
                    int result = 0;
                    for (int n : nums) result ^= n;
                    return result;
                }
                public static void main(String[] args) {
                    System.out.println(singleNumber(new int[]{2, 2, 1}));
                    System.out.println(singleNumber(new int[]{4, 1, 2, 1, 2}));
                }
            }
        """.trimIndent(),
        expectedOutput = "1\n4"
    )

    val ALL = listOf(
        FIZZ_BUZZ, FIBONACCI, TWO_SUM, IS_PALINDROME, BINARY_SEARCH, GROUP_ANAGRAMS,
        MAX_SUB_ARRAY, REVERSE_STRING, VALID_ANAGRAM, CONTAINS_DUPLICATE, MOVE_ZEROES,
        REVERSE_LINKED_LIST, MERGE_TWO_SORTED_LISTS, LINKED_LIST_HAS_CYCLE,
        VALID_PARENTHESES, MIN_STACK,
        TREE_INORDER_TRAVERSAL, TREE_MAX_DEPTH, IS_SAME_TREE, TREE_LEVEL_ORDER,
        GRAPH_BFS, GRAPH_DFS,
        MERGE_SORT, SEARCH_INSERT_POSITION,
        FACTORIAL, PERMUTATIONS, SUBSETS,
        CLIMBING_STAIRS, COIN_CHANGE,
        SINGLE_NUMBER
    )
}
