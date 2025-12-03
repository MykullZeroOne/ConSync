package com.consync.client.confluence.exception

/**
 * Base exception for all Confluence API errors.
 */
open class ConfluenceException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when authentication fails (401).
 */
class AuthenticationException(
    message: String = "Authentication failed. Check your credentials.",
    cause: Throwable? = null
) : ConfluenceException(message, cause)

/**
 * Exception thrown when access is forbidden (403).
 */
class ForbiddenException(
    message: String = "Access forbidden. Check your permissions.",
    val resource: String? = null,
    cause: Throwable? = null
) : ConfluenceException(message, cause)

/**
 * Exception thrown when a resource is not found (404).
 */
class NotFoundException(
    message: String = "Resource not found.",
    val resourceType: String? = null,
    val resourceId: String? = null,
    cause: Throwable? = null
) : ConfluenceException(message, cause)

/**
 * Exception thrown when there's a version conflict (409).
 */
class ConflictException(
    message: String = "Version conflict detected.",
    val pageId: String? = null,
    val expectedVersion: Int? = null,
    val actualVersion: Int? = null,
    cause: Throwable? = null
) : ConfluenceException(message, cause)

/**
 * Exception thrown when rate limited (429).
 */
class RateLimitException(
    message: String = "Rate limit exceeded.",
    val retryAfterSeconds: Int? = null,
    cause: Throwable? = null
) : ConfluenceException(message, cause)

/**
 * Exception thrown for server errors (5xx).
 */
class ServerException(
    message: String = "Confluence server error.",
    val statusCode: Int,
    cause: Throwable? = null
) : ConfluenceException(message, cause)

/**
 * Exception thrown for network/connection errors.
 */
class NetworkException(
    message: String = "Network error communicating with Confluence.",
    cause: Throwable? = null
) : ConfluenceException(message, cause)

/**
 * Exception thrown when request validation fails.
 */
class ValidationException(
    message: String,
    val field: String? = null,
    cause: Throwable? = null
) : ConfluenceException(message, cause)

/**
 * Exception thrown when max retries exceeded.
 */
class MaxRetriesExceededException(
    message: String = "Maximum retry attempts exceeded.",
    val attempts: Int,
    cause: Throwable? = null
) : ConfluenceException(message, cause)
