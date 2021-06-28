package com.adkom666.shrednotes.util

import android.content.SharedPreferences
import com.adkom666.shrednotes.util.time.Days

/**
 * Retrieve a [Days] value from the preferences or return null if it does not need (preference with
 * the key [presenceAttributeKey] does not contain true value).
 *
 * @param key the name of the preference to retrieve.
 * @param presenceAttributeKey the name of the preference which contain true if the target
 * preference is really need to user. If it contains false, null will be returned.
 * @param defaultValue value to return if this preference is need but does not exist.
 * @return the preference value if it exists and need (true in the preference with key
 * [presenceAttributeKey]), or null (where is not true in the preference with key
 * [presenceAttributeKey]), or [defaultValue] if it need (true in preference with the key
 * presenceAttributeKey]), but does not exist.
 * @throws ClassCastException if there is a preference with [key] that is not a [Days].
 */
fun SharedPreferences.getNullableDays(
    key: String,
    presenceAttributeKey: String,
    defaultValue: Days
): Days? = getNullableLong(
    key,
    presenceAttributeKey,
    defaultValue.epochMillis
)?.let {
    Days(it)
}

/**
 * Retrieve a [Long] value from the preferences or return null if it does not need (preference with
 * the key [presenceAttributeKey] does not contain true value).
 *
 * @param key the name of the preference to retrieve.
 * @param presenceAttributeKey the name of the preference which contain true if the target
 * preference is really need to user. If it contains false, null will be returned.
 * @param defaultValue value to return if this preference is need but does not exist.
 * @return the preference value if it exists and need (true in the preference with key
 * [presenceAttributeKey]), or null (where is not true in the preference with key
 * [presenceAttributeKey]), or [defaultValue] if it need (true in preference with the key
 * presenceAttributeKey]), but does not exist.
 * @throws ClassCastException if there is a preference with [key] that is not a [Long].
 */
fun SharedPreferences.getNullableLong(
    key: String,
    presenceAttributeKey: String,
    defaultValue: Long
): Long? = if (getBoolean(presenceAttributeKey, false)) {
    getLong(key, defaultValue)
} else {
    null
}

/**
 * Retrieve an [Int] value from the preferences or return null if it does not need (preference with
 * the key [presenceAttributeKey] does not contain true value).
 *
 * @param key the name of the preference to retrieve.
 * @param presenceAttributeKey the name of the preference which contain true if the target
 * preference is really need to user. If it contains false, null will be returned.
 * @param defaultValue value to return if this preference is need but does not exist.
 * @return the preference value if it exists and need (true in the preference with key
 * [presenceAttributeKey]), or null (where is not true in the preference with key
 * [presenceAttributeKey]), or [defaultValue] if it need (true in preference with the key
 * presenceAttributeKey]), but does not exist.
 * @throws ClassCastException if there is a preference with [key] that is not an [Int].
 */
fun SharedPreferences.getNullableInt(
    key: String,
    presenceAttributeKey: String,
    defaultValue: Int
): Int? = if (getBoolean(presenceAttributeKey, false)) {
    getInt(key, defaultValue)
} else {
    null
}

/**
 * Set a [Days] value, which can be null, in the preferences editor, to be written back once
 * [SharedPreferences.Editor.commit] or [SharedPreferences.Editor.apply] are called.
 *
 * @param key the name of the preference to modify.
 * @param presenceAttributeKey the name of the preference to write true if [value] is not null or to
 * write false otherwise.
 * @param value the new value to save.
 * @return a reference to the same [SharedPreferences.Editor] object, so you can chain put calls
 * together.
 */
fun SharedPreferences.Editor.putNullableDays(
    key: String,
    presenceAttributeKey: String,
    value: Days?
): SharedPreferences.Editor = putNullableLong(
    key,
    presenceAttributeKey,
    value?.epochMillis
)

/**
 * Set a [Long] value, which can be null, in the preferences editor, to be written back once
 * [SharedPreferences.Editor.commit] or [SharedPreferences.Editor.apply] are called.
 *
 * @param key the name of the preference to modify.
 * @param presenceAttributeKey the name of the preference to write true if [value] is not null or to
 * write false otherwise.
 * @param value the new value to save.
 * @return Returns a reference to the same [SharedPreferences.Editor] object, so you can chain put
 * calls together.
 */
fun SharedPreferences.Editor.putNullableLong(
    key: String,
    presenceAttributeKey: String,
    value: Long?
): SharedPreferences.Editor = value?.let { safeValue ->
    putLong(key, safeValue)
    putBoolean(presenceAttributeKey, true)
} ?: putBoolean(presenceAttributeKey, false)

/**
 * Set an [Int] value, which can be null, in the preferences editor, to be written back once
 * [SharedPreferences.Editor.commit] or [SharedPreferences.Editor.apply] are called.
 *
 * @param key the name of the preference to modify.
 * @param presenceAttributeKey the name of the preference to write true if [value] is not null or to
 * write false otherwise.
 * @param value the new value to save.
 * @return Returns a reference to the same [SharedPreferences.Editor] object, so you can chain put
 * calls together.
 */
fun SharedPreferences.Editor.putNullableInt(
    key: String,
    presenceAttributeKey: String,
    value: Int?
): SharedPreferences.Editor = value?.let { safeValue ->
    putInt(key, safeValue)
    putBoolean(presenceAttributeKey, true)
} ?: putBoolean(presenceAttributeKey, false)
