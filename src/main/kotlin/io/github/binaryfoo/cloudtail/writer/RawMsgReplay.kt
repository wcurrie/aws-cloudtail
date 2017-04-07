package io.github.binaryfoo.cloudtail.writer

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import io.github.binaryfoo.cloudtail.parser.parseEvents
import io.reactivex.Observable
import java.io.File

/**
 * Replay events from .json written by WsdWriter.
 */
fun readRawMsgsJson(file: File): Observable<CloudTrailEvent> {
    val cleaner = Regex("^(var rawMsgs = \\[)?(.*),$")
    return Observable.create { subscriber ->
        file.reader().useLines { lines ->
            lines
                .filter { it != "];" }
                .map { it.replace(cleaner, "$2") }
                .flatMap { parseEvents(it, false).asSequence() }
                .forEach { event -> subscriber.onNext(event) }
        }
        subscriber.onComplete()
    }
}

