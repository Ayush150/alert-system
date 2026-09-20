package com.sih26001.mobilealert.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationParityTest {

    @Test
    fun `all 4 supported locales are configured in LocaleManager`() {
        val languages = AppLanguage.entries
        assertEquals(4, languages.size)

        val codes = languages.map { it.code }.toSet()
        assertTrue("Must support English", codes.contains("en"))
        assertTrue("Must support Hindi", codes.contains("hi"))
        assertTrue("Must support Marathi", codes.contains("mr"))
        assertTrue("Must support Assamese", codes.contains("as"))
    }

    @Test
    fun `all string keys in base strings_xml are translated across Hindi, Marathi, and Assamese`() {
        val baseFile = findResFile("values/strings.xml")
        val hiFile = findResFile("values-hi/strings.xml")
        val mrFile = findResFile("values-mr/strings.xml")
        val asFile = findResFile("values-as/strings.xml")

        assertTrue("Base strings.xml must exist", baseFile.exists())
        assertTrue("values-hi/strings.xml must exist", hiFile.exists())
        assertTrue("values-mr/strings.xml must exist", mrFile.exists())
        assertTrue("values-as/strings.xml must exist", asFile.exists())

        val baseKeys = parseStringKeys(baseFile)
        val hiKeys = parseStringKeys(hiFile)
        val mrKeys = parseStringKeys(mrFile)
        val asKeys = parseStringKeys(asFile)

        assertFalse("Base keys cannot be empty", baseKeys.isEmpty())

        val missingInHi = baseKeys.keys - hiKeys.keys
        val missingInMr = baseKeys.keys - mrKeys.keys
        val missingInAs = baseKeys.keys - asKeys.keys

        assertTrue("Missing in Hindi: $missingInHi", missingInHi.isEmpty())
        assertTrue("Missing in Marathi: $missingInMr", missingInMr.isEmpty())
        assertTrue("Missing in Assamese: $missingInAs", missingInAs.isEmpty())

        // Ensure no empty string values in any language
        hiKeys.forEach { (key, value) ->
            assertFalse("Hindi key '$key' has blank translation", value.isBlank())
        }
        mrKeys.forEach { (key, value) ->
            assertFalse("Marathi key '$key' has blank translation", value.isBlank())
        }
        asKeys.forEach { (key, value) ->
            assertFalse("Assamese key '$key' has blank translation", value.isBlank())
        }
    }

    @Test
    fun `critical emergency and settings strings are present across all languages`() {
        val baseFile = findResFile("values/strings.xml")
        val keys = parseStringKeys(baseFile).keys

        val requiredKeys = setOf(
            "action_evacuate_now",
            "hazard_move_away_desc",
            "siren_active",
            "siren_silenced",
            "label_safe_place",
            "action_view_safe_route",
            "action_silence_siren",
            "action_im_safe_ack",
            "settings_title",
            "language_title",
            "lang_en",
            "lang_hi",
            "lang_mr",
            "lang_as",
            "role_citizen_name",
            "role_police_name",
            "role_rescue_name",
            "role_admin_name",
            "role_emergency_name"
        )

        for (req in requiredKeys) {
            assertTrue("Key '$req' must be defined in ${baseFile.absolutePath}, total keys found: ${keys.size}", keys.contains(req))
        }
    }

    private fun findResFile(subPath: String): File {
        val candidates = listOf(
            File("src/main/res", subPath),
            File("app/src/main/res", subPath),
            File("../app/src/main/res", subPath)
        )
        return candidates.firstOrNull { it.exists() } ?: File("src/main/res", subPath)
    }

    private fun parseStringKeys(file: File): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        doc.documentElement.normalize()

        val stringNodes = doc.getElementsByTagName("string")
        val map = mutableMapOf<String, String>()

        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i) as? Element ?: continue
            val name = node.getAttribute("name")
            val content = node.textContent ?: ""
            if (name.isNotBlank()) {
                map[name] = content
            }
        }
        return map
    }
}
