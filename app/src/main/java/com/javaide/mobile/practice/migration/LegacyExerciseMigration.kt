package com.javaide.mobile.practice.migration

import com.javaide.mobile.compiler.PracticeCategories
import com.javaide.mobile.practice.model.Difficulty
import com.javaide.mobile.practice.model.ExerciseExample
import com.javaide.mobile.practice.model.ExerciseTestCase
import com.javaide.mobile.practice.model.InterviewExercise

/**
 * Converts the existing 30 exercises ([com.javaide.mobile.compiler.InterviewExercises], grouped
 * by [PracticeCategories]) into the V2 [InterviewExercise] model (Milestone 2), so the new model
 * and [com.javaide.mobile.practice.validation.ExerciseValidator] can be exercised against real
 * data ahead of the catalog/runner/UI cutover (Milestones 3-5). Purely additive: nothing here is
 * wired into the app yet, and the legacy model/UI keep working unchanged.
 *
 * The legacy model has no title/description/examples/hints/complexity/difficulty data, so those
 * fields are filled with clearly-labeled placeholders derived from what *is* available (the
 * class name and the single expected-output string) rather than fabricated content. Real content
 * authoring for these 30 — and the other 70 the plan calls for — is later milestones' job
 * (6-11 for new content, 20 for a full quality review); this adapter's job is just to prove the
 * new model can represent the old data without losing anything, not to write copy.
 *
 * IDs are generated mechanically as `<categoryId>-<class-name-kebab-case>` for guaranteed
 * uniqueness across all 30. This differs from the illustrative hand-picked IDs in the plan
 * (`arrays-two-sum`, `tree-max-depth`, ...) — those read as curated shorthand for *new*,
 * individually-authored content, not something a bulk migration should try to reverse-engineer.
 */
object LegacyExerciseMigration {

    /** Placeholder pending real content authoring — see class doc. */
    private val PLACEHOLDER_DIFFICULTY = Difficulty.MEDIUM
    private const val PLACEHOLDER_ESTIMATED_MINUTES = 20
    private const val PLACEHOLDER_COMPLEXITY = "Not yet documented (migrated from the legacy catalog)"

    fun migrateAll(): List<InterviewExercise> =
        PracticeCategories.ALL.flatMap { category ->
            val categoryId = categoryIdFor(category.title)
            category.exercises.map { legacy -> migrate(legacy, categoryId, category.title) }
        }

    /**
     * The category id a legacy category title slugifies to, e.g. "Arrays & Strings" ->
     * "arrays-strings". Exposed so the per-topic catalog files (Milestone 3,
     * `com.javaide.mobile.practice.catalog`) can derive the same id used here instead of
     * hardcoding (and risking a typo'd duplicate of) the slug themselves.
     */
    fun categoryIdFor(categoryTitle: String): String = slugify(categoryTitle)

    fun migrate(
        legacy: com.javaide.mobile.compiler.InterviewExercise,
        categoryId: String,
        categoryTitle: String
    ): InterviewExercise {
        val title = PracticeCategories.displayTitle(legacy)

        return InterviewExercise(
            id = "$categoryId-${slugify(legacy.className)}",
            title = title,
            className = legacy.className,
            categoryId = categoryId,
            difficulty = PLACEHOLDER_DIFFICULTY,
            description = "Migrated from the original \"$categoryTitle\" practice catalog. " +
                "Study the reference solution below; a full problem statement has not been " +
                "authored for this exercise yet.",
            constraints = emptyList(),
            examples = listOf(
                ExerciseExample(
                    input = "(no structured input — see the reference solution's own main())",
                    output = legacy.expectedOutput,
                    explanation = "This is the exact console output the original exercise checked for."
                )
            ),
            // Starter/solution separation (Milestone 5) hasn't happened for legacy content yet,
            // so both fields hold the same known-correct source for now.
            starterCode = legacy.source,
            solutionCode = legacy.source,
            hints = emptyList(),
            testCases = listOf(
                ExerciseTestCase(
                    id = "legacy-output",
                    input = "",
                    expectedOutput = legacy.expectedOutput,
                    visible = true,
                    description = "Migrated from the legacy single exact-output check."
                )
            ),
            timeComplexity = PLACEHOLDER_COMPLEXITY,
            spaceComplexity = PLACEHOLDER_COMPLEXITY,
            patterns = emptySet(),
            tags = setOf("legacy-migration"),
            estimatedMinutes = PLACEHOLDER_ESTIMATED_MINUTES
        )
    }

    /** "Arrays & Strings" -> "arrays-strings", "MaxSubArray" -> "max-sub-array". */
    private fun slugify(text: String): String {
        val camelSplit = text.replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), "-")
        val normalized = camelSplit
            .replace("&", " and ")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return normalized
    }
}
