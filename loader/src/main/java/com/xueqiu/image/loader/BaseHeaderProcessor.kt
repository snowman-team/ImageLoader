package com.xueqiu.image.loader

import okhttp3.Interceptor
import okhttp3.Response
import java.net.URL

abstract class BaseHeaderProcessor : Interceptor {

    abstract fun setHeaders(url: URL): Map<String, String>?

    override fun intercept(chain: Interceptor.Chain): Response? {
        val builder = chain.request().newBuilder()

        setHeaders(chain.request().url().url())?.forEach {
            builder.addHeader(it.key, it.value)
        }

        return chain.proceed(builder.build())
    }
}