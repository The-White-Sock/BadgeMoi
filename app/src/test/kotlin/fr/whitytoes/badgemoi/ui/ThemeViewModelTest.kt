package fr.whitytoes.badgemoi.ui

import fr.whitytoes.badgemoi.domain.SettingsRepository
import fr.whitytoes.badgemoi.domain.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `la préférence est nulle tant qu'elle n'a pas été lue`() =
        runTest(dispatcher) {
            val viewModel = ThemeViewModel(FakeSettingsRepository())

            assertNull(viewModel.themeMode.value)
        }

    @Test
    fun `la préférence stockée est exposée une fois lue`() =
        runTest(dispatcher) {
            val viewModel = ThemeViewModel(FakeSettingsRepository(ThemeMode.DAY))

            advanceUntilIdle()

            assertEquals(ThemeMode.DAY, viewModel.themeMode.value)
        }

    @Test
    fun `la bascule persiste le mode opposé`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository(ThemeMode.NIGHT)
            val viewModel = ThemeViewModel(repository)
            advanceUntilIdle()

            viewModel.toggleTheme(systemInDarkTheme = false)
            advanceUntilIdle()

            assertEquals(ThemeMode.DAY, repository.current)
        }

    /**
     * Garde-fou : basculer avant que la préférence ne soit lue écraserait une valeur
     * encore inconnue — le dépôt ne doit donc pas être touché.
     */
    @Test
    fun `la bascule est sans effet tant que la préférence n'est pas lue`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository(ThemeMode.DAY)
            val viewModel = ThemeViewModel(repository)

            viewModel.toggleTheme(systemInDarkTheme = false)
            advanceUntilIdle()

            assertEquals(ThemeMode.DAY, repository.current)
        }
}

private class FakeSettingsRepository(
    initial: ThemeMode = ThemeMode.NIGHT,
) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    val current: ThemeMode get() = state.value

    override fun observeThemeMode(): Flow<ThemeMode> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = mode
    }
}
