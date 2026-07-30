package com.javaide.mobile.compiler

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Verifies the compile -> dex steps of the pipeline against classic coding-interview-style
 * exercises. Runs as a plain JVM test (no device needed). Actually *executing* the exercises
 * needs a real Android runtime (DexClassLoader) and is covered by the instrumented
 * counterpart, InterviewExercisesRunTest.
 */
class InterviewExercisesCompileDexTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var androidJar: File

    @Before
    fun setUp() {
        val path = System.getProperty("android.jar.path")
            ?: error("android.jar.path system property not set; see app/build.gradle.kts")
        androidJar = File(path)
        check(androidJar.isFile) { "android.jar not found at $path" }
    }

    @Test fun fizzBuzz() = assertCompilesAndDexes(InterviewExercises.FIZZ_BUZZ)

    @Test fun fibonacci() = assertCompilesAndDexes(InterviewExercises.FIBONACCI)

    @Test fun twoSum() = assertCompilesAndDexes(InterviewExercises.TWO_SUM)

    @Test fun isPalindrome() = assertCompilesAndDexes(InterviewExercises.IS_PALINDROME)

    @Test fun binarySearch() = assertCompilesAndDexes(InterviewExercises.BINARY_SEARCH)

    @Test fun groupAnagrams() = assertCompilesAndDexes(InterviewExercises.GROUP_ANAGRAMS)

    @Test fun maxSubArray() = assertCompilesAndDexes(InterviewExercises.MAX_SUB_ARRAY)

    @Test fun reverseString() = assertCompilesAndDexes(InterviewExercises.REVERSE_STRING)

    @Test fun validAnagram() = assertCompilesAndDexes(InterviewExercises.VALID_ANAGRAM)

    @Test fun containsDuplicate() = assertCompilesAndDexes(InterviewExercises.CONTAINS_DUPLICATE)

    @Test fun moveZeroes() = assertCompilesAndDexes(InterviewExercises.MOVE_ZEROES)

    @Test fun reverseLinkedList() = assertCompilesAndDexes(InterviewExercises.REVERSE_LINKED_LIST)

    @Test fun mergeTwoSortedLists() = assertCompilesAndDexes(InterviewExercises.MERGE_TWO_SORTED_LISTS)

    @Test fun linkedListHasCycle() = assertCompilesAndDexes(InterviewExercises.LINKED_LIST_HAS_CYCLE)

    @Test fun validParentheses() = assertCompilesAndDexes(InterviewExercises.VALID_PARENTHESES)

    @Test fun minStack() = assertCompilesAndDexes(InterviewExercises.MIN_STACK)

    @Test fun treeInorderTraversal() = assertCompilesAndDexes(InterviewExercises.TREE_INORDER_TRAVERSAL)

    @Test fun treeMaxDepth() = assertCompilesAndDexes(InterviewExercises.TREE_MAX_DEPTH)

    @Test fun isSameTree() = assertCompilesAndDexes(InterviewExercises.IS_SAME_TREE)

    @Test fun treeLevelOrder() = assertCompilesAndDexes(InterviewExercises.TREE_LEVEL_ORDER)

    @Test fun graphBFS() = assertCompilesAndDexes(InterviewExercises.GRAPH_BFS)

    @Test fun graphDFS() = assertCompilesAndDexes(InterviewExercises.GRAPH_DFS)

    @Test fun mergeSort() = assertCompilesAndDexes(InterviewExercises.MERGE_SORT)

    @Test fun searchInsertPosition() = assertCompilesAndDexes(InterviewExercises.SEARCH_INSERT_POSITION)

    @Test fun factorial() = assertCompilesAndDexes(InterviewExercises.FACTORIAL)

    @Test fun permutations() = assertCompilesAndDexes(InterviewExercises.PERMUTATIONS)

    @Test fun subsets() = assertCompilesAndDexes(InterviewExercises.SUBSETS)

    @Test fun climbingStairs() = assertCompilesAndDexes(InterviewExercises.CLIMBING_STAIRS)

    @Test fun coinChange() = assertCompilesAndDexes(InterviewExercises.COIN_CHANGE)

    @Test fun singleNumber() = assertCompilesAndDexes(InterviewExercises.SINGLE_NUMBER)

    private fun assertCompilesAndDexes(exercise: InterviewExercise) {
        val projectDir = tempFolder.newFolder(exercise.className)
        val javaDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(javaDir, "${exercise.className}.java").writeText(exercise.source)

        val classesDir = File(projectDir, "classes")
        val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar)
        assertTrue("Compilation failed for ${exercise.className}:\n${compileResult.log}", compileResult.success)

        val dexDir = File(projectDir, "dex")
        val dexResult = Dexer.dex(classesDir, dexDir, androidJar)
        assertTrue("Dexing failed for ${exercise.className}:\n${dexResult.log}", dexResult.success)
        assertTrue(File(dexDir, "classes.dex").isFile)
    }
}
