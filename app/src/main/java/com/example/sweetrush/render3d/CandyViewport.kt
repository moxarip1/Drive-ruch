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
    allowRaceCameraDrag: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentAllowOrbit = androidx.compose.runtime.rememberUpdatedState(allowOrbitDrag)
    val currentAllowRaceDrag = androidx.compose.runtime.rememberUpdatedState(allowRaceCameraDrag)

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
                        if (currentAllowOrbit.value) {
                            renderer.cancelShowroomCameraPreset()
                            renderer.showroomZoom = (renderer.showroomZoom / detector.scaleFactor).coerceIn(3.2f, 8.5f)
                            return true
                        }
                        if (currentAllowRaceDrag.value) {
                            val zoomDelta = (1f - detector.scaleFactor) * 4.2f
                            renderer.raceCamZoomOffset = (renderer.raceCamZoomOffset + zoomDelta).coerceIn(-3.0f, 5.5f)
                            return true
                        }
                        return false
                    }
                }
            )
            glView.setOnTouchListener { _, ev ->
                val showroom = currentAllowOrbit.value
                val raceDrag = currentAllowRaceDrag.value
                if (!showroom && !raceDrag) return@setOnTouchListener false

                scaleDetector.onTouchEvent(ev)
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = ev.x
                        lastY = ev.y
                        if (showroom) {
                            renderer.cancelShowroomCameraPreset()
                            renderer.showroomAutoRotate = false
                        } else if (raceDrag) {
                            renderer.isDraggingRaceCam = true
                        }
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!scaleDetector.isInProgress) {
                            val dx = ev.x - lastX
                            val dy = ev.y - lastY
                            lastX = ev.x
                            lastY = ev.y
                            if (showroom) {
                                renderer.showroomOrbitYaw -= dx * 0.01f
                                renderer.showroomOrbitPitch = (renderer.showroomOrbitPitch + dy * 0.008f).coerceIn(0.08f, 0.78f)
                            } else if (raceDrag) {
                                renderer.raceCamYawOffset = (renderer.raceCamYawOffset - dx * 0.0065f).coerceIn(-3.14f, 3.14f)
                                renderer.raceCamPitchOffset = (renderer.raceCamPitchOffset + dy * 0.0048f).coerceIn(-0.42f, 0.92f)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        renderer.isDraggingRaceCam = false
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
            if (!allowRaceCameraDrag) {
                renderer.isDraggingRaceCam = false
            }
        },
        modifier = modifier
    )
}

