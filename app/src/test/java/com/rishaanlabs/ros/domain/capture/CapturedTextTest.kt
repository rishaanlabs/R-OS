package com.rishaanlabs.ros.domain.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturedTextTest {

    @Test
    fun `a short single line is the title and nothing else`() {
        val result = splitCapturedText("Call the bank")
        assertEquals("Call the bank", result.proposedTitle)
        assertEquals("", result.body)
        assertFalse(result.titleNeedsConfirmation)
    }

    @Test
    fun `a long single line keeps its tail in the body instead of losing it`() {
        val line = "Ask the landlord about the lease renewal and whether the deposit rolls over " +
            "into the new term or has to be paid again from scratch"
        val result = splitCapturedText(line)

        assertTrue("title should be trimmed to something readable", result.proposedTitle.length <= 80)
        assertFalse("no word should be cut in half", result.proposedTitle.endsWith(" "))
        assertEquals(
            "every word must survive somewhere",
            line.split(" ").size,
            (result.proposedTitle + " " + result.body).trim().split(" ").size
        )
    }

    @Test
    fun `the reported case - heading plus list items`() {
        val result = splitCapturedText("Shopping\nMilk\nEggs\nBread")

        assertEquals("Shopping", result.proposedTitle)
        assertEquals("Milk\nEggs\nBread", result.body)
        assertEquals(listOf("Shopping", "Milk", "Eggs", "Bread"), result.items)
    }

    @Test
    fun `a bare list does not silently become a task called Milk`() {
        val result = splitCapturedText("Milk\nEggs\nBread")

        assertTrue(
            "a list with no heading must be confirmed, not assumed",
            result.titleNeedsConfirmation
        )
        assertEquals(
            "retitling means the first line was an item, so it stays",
            "Milk\nEggs\nBread",
            result.bodyFor("Shopping list")
        )
    }

    @Test
    fun `keeping the first line as the title drops it from the body`() {
        val result = splitCapturedText("Shopping\nMilk\nEggs\nBread")
        assertEquals("Milk\nEggs\nBread", result.bodyFor("Shopping"))
    }

    @Test
    fun `an explicit heading stays consumed even when retitled`() {
        val result = splitCapturedText("Shopping:\nMilk\nEggs")
        assertEquals(
            "a heading was never an item, so renaming it must not resurrect it",
            "Milk\nEggs",
            result.bodyFor("Groceries")
        )
    }

    @Test
    fun `no line is ever lost, whichever title is chosen`() {
        val result = splitCapturedText("Milk\nEggs\nBread")
        listOf("Milk", "Shopping list", "Anything else").forEach { title ->
            val kept = (title + "\n" + result.bodyFor(title))
            listOf("Milk", "Eggs", "Bread").forEach { item ->
                assertTrue("$item disappeared when titled '$title'", kept.contains(item))
            }
        }
    }

    @Test
    fun `a recognisable list is offered a better title than its first item`() {
        val result = splitCapturedText("Milk\nEggs\nBread")
        assertEquals("Shopping list", result.suggestedAlternativeTitle)
    }

    @Test
    fun `an unrecognisable list is not given an invented title`() {
        val result = splitCapturedText("Rashid\nNasma\nAhmed")
        assertNull(result.suggestedAlternativeTitle)
        assertTrue(result.titleNeedsConfirmation)
    }

    @Test
    fun `a trailing colon is a heading the user actually wrote`() {
        val result = splitCapturedText("Shopping:\nMilk\nEggs")

        assertEquals("Shopping", result.proposedTitle)
        assertFalse("an explicit heading needs no confirmation", result.titleNeedsConfirmation)
        assertEquals("Milk\nEggs", result.body)
    }

    @Test
    fun `bulleted lines under a plain line make that line the heading`() {
        val result = splitCapturedText("Trip packing\n- passport\n- charger\n- adapter")

        assertEquals("Trip packing", result.proposedTitle)
        assertFalse(result.titleNeedsConfirmation)
        assertEquals("- passport\n- charger\n- adapter", result.body)
    }

    @Test
    fun `a bulleted first line is an item, not a heading`() {
        val result = splitCapturedText("- passport\n- charger\n- adapter")
        assertTrue(result.titleNeedsConfirmation)
    }

    @Test
    fun `a markdown heading is used without its hashes`() {
        val result = splitCapturedText("# Weekend plan\nfix the shelf\ncall Ahmed")
        assertEquals("Weekend plan", result.proposedTitle)
        assertFalse(result.titleNeedsConfirmation)
    }

    @Test
    fun `a sentence followed by short lines reads as a heading`() {
        val result = splitCapturedText("Things to sort out before Friday\nvisa\ntickets\nhotel")
        assertEquals("Things to sort out before Friday", result.proposedTitle)
        assertFalse(result.titleNeedsConfirmation)
    }

    @Test
    fun `blank lines between items are ignored but the items survive`() {
        val result = splitCapturedText("Shopping:\n\nMilk\n\n\nEggs\n")
        assertEquals("Shopping", result.proposedTitle)
        assertEquals(listOf("Shopping:", "Milk", "Eggs"), result.items)
        assertEquals("Milk\nEggs", result.body)
    }

    @Test
    fun `items are exposed so multiple tasks can be created later`() {
        val result = splitCapturedText("Shopping\nMilk\nEggs\nBread")
        assertTrue(result.looksLikeList)
        assertEquals(4, result.items.size)
    }

    @Test
    fun `empty input does not crash the processor`() {
        val result = splitCapturedText("   \n \n ")
        assertEquals("Untitled", result.proposedTitle)
        assertTrue(result.items.isEmpty())
    }
}
