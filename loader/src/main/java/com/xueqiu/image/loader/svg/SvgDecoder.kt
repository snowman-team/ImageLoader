package com.xueqiu.image.loader.svg

import com.caverock.androidsvg.SVG
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.decoder.ImageDecoder
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.image.QualityInfo

class SvgDecoder : ImageDecoder {

    override fun decode(encodedImage: EncodedImage,
                        length: Int,
                        qualityInfo: QualityInfo,
                        options: ImageDecodeOptions): CloseableImage? {
        val svg = SVG.getFromInputStream(encodedImage.inputStream)
        return CloseableSvgImage(svg)
    }

}