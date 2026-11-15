package com.antigravity.filemanager.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class FileItemTest {

    @Test
    fun formatFileSize_formatsZeroBytes() {
        assertEquals("0 B", FileItem.formatFileSize(0L))
    }

    @Test
    fun formatFileSize_formatsNegativeAsZero() {
        assertEquals("0 B", FileItem.formatFileSize(-42L))
    }

    @Test
    fun formatFileSize_formatsKilobytes() {
        assertEquals("1.0 KB", FileItem.formatFileSize(1024L))
    }

    @Test
    fun formatFileSize_formatsMegabytesAndGigabytes() {
        assertEquals("1.5 MB", FileItem.formatFileSize((1.5 * 1024 * 1024).toLong()))
        assertEquals("1.0 GB", FileItem.formatFileSize(1024L * 1024 * 1024))
    }

    @Test
    fun determineFileType_detectsImages() {
        assertEquals(FileType.IMAGE, FileItem.determineFileType(File("holiday.JPG")))
        assertEquals(FileType.IMAGE, FileItem.determineFileType(File("icon.webp")))
    }

    @Test
    fun determineFileType_detectsDocumentsArchivesAndApks() {
        assertEquals(FileType.DOCUMENT, FileItem.determineFileType(File("resume.pdf")))
        assertEquals(FileType.ARCHIVE, FileItem.determineFileType(File("backup.zip")))
        assertEquals(FileType.APK, FileItem.determineFileType(File("app.apk")))
        assertEquals(FileType.CODE, FileItem.determineFileType(File("Main.kt")))
        assertEquals(FileType.OTHER, FileItem.determineFileType(File("data.bin")))
    }

    @Test
    fun isTextFile_recognisesCommonCodeAndTextExtensions() {
        assertEquals(true, FileItem(File("notes.txt")).isTextFile)
        assertEquals(true, FileItem(File("script.sh")).isTextFile)
        assertEquals(false, FileItem(File("photo.png")).isTextFile)
    }
}
