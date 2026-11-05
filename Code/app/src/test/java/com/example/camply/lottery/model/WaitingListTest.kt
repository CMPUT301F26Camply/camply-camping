package com.example.camply.lottery.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WaitingListTest {
    @Test
    fun duplicateJoinDoesNotUseAnotherPlace() {
        val list = WaitingList("lottery", capacity = 2)
        assertEquals(WaitingList.JoinResult.JOINED, list.join("one", true))
        assertEquals(WaitingList.JoinResult.ALREADY_JOINED, list.join("one", true))
        assertEquals(WaitingList.JoinResult.JOINED, list.join("two", true))
        assertEquals(setOf("one", "two"), list.getEntrantIds())
    }

    @Test
    fun fullListRejectsEntryWithoutChangingMembership() {
        val list = WaitingList("lottery", setOf("one"), capacity = 1)
        assertEquals(WaitingList.JoinResult.FULL, list.join("two", true))
        assertFalse(list.contains("two"))
    }

    @Test
    fun closedRegistrationRejectsEntry() {
        val list = WaitingList("lottery")
        assertEquals(WaitingList.JoinResult.REGISTRATION_CLOSED, list.join("one", false))
        assertEquals(emptySet<String>(), list.getEntrantIds())
    }

    @Test
    fun omittedCapacityAllowsAdditionalEntrants() {
        val list = WaitingList("lottery", setOf("one"))
        assertEquals(WaitingList.JoinResult.JOINED, list.join("two", true))
    }

    @Test
    fun zeroCapacityRejectsEntry() {
        val list = WaitingList("lottery", capacity = 0)
        assertEquals(WaitingList.JoinResult.FULL, list.join("one", true))
    }
}
