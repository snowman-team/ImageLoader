package com.aquarids.image.app

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.xueqiu.image.loader.ImageBuilder
import com.xueqiu.image.loader.ImageLoader
import com.xueqiu.image.loader.ImageObserver
import com.xueqiu.image.loader.transform.BlurTransformer
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mCompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val builder = ImageBuilder()
                .withNetImage("https://xqimg.imedao.com/16bf87d9eba8e3fb87b00041.jpg")
                .withTransformer(BlurTransformer(2, 5))
                .isSmall(false)

        val disposable = ImageLoader.loadImage(builder)
                .subscribeWith(object : ImageObserver() {
                    override fun onSuccess(bitmap: Bitmap) {
                        iv_image.setImageBitmap(bitmap)
                    }

                    override fun onOutOfMemory() {
                        Log.e("load image", "out of memory")
                    }

                    override fun onLoadError(e: Throwable) {
                        Log.e("load image", "fail", e)
                    }
                }).disposable
        mCompositeDisposable.add(disposable)

        iv_image_2.autoPlay = true
        iv_image_2.loadImage("https://xqimg.imedao.com/16b9166559dcf53fd69991f4.gif")

        iv_image_3.loadImage("https://xqimg.imedao.com/16bf87d9eba8e3fb87b00041.jpg")
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!mCompositeDisposable.isDisposed) {
            mCompositeDisposable.dispose()
        }
    }
}
