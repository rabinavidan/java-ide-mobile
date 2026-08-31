package com.javaide.mobile.practice.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeAttemptTest {

    @Test
    fun `a fresh attempt has no solution reveal and no hints revealed`() {
        val attempt = ChallengeAttempt(exerciseId = "ex-1")
        assertFalse(attempt.solutionRevealed)
        assertEquals(0, attempt.hintsRevealed)
    }

    @Test
    fun `revealSolution sets the flag without affecting hint count`() {
        val attempt = ChallengeAttempt(exerciseId = "ex-1", hintsRevealed = 1)
        val revealed = attempt.revealSolution()
        assertTrue(revealed.solutionRevealed)
        assertEquals(1, revealed.hintsRevealed)
    }

    @Test
    fun `revealNextHint increments the hint count without revealing the solution`() {
        val attempt = ChallengeAttempt(exerciseId = "ex-1")
        val afterOneHint = attempt.revealNextHint()
        val afterTwoHints = afterOneHint.revealNextHint()

        assertEquals(1, afterOneHint.hintsRevealed)
        assertEquals(2, afterTwoHints.hintsRevealed)
        assertFalse(afterTwoHints.solutionRevealed)
    }

    // --- isIndependentSolve ---

    @Test
    fun `a pass without revealing the solution is an independent solve`() {
        val attempt = ChallengeAttempt(exerciseId = "ex-1")
        assertTrue(isIndependentSolve(attempt, passed = true))
    }

    @Test
    fun `a pass after revealing the solution is not an independent solve`() {
        val attempt = ChallengeAttempt(exerciseId = "ex-1").revealSolution()
        assertFalse(isIndependentSolve(attempt, passed = true))
    }

    @Test
    fun `revealing hints alone does not disqualify an independent solve`() {
        val attempt = ChallengeAttempt(exerciseId = "ex-1").revealNextHint().revealNextHint()
        assertTrue(isIndependentSolve(attempt, passed = true))
    }

    @Test
    fun `a failing run is never an independent solve, revealed or not`() {
        assertFalse(isIndependentSolve(ChallengeAttempt(exerciseId = "ex-1"), passed = false))
        assertFalse(isIndependentSolve(ChallengeAttempt(exerciseId = "ex-1").revealSolution(), passed = false))
    }

    // --- wouldDiscardEdits ---

    @Test
    fun `wouldDiscardEdits is false when the code still matches the starter`() {
        assertFalse(wouldDiscardEdits(currentCode = "public class X {}", starterCode = "public class X {}"))
    }

    @Test
    fun `wouldDiscardEdits is true once the user has changed anything`() {
        assertTrue(wouldDiscardEdits(currentCode = "public class X { /* edited */ }", starterCode = "public class X {}"))
    }
}
