package io.github.binaryfoo.cloudtail.s3

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import io.github.binaryfoo.cloudtail.propertiesFrom
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

fun main(args: Array<String>) {
    val properties = propertiesFrom("config.properties")
    val s3Client = AmazonS3Client()
    val bucketName = properties.getProperty("bucket_name")
    val response = s3Client.listObjects(bucketName, properties.getProperty("key_prefix"))
    val pool = ThreadPoolExecutor(5, 10, 5, TimeUnit.SECONDS, LinkedBlockingQueue())
    val futures = response.objectSummaries.parallelStream().map {
        println(it.key)
        val name = File(it.key).name
        pool.submit(Callable<String> {
            s3Client.getObject(GetObjectRequest(bucketName, it.key), File("tmp", name))
            name
        })
    }.collect(Collectors.toList())
    futures.forEach {
        println("Downloaded " + it.get())
    }
    val totalBytes = response.objectSummaries.map { it.size }.sum()
    println("size $totalBytes")
    pool.shutdown()
}
