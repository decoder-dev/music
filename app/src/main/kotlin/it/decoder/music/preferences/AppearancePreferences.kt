package it.decoder.music.preferences

import it.decoder.music.GlobalPreferencesHolder
import it.decoder.music.enums.ColorPaletteMode
import it.decoder.music.enums.ColorPaletteName
import it.decoder.music.enums.ThumbnailRoundness

object AppearancePreferences : GlobalPreferencesHolder() {
    val colorPaletteNameProperty = enum(ColorPaletteName.Dynamic)
    var colorPaletteName by colorPaletteNameProperty
    val colorPaletteModeProperty = enum(ColorPaletteMode.System)
    var colorPaletteMode by colorPaletteModeProperty
    val thumbnailRoundnessProperty = enum(ThumbnailRoundness.Light)
    var thumbnailRoundness by thumbnailRoundnessProperty
    val useSystemFontProperty = boolean(false)
    var useSystemFont by useSystemFontProperty
    val applyFontPaddingProperty = boolean(false)
    var applyFontPadding by applyFontPaddingProperty
    val isShowingThumbnailInLockscreenProperty = boolean(false)
    var isShowingThumbnailInLockscreen by isShowingThumbnailInLockscreenProperty
    var swipeToHideSong by boolean(false)
    var maxThumbnailSize by int(1920)
}
