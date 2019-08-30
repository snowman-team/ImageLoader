package com.xueqiu.image.loader.svg

import com.facebook.imageformat.ImageFormat
import com.facebook.imageformat.ImageFormatCheckerUtils

class SvgFormatChecker : ImageFormat.FormatChecker {

    private val mSvgHeader = ImageFormatCheckerUtils.asciiBytes(SvgConstant.HEADER_TAG_SVG)
    private val mXmlHeader = ImageFormatCheckerUtils.asciiBytes(SvgConstant.HEADER_XML)

    override fun getHeaderSize(): Int {
        return mSvgHeader.size
    }

    override fun determineFormat(headerBytes: ByteArray, headerSize: Int): ImageFormat? {
        if (headerSize < getHeaderSize()) {
            return null
        }
        if (ImageFormatCheckerUtils.startsWithPattern(headerBytes, mSvgHeader)) {
            return SvgConstant.SVG_FORMAT
        }
        for (possibleHeaderTag in arrayOf(mXmlHeader)) {
            if (ImageFormatCheckerUtils.startsWithPattern(headerBytes, possibleHeaderTag)
                    && ImageFormatCheckerUtils.indexOfPattern(headerBytes, headerBytes.size, mSvgHeader, mSvgHeader.size) > -1) {
                return SvgConstant.SVG_FORMAT
            }
        }
        return null
    }

}