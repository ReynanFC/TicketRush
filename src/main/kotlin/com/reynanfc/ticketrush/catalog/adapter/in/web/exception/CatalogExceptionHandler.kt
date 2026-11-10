package com.reynanfc.ticketrush.catalog.adapter.`in`.web.exception

import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.error.ErrorResponseDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.error.FieldErrorDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.error.ValidationErrorResponseDTO
import com.reynanfc.ticketrush.catalog.domain.exception.CatalogException
import com.reynanfc.ticketrush.catalog.domain.exception.DuplicateLotSortOrderException
import com.reynanfc.ticketrush.catalog.domain.exception.DuplicateSectorNameException
import com.reynanfc.ticketrush.catalog.domain.exception.EventHasNoSectorsException
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotFoundException
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotInDraftException
import com.reynanfc.ticketrush.catalog.domain.exception.OverlappingLotWindowException
import com.reynanfc.ticketrush.catalog.domain.exception.SectorHasNoLotsException
import com.reynanfc.ticketrush.catalog.domain.exception.SectorNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import tools.jackson.core.JacksonException
import java.time.Instant

// Translates exceptions into HTTP responses for the whole web adapter.
// Field problems -> ValidationErrorResponseDTO (400). Everything else -> ErrorResponseDTO.
@RestControllerAdvice
class CatalogExceptionHandler {

    // Business rules. CatalogException is sealed, so this when is exhaustive:
    // a new exception in the domain does not compile until it gets a status here.
    @ExceptionHandler(CatalogException::class)
    fun handleCatalog(ex: CatalogException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        val status = when (ex) {
            is EventNotFoundException,
            is SectorNotFoundException -> HttpStatus.NOT_FOUND
            is DuplicateSectorNameException,
            is DuplicateLotSortOrderException,
            is OverlappingLotWindowException,
            is EventNotInDraftException,
            is EventHasNoSectorsException,
            is SectorHasNoLotsException -> HttpStatus.UNPROCESSABLE_CONTENT
        }
        log.warn("Request rejected: {} {} -> {} {}: {}", request.method, request.requestURI, status.value(), ex.javaClass.simpleName, ex.message)
        return error(status, ex.message.orEmpty(), request)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidBody(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ValidationErrorResponseDTO> {
        val fieldErrors = ex.bindingResult.fieldErrors
            .map { FieldErrorDTO(it.field, it.defaultMessage ?: "is invalid", it.rejectedValue) }
            .sortedBy { it.field }
        log.debug("Invalid body: {} {} -> {}", request.method, request.requestURI, fieldErrors.map { it.field })
        return validationError(fieldErrors, request)
    }

    // JSON could not be read: malformed, a required field is missing or a value has the wrong type
    // (e.g. "abc" in a UUID). This happens before @Valid runs.
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<*> {
        val field = (ex.cause as? JacksonException)?.path
            ?.mapNotNull { it.propertyName }
            ?.joinToString(".")
            ?.takeIf { it.isNotEmpty() }
            ?: return error(HttpStatus.BAD_REQUEST, "Malformed JSON", request)

        val fieldError = FieldErrorDTO(field, "is missing or has an invalid format", rejectedValue = null)
        return validationError(listOf(fieldError), request)
    }

    // A path variable has the wrong type, e.g. /events/abc instead of a UUID.
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<ValidationErrorResponseDTO> {
        val expected = ex.requiredType?.simpleName ?: "a valid value"
        val fieldError = FieldErrorDTO(ex.name, "must be a valid $expected", ex.value)
        return validationError(listOf(fieldError), request)
    }

    // Input rules checked by the domain's require(...), e.g. a lot whose endsAt is not after startsAt.
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        log.warn("Request rejected: {} {} -> 400: {}", request.method, request.requestURI, ex.message)
        return error(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request", request)
    }

    // Anything else. Spring's own errors (405 wrong method, 415 wrong Content-Type...) implement ErrorResponse
    // and keep their status. The rest is a bug: the real cause goes to the log, never to the client.
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        if (ex is ErrorResponse) {
            val status = HttpStatus.valueOf(ex.statusCode.value())
            return error(status, ex.body.detail ?: status.reasonPhrase, request)
        }
        log.error("Unexpected error on {} {}", request.method, request.requestURI, ex)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error. Try again later.", request)
    }

    private fun error(status: HttpStatus, message: String, request: HttpServletRequest) =
        ResponseEntity.status(status).body(
            ErrorResponseDTO(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
                path = request.requestURI,
            ),
        )

    private fun validationError(fieldErrors: List<FieldErrorDTO>, request: HttpServletRequest) =
        ResponseEntity.badRequest().body(
            ValidationErrorResponseDTO(
                timestamp = Instant.now(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                message = "One or more fields are invalid",
                path = request.requestURI,
                fieldErrors = fieldErrors,
            ),
        )

    companion object {
        private val log = LoggerFactory.getLogger(CatalogExceptionHandler::class.java)
    }
}
