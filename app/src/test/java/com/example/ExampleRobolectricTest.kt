package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.StorageRepository
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.SortOption
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Infinity Files", appName)
  }

  @Test
  fun `test storage repository seeds and file operations`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = StorageRepository(context)

    // Verify workspace path exists
    val wsPath = repo.defaultWorkspacePath
    assertTrue(File(wsPath).exists())

    // List directory
    val listRes = repo.listDirectory(wsPath, showHidden = false, sortOption = SortOption.TYPE_ASC)
    assertTrue(listRes.isSuccess)
    val items = listRes.getOrNull()
    assertNotNull(items)
    assertTrue(items!!.isNotEmpty())

    // Test creating folder
    val newFolderRes = repo.createFolder(wsPath, "TestUnitFolder")
    assertTrue(newFolderRes.isSuccess)

    // Test creating file
    val newFileRes = repo.createFile(newFolderRes.getOrThrow().absolutePath, "hello.txt", "Hello Infinity!")
    assertTrue(newFileRes.isSuccess)

    // Test reading file
    val textRes = repo.readFileText(newFileRes.getOrThrow().absolutePath)
    assertTrue(textRes.isSuccess)
    assertEquals("Hello Infinity!", textRes.getOrNull())

    // Test file size formatting
    assertEquals("0 B", FileItem.formatByteSize(0L))
    assertEquals("1 KB", FileItem.formatByteSize(1024L))
    assertEquals("1.5 MB", FileItem.formatByteSize((1.5 * 1024 * 1024).toLong()))
  }
}
