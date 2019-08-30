package com.xueqiu.image.loader.svg

import com.facebook.imageformat.ImageFormat

class SvgConstant {

    companion object {
        const val HEADER_TAG_SVG = "<svg"
        const val HEADER_XML = "<?xml"

        val SVG_FORMAT = ImageFormat("SVG_FORMAT", "svg")
    }

}