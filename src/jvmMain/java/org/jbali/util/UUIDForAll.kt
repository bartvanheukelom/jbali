package org.jbali.util

import java.util.*

/**
 * Data types that implement this interface are identified by a [UUID].
 * This UUID must be unique among all objects of that type. It may be shared by objects
 * of different, but related types. For example, a single UUID can be used by a request, its response,
 * an error contained in said response, and the [Throwable] that is used to transport this error.
 */
interface UUIdentifiable {
    val uuid: UUID
}

/** @return The UUID of the receiver if it natively has one (implements [UUIdentifiable]), otherwise one that is linked to the object's identity. */
val Any.uuid: UUID get() =
    if (this is UUIdentifiable) uuid
    else objectIdentityUUID

/**
 * Returns a UUID that is linked to the receiver object's identity. This exact extension property will always return
 * the same UUID for a given object during its lifetime in this process. No further guarantees are given, so:
 * - Different results may be returned by this property in different [ClassLoader]s.
 * - Serialization of the receiving object changes its identity and the value of this property.
 * - There is no deterministic conversion from identity (if it were made a concrete number) to UUID.
 */
val Any.objectIdentityUUID: UUID
        by StoredExtensionProperty.ignoringReceiver(UUID::randomUUID)
