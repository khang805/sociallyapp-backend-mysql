package com.example.socially

import com.example.socially.api.UserData
import org.junit.Assert
import org.junit.Test

class DmPageFilterTest {

    @Test
    fun filter_by_username_case_insensitive() {
        val users = listOf(
            UserData(1, "alice", null, null, true),
            UserData(2, "Bob", null, null, false),
            UserData(3, "carol", null, null, true)
        )

        val q1 = "bo"
        val res1 = users.filter { it.username.contains(q1, true) }
        Assert.assertEquals(1, res1.size)
        Assert.assertEquals("Bob", res1[0].username)

        val q2 = "AL"
        val res2 = users.filter { it.username.contains(q2, true) }
        Assert.assertEquals(1, res2.size)
        Assert.assertEquals("alice", res2[0].username)

        val q3 = ""
        val res3 = if (q3.isEmpty()) users else users.filter { it.username.contains(q3, true) }
        Assert.assertEquals(3, res3.size)
    }
}

