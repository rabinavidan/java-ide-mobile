package com.javaide.mobile.compiler

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

/**
 * Runs classic coding-interview-style exercises through the full on-device pipeline
 * (compile -> dex -> execute) and checks the captured output against a known-correct value,
 * using the same AndroidJarProvider/JavaCompiler/Dexer/JavaRunner path the app itself uses.
 */
@RunWith(AndroidJUnit4::class)
class InterviewExercisesRunTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var androidJar: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        androidJar = AndroidJarProvider.get(context)
    }

    @Test fun fizzBuzz() = assertRunsWithExpectedOutput(InterviewExercises.FIZZ_BUZZ)

    @Test fun fibonacci() = assertRunsWithExpectedOutput(InterviewExercises.FIBONACCI)

    @Test fun twoSum() = assertRunsWithExpectedOutput(InterviewExercises.TWO_SUM)

    @Test fun isPalindrome() = assertRunsWithExpectedOutput(InterviewExercises.IS_PALINDROME)

    @Test fun binarySearch() = assertRunsWithExpectedOutput(InterviewExercises.BINARY_SEARCH)

    @Test fun groupAnagrams() = assertRunsWithExpectedOutput(InterviewExercises.GROUP_ANAGRAMS)

    @Test fun maxSubArray() = assertRunsWithExpectedOutput(InterviewExercises.MAX_SUB_ARRAY)

    @Test fun reverseString() = assertRunsWithExpectedOutput(InterviewExercises.REVERSE_STRING)

    @Test fun validAnagram() = assertRunsWithExpectedOutput(InterviewExercises.VALID_ANAGRAM)

    @Test fun containsDuplicate() = assertRunsWithExpectedOutput(InterviewExercises.CONTAINS_DUPLICATE)

    @Test fun moveZeroes() = assertRunsWithExpectedOutput(InterviewExercises.MOVE_ZEROES)

    @Test fun reverseLinkedList() = assertRunsWithExpectedOutput(InterviewExercises.REVERSE_LINKED_LIST)

    @Test fun mergeTwoSortedLists() = assertRunsWithExpectedOutput(InterviewExercises.MERGE_TWO_SORTED_LISTS)

    @Test fun linkedListHasCycle() = assertRunsWithExpectedOutput(InterviewExercises.LINKED_LIST_HAS_CYCLE)

    @Test fun validParentheses() = assertRunsWithExpectedOutput(InterviewExercises.VALID_PARENTHESES)

    @Test fun minStack() = assertRunsWithExpectedOutput(InterviewExercises.MIN_STACK)

    @Test fun treeInorderTraversal() = assertRunsWithExpectedOutput(InterviewExercises.TREE_INORDER_TRAVERSAL)

    @Test fun treeMaxDepth() = assertRunsWithExpectedOutput(InterviewExercises.TREE_MAX_DEPTH)

    @Test fun isSameTree() = assertRunsWithExpectedOutput(InterviewExercises.IS_SAME_TREE)

    @Test fun treeLevelOrder() = assertRunsWithExpectedOutput(InterviewExercises.TREE_LEVEL_ORDER)

    @Test fun graphBFS() = assertRunsWithExpectedOutput(InterviewExercises.GRAPH_BFS)

    @Test fun graphDFS() = assertRunsWithExpectedOutput(InterviewExercises.GRAPH_DFS)

    @Test fun mergeSort() = assertRunsWithExpectedOutput(InterviewExercises.MERGE_SORT)

    @Test fun searchInsertPosition() = assertRunsWithExpectedOutput(InterviewExercises.SEARCH_INSERT_POSITION)

    @Test fun factorial() = assertRunsWithExpectedOutput(InterviewExercises.FACTORIAL)

    @Test fun permutations() = assertRunsWithExpectedOutput(InterviewExercises.PERMUTATIONS)

    @Test fun subsets() = assertRunsWithExpectedOutput(InterviewExercises.SUBSETS)

    @Test fun climbingStairs() = assertRunsWithExpectedOutput(InterviewExercises.CLIMBING_STAIRS)

    @Test fun coinChange() = assertRunsWithExpectedOutput(InterviewExercises.COIN_CHANGE)

    @Test fun singleNumber() = assertRunsWithExpectedOutput(InterviewExercises.SINGLE_NUMBER)

    private fun assertRunsWithExpectedOutput(exercise: InterviewExercise) {
        val projectDir = tempFolder.newFolder(exercise.className)
        val javaDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(javaDir, "${exercise.className}.java").writeText(exercise.source)

        val classesDir = File(projectDir, "classes")
        val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar)
        assertTrue("Compilation failed for ${exercise.className}:\n${compileResult.log}", compileResult.success)

        val dexDir = File(projectDir, "dex")
        val dexResult = Dexer.dex(classesDir, dexDir, androidJar)
        assertTrue("Dexing failed for ${exercise.className}:\n${dexResult.log}", dexResult.success)

        val optimizedDir = File(projectDir, "dex-opt")
        val runResult = JavaRunner.run(classesDir, File(dexDir, "classes.dex"), optimizedDir)
        assertTrue("Run failed for ${exercise.className}:\n${runResult.output}", runResult.success)
        assertEquals(exercise.expectedOutput, runResult.output.trim())
    }
}
