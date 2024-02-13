package it.decoder.music.preferences

import it.decoder.music.GlobalPreferencesHolder
import it.decoder.music.enums.ColorPaletteMode
import it.decoder.music.enums.ColorPaletteName
import it.decoder.music.enums.ThumbnailRoundness

object AppearancePreferences : GlobalPreferencesHolder() {
    var colorPaletteName by enum(ColorPaletteName.Dynamic)
    var colorPaletteMode by enum(ColorPaletteMode.System)
    var thumbnailRoundness by enum(ThumbnailRoundness.Light)
    var useSystemFont by boolean(false)
    var applyFontPadding by boolean(false)
    val isShowingThumbnailInLockscreenProperty = boolean(false)
    var isShowingThumbnailInLockscreen by isShowingThumbnailInLockscreenProperty
    var swipeToHideSong by boolean(false)
}
