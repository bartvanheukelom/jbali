package org.jbali.util

/**
 * Apply to code that only serves as a workaround for the bug https://youtrack.jetbrains.com/issue/KT-43191
 */
@Retention(AnnotationRetention.SOURCE)
annotation class WorksAroundNonStaticCompanionDTS

/**
 * Apply to code that only serves as a workaround for the bug https://youtrack.jetbrains.com/issue/KT-37881
 */
@Retention(AnnotationRetention.SOURCE)
annotation class WorksAroundBaseNameMangling
