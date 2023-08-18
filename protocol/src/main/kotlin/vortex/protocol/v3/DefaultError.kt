package vortex.protocol.v3

enum class DefaultError(val code: Int, val definite: Boolean) {
    /**
     * Indicates that the requested operation could not be completed within a
     * timeout.
     **/
    TIMEOUT(0, false),

    /**
     * Thrown when a client sends an RPC request to a node which does not exist.
     */
    NODE_NOT_FOUND(1, true),

    /**
     * Use this error to indicate that a requested operation is not supported by
     * the current implementation. Helpful for stubbing out APIs during
     * development.
     */
    NOT_SUPPORTED(10, true),

    /**
     * Indicates that the operation definitely cannot be performed at this
     * time--perhaps because the server is in a read-only state, has not yet
     * been initialized, believes its peers to be down, and so on. Do not use
     * this error for indeterminate cases, when the operation may actually
     * have taken place.
     */
    TEMPORARILY_UNAVAILABLE(11, true),

    /**
     * The client's request did not conform to the server's expectations, and
     * could not possibly have been processed.
     */
    MALFORMED_REQUEST(12, true),

    /**
     * Indicates that some kind of general, indefinite error occurred. Use this
     * as a catch-all for errors you can't otherwise categorize, or as a
     * starting point for your error handler: it's safe to return internal-error
     * for every problem by default, then add special cases for more specific
     * errors later.
     */
    CRASH(13, false),

    /**
     * Indicates that some kind of general, definite error occurred. Use this as
     * a catch-all for errors you can't otherwise categorize, when you
     * specifically know that the requested operation has not taken place. For
     * instance, you might encounter an indefinite failure during the prepare
     * phase of a transaction: since you haven't started the commit process yet,
     * the transaction can't have taken place. It's therefore safe to return a
     * definite abort to the client.
     */
    ABORT(14, true),

    /**
     * The client requested an operation on a key which does not exist (assuming
     * the operation should not automatically create missing keys).
     */
    KEY_DOES_NOT_EXIST(20, true),

    /**
     * The client requested the creation of a key which already exists, and the
     * server will not overwrite it.
     */
    KEY_ALREADY_EXISTS(21, true),

    /**
     * The requested operation expected some conditions to hold, and those
     * conditions were not met. For instance, a compare-and-set operation might
     * assert that the value of a key is currently 5; if the value is 3, the
     * server would return precondition-failed.
     */
    PRECONDITION_FAILED(22, true),

    /**
     * The requested transaction has been aborted because of a conflict with
     * another transaction. Servers need not return this error on every
     * conflict: they may choose to retry automatically instead.
     */
    TXN_CONFLICT(30, true),
    ;

    companion object {
        fun get(code: Int): DefaultError? =
            values().firstOrNull { it.code == code }
    }
}
