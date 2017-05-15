package io.github.binaryfoo.cloudtail.s3

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.S3ObjectSummary
import io.github.binaryfoo.cloudtail.propertiesFrom
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

fun main(args: Array<String>) {
    val s3Client = AmazonS3Client()
    val properties = propertiesFrom("config.properties")
    val bucketName = properties.getProperty("bucket_name")
    val prefix = properties.getProperty("key_prefix")
    val allFiles = findLogFiles(s3Client, bucketName, prefix)

    download(s3Client, allFiles)
}

private fun download(s3Client: AmazonS3Client, allFiles: MutableList<S3ObjectSummary>) {
    val pool = ThreadPoolExecutor(5, 10, 5, TimeUnit.SECONDS, LinkedBlockingQueue())
    val futures = allFiles.parallelStream().map {
        println(it.key)
        val name = File(it.key).name
        pool.submit(Callable<String> {
            s3Client.getObject(GetObjectRequest(it.bucketName, it.key), File("tmp", name))
            name
        })
    }.collect(Collectors.toList())
    futures.forEach {
        println("Downloaded " + it.get())
    }
    pool.shutdown()
}

private fun findLogFiles(s3Client: AmazonS3Client, bucketName: String?, prefix: String?): MutableList<S3ObjectSummary> {
    val request = ListObjectsRequest().apply {
        this.bucketName = bucketName
        this.prefix = prefix
    }
    val allFiles = mutableListOf<S3ObjectSummary>()
    do {
        val response = s3Client.listObjects(request)
        val files = response.objectSummaries.filter { !it.key.contains("-Digest") }
        allFiles.addAll(files)
        println("Found ${files.size} files. Next marker ${response.nextMarker}")
        request.marker = response.nextMarker
    } while (response.nextMarker != null)
    return allFiles
}
