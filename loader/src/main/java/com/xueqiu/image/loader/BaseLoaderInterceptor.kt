package com.xueqiu.image.loader

abstract class BaseLoaderInterceptor {

    abstract fun onIntercept(builder: ImageBuilder): ImageBuilder

}