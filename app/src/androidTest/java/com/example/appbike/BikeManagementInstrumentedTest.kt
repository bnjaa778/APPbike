package com.example.appbike

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.appbike.ui.theme.APPbikeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BikeManagementInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    private val account = AccountSession(
        userId = "123e4567-e89b-42d3-a456-426614174000",
        email = "rider@appbike.cl"
    )
    private val bike = Bike(
        name = "Ruta diaria",
        brand = "Trek",
        model = "Domane",
        type = "Ruta",
        serialNumber = "SERIE-77",
        remoteId = 77L,
        userId = account.userId,
        photoId = "foto-77.webp",
        imageUri = "appbike-photo://foto-77.webp"
    )

    @Test
    fun editFormKeepsServerIdentityAndDoesNotRequireANewPhoto() {
        var saved: Bike? = null
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                BikeFormDialog(
                    initialBike = bike,
                    isSaving = false,
                    onDismiss = {},
                    onSave = { saved = it }
                )
            }
        }

        compose.onNodeWithText("Editar bicicleta").assertIsDisplayed()
        compose.onNodeWithTag(BIKE_SAVE_CHANGES_TEST_TAG)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        compose.runOnIdle {
            assertEquals(77L, saved?.remoteId)
            assertEquals(account.userId, saved?.userId)
            assertEquals("foto-77.webp", saved?.photoId)
            assertEquals("appbike-photo://foto-77.webp", saved?.imageUri)
        }
    }

    @Test
    fun detailExposesRealEditAndDeleteActions() {
        var editRequested = false
        var deleteRequested = false
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                BikeDetailDialog(
                    account = account,
                    bike = bike,
                    reminders = mutableListOf(),
                    bookings = mutableListOf(),
                    isMutatingBike = false,
                    onEdit = { editRequested = true },
                    onDelete = { deleteRequested = true },
                    onDismiss = {}
                )
            }
        }

        compose.onNodeWithTag(BIKE_EDIT_ACTION_TEST_TAG)
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        compose.onNodeWithTag(BIKE_DELETE_ACTION_TEST_TAG)
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        compose.runOnIdle {
            assertTrue(editRequested)
            assertTrue(deleteRequested)
        }
    }

    @Test
    fun deletionNeedsTheExplicitDestructiveConfirmation() {
        var confirmed = false
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                BikeDeleteConfirmationDialog(
                    bike = bike,
                    isDeleting = false,
                    errorMessage = null,
                    onDismiss = {},
                    onConfirm = { confirmed = true }
                )
            }
        }

        compose.onNodeWithText("Esta acción no se puede deshacer.", substring = true)
            .assertIsDisplayed()
        compose.runOnIdle { assertFalse(confirmed) }

        compose.onNodeWithTag(BIKE_CONFIRM_DELETE_TEST_TAG)
            .assertHasClickAction()
            .performClick()

        compose.runOnIdle { assertTrue(confirmed) }
    }

    @Test
    fun maintenanceAndServiceCardsExposeEveryServerAction() {
        var maintenanceEdited = false
        var maintenanceDeleted = false
        var serviceEdited = false
        var serviceCompleted = false
        var serviceDeleted = false
        val reminder = MaintenanceReminder(
            bike = bike.name,
            component = "Cambio de cadena",
            date = "2026-08-05",
            notes = "Cadena nueva",
            remoteId = 31L,
            bikeId = bike.remoteId
        )
        val booking = ServiceBooking(
            workshop = "Taller Central",
            service = "Ajuste de frenos",
            date = "2026-08-20",
            contact = "contacto@appbike.cl",
            bike = bike.name,
            remoteId = 32L,
            bikeId = bike.remoteId
        )
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PastMaintenanceCard(
                        reminder = reminder,
                        enabled = true,
                        onEdit = { maintenanceEdited = true },
                        onDelete = { maintenanceDeleted = true }
                    )
                    FutureServiceCard(
                        booking = booking,
                        enabled = true,
                        onEdit = { serviceEdited = true },
                        onComplete = { serviceCompleted = true },
                        onDelete = { serviceDeleted = true }
                    )
                }
            }
        }

        compose.onNodeWithTag(MAINTENANCE_EDIT_TEST_TAG)
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        compose.onNodeWithTag(MAINTENANCE_DELETE_TEST_TAG)
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        compose.onNodeWithTag(SERVICE_EDIT_TEST_TAG)
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        compose.onNodeWithTag(SERVICE_COMPLETE_TEST_TAG)
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        compose.onNodeWithTag(SERVICE_DELETE_TEST_TAG)
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        compose.runOnIdle {
            assertTrue(maintenanceEdited)
            assertTrue(maintenanceDeleted)
            assertTrue(serviceEdited)
            assertTrue(serviceCompleted)
            assertTrue(serviceDeleted)
        }
    }

    @Test
    fun maintenanceEditorPreservesRemoteIdentity() {
        val reminder = MaintenanceReminder(
            bike = bike.name,
            component = "Cambio de cadena",
            date = "2026-08-05",
            notes = "Cadena nueva",
            remoteId = 31L,
            bikeId = bike.remoteId
        )
        var saved: MaintenanceReminder? = null
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                PastMaintenanceEditorDialog(
                    reminder = reminder,
                    isSaving = false,
                    errorMessage = null,
                    onDismiss = {},
                    onSave = { saved = it }
                )
            }
        }

        compose.onNodeWithTag(MAINTENANCE_SAVE_TEST_TAG)
            .assertIsEnabled()
            .performClick()

        compose.runOnIdle {
            assertEquals(31L, saved?.remoteId)
            assertEquals(77L, saved?.bikeId)
            assertEquals("Cambio de cadena", saved?.component)
        }
    }

    @Test
    fun serviceEditorAndCompletionPreserveTheScheduledRecord() {
        val booking = ServiceBooking(
            workshop = "Taller Central",
            service = "Ajuste de frenos",
            date = "2026-08-20",
            contact = "contacto@appbike.cl",
            bike = bike.name,
            remoteId = 32L,
            bikeId = bike.remoteId
        )
        var saved: ServiceBooking? = null
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                FutureServiceEditorDialog(
                    booking = booking,
                    isSaving = false,
                    errorMessage = null,
                    onDismiss = {},
                    onSave = { saved = it }
                )
            }
        }

        compose.onNodeWithTag(SERVICE_SAVE_TEST_TAG)
            .assertIsEnabled()
            .performClick()

        compose.runOnIdle {
            assertEquals(32L, saved?.remoteId)
            assertEquals(77L, saved?.bikeId)
            assertEquals("Ajuste de frenos", saved?.service)
        }
    }

    @Test
    fun serviceCompletionRequiresAnExplicitValidDate() {
        val booking = ServiceBooking(
            workshop = "Taller Central",
            service = "Ajuste de frenos",
            date = "2026-08-20",
            contact = "",
            bike = bike.name,
            remoteId = 32L,
            bikeId = bike.remoteId
        )
        var completedDate: String? = null
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                ServiceCompletionDialog(
                    booking = booking,
                    isSaving = false,
                    errorMessage = null,
                    onDismiss = {},
                    onConfirm = { date, _ -> completedDate = date }
                )
            }
        }

        compose.onNodeWithTag(SERVICE_CONFIRM_COMPLETE_TEST_TAG)
            .assertIsEnabled()
            .performClick()

        compose.runOnIdle {
            assertTrue(isValidApiDateInput(completedDate.orEmpty()))
        }
    }
}
