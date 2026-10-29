package com.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.jsoup.Jsoup

class BlankFragment(private val plugin: ExamplePlugin) : BottomSheetDialogFragment() {

    @SuppressLint("DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = plugin.resources?.getIdentifier(
            "fragment_blank",
            "layout",
            BuildConfig.LIBRARY_PACKAGE_NAME
        ) ?: return null

        return inflater.inflate(
            plugin.resources?.getLayout(layoutId) ?: return null,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val siteUrlInput = view.findViewById<EditText>(R.id.siteUrlInput)
        val selectorInput = view.findViewById<EditText>(R.id.selectorInput)
        val testButton = view.findViewById<Button>(R.id.testButton)
        val extractButton = view.findViewById<Button>(R.id.extractButton)
        val resultText = view.findViewById<TextView>(R.id.resultText)

        extractButton.isEnabled = false

        testButton.setOnClickListener {
            val url = siteUrlInput.text.toString().trim()
            val selector = selectorInput.text.toString().trim()

            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                resultText.text = "Error: Enter a valid URL starting with http:// or https://"
                return@setOnClickListener
            }

            if (selector.isEmpty()) {
                resultText.text = "Error: Enter a CSS selector first."
                return@setOnClickListener
            }

            testButton.isEnabled = false
            extractButton.isEnabled = false
            resultText.text = "Loading website...\n$url"

            Thread {
                val output = try {
                    val document = Jsoup.connect(url)
                        .userAgent(
                            "Mozilla/5.0 (Linux; Android 15) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/130.0.0.0 Mobile Safari/537.36"
                        )
                        .timeout(20000)
                        .get()

                    val elements = document.select(selector)
                    val result = StringBuilder()

                    result.append("Website loaded successfully\n")
                    result.append("Page title: ${document.title()}\n")
                    result.append("Selector: $selector\n")
                    result.append("Matches found: ${elements.size}\n")
                    result.append("--------------------------------\n")

                    if (elements.isEmpty()) {
                        result.append(
                            "No matching elements found.\n\n" +
                            "Check the selector or whether the page " +
                            "creates its content using JavaScript."
                        )
                    } else {
                        elements.take(50).forEachIndexed { index, element ->
                            result.append("\n[${index + 1}]\n")

                            val text = element.text()
                                .replace(Regex("\\s+"), " ")
                                .take(300)

                            result.append("Text: $text\n")

                            val href = element.absUrl("href")
                            if (href.isNotEmpty()) {
                                result.append("Link: $href\n")
                            }

                            val src = element.absUrl("src")
                            if (src.isNotEmpty()) {
                                result.append("Source: $src\n")
                            }

                            listOf(
                                "data-url",
                                "data-href",
                                "data-src",
                                "data-link",
                                "data-video"
                            ).forEach { attribute ->
                                val value = element.attr(attribute)
                                if (value.isNotBlank()) {
                                    result.append("$attribute: $value\n")
                                }
                            }

                            result.append("--------------------------------\n")
                        }

                        if (elements.size > 50) {
                            result.append(
                                "\nOnly the first 50 results are displayed."
                            )
                        }
                    }

                    result.toString()
                } catch (e: Exception) {
                    "Request failed:\n" +
                    "${e.javaClass.simpleName}: ${e.message ?: "Unknown error"}"
                }

                activity?.runOnUiThread {
                    if (isAdded && view.isAttachedToWindow) {
                        resultText.text = output
                        testButton.isEnabled = true
                        extractButton.isEnabled = false
                    }
                }
            }.start()
        }
    }
}