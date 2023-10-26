package it.decoder.music.ui.screens.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.decoder.music.LocalPlayerAwareWindowInsets
import it.decoder.music.R
import it.decoder.music.enums.ColorPaletteMode
import it.decoder.music.enums.ColorPaletteName
import it.decoder.music.enums.ThumbnailRoundness
import it.decoder.music.ui.components.themed.Header
import it.decoder.music.ui.styling.LocalAppearance
import it.decoder.music.utils.applyFontPaddingKey
import it.decoder.music.utils.colorPaletteModeKey
import it.decoder.music.utils.colorPaletteNameKey
import it.decoder.music.utils.getI18String
import it.decoder.music.utils.isAtLeastAndroid13
import it.decoder.music.utils.isShowingThumbnailInLockscreenKey
import it.decoder.music.utils.rememberPreference
import it.decoder.music.utils.thumbnailRoundnessKey
import it.decoder.music.utils.useSystemFontKey

@ExperimentalAnimationApi
@Composable
fun AppearanceSettings() {
    val (colorPalette) = LocalAppearance.current

    var colorPaletteName by rememberPreference(colorPaletteNameKey, ColorPaletteName.PureBlack)
    var colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.System)
    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )
    var useSystemFont by rememberPreference(useSystemFontKey, false)
    var applyFontPadding by rememberPreference(applyFontPaddingKey, false)
    var isShowingThumbnailInLockscreen by rememberPreference(
        isShowingThumbnailInLockscreenKey,
        false
    )

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
        Header(title = stringResource(R.string.appearance))

        SettingsEntryGroupText(title = stringResource(R.string.colors))

        EnumValueSelectorSettingsEntry(
            title = stringResource(R.string.theme),
            selectedValue = colorPaletteName,
            onValueSelected = { colorPaletteName = it }
        )

        EnumValueSelectorSettingsEntry(
            title = stringResource(R.string.theme_mode),
            selectedValue = colorPaletteMode,
            isEnabled = colorPaletteName != ColorPaletteName.PureBlack,
            onValueSelected = { colorPaletteMode = it }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.shapes))

        SettingsDescription(
            text = stringResource(R.string.current_shape_name) + " ${getI18String(thumbnailRoundness.name)}"
        )

        EnumValueSelectorSettingsEntry(
            title = stringResource(R.string.thumbnail_roundness),
            selectedValue = thumbnailRoundness,
            onValueSelected = { thumbnailRoundness = it },
            trailingContent = {
                Spacer(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = colorPalette.accent,
                            shape = thumbnailRoundness.shape()
                        )
                        .background(
                            color = colorPalette.background1,
                            shape = thumbnailRoundness.shape()
                        )
                        .size(36.dp)
                )
            }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.text))

        SwitchSettingEntry(
            title = stringResource(R.string.use_system_font),
            text = stringResource(R.string.use_font_by_the_system),
            isChecked = useSystemFont,
            onCheckedChange = { useSystemFont = it }
        )

        SwitchSettingEntry(
            title = stringResource(R.string.apply_font_padding),
            text = stringResource(R.string.add_spacing_around_texts),
            isChecked = applyFontPadding,
            onCheckedChange = { applyFontPadding = it }
        )

        if (!isAtLeastAndroid13) {
            SettingsGroupSpacer()

            SettingsEntryGroupText(title = stringResource(R.string.lockscreen))

            SwitchSettingEntry(
                title = stringResource(R.string.show_song_cover),
                text = stringResource(R.string.use_song_cover_on_lockscreen),
                isChecked = isShowingThumbnailInLockscreen,
                onCheckedChange = { isShowingThumbnailInLockscreen = it }
            )
        }
    }
}
