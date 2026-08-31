package com.javaide.mobile.practice.model

/**
 * In-memory attempt state for one exercise-editing session (Milestone 5) — whether the user has
 * revealed the reference solution and how many hints they've asked for, so [isIndependentSolve]
 * can tell a "solved with help" pass from a genuinely independent one. Deliberately just a plain
 * data holder with no persistence: saving/restoring attempt history across app restarts is
 * Progress Tracking's job (Milestone 15), not this one.
 */
data class ChallengeAttempt(
    val exerciseId: String,
    val solutionRevealed: Boolean = false,
    val hintsRevealed: Int = 0
) {
    fun revealSolution(): ChallengeAttempt = copy(solutionRevealed = true)

    fun revealNextHint(): ChallengeAttempt = copy(hintsRevealed = hintsRevealed + 1)
}

/**
 * A challenge only counts as independently solved when it passed *and* the reference solution
 * was never revealed during the attempt that passed it. Revealing hints doesn't disqualify a
 * solve — only viewing the solution does, per the plan's own distinction between "solved with
 * help" and "solved independently".
 */
fun isIndependentSolve(attempt: ChallengeAttempt, passed: Boolean): Boolean =
    passed && !attempt.solutionRevealed

/**
 * Whether resetting the editor to [starterCode] (or replacing it with the solution) would
 * discard changes the user actually made. A UI layer can skip the "are you sure?" confirmation
 * when this is false — there's nothing to lose.
 */
fun wouldDiscardEdits(currentCode: String, starterCode: String): Boolean =
    currentCode != starterCode
