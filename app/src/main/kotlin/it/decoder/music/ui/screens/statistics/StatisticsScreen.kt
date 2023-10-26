package it.decoder.music.ui.screens.statistics

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import it.decoder.compose.persist.PersistMapCleanup
import it.decoder.compose.routing.RouteHandler
import it.decoder.music.R
import it.decoder.music.enums.StatisticsType
import it.decoder.music.ui.components.themed.Scaffold
import it.decoder.music.ui.screens.globalRoutes

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun StatisticsScreen(
    statisticsType: StatisticsType
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabIndexChanged) = rememberSaveable {
        mutableStateOf(when (statisticsType) {
            StatisticsType.OneWeek -> 0
            StatisticsType.OneMonth -> 1
            StatisticsType.ThreeMonths -> 2
            StatisticsType.SixMonth -> 3
            StatisticsType.OneYear -> 4
            StatisticsType.All -> 5

        })
    }

    PersistMapCleanup(tagPrefix = "${statisticsType.name}/")

    RouteHandler(listenToGlobalEmitter = true) {
        globalRoutes()

        host {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChanged = onTabIndexChanged,
                tabColumnContent = { Item ->
                    Item(0, "One week", R.drawable.query_stats)
                    Item(1, "One month", R.drawable.query_stats)
                    Item(2, "Three months", R.drawable.query_stats)
                    Item(3, "Six months", R.drawable.query_stats)
                    Item(4, "One year", R.drawable.query_stats)
                    Item(5, "All", R.drawable.query_stats)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> StatisticsItems(statisticsType = StatisticsType.OneWeek)
                        1 -> StatisticsItems(statisticsType = StatisticsType.OneMonth)
                        2 -> StatisticsItems(statisticsType = StatisticsType.ThreeMonths)
                        3 -> StatisticsItems(statisticsType = StatisticsType.SixMonth)
                        4 -> StatisticsItems(statisticsType = StatisticsType.OneYear)
                        5 -> StatisticsItems(statisticsType = StatisticsType.All)
                    }
                }
            }
        }
    }
}
