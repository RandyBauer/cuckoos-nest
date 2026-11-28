package io.github.randybauer.cuckoosnest.decoy;

/**
 * A kind of decoy. Each kind is a plugin: a Spring bean that implements this interface.
 *
 * <p>Adding a new kind of decoy means writing one class like this (plus its tests). The registry
 * finds it automatically, so nothing else has to change.
 */
public interface DecoyType {

    /** Stable id stored in the database, for example {@code URL}. Uppercase letters and underscores. */
    String id();

    /** Short human name, for example "URL decoy". */
    String displayName();

    /** Makes a new secret token for a decoy of this kind. */
    String newToken(TokenGenerator tokens);

    /**
     * The bait the owner plants: the text to paste into a document, config file, or tool list.
     *
     * @param decoy a decoy of this type
     * @param baseUrl the public address of this Nest, without a trailing slash
     */
    String bait(Decoy decoy, String baseUrl);
}
