package androidx.compose.material.icons.automirrored.filled

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun makeAutoMirroredIcon(name: String, block: PathBuilder.() -> Unit): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = true
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathBuilder = block
        )
    }.build()
}

val Icons.AutoMirrored.Filled.Chat: ImageVector by lazy {
    makeAutoMirroredIcon("AutoMirrored.Filled.Chat") {
        moveTo(20f, 2f)
        horizontalLineTo(4f)
        verticalLineTo(22f)
        lineTo(8f, 18f)
        horizontalLineTo(20f)
        verticalLineTo(2f)
        close()
        moveTo(6f, 9f)
        horizontalLineTo(18f)
        verticalLineTo(11f)
        horizontalLineTo(6f)
        verticalLineTo(9f)
        close()
        moveTo(14f, 14f)
        horizontalLineTo(6f)
        verticalLineTo(12f)
        horizontalLineTo(14f)
        verticalLineTo(14f)
        close()
    }
}

val Icons.AutoMirrored.Filled.VolumeUp: ImageVector by lazy {
    makeAutoMirroredIcon("AutoMirrored.Filled.VolumeUp") {
        moveTo(3f, 9f)
        verticalLineTo(15f)
        horizontalLineTo(7f)
        lineTo(12f, 20f)
        verticalLineTo(4f)
        lineTo(7f, 9f)
        horizontalLineTo(3f)
        close()
        moveTo(16.5f, 12f)
        lineTo(14f, 8f)
        verticalLineTo(16f)
        lineTo(16.5f, 12f)
        close()
        moveTo(14f, 3.23f)
        verticalLineTo(5.29f)
        lineTo(19f, 12f)
        lineTo(14f, 18.71f)
        verticalLineTo(20.77f)
        lineTo(21f, 12f)
        lineTo(14f, 3.23f)
        close()
    }
}

val Icons.AutoMirrored.Filled.VolumeOff: ImageVector by lazy {
    makeAutoMirroredIcon("AutoMirrored.Filled.VolumeOff") {
        moveTo(4.27f, 3f)
        lineTo(3f, 4.27f)
        lineTo(7.73f, 9f)
        horizontalLineTo(3f)
        verticalLineTo(15f)
        horizontalLineTo(7f)
        lineTo(12f, 20f)
        verticalLineTo(13.27f)
        lineTo(19.73f, 21f)
        lineTo(21f, 19.73f)
        lineTo(4.27f, 3f)
        close()
        moveTo(12f, 4f)
        lineTo(9.91f, 6.09f)
        lineTo(12f, 8.18f)
        verticalLineTo(4f)
        close()
    }
}
