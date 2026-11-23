package com.syedtechdevelopers.tuberankseo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syedtechdevelopers.tuberankseo.data.Platform
import com.syedtechdevelopers.tuberankseo.data.SeoEngine
import com.syedtechdevelopers.tuberankseo.data.SeoResult
import com.syedtechdevelopers.tuberankseo.data.Store
import com.syedtechdevelopers.tuberankseo.data.Suggest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class Tab { Home, History, Settings }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val store = Store(app)

    var tab by mutableStateOf(Tab.Home)
    var showResult by mutableStateOf(false)

    var keyword by mutableStateOf("")
    var lang by mutableStateOf(store.lang)
        private set
    var country by mutableStateOf(store.country)
        private set

    var platform by mutableStateOf(store.platform)
        private set

    fun setPlatformTo(p: Platform) {
        platform = p; store.platform = p
    }

    var loading by mutableStateOf(false)
        private set
    var result by mutableStateOf<SeoResult?>(null)
        private set
    var ideasUnlocked by mutableStateOf(false)
    private var titleSeed = 0

    val history = mutableStateListOf<SeoResult>().apply { addAll(store.loadHistory()) }

    private var job: Job? = null

    fun setLanguage(v: String) {
        lang = v; store.lang = v
    }

    fun setRegion(v: String) {
        country = v; store.country = v
    }

    fun canGenerate() = keyword.trim().length >= 2 && !loading

    fun generate() {
        val kw = keyword.trim()
        if (kw.length < 2) return
        job?.cancel()
        loading = true
        result = null
        ideasUnlocked = false
        titleSeed = 0
        showResult = true
        val p = platform
        job = viewModelScope.launch {
            val (weights, online) = Suggest.collect(kw, lang, country, p)
            val r = SeoEngine.build(kw, weights, online, lang, country, p)
            result = r
            loading = false
            store.generations = store.generations + 1
            history.removeAll { it.keyword.equals(r.keyword, true) && it.platform == r.platform }
            history.add(0, r)
            while (history.size > 50) history.removeAt(history.lastIndex)
            store.saveHistory(history)
        }
    }

    fun open(r: SeoResult) {
        result = r
        keyword = r.keyword
        setPlatformTo(r.platform)
        loading = false
        ideasUnlocked = false
        titleSeed = 0
        showResult = true
    }

    fun moreTitles() {
        val r = result ?: return
        titleSeed++
        val mods = SeoEngine.modifiers(r.keyword.lowercase(), r.ideas.map { it.lowercase() })
        val fresh = SeoEngine.headlines(r.platform, r.keyword, r.ideas.map { it.lowercase() }, mods.shuffled().ifEmpty { mods }, r.lang, titleSeed)
        result = r.copy(titles = fresh)
    }

    fun deleteHistory(r: SeoResult) {
        history.remove(r); store.saveHistory(history)
    }

    fun clearHistory() {
        history.clear(); store.saveHistory(history)
    }
}
