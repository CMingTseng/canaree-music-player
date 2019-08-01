package dev.olog.presentation.equalizer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.olog.core.entity.EqualizerPreset
import dev.olog.core.gateway.EqualizerGateway
import dev.olog.core.prefs.EqualizerPreferencesGateway
import dev.olog.equalizer.IBassBoost
import dev.olog.equalizer.IEqualizer
import dev.olog.equalizer.IVirtualizer
import dev.olog.shared.android.utils.isP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class EqualizerFragmentPresenter @Inject constructor(
    private val equalizer: IEqualizer,
    private val bassBoost: IBassBoost,
    private val virtualizer: IVirtualizer,
    private val equalizerPrefsUseCase: EqualizerPreferencesGateway,
    private val equalizerGateway: EqualizerGateway
) : ViewModel() {

    private val currentPresetLiveData = MutableLiveData<EqualizerPreset>()

    init {
        viewModelScope.launch {
            equalizer.observeCurrentPreset()
                .flowOn(Dispatchers.IO)
                .collect { currentPresetLiveData.value = it }
        }
    }

    fun getBandLimit() = equalizer.getBandLimit()
    fun getCurrentPreset() = equalizer.getCurrentPreset()
    fun getBandCount() = equalizer.getBandCount()
    fun setCurrentPreset(preset: EqualizerPreset) = equalizer.setCurrentPreset(preset)
    fun getPresets() = equalizer.getPresets()
    fun setBandLevel(band: Int, level: Float) = equalizer.setBandLevel(band, level)

    override fun onCleared() {
        viewModelScope.cancel()
    }

    fun observePreset(): LiveData<EqualizerPreset> = currentPresetLiveData

    fun isEqualizerEnabled(): Boolean = equalizerPrefsUseCase.isEqualizerEnabled()

    fun setEqualizerEnabled(enabled: Boolean) {
        equalizer.setEnabled(enabled)
        virtualizer.setEnabled(enabled)
        bassBoost.setEnabled(enabled)
        equalizerPrefsUseCase.setEqualizerEnabled(enabled)
    }

    fun getBassStrength(): Int = bassBoost.getStrength() / 10

    fun setBassStrength(value: Int) {
        bassBoost.setStrength(value * 10)
    }

    fun getVirtualizerStrength(): Int = virtualizer.getStrength() / 10

    fun setVirtualizerStrength(value: Int) {
        virtualizer.setStrength(value * 10)
    }

    fun getBandStep(): Float {
        if (isP()) {
            return .1f
        }
        TODO()
    }

    fun deleteCurrentPreset() = viewModelScope.launch(Dispatchers.IO) {
        val currentPreset = currentPresetLiveData.value!!
        equalizerPrefsUseCase.setCurrentPresetId(0)
        equalizerGateway.deletePreset(currentPreset)
    }

    suspend fun saveCurrentPreset(title: String): Boolean = withContext(Dispatchers.IO){
        val preset = EqualizerPreset(
            id = -1,
            name = title,
            isCustom = true,
            bands = equalizer.getAllBandsLevel()
        )
        equalizerGateway.saveCurrentPreset(preset)
        true
    }

}
