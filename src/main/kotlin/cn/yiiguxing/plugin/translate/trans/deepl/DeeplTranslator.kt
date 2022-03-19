@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans.deepl

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.DEEPL
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.md5
import com.google.gson.Gson
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import java.util.*
import javax.swing.Icon

/**
 * Deepl translator
 */
object DeeplTranslator : AbstractTranslator() {

    private const val DEEPL_FREE_TRANSLATE_API_URL = "https://api-free.deepl.com/v2/translate"
    private const val DEEPL_PRO_TRANSLATE_API_URL = "https://api.deepl.com/v2/translate"
    private const val DEEPL_PRODUCT_URL = "https://www.deepl.com/translator"


    /** 通用版支持的语言列表 */
    private val SUPPORTED_LANGUAGES: List<Lang> = listOf(
        Lang.BULGARIAN,
        Lang.CHINESE,
        Lang.CZECH,
        Lang.DANISH,
        Lang.DUTCH,
        Lang.GERMAN,
        Lang.GREEK,
        Lang.ENGLISH,
        Lang.ESTONIAN,
        Lang.FINNISH,
        Lang.FRENCH,
        Lang.HUNGARIAN,
        Lang.ITALIAN,
        Lang.JAPANESE,
        Lang.LATVIAN,
        Lang.LITHUANIAN,
        Lang.POLISH,
        Lang.PORTUGUESE,
        Lang.ROMANIAN,
        Lang.RUSSIAN,
        Lang.SLOVAK,
        Lang.SLOVENIAN,
        Lang.SPANISH,
        Lang.SWEDISH,
    )

    private val SUPPORTED_TARGET_LANGUAGES: List<Lang> = listOf(
        Lang.BULGARIAN,
        Lang.CHINESE,
        Lang.CZECH,
        Lang.DANISH,
        Lang.DUTCH,
        Lang.GERMAN,
        Lang.GREEK,
        Lang.ENGLISH,
        Lang.ENGLISH_AMERICAN,
        Lang.ENGLISH_BRITISH,
        Lang.ESTONIAN,
        Lang.FINNISH,
        Lang.FRENCH,
        Lang.HUNGARIAN,
        Lang.ITALIAN,
        Lang.JAPANESE,
        Lang.LATVIAN,
        Lang.LITHUANIAN,
        Lang.POLISH,
        Lang.PORTUGUESE,
        Lang.PORTUGUESE_BRAZILIAN,
        Lang.PORTUGUESE_PORTUGUESE,
        Lang.ROMANIAN,
        Lang.RUSSIAN,
        Lang.SLOVAK,
        Lang.SLOVENIAN,
        Lang.SPANISH,
        Lang.SWEDISH,
    )

    private val logger: Logger = Logger.getInstance(DeeplTranslator::class.java)

    override val id: String = DEEPL.id

    override val name: String = DEEPL.translatorName

    override val icon: Icon = DEEPL.icon

    override val intervalLimit: Int = DEEPL.intervalLimit

    override val contentLengthLimit: Int = DEEPL.contentLengthLimit

    override val primaryLanguage: Lang
        get() = DEEPL.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES
        .toMutableList()
        .apply { add(0, Lang.AUTO) }
    override val supportedTargetLanguages: List<Lang> = SUPPORTED_TARGET_LANGUAGES

    private val errorMessageMap: Map<Int, String> by lazy {
        mapOf(
            400 to message("error.bad.request"), // Bad request. Please check error message and your parameters.
            403 to message("error.invalidAccount"), // Authorization failed. Please supply a valid auth_key parameter.
            404 to message("error.request.not.found"), // The requested resource could not be found.
            413 to message("error.text.too.long"), // The request size exceeds the limit.
            414 to message("error.request.uri.too.long"), // The request URL is too long. You can avoid this error by using a POST request instead of a GET request, and sending the parameters in the HTTP body.
            429 to message("error.too.many.requests"), // Too many requests. Please wait and resend your request.
            456 to message("error.access.limited"), // Quota exceeded. The character limit has been reached.
            503 to message("error.deepl.service.is.down"), // Resource currently unavailable. Try again later.
            529 to message("error.too.many.requests"), // Too many requests. Please wait and resend your request.
            500 to message("error.systemError"), // Internal error
        )
    }

    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || Settings.deeplTranslateSettings.let { it.appId.isEmpty() || it.getAppKey().isEmpty() }) {
            return DEEPL.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(this, DeeplTranslator::call, DeeplTranslator::parseTranslation).execute(
            text,
            srcLang,
            targetLang
        )
    }

    private fun call(text: String, srcLang: Lang, targetLang: Lang): String {
        val settings = Settings.deeplTranslateSettings
        val privateKey = settings.getAppKey()
        val splitPrivatekeys = privateKey.split(":")
        val isFree = (splitPrivatekeys.size == 2 && splitPrivatekeys[1] == "fx")
        val requestURL = if (isFree) DEEPL_FREE_TRANSLATE_API_URL else DEEPL_PRO_TRANSLATE_API_URL

        return Http.post(
            requestURL,
            "auth_key" to privateKey,
            "tag_handling" to "xml",
            "target_lang" to targetLang.deeplLanguageCode,
            "text" to text
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun parseTranslation(translation: String, original: String, srcLang: Lang, targetLang: Lang): Translation {
        logger.i("Translate result: $translation")

        return Gson().fromJson(translation, DeeplTranslations::class.java).apply {
            this.original = original
            this.targetLang = targetLang
            if (!isSuccessful) {
                throw TranslationResultException(translations[0].code)
            }
        }.toTranslation()
    }
}