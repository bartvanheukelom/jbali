package org.jbali.serialize

internal object SerializableProcessors {
    val collectionIfaces = setOf(
        "java.util.Collection",
        "java.util.List",
        "java.util.Set",
        "java.util.Map",
        "java.util.Map\$Entry",
    )
    val collectionIfacesSimple = collectionIfaces.map { it
        .removePrefix("java.util.")
        .replace('$', '.') // inner classes
    }
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.TYPE)
annotation class SuppressNotSerializable(
    val value: String = "",
)
