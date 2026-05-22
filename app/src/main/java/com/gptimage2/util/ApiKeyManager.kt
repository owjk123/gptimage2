package com.gptimage2.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gptimage2.data.model.PromptTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ApiKeyManager {

    private const val PREFS_NAME = "gptimage2_prefs"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_MODEL = "model"
    private const val KEY_CUSTOM_TEMPLATES = "custom_templates"
    const val DEFAULT_BASE_URL = "https://api.apiyi.com"
    const val DEFAULT_MODEL = "gpt-image-2-all"

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _apiKey.value = loadApiKey(context)
    }

    fun saveApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_API_KEY, key.trim()).apply()
        _apiKey.value = key.trim()
    }

    fun loadApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_API_KEY, "") ?: ""
    }

    fun saveBaseUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_BASE_URL, url.trimEnd('/')).apply()
    }

    fun loadBaseUrl(context: Context): String {
        return getPrefs(context).getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun saveModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_MODEL, model.trim()).apply()
    }

    fun loadModel(context: Context): String {
        return getPrefs(context).getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun hasKey(context: Context) = loadApiKey(context).isNotBlank()

    // Custom template management
    fun saveCustomTemplate(context: Context, template: PromptTemplate) {
        val templates = getCustomTemplates(context).toMutableList()
        val existingIndex = templates.indexOfFirst { it.id == template.id }
        if (existingIndex >= 0) {
            templates[existingIndex] = template
        } else {
            templates.add(template)
        }
        saveCustomTemplates(context, templates)
    }

    fun deleteCustomTemplate(context: Context, templateId: String) {
        val templates = getCustomTemplates(context).filter { it.id != templateId }
        saveCustomTemplates(context, templates)
    }

    fun getCustomTemplates(context: Context): List<PromptTemplate> {
        val json = getPrefs(context).getString(KEY_CUSTOM_TEMPLATES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PromptTemplate>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCustomTemplates(context: Context, templates: List<PromptTemplate>) {
        val json = gson.toJson(templates)
        getPrefs(context).edit().putString(KEY_CUSTOM_TEMPLATES, json).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return prefs
    }
}
