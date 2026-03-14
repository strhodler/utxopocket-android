package com.strhodler.utxopocket.presentation

import com.strhodler.utxopocket.domain.model.PinVerificationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class MainActivityViewModelDuressUnlockTest {

    @Test
    fun duressTriggeredKeepsLockUntilFakeSessionIsReady() = runTest {
        val events = mutableListOf<String>()

        val result = executePinUnlockFlow(
            pinEnabled = true,
            duressAlreadyActive = false,
            verifyPin = {
                events += "verifyPin"
                PinVerificationResult.DuressTriggered(decoyBalanceSats = 42L)
            },
            verifyPinIgnoringDuress = {
                events += "verifyPinIgnoringDuress"
                PinVerificationResult.Success
            },
            markPinUnlocked = {
                events += "markPinUnlocked"
            },
            setAppLocked = { locked ->
                events += "setAppLocked:$locked"
            },
            setDuressUnlockInProgress = { inProgress ->
                events += "duressUnlockInProgress:$inProgress"
            },
            activateFake = { decoyBalanceSats ->
                events += "activateFake:$decoyBalanceSats"
            },
            awaitFakeActivation = {
                events += "awaitFakeActivation"
            }
        )

        assertEquals(PinVerificationResult.Success, result)
        assertEquals(
            listOf(
                "verifyPin",
                "duressUnlockInProgress:true",
                "markPinUnlocked",
                "activateFake:42",
                "awaitFakeActivation",
                "setAppLocked:false",
                "duressUnlockInProgress:false"
            ),
            events
        )
    }

    @Test
    fun duressUnlockInProgressAlwaysClearsEvenIfActivationWaitFails() = runTest {
        val events = mutableListOf<String>()

        runCatching {
            executePinUnlockFlow(
                pinEnabled = true,
                duressAlreadyActive = false,
                verifyPin = { PinVerificationResult.DuressTriggered(decoyBalanceSats = 0L) },
                verifyPinIgnoringDuress = { PinVerificationResult.Success },
                markPinUnlocked = { events += "markPinUnlocked" },
                setAppLocked = { locked -> events += "setAppLocked:$locked" },
                setDuressUnlockInProgress = { inProgress ->
                    events += "duressUnlockInProgress:$inProgress"
                },
                activateFake = { events += "activateFake" },
                awaitFakeActivation = {
                    events += "awaitFakeActivation"
                    error("boom")
                }
            )
        }

        assertEquals(
            listOf(
                "duressUnlockInProgress:true",
                "markPinUnlocked",
                "activateFake",
                "awaitFakeActivation",
                "duressUnlockInProgress:false"
            ),
            events
        )
    }

    @Test
    fun normalUnlockStillMarksUnlockedAndClearsLock() = runTest {
        val events = mutableListOf<String>()

        val result = executePinUnlockFlow(
            pinEnabled = true,
            duressAlreadyActive = false,
            verifyPin = {
                events += "verifyPin"
                PinVerificationResult.Success
            },
            verifyPinIgnoringDuress = {
                events += "verifyPinIgnoringDuress"
                PinVerificationResult.Success
            },
            markPinUnlocked = {
                events += "markPinUnlocked"
            },
            setAppLocked = { locked ->
                events += "setAppLocked:$locked"
            },
            setDuressUnlockInProgress = { inProgress ->
                events += "duressUnlockInProgress:$inProgress"
            },
            activateFake = {
                events += "activateFake"
            },
            awaitFakeActivation = {
                events += "awaitFakeActivation"
            }
        )

        assertEquals(PinVerificationResult.Success, result)
        assertEquals(
            listOf(
                "verifyPin",
                "markPinUnlocked",
                "setAppLocked:false"
            ),
            events
        )
    }

    @Test
    fun failedVerificationDoesNotUnlockOrActivateDuress() = runTest {
        val events = mutableListOf<String>()

        val result = executePinUnlockFlow(
            pinEnabled = true,
            duressAlreadyActive = false,
            verifyPin = {
                events += "verifyPin"
                PinVerificationResult.Incorrect(attempts = 1, lockDurationMillis = 30_000L)
            },
            verifyPinIgnoringDuress = {
                events += "verifyPinIgnoringDuress"
                PinVerificationResult.Success
            },
            markPinUnlocked = {
                events += "markPinUnlocked"
            },
            setAppLocked = { locked ->
                events += "setAppLocked:$locked"
            },
            setDuressUnlockInProgress = { inProgress ->
                events += "duressUnlockInProgress:$inProgress"
            },
            activateFake = {
                events += "activateFake"
            },
            awaitFakeActivation = {
                events += "awaitFakeActivation"
            }
        )

        assertEquals(
            PinVerificationResult.Incorrect(attempts = 1, lockDurationMillis = 30_000L),
            result
        )
        assertEquals(listOf("verifyPin"), events)
    }
}
