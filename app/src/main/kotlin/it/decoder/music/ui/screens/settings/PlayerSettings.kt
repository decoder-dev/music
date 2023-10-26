package it.decoder.music.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import it.decoder.music.LocalPlayerAwareWindowInsets
import it.decoder.music.LocalPlayerServiceBinder
import it.decoder.music.R
import it.decoder.music.enums.ExoPlayerMinTimeForEvent
import it.decoder.music.enums.PlayerThumbnailSize
import it.decoder.music.ui.components.themed.Header
import it.decoder.music.ui.styling.LocalAppearance
import it.decoder.music.utils.closebackgroundPlayerKey
import it.decoder.music.utils.exoPlayerMinTimeForEventKey
import it.decoder.music.utils.getI18String
import it.decoder.music.utils.isAtLeastAndroid6
import it.decoder.music.utils.persistentQueueKey
import it.decoder.music.utils.playerThumbnailSizeKey
import it.decoder.music.utils.rememberPreference
import it.decoder.music.utils.resumePlaybackWhenDeviceConnectedKey
import it.decoder.music.utils.skipSilenceKey
import it.decoder.music.utils.toast
import it.decoder.music.utils.volumeNormalizationKey

@ExperimentalAnimationApi
@Composable
fun PlayerSettings() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current

    var persistentQueue by rememberPreference(persistentQueueKey, false)
    var closebackgroundPlayer by rememberPreference(closebackgroundPlayerKey, false)
    var resumePlaybackWhenDeviceConnected by rememberPreference(
        resumePlaybackWhenDeviceConnectedKey,
        false
    )
    var skipSilence by rememberPreference(skipSilenceKey, false)
    var volumeNormalization by rememberPreference(volumeNormalizationKey, false)

    var exoPlayerMinTimeForEvent by rememberPreference(
        exoPlayerMinTimeForEventKey,
        ExoPlayerMinTimeForEvent.`20s`
    )

    var playerThumbnailSize by rememberPreference(playerThumbnailSizeKey, PlayerThumbnailSize.Medium)

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
    ) {
        Header(title = stringResource(R.string.player))

        SettingsEntryGroupText(title = stringResource(R.string.player_thumbnail_size))

        SettingsDescription(
            text = stringResource(R.string.current_size_name) + " ${getI18String(playerThumbnailSize.name)} \n${stringResource(R.string.restarting_music_is_required)}"
        )

        EnumValueSelectorSettingsEntry(
            title = stringResource(R.string.size_name),
            selectedValue = playerThumbnailSize,
            onValueSelected = { playerThumbnailSize = it }
        )

        SettingsEntryGroupText(title = stringResource(R.string.quick_pics_and_tips))

        SettingsDescription(text = stringResource(R.string.is_min_list_time_for_tips_or_quick_pics))
        EnumValueSelectorSettingsEntry(
            title = stringResource(R.string.min_listening_time),
            selectedValue = exoPlayerMinTimeForEvent,
            onValueSelected = { exoPlayerMinTimeForEvent = it }
        )

        SettingsEntryGroupText(title = "PREFERENCES")

        SwitchSettingEntry(
            title = stringResource(R.string.persistent_queue),
            text = stringResource(R.string.save_and_restore_playing_songs),
            isChecked = persistentQueue,
            onCheckedChange = {
                persistentQueue = it
            }
        )


        if (isAtLeastAndroid6) {
            SwitchSettingEntry(
                title = stringResource(R.string.resume_playback),
                text = stringResource(R.string.when_device_is_connected),
                isChecked = resumePlaybackWhenDeviceConnected,
                onCheckedChange = {
                    resumePlaybackWhenDeviceConnected = it
                }
            )
        }

        SwitchSettingEntry(
            title = stringResource(R.string.close_background_player),
            text = stringResource(R.string.when_app_swipe_out_from_task_manager),
            isChecked = closebackgroundPlayer,
            onCheckedChange = {
                closebackgroundPlayer = it
            }
        )

        //SettingsGroupSpacer()

        //SettingsEntryGroupText(title = stringResource(R.string.audio))

        SwitchSettingEntry(
            title = stringResource(R.string.skip_silence),
            text = stringResource(R.string.skip_silent_parts_during_playback),
            isChecked = skipSilence,
            onCheckedChange = {
                skipSilence = it
            }
        )

        SwitchSettingEntry(
            title = stringResource(R.string.loudness_normalization),
            text = stringResource(R.string.autoadjust_the_volume),
            isChecked = volumeNormalization,
            onCheckedChange = {
                volumeNormalization = it
            }
        )

        SettingsEntry(
            title = stringResource(R.string.equalizer),
            text = stringResource(R.string.interact_with_the_system_equalizer),
            onClick = {
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder?.player?.audioSessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }

                try {
                    activityResultLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    context.toast("Couldn't find an application to equalize audio")
                }
            }
        )
        SettingsGroupSpacer()
    }
}
