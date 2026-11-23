package com.syedtechdevelopers.tuberankseo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
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

    // ---------- Free limit & PRO ----------
    companion object {
        const val FREE_PER_DAY = 5
        const val ADS_FOR_PRO = 5
        const val PRO_HOURS = 5
    }

    private fun today() = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())

    var proUntil by mutableStateOf(store.proUntil)
        private set
    var proAds by mutableStateOf(store.proAds)
        private set
    var freeUsed by mutableIntStateOf(0)
        private set
    var showLimit by mutableStateOf(false)
    var showPro by mutableStateOf(false)

    /** Updated once a minute by the UI so the timer and PRO state stay current. */
    var now by mutableStateOf(System.currentTimeMillis())

    val isPro: Boolean get() = proUntil > now
    val freeLeft: Int get() = (FREE_PER_DAY - freeUsed).coerceAtLeast(0)

    init {
        refreshDay()
    }

    fun refreshDay() {
        now = System.currentTimeMillis()
        if (store.freeDay != today()) {
            store.freeDay = today(); store.freeUsed = 0
        }
        freeUsed = store.freeUsed
    }

    /** Call after each fully watched rewarded ad. Returns true when PRO was just activated. */
    fun onProAdWatched(): Boolean {
        proAds = (proAds + 1).also { store.proAds = it }
        if (proAds >= ADS_FOR_PRO) {
            proUntil = System.currentTimeMillis() + PRO_HOURS * 3_600_000L
            store.proUntil = proUntil
            proAds = 0; store.proAds = 0
            now = System.currentTimeMillis()
            return true
        }
        return false
    }

    /** False when the free limit is used up (then the limit dialog is shown instead). */
    fun mayGenerate(): Boolean {
        refreshDay()
        if (isPro || freeLeft > 0) return true
        showLimit = true
        return false
    }

    // ---------- Google account ----------
    var userName by mutableStateOf(store.userName)
        private set
    var userEmail by mutableStateOf(store.userEmail)
        private set
    var userPhoto by mutableStateOf(store.userPhoto)
        private set
    var showWelcome by mutableStateOf(store.userEmail.isBlank() && !store.loginSkipped)
        private set

    fun signedIn(p: com.syedtechdevelopers.tuberankseo.ui.GoogleProfile) {
        userName = p.name; userEmail = p.email; userPhoto = p.photo
        store.userName = p.name; store.userEmail = p.email; store.userPhoto = p.photo
        showWelcome = false
    }

    fun skipLogin() {
        store.loginSkipped = true; showWelcome = false
    }

    fun signOut() {
        userName = ""; userEmail = ""; userPhoto = ""
        store.userName = ""; store.userEmail = ""; store.userPhoto = ""
    }

    var loading by mutableStateOf(false)
        private set
    var result by mutableStateOf<SeoResult?>(null)
        private set
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
        titleSeed = 0
        showResult = true
        val p = platform
        val pro = isPro
        if (!pro) {
            freeUsed += 1; store.freeUsed = freeUsed
        }
        job = viewModelScope.launch {
            val (weights, online) = Suggest.collect(kw, lang, country, p)
            val r = SeoEngine.build(kw, weights, online, lang, country, p, pro)
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
