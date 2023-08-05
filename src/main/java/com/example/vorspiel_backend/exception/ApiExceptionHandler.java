package com.example.vorspiel_backend.exception;

import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.vorspiel_backend.VorspielApplication;

import lombok.extern.log4j.Log4j2;


/**
 * Return nicely formatted responses for http requests.
 * 
 * @since 0.0.1
 */
@Log4j2
@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(value = HttpStatusCodeException.class) 
    public static ResponseEntity<ApiExceptionFormat> handleBadRequest() {

        System.out.println("called");
        return null;
    }

    /**
     * Catches any {@link ApiException}. Returns a {@link ResponseEntity} object with an {@link ApiExceptionFormat} and 
     * logs neccessary stackTrace information.
     * 
     * @param exception ApiException that was thrown
     * @param request request object that caused the exception
     * @return ResponseEntity with internalServerError status and an ApiExceptionFormat object
     */
    @ExceptionHandler(value = ApiException.class)
    public static ResponseEntity<ApiExceptionFormat> handleApiException(ApiException exception) {

        Exception originalException = exception.getOriginalException();

        // log messages
        if (originalException == null) {
            log.error(exception.getMessage());

        } else {
            log.error(exception.getMessage() + " Cause: " + originalException.getMessage());
            log.error("     " + originalException.getClass());
        }
        
        // log relevant stackTrace parts
        logPackageStackTrace(exception.getStackTrace());

        return ResponseEntity.internalServerError().body(returnPretty(exception.getStatus(), exception.getMessage()));
    }


    /**
     * Logs and formats parts of given stacktrace array that include classes of the {@link VorspielApplication} package (e.g. com.example...) but will 
     * exclude any other package (like java.lang etc.).
     * 
     * @param stackTrace array to format and log elements from
     */
    private static void logPackageStackTrace(StackTraceElement[] stackTrace) {

        Arrays.asList(stackTrace).forEach(trace -> {
            if (isPackageStackTrace(trace)) 
                log.error("     at " + trace.getClassName() + "." + trace.getMethodName() + "(" + trace.getFileName() + ":" + trace.getLineNumber() + ")");
        });
    }


    /**
     * Checks if given {@link StackTraceElement} references a class of the {@link VorspielApplication} package.
     * 
     * @param trace to check
     * @return true if referenced class is in {@link VorspielApplication} package
     */
    private static boolean isPackageStackTrace(StackTraceElement trace) {

        return trace.getClassName().startsWith(VorspielApplication.class.getPackage().getName());
    }


    /**
     * Log given error message and return {@link ApiExceptionFormat} with http status, error message and servlet path.
     * 
     * @param status http status
     * @param errorMessage brief description of the error
     * @return formatted response holding status, error message and servlet path
     */
    public static ApiExceptionFormat returnPretty(HttpStatus status, String errorMessage) {

        return new ApiExceptionFormat(status.value(), 
                                      status.getReasonPhrase(), 
                                      errorMessage, 
                                      ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                                                                                      .getRequest()
                                                                                      .getServletPath());
    }
    
    
    /**
     * Overloading {@link #returnPretty(HttpStatus, String)}.
     * 
     * @param status
     * @param bindingResult
     * @return
     */
    public static ApiExceptionFormat returnPretty(HttpStatus status, BindingResult bindingResult) {

        String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
        
        return returnPretty(status, errorMessage);
    }


    /**
     * Overloading {@link #returnPretty(HttpStatus, String)}.
     * 
     * @param status
     * @param bindingResult
     * @return
     */
    public static ApiExceptionFormat returnPretty(HttpStatus status) {

        return returnPretty(status, "Failed to process http request");
    }

    
    /**
     * Overloading {@link #returnPretty(HttpStatus, String)}.
     * 
     * @param status
     * @param bindingResult
     * @return
     */
    public static ApiExceptionFormat returnPrettySuccess(HttpStatus status) {

        return new ApiExceptionFormat(status.value(), 
                                null, 
                                "Http request successful",
                                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                                                                                .getRequest()
                                                                                .getServletPath());    
    }
}