package com.javaide.mobile.compiler

/** Groups [InterviewExercises.ALL] the same way the file's own section comments already do. */
object PracticeCategories {

    data class Category(val title: String, val exercises: List<InterviewExercise>)

    val ALL: List<Category> = listOf(
        Category(
            "Fundamentals",
            listOf(
                InterviewExercises.FIZZ_BUZZ,
                InterviewExercises.FIBONACCI,
                InterviewExercises.TWO_SUM,
                InterviewExercises.IS_PALINDROME,
                InterviewExercises.BINARY_SEARCH,
                InterviewExercises.GROUP_ANAGRAMS
            )
        ),
        Category(
            "Arrays & Strings",
            listOf(
                InterviewExercises.MAX_SUB_ARRAY,
                InterviewExercises.REVERSE_STRING,
                InterviewExercises.VALID_ANAGRAM,
                InterviewExercises.CONTAINS_DUPLICATE,
                InterviewExercises.MOVE_ZEROES
            )
        ),
        Category(
            "Linked Lists",
            listOf(
                InterviewExercises.REVERSE_LINKED_LIST,
                InterviewExercises.MERGE_TWO_SORTED_LISTS,
                InterviewExercises.LINKED_LIST_HAS_CYCLE
            )
        ),
        Category(
            "Stacks & Queues",
            listOf(InterviewExercises.VALID_PARENTHESES, InterviewExercises.MIN_STACK)
        ),
        Category(
            "Trees",
            listOf(
                InterviewExercises.TREE_INORDER_TRAVERSAL,
                InterviewExercises.TREE_MAX_DEPTH,
                InterviewExercises.IS_SAME_TREE,
                InterviewExercises.TREE_LEVEL_ORDER
            )
        ),
        Category("Graphs", listOf(InterviewExercises.GRAPH_BFS, InterviewExercises.GRAPH_DFS)),
        Category(
            "Sorting & Searching",
            listOf(InterviewExercises.MERGE_SORT, InterviewExercises.SEARCH_INSERT_POSITION)
        ),
        Category(
            "Recursion & Backtracking",
            listOf(InterviewExercises.FACTORIAL, InterviewExercises.PERMUTATIONS, InterviewExercises.SUBSETS)
        ),
        Category(
            "Dynamic Programming",
            listOf(InterviewExercises.CLIMBING_STAIRS, InterviewExercises.COIN_CHANGE)
        ),
        Category("Bit Manipulation", listOf(InterviewExercises.SINGLE_NUMBER))
    )

    private val camelBoundary = Regex("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")

    /** "GraphBFS" -> "Graph BFS", "IsSameTree" -> "Is Same Tree". */
    fun displayTitle(exercise: InterviewExercise): String =
        exercise.className.replace(camelBoundary, " ")

    fun find(className: String): InterviewExercise? =
        ALL.asSequence().flatMap { it.exercises }.firstOrNull { it.className == className }
}
