package io.github.binaryfoo.cloudtail.lambda

import com.amazonaws.serverless.exceptions.ContainerInitializationException
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse
import com.amazonaws.serverless.proxy.spark.SparkLambdaContainerHandler
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.log4j.LambdaAppender
import io.github.binaryfoo.cloudtail.spark.defineResources
import org.apache.log4j.Logger

class LambdaHandler : RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    private var isInitialized = false
    private var handler: SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse>? = null

    override fun handleRequest(awsProxyRequest: AwsProxyRequest, context: Context): AwsProxyResponse? {
        if (!isInitialized) {
            isInitialized = true
            try {
                Logger.getRootLogger().addAppender(LambdaAppender())
                handler = SparkLambdaContainerHandler.getAwsProxyHandler()
                defineResources()
            } catch (e: ContainerInitializationException) {
                e.printStackTrace()
                return null
            }

        }
        return handler!!.proxy(awsProxyRequest, context)
    }
}