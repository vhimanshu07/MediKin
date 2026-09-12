package com.medikin.tracker.data

import com.medikin.tracker.domain.Caregiver
import com.medikin.tracker.domain.DoseTime
import com.medikin.tracker.domain.Medicine
import com.medikin.tracker.domain.TrackerSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EncryptedBackupCodecTest {
    private val snapshot = TrackerSnapshot(
        medicines = listOf(
            Medicine("one", "Aspirin", "75 mg", "After food", listOf(DoseTime.MORNING), 9, 3),
        ),
        caregiver = Caregiver("Dad", "Neha", "+919999999999"),
    )

    @Test
    fun `encrypted backup round trips`() {
        val codec = EncryptedBackupCodec()

        val encoded = codec.encode(snapshot, "strong password".toCharArray())
        val decoded = codec.decode(encoded, "strong password".toCharArray())

        assertEquals(snapshot, decoded)
        assertFalse(encoded.toString(Charsets.UTF_8).contains("Aspirin"))
    }

    @Test(expected = Exception::class)
    fun `wrong password cannot decrypt backup`() {
        val codec = EncryptedBackupCodec()
        val encoded = codec.encode(snapshot, "strong password".toCharArray())

        codec.decode(encoded, "wrong password".toCharArray())
    }
}
