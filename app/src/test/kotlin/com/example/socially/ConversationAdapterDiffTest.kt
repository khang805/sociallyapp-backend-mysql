package com.example.socially

import com.example.socially.api.UserData
import org.junit.Assert
import org.junit.Test

class ConversationAdapterDiffTest {

    @Test
    fun userData_equality_and_diff() {
        val u1 = UserData(id = 1, username = "alice", profile_picture = "", bio = null, is_online = 1)
        val u2 = UserData(id = 1, username = "alice", profile_picture = "", bio = null, is_online = 1)
        val u3 = UserData(id = 2, username = "bob", profile_picture = null, bio = null, is_online = 0)

        // Data class equality
        Assert.assertEquals(u1, u2)
        Assert.assertNotEquals(u1, u3)

        // areItemsTheSame semantics (simulated)
        Assert.assertTrue(u1.id == u2.id)
        Assert.assertFalse(u1.id == u3.id)
    }
}

