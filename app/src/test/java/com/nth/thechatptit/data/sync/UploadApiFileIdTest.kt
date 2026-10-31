package com.nth.thechatptit.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UploadApiFileIdTest {
    private val baseUrl = "https://keyauth.click/kaiwalker-api"
    private val fileId = "0123456789abcdef0123456789abcdef"

    @Test
    fun acceptsRelativeApiPath() {
        assertEquals(fileId, uploadApiFileId("/api/v1/files/$fileId", baseUrl))
    }

    @Test
    fun acceptsAbsoluteUrlFromConfiguredApi() {
        assertEquals(fileId, uploadApiFileId("$baseUrl/api/v1/files/$fileId", baseUrl))
    }

    @Test
    fun rejectsPathsOutsideConfiguredApi() {
        assertNull(uploadApiFileId("https://other.example/api/v1/files/$fileId", baseUrl))
        assertNull(uploadApiFileId("/api/v1/files/not-an-id", baseUrl))
    }
}
