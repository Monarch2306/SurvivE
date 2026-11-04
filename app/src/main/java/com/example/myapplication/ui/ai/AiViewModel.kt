package com.example.myapplication.ui.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ChatMessage(val text: String, val isUser: Boolean)

class AiViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _isCopying = MutableStateFlow(false)
    val isCopying: StateFlow<Boolean> = _isCopying.asStateFlow()

    private val _copyProgress = MutableStateFlow(0f)
    val copyProgress: StateFlow<Float> = _copyProgress.asStateFlow()

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private var llmInference: LlmInference? = null

    private val MODEL_FILENAME = "qwen2_5_0_5b.task"
    private val MODEL_URL = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"

    private val SYSTEM_PROMPT = """
        You are Emergence AI, an offline, safety-critical field assistant designed for survival strategies, emergency triage, outdoor navigation, and daily practical commerce.
        
        Core Directives:
        1. Conciseness First: Provide actionable, step-by-step instructions. Avoid filler, disclaimers, or conversational pleasantries.
        2. Safety & Ground Truth: If an edible plant, chemical, or medical procedure is dangerous or uncertain, advise against it directly. Never guess dosages.
        3. Resourcefulness: Focus on low-tech, field-expedient solutions using commonly scavenged or commercial daily items.
        4. Formatting: Use numbered steps for procedures, bold text for critical warnings.
    """.trimIndent()

    fun checkModelExists(context: Context): Boolean {
        val file = File(context.filesDir, MODEL_FILENAME)
        return file.exists() && file.length() > 50 * 1024 * 1024
    }

    fun openDownloadLinkInBrowser(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(MODEL_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _statusMessage.value = "Unable to open browser: ${e.message}"
        }
    }

    fun importModelFromUri(context: Context, uri: Uri, onComplete: () -> Unit) {
        if (_isCopying.value) return

        _isCopying.value = true
        _copyProgress.value = 0f
        _statusMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val destFile = File(context.filesDir, MODEL_FILENAME)
            val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")

            try {
                if (tempFile.exists()) tempFile.delete()

                val contentResolver = context.contentResolver
                val fileDescriptor = contentResolver.openAssetFileDescriptor(uri, "r")
                val totalBytes = fileDescriptor?.length ?: -1L
                fileDescriptor?.close()

                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        var copiedBytes = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            copiedBytes += bytesRead
                            if (totalBytes > 0) {
                                _copyProgress.value = copiedBytes.toFloat() / totalBytes
                            }
                        }
                    }
                } ?: throw Exception("Cannot read selected file.")

                if (destFile.exists()) destFile.delete()
                tempFile.renameTo(destFile)

                _isCopying.value = false
                _statusMessage.value = "Model imported successfully!"
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                _isCopying.value = false
                _statusMessage.value = "Import failed: ${e.message}"
            }
        }
    }

    fun initModel(context: Context) {
        if (_isModelLoaded.value || _isInitializing.value) return

        _isInitializing.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            try {
                val modelFile = File(context.filesDir, MODEL_FILENAME)
                if (modelFile.exists()) {
                    withContext(Dispatchers.IO) {
                        val options = LlmInference.LlmInferenceOptions.builder()
                            .setModelPath(modelFile.absolutePath)
                            .setMaxTokens(1024)
                            .build()
                        llmInference = LlmInference.createFromOptions(context, options)
                    }
                    _isModelLoaded.value = true
                } else {
                    _statusMessage.value = "Model file not found. Please select the file first."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading model: ${e.message}"
                _messages.value += ChatMessage("Error loading model: ${e.message}", false)
            } finally {
                _isInitializing.value = false
            }
        }
    }

    private fun onTokenChunkReceived(partialResult: String, isDone: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            val currentList = _messages.value.toMutableList()
            if (currentList.isNotEmpty() && !currentList.last().isUser) {
                val accumulated = currentList.last().text + partialResult
                val cleanText = accumulated
                    .replace(Regex("<\\|im_start\\|>.*?<\\|im_end\\|>"), "")
                    .replace("<|im_end|>", "")
                    .replace("<|im_start|>", "")
                currentList[currentList.size - 1] = ChatMessage(cleanText, false)
                _messages.value = currentList
            }
            if (isDone) {
                _isGenerating.value = false
            }
        }
    }

    fun closeModel() {
        llmInference?.close()
        llmInference = null
        _isModelLoaded.value = false
        _isGenerating.value = false
    }

    override fun onCleared() {
        super.onCleared()
        llmInference?.close()
    }

    fun sendMessage(text: String, fieldNotesContext: String = "") {
        if (_isGenerating.value) return

        val userMsg = ChatMessage(text, true)
        val assistantPlaceholder = ChatMessage("", false)
        _messages.value += userMsg
        _messages.value += assistantPlaceholder

        if (llmInference == null) {
            _messages.value = _messages.value.dropLast(1) + ChatMessage("SurviveAI engine not initialized.", false)
            return
        }

        _isGenerating.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contextHeader = if (fieldNotesContext.isNotBlank()) "\nVerified Offline Context:\n$fieldNotesContext" else ""
                val prompt = "<|im_start|>system\n$SYSTEM_PROMPT$contextHeader<|im_end|>\n" +
                        "<|im_start|>user\n$text<|im_end|>\n" +
                        "<|im_start|>assistant\n"

                llmInference?.generateResponseAsync(prompt) { partialResult, isDone ->
                    onTokenChunkReceived(partialResult ?: "", isDone)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    val currentList = _messages.value.toMutableList()
                    if (currentList.isNotEmpty() && !currentList.last().isUser) {
                        currentList[currentList.size - 1] = ChatMessage("SurviveAI Engine Error: ${e.message}", false)
                        _messages.value = currentList
                    }
                }
            }
        }
    }
}
