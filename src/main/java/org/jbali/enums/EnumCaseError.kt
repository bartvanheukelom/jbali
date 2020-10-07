package org.jbali.enums

class EnumCaseError(enumValue: Enum<*>)
    : AssertionError("No handler defined for ${enumValue.fullname}")
