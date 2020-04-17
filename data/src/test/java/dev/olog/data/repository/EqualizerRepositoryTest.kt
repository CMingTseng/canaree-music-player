package dev.olog.data.repository

import com.nhaarman.mockitokotlin2.*
import dev.olog.data.db.EqualizerPresetsDao
import dev.olog.data.model.db.EqualizerPresetEntity
import dev.olog.domain.entity.EqualizerPreset
import dev.olog.domain.prefs.EqualizerPreferencesGateway
import dev.olog.test.shared.MainCoroutineRule
import dev.olog.test.shared.runBlockingTest
import dev.olog.test.shared.schedulers
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import org.junit.Rule
import org.junit.Test

internal class EqualizerRepositoryTest {

    private val presetsEntities = listOf(
        EqualizerPresetEntity(
            1,
            "eq 1",
            emptyList(),
            false
        ),
        EqualizerPresetEntity(
            2,
            "eq 2",
            emptyList(),
            true
        )
    )

    private val domainPresets = listOf(
        EqualizerPreset(1, "eq 1", emptyList(), false),
        EqualizerPreset(2, "eq 2", emptyList(), true)
    )

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val dao = mock<EqualizerPresetsDao>()
    private val prefs = mock<EqualizerPreferencesGateway>()
    private val sut by lazy { EqualizerRepository(dao, prefs, coroutineRule.schedulers) }

    @Test
    fun verifySetup() = coroutineRule.runBlockingTest {
        whenever(dao.getPresets()).thenReturn(emptyList())
        sut.toString() // just to initialize

        verify(dao).insertPresets(*EqualizerDefaultPresets.createDefaultPresets().toTypedArray())
    }

    @Test
    fun verifyNoSetup() = coroutineRule.runBlockingTest {
        whenever(dao.getPresets()).thenReturn(presetsEntities)
        sut.toString() // just to initialize
        verify(dao, never()).insertPresets(any())
    }

    @Test
    fun testGetPresets() {
        // given
        whenever(dao.getPresets()).thenReturn(presetsEntities)

        // when
        val actual = sut.getPresets()

        // then
        assertEquals(
            domainPresets,
            actual
        )
    }

    @Test
    fun testGetCurrentPreset() {
        // given
        val presetId = 1L
        whenever(prefs.getCurrentPresetId()).thenReturn(presetId)
        whenever(dao.getPresetById(presetId)).thenReturn(presetsEntities[0])

        // when
        val actual = sut.getCurrentPreset()

        // then
        assertEquals(
            domainPresets[0],
            actual
        )
    }

    @Test
    fun testAddPresetSuccess() = coroutineRule.runBlockingTest {
        // given
        val preset = EqualizerPreset(-1, "new name", emptyList(), true)

        // when
        sut.addPreset(preset)

        // then
        val expected = EqualizerPresetEntity(
            3,
            "new name",
            emptyList(),
            true
        )
        verify(dao).insertPresets(expected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAddPresetShouldFailIllegalId() = coroutineRule.runBlockingTest {
        // given
        val preset = EqualizerPreset(1, "", emptyList(), true)

        // when
        sut.addPreset(preset)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAddPresetShouldFailNotCustom() = coroutineRule.runBlockingTest {
        // given
        val preset = EqualizerPreset(-1, "", emptyList(), false)

        // when
        sut.addPreset(preset)
    }

    @Test
    fun testUpdatePreset() = coroutineRule.runBlockingTest {
        // given
        val item = domainPresets[0]

        // when
        sut.updatePreset(item)

        // then
        verify(dao).insertPresets(presetsEntities[0])
    }

    @Test
    fun testDeletePreset() = coroutineRule.runBlockingTest {
        // given
        val item = domainPresets[0]

        // when
        sut.deletePreset(item)

        // then
        verify(dao).deletePreset(presetsEntities[0])
    }

    @Test
    fun testObserveCurrentPreset() = coroutineRule.runBlockingTest {
        // given
        val id = 1L
        whenever(prefs.observeCurrentPresetId()).thenReturn(flowOf(id))
        whenever(dao.observePresetById(id)).thenReturn(flowOf(presetsEntities[0]))

        // when
        val actual = sut.observeCurrentPreset().single()

        // then
        assertEquals(
            domainPresets[0],
            actual
        )
    }


}