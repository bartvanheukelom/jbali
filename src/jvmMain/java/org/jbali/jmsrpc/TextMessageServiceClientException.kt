package org.jbali.jmsrpc

class TextMessageServiceClientException : RuntimeException {
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(message: String) : super(message)
}
