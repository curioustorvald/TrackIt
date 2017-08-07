package net.torvald.terrarum.langpack

import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.trackit.TrackIt
import java.io.*
import java.util.*

/**
 * Created by minjaesong on 16-01-22.
 */
object Lang {

    /**
     * Get record by its STRING_ID
     *
     * HashMap<"$key_$language", Value>
     *
     *     E.g. langpack["MENU_LANGUAGE_THIS_fiFI"]
     */
    val langpack = HashMap<String, String>()
    private val FALLBACK_LANG_CODE = "en"

    val languageList = HashSet<String>()

    val POLYGLOT_VERSION = "100"
    private val PREFIX_POLYGLOT = "Polyglot-${POLYGLOT_VERSION}_"

    init {
        // load base langs
        load("./assets/locales/")
    }

    fun load(localesDir: String) {
        println("[Lang] Loading languages from $localesDir")

        val localesDir = File(localesDir)

        // get all of the languages installed
        localesDir.listFiles().filter { it.isDirectory }.forEach { languageList.add(it.name) }

        for (lang in languageList) {
            val langFileListFiles = File("$localesDir/$lang/").listFiles()
            langFileListFiles.forEach {
                // not a polyglot
                if (!it.name.startsWith("Polyglot") && it.name.endsWith(".json")) {
                    processRegularLangfile(it, lang)
                }
                else if (it.name.startsWith("Polyglot") && it.name.endsWith(".json")) {
                    processPolyglotLangFile(it, lang)
                }
                // else, ignore
            }

        }
    }

    private fun processRegularLangfile(file: File, lang: String) {
        val json = JsonFetcher(file)
        /*
         * Terrarum langpack JSON structure is:
         *
         * (root object)
         *      "<<STRING ID>>" = "<<LOCALISED TEXT>>"
         */
        //println(json.entrySet())
        json.entrySet().forEach {
            langpack.put("${it.key}_$lang", it.value.asString)
        }
    }

    private fun processPolyglotLangFile(file: File, lang: String) {
        val json = JsonFetcher(file)
        /*
         * Polyglot JSON structure is:
         *
         * (root object)
         *      "resources": object
         *          "polyglot": object
         *              (polyglot meta)
         *          "data": array
         *             [0]: object
         *                  n = "CONTEXT_CHARACTER_CLASS"
         *                  s = "Class"
         *             [1]: object
         *                  n = "CONTEXT_CHARACTER_DELETE"
         *                  s = "Delecte Character"
         *             (the array continues)
         *
         */
        json.getAsJsonObject("resources").getAsJsonArray("data").forEach {
            langpack.put(
                    "${it.asJsonObject["n"].asString}_$lang",
                    it.asJsonObject["s"].asString
            )
        }
    }

    operator fun get(key: String): String {
        fun fallback(): String = langpack["${key}_$FALLBACK_LANG_CODE"] ?: "$$key"


        val ret = langpack["${key}_${TrackIt.sysLang}"]
        val ret2 = if (ret.isNullOrEmpty()) fallback() else ret!!

        return ret2.capitalize()
    }


    private fun getLastChar(s: String): Char {
        return s[s.length - 1]
    }
}
