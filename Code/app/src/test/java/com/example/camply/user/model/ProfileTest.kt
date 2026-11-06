package com.example.camply.user.model

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for profile information and validation (US 01.07.03).
 */
class ProfileTest {

    private lateinit var profile: Profile

    /**
     * Creates a profile with existing information before each test.
     */
    @Before
    fun setUp() {
        profile = Profile("Alex", "alex@example.com", "7801234567")
    }

    /**
     * Checks that the profile stores the provided personal information.
     */
    @Test
    fun getProfileInformation_validProfile_returnsInformation() {
        assertEquals("Alex", profile.getName())
        assertEquals("alex@example.com", profile.getEmail())
        assertEquals("7801234567", profile.getPhoneNumber())
    }

    /**
     * Checks that updating the name saves the trimmed value.
     */
    @Test
    fun updateName_validName_updatesName() {
        assertTrue(profile.updateName(" Alex Smith "))
        assertEquals("Alex Smith", profile.getName())
    }

    /**
     * Checks that a blank name is rejected without changing the existing name.
     */
    @Test
    fun updateName_blankName_keepsExistingName() {
        assertFalse(profile.updateName("   "))
        assertEquals("Alex", profile.getName())
    }

    /**
     * Checks that updating the email saves the trimmed value.
     */
    @Test
    fun updateEmail_validEmail_updatesEmail() {
        assertTrue(profile.updateEmail(" alex.smith@example.com "))
        assertEquals("alex.smith@example.com", profile.getEmail())
    }

    /**
     * Checks that an invalid email is rejected without changing the existing email.
     */
    @Test
    fun updateEmail_invalidEmail_keepsExistingEmail() {
        assertFalse(profile.updateEmail("invalidemail"))
        assertEquals("alex@example.com", profile.getEmail())
    }

    /**
     * Checks that updating the phone number saves the trimmed value.
     */
    @Test
    fun updatePhoneNumber_validPhoneNumber_updatesPhoneNumber() {
        assertTrue(profile.updatePhoneNumber(" 7805551234 "))
        assertEquals("7805551234", profile.getPhoneNumber())
    }

    /**
     * Checks that a blank phone number is rejected without changing the existing number.
     */
    @Test
    fun updatePhoneNumber_blankPhoneNumber_keepsExistingPhoneNumber() {
        assertFalse(profile.updatePhoneNumber("   "))
        assertEquals("7801234567", profile.getPhoneNumber())
    }
}