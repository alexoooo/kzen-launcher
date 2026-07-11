package tech.kzen.launcher.server.project

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull


class ProjectNameValidationTest {
    //-----------------------------------------------------------------------------------------------------------------
    @Test
    fun `ordinary names pass`() {
        assertNull(ProjectNameValidation.errorOrNull("my-project"))
        assertNull(ProjectNameValidation.errorOrNull("My Project 2"))
        assertNull(ProjectNameValidation.errorOrNull("foo.bar"))
        assertNull(ProjectNameValidation.errorOrNull("a".repeat(128)))
        assertNull(ProjectNameValidation.errorOrNull("console"))
        assertNull(ProjectNameValidation.errorOrNull("com10"))
    }


    @Test
    fun `path escapes rejected`() {
        assertNotNull(ProjectNameValidation.errorOrNull(".."))
        assertNotNull(ProjectNameValidation.errorOrNull("."))
        assertNotNull(ProjectNameValidation.errorOrNull("../pwn"))
        assertNotNull(ProjectNameValidation.errorOrNull("a/b"))
        assertNotNull(ProjectNameValidation.errorOrNull("a\\b"))
    }


    @Test
    fun `reserved routing names rejected`() {
        assertNotNull(ProjectNameValidation.errorOrNull("main"))
        assertNotNull(ProjectNameValidation.errorOrNull("Main"))
        assertNotNull(ProjectNameValidation.errorOrNull("shell"))
    }


    @Test
    fun `windows-invalid names rejected`() {
        assertNotNull(ProjectNameValidation.errorOrNull("CON"))
        assertNotNull(ProjectNameValidation.errorOrNull("con.txt"))
        assertNotNull(ProjectNameValidation.errorOrNull("nul"))
        assertNotNull(ProjectNameValidation.errorOrNull("COM1"))
        assertNotNull(ProjectNameValidation.errorOrNull("a:b"))
        assertNotNull(ProjectNameValidation.errorOrNull("a?b"))
        assertNotNull(ProjectNameValidation.errorOrNull("a\u0001b"))
        assertNotNull(ProjectNameValidation.errorOrNull("trailing."))
        assertNotNull(ProjectNameValidation.errorOrNull("trailing "))
    }


    @Test
    fun `empty and oversized rejected`() {
        assertNotNull(ProjectNameValidation.errorOrNull(""))
        assertNotNull(ProjectNameValidation.errorOrNull("   "))
        assertNotNull(ProjectNameValidation.errorOrNull("a".repeat(129)))
    }
}
