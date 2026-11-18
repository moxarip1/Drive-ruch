package com.example.sweetrush.render3d

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CandyViewport3D(
    renderer: CandyWorldRenderer,
    allowOrbitDrag: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { ctx ->
            var lastX = 0f
            var lastY = 0f
            val scaleDetector = ScaleGestureDetector(
                ctx,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        if (!allowOrbitDrag) return false
                        renderer.showroomZoom = (renderer.showroomZoom / detector.scaleFactor).coerceIn(3.2f, 8.5f)
                        return true
                    }
                }
            )
            glView.setOnTouchListener { _, ev ->
                if (!allowOrbitDrag) return@setOnTouchListener false
                scaleDetector.onTouchEvent(ev)
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = ev.x
                        lastY = ev.y
                        renderer.showroomAutoRotate = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!scaleDetector.isInProgress) {
                            val dx = ev.x - lastX
                            val dy = ev.y - lastY
                            lastX = ev.x
                            lastY = ev.y
                            renderer.showroomOrbitYaw -= dx * 0.01f
                            renderer.showroomOrbitPitch = (renderer.showroomOrbitPitch + dy * 0.008f).coerceIn(0.08f, 0.78f)
                        }
                        true
                    }
                    else -> false
                }
            }
            glView
        },
        update = {
            if (!allowOrbitDrag) {
                renderer.showroomAutoRotate = true
            }
        },
        modifier = modifier
    )
}
