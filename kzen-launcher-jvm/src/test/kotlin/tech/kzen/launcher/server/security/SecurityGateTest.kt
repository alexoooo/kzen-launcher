package tech.kzen.launcher.server.security

import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class SecurityGateTest {
    //-----------------------------------------------------------------------------------------------------------------
    private fun denied(
        host: String? = "localhost:8080",
        site: String? = null,
        mode: String? = null,
        method: HttpMethod = HttpMethod.Get,
        path: String = "/index.html"
    ): Boolean {
        return SecurityGate.deniedReasonOrNull(host, site, mode, method, path) != null
    }


    //-----------------------------------------------------------------------------------------------------------------
    @Test
    fun `no fetch metadata passes`() {
        assertFalse(denied())
        assertFalse(denied(host = null))
        assertFalse(denied(path = "/rs/command/project/delete"))
    }


    @Test
    fun `local hosts pass, non-local denied`() {
        assertFalse(denied(host = "localhost"))
        assertFalse(denied(host = "127.0.0.1:8080"))
        assertTrue(denied(host = "evil.test:8080"))
        assertTrue(denied(host = "localhost.evil.test"))
    }


    @Test
    fun `same-site fetch metadata passes`() {
        assertFalse(denied(site = "same-origin", mode = "cors"))
        assertFalse(denied(site = "same-site", mode = "no-cors"))
        assertFalse(denied(site = "none", mode = "navigate"))
    }


    @Test
    fun `cross-site subresource and fetch denied`() {
        assertTrue(denied(site = "cross-site", mode = "no-cors"))
        assertTrue(denied(site = "cross-site", mode = "cors"))
        assertTrue(denied(site = "cross-site", mode = "no-cors", path = "/rs/command/project/delete"))
        assertTrue(denied(site = "other", mode = "cors"))
    }


    @Test
    fun `cross-site top-level navigation passes except to mutating endpoints`() {
        assertFalse(denied(site = "cross-site", mode = "navigate"))
        assertFalse(denied(site = "cross-site", mode = "navigate", path = "/rs/query/project"))

        assertTrue(denied(site = "cross-site", mode = "navigate", path = "/rs/command/project/delete"))
        assertTrue(denied(site = "cross-site", mode = "navigate", path = "/rs/command/project/rename"))
        assertTrue(denied(site = "cross-site", mode = "navigate", path = "/rs/command/project/upgrade"))
        assertTrue(denied(site = "cross-site", mode = "navigate", path = "/shell/project/start"))
        assertTrue(denied(site = "cross-site", mode = "navigate", path = "/shell/project/stop"))
    }


    @Test
    fun `non-GET cross-site navigation denied`() {
        assertTrue(denied(site = "cross-site", mode = "navigate", method = HttpMethod.Post))
    }
}
