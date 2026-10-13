package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("左右腦鍛鍊", appName)
  }

  @Test
  fun `read admob app id from string resource`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appId = context.getString(R.string.admob_app_id)
    assertEquals("ca-app-pub-5014630903713895~9913828625", appId)
  }
}
