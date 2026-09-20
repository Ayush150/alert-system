package com.sih26001.mobilealert.data.fcm

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.sih26001.mobilealert.core.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FcmTopicSubscriberTest {

    @Test
    fun `topic name is exactly sih26001_alerts per contract`() {
        assertEquals("sih26001_alerts", FcmTopicSubscriber.TOPIC_ALERTS)
        assertEquals("sih26001_alerts", Constants.FCM_TOPIC_ALERTS)
    }

    @Test
    fun `subscribeToAlertsTopic calls subscribeToTopic on FirebaseMessaging with correct topic`() {
        val mockMessaging: FirebaseMessaging = mock()
        val mockTask: Task<Void> = mock()
        whenever(mockMessaging.subscribeToTopic("sih26001_alerts")).thenReturn(mockTask)

        FcmTopicSubscriber.subscribeToAlertsTopic(
            messaging = mockMessaging,
            topic = "sih26001_alerts"
        )

        verify(mockMessaging).subscribeToTopic("sih26001_alerts")
    }

    @Test
    fun `subscribeToAlertsTopic triggers success callback when task succeeds`() {
        val mockMessaging: FirebaseMessaging = mock()
        val mockTask: Task<Void> = mock()
        whenever(mockMessaging.subscribeToTopic("sih26001_alerts")).thenReturn(mockTask)

        var completedSuccess: Boolean? = null
        var completedError: String? = null

        whenever(mockTask.addOnCompleteListener(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val listener = invocation.arguments[0] as OnCompleteListener<Void>
            whenever(mockTask.isSuccessful).thenReturn(true)
            whenever(mockTask.exception).thenReturn(null)
            listener.onComplete(mockTask)
            mockTask
        }

        FcmTopicSubscriber.subscribeToAlertsTopic(
            messaging = mockMessaging,
            topic = "sih26001_alerts",
            onComplete = { success, error ->
                completedSuccess = success
                completedError = error
            }
        )

        assertTrue(completedSuccess == true)
        assertNull(completedError)
    }

    @Test
    fun `subscribeToAlertsTopic triggers failure callback when task fails`() {
        val mockMessaging: FirebaseMessaging = mock()
        val mockTask: Task<Void> = mock()
        whenever(mockMessaging.subscribeToTopic("sih26001_alerts")).thenReturn(mockTask)

        var completedSuccess: Boolean? = null
        var completedError: String? = null

        whenever(mockTask.addOnCompleteListener(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val listener = invocation.arguments[0] as OnCompleteListener<Void>
            whenever(mockTask.isSuccessful).thenReturn(false)
            whenever(mockTask.exception).thenReturn(RuntimeException("Quota exceeded"))
            listener.onComplete(mockTask)
            mockTask
        }

        FcmTopicSubscriber.subscribeToAlertsTopic(
            messaging = mockMessaging,
            topic = "sih26001_alerts",
            onComplete = { success, error ->
                completedSuccess = success
                completedError = error
            }
        )

        assertTrue(completedSuccess == false)
        assertEquals("Quota exceeded", completedError)
    }

    @Test
    fun `subscribeToAlertsTopic catches exceptions gracefully without throwing`() {
        val mockMessaging: FirebaseMessaging = mock()
        whenever(mockMessaging.subscribeToTopic("sih26001_alerts")).thenThrow(IllegalStateException("Firebase not available"))

        var completedSuccess: Boolean? = null
        var completedError: String? = null

        val result = FcmTopicSubscriber.subscribeToAlertsTopic(
            messaging = mockMessaging,
            topic = "sih26001_alerts",
            onComplete = { success, error ->
                completedSuccess = success
                completedError = error
            }
        )

        assertNull(result)
        assertFalse(completedSuccess ?: true)
        assertEquals("Firebase not available", completedError)
    }
}
