package org.jbali.runtime

import org.jbali.util.logger
import kotlin.collections.iterator

/**
 * Manages runtime updates to system properties with ownership tracking and automatic restoration.
 *
 * This class provides safe, auditable updates to [System.getProperties]. It "owns" the properties it sets
 * and can restore them to their original values when they're removed from subsequent updates.
 *
 * **Value handling:**
 * - All values are converted to strings (via [toString] or empty string for null)
 * - External property values (even non-string objects) are converted to strings when captured and restored
 * - Distinguishes between absent keys and keys with empty/null values
 * - Values are compared using string equality
 *
 * **Ownership model:**
 * - When a property is set via [update], this class takes ownership and records:
 *   - Whether the key was originally present
 *   - The original string value (converted via toString if needed, or null if key was absent)
 *   - The new string value that was set
 * - When a previously-owned property is absent from a subsequent [update]:
 *   - If the current system value equals our set value → restore original state (value or absent)
 *   - If the current system value differs → log a warning (external modification detected)
 *   - Ownership is always released
 *
 * **Logging:**
 * - All property additions, modifications, and restorations are logged at INFO level
 * - External modifications (when detected during restoration) are logged at WARN level
 * - The [name] parameter is included in all log messages for traceability
 *
 * **Thread safety:**
 * - Not thread-safe. Callers must synchronize if calling [update] from multiple threads.
 *
 * **Example usage:**
 * ```kotlin
 * val updater = SystemPropertyRuntimeUpdater("config.properties")
 * updater.update(mapOf("foo" to "bar", "baz" to "qux"))  // Sets foo=bar, baz=qux
 * updater.update(mapOf("foo" to "modified"))              // Updates foo, restores baz to original
 * ```
 *
 * @param name Optional identifier for this property source, used in logging (e.g., "file:/etc/app.properties").
 */
class SystemPropertyRuntimeUpdater(
    val name: String? = null,
) {

    private val log = logger<SystemPropertyRuntimeUpdater>()

    private data class OwnedProperty(
        val originalKeyPresent: Boolean,
        val originalValue: String?,
        val currentValue: String
    )

    private val ownedProperties = mutableMapOf<String, OwnedProperty>()

    private val sourceName get() = name?.let { " from $it" } ?: ""

    /**
     * Updates system properties to match the given [props] map.
     *
     * This method:
     * 1. Sets or updates properties present in [props] (taking ownership if not already owned)
     * 2. Restores properties that were previously owned but are now absent from [props]
     *
     * All changes are logged with details about previous and new values.
     *
     * @param props The desired system properties. Keys are property names, values are strings (null becomes empty string).
     */
    fun update(props: Map<String, String?>) {
        // Handle new and updated properties
        for ((key, newValue) in props) {
            val newValueStr = newValue ?: ""
            val currentSystemValueObj = System.getProperties()[key]
            val currentSystemValue = currentSystemValueObj?.toString()
            val owned = ownedProperties[key]

            when {
                owned == null -> {
                    // New property - take ownership
                    System.setProperty(key, newValueStr)
                    ownedProperties[key] = OwnedProperty(
                        originalKeyPresent = currentSystemValueObj != null,
                        originalValue = currentSystemValue,
                        currentValue = newValueStr
                    )
                    if (currentSystemValueObj == null) {
                        log.info("Set system property$sourceName: $key=$newValueStr")
                    } else {
                        log.info("Override system property$sourceName: $key: $currentSystemValue → $newValueStr")
                    }
                }
                owned.currentValue != newValueStr -> {
                    // Owned property with new value
                    if (currentSystemValue != owned.currentValue) {
                        log.warn("System property $key was externally modified$sourceName: " +
                                "expected=${owned.currentValue}, actual=$currentSystemValue, setting to $newValueStr")
                    }
                    System.setProperty(key, newValueStr)
                    ownedProperties[key] = owned.copy(currentValue = newValueStr)
                    log.info("Update system property$sourceName: $key: ${owned.currentValue} → $newValueStr")
                }
                // else: owned.currentValue == newValueStr, no change needed
            }
        }

        // Handle removed properties (restore originals)
        val removedKeys = ownedProperties.keys - props.keys
        for (key in removedKeys) {
            val owned = ownedProperties[key]!!
            val currentSystemValueObj = System.getProperties()[key]
            val currentSystemValue = currentSystemValueObj?.toString()

            if (currentSystemValue == owned.currentValue) {
                // Our value is still in place - restore original
                if (!owned.originalKeyPresent) {
                    System.clearProperty(key)
                    log.info("Restore system property$sourceName: $key removed (was ${owned.currentValue})")
                } else {
                    System.setProperty(key, owned.originalValue!!)
                    log.info("Restore system property$sourceName: $key: ${owned.currentValue} → ${owned.originalValue}")
                }
            } else {
                // Value was modified externally - log but don't restore
                log.warn("System property $key was externally modified$sourceName: " +
                        "expected=${owned.currentValue}, actual=$currentSystemValue, releasing ownership without restoration")
            }
            ownedProperties.remove(key)
        }
    }

}