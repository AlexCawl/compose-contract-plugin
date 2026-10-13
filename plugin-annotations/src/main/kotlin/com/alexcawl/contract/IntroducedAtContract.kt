package com.alexcawl.contract

/**
 * Marks the version in which a default method was added to a [GenerateContract] interface.
 *
 * Methods without this annotation belong to the initial version, `0`. Explicit versions must
 * be positive; gaps and any source order are supported. Methods with the same version are
 * introduced together. Every annotated method must have a default body.
 *
 * The compiler plugin generates a callback-container constructor, factory, and copy extension
 * for the initial version and each distinct version, ordered newest first. The current container
 * constructor is primary; older constructors are secondary. Older signatures are hidden from
 * source and delegate to the current signature. Parameters are ordered by ascending version,
 * preserving source order within each version. New versions append callbacks to the signatures.
 * Preserve each published version's method set and relative method order. New methods must use
 * a version greater than every published version to retain existing signatures and argument
 * positions.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class IntroducedAtContract(public val version: Int)
