package com.example.pocketenglish.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

// DataStoreはプロセス内で1インスタンスに限定する必要があるため、トップレベルのデリゲートで定義する
private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/** お気に入りと再生設定を DataStore に永続化するリポジトリ。 */
class UserPreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.userPrefsDataStore

    private val safeData: Flow<Preferences> = dataStore.data.catch { e ->
        // ファイル破損などの読み込みエラー時は空設定で続行し、アプリを落とさない
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val favoriteIds: Flow<Set<String>> = safeData
        .map { it[KEY_FAVORITES] ?: emptySet() }
        .distinctUntilChanged()

    val slowSpeech: Flow<Boolean> = safeData
        .map { it[KEY_SLOW_SPEECH] ?: false }
        .distinctUntilChanged()

    suspend fun toggleFavorite(phraseId: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES] ?: emptySet()
            prefs[KEY_FAVORITES] = if (phraseId in current) current - phraseId else current + phraseId
        }
    }

    suspend fun setSlowSpeech(enabled: Boolean) {
        dataStore.edit { it[KEY_SLOW_SPEECH] = enabled }
    }

    private companion object {
        val KEY_FAVORITES = stringSetPreferencesKey("favorite_phrase_ids")
        val KEY_SLOW_SPEECH = booleanPreferencesKey("slow_speech")
    }
}
