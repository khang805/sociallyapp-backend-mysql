package com.example.socially

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.filters.LargeTest

@RunWith(AndroidJUnit4::class)
@LargeTest
class FeedUITest {

    // ✅ Test 1: Login Screen Verification
    @Test
    fun testLoginFlow() {
        // Launch LoginActivity with proper class reference
        ActivityScenario.launch(Login_Username_Password::class.java)

        // Wait for UI to load
        Thread.sleep(2000)

        // Enter email and password
        onView(withId(R.id.username))
            .perform(typeText("f@gmail.com"), closeSoftKeyboard())
        onView(withId(R.id.password))
            .perform(typeText("123456"), closeSoftKeyboard())

        // Click login button
        onView(withId(R.id.loginButton)).perform(click())

        // Wait for login process
        Thread.sleep(3000)
    }

    // ✅ Test 2: Feed Display Verification
    @Test
    fun testFeedDisplay() {
        // Launch FeedActivity
        ActivityScenario.launch(Main_feed::class.java)

        // Wait for data to load
        Thread.sleep(2000)

        // Verify RecyclerView is displayed
        onView(withId(R.id.recyclerViewPosts)).check(matches(isDisplayed()))

        // Check if any item in RecyclerView is displayed
        onView(withId(R.id.recyclerViewPosts))
            .check(matches(hasMinimumChildCount(1)))
    }
}