package com.example.mushaf.data.db

import android.content.Context
import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.model.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader

class QuranMetadataDataSource(private val context: Context) {

    private var surahs: List<Surah> = emptyList()
    private var juzs: List<Juz> = emptyList()
    private var hizbs: List<Hizb> = emptyList()
    
    private var isLoaded = false

    private suspend fun loadIfNeeded() = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext
        try {
            // Load Surahs
            val surahsString = context.assets.open("quran_surahs.json").use { inputStream ->
                InputStreamReader(inputStream).readText()
            }
            val surahsArray = org.json.JSONArray(surahsString)
            val surahList = mutableListOf<Surah>()
            for (i in 0 until surahsArray.length()) {
                val obj = surahsArray.getJSONObject(i)
                val type = obj.getString("type")
                surahList.add(
                    Surah(
                        number = obj.getInt("id"),
                        nameArabic = obj.getString("name_ar"),
                        nameEnglish = obj.getString("name"),
                        origin = if (type.equals("meccan", ignoreCase = true)) 
                            com.example.mushaf.domain.model.SurahOrigin.MECCAN 
                        else 
                            com.example.mushaf.domain.model.SurahOrigin.MEDINAN,
                        revelationType = type,
                        verseCount = obj.getInt("total_verses")
                    )
                )
            }
            
            // Load Juzs
            val juzsString = context.assets.open("quran_juzs.json").use { inputStream ->
                InputStreamReader(inputStream).readText()
            }
            val juzsArray = org.json.JSONArray(juzsString)
            val juzList = mutableListOf<Juz>()
            for (i in 0 until juzsArray.length()) {
                val obj = juzsArray.getJSONObject(i)
                juzList.add(
                    Juz(
                        number = obj.getInt("number"),
                        nameEn = obj.getString("name"),
                        nameAr = obj.getString("name_ar")
                    )
                )
            }

            // No Hizb file provided yet, returning empty
            val hizbList = mutableListOf<Hizb>()
            
            surahs = surahList
            juzs = juzList
            hizbs = hizbList
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun searchSurah(query: String): List<Surah> = withContext(Dispatchers.Default) {
        loadIfNeeded()
        if (query.isBlank()) return@withContext surahs
        val q = query.lowercase().trim()
        
        surahs.mapNotNull { surah ->
            val matchScore = when {
                surah.nameEn.lowercase() == q || surah.nameAr == q || surah.number.toString() == q -> 3
                surah.nameEn.lowercase().startsWith(q) || surah.nameAr.startsWith(q) -> 2
                surah.nameEn.lowercase().contains(q) || surah.nameAr.contains(q) -> 1
                else -> 0
            }
            if (matchScore > 0) Pair(surah, matchScore) else null
        }.sortedByDescending { it.second }.map { it.first }
    }

    suspend fun searchJuz(query: String): List<Juz> = withContext(Dispatchers.Default) {
        loadIfNeeded()
        if (query.isBlank()) return@withContext juzs
        val q = query.lowercase().trim()
        juzs.filter { 
            it.number.toString() == q || it.nameEn.lowercase().contains(q) || it.nameAr.contains(q)
        }
    }

    suspend fun searchHizb(query: String): List<Hizb> = withContext(Dispatchers.Default) {
        loadIfNeeded()
        if (query.isBlank()) return@withContext hizbs
        val q = query.trim()
        hizbs.filter { 
            it.number.toString().contains(q)
        }
    }

    suspend fun getSurah(id: Int): Surah? = withContext(Dispatchers.Default) {
        loadIfNeeded()
        surahs.find { it.number == id }
    }
}
