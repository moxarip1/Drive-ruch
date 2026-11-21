package androidx.compose.material.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun makeIcon(name: String, block: PathBuilder.() -> Unit): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
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

val Icons.Filled.ArrowUpward: ImageVector by lazy {
    makeIcon("Filled.ArrowUpward") {
        moveTo(4f, 12f)
        lineTo(5.41f, 13.41f)
        lineTo(11f, 7.83f)
        verticalLineTo(20f)
        horizontalLineTo(13f)
        verticalLineTo(7.83f)
        lineTo(18.58f, 13.42f)
        lineTo(20f, 12f)
        lineTo(12f, 4f)
        lineTo(4f, 12f)
        close()
    }
}

val Icons.Filled.Bolt: ImageVector by lazy {
    makeIcon("Filled.Bolt") {
        moveTo(11f, 21f)
        horizontalLineTo(10f)
        lineTo(11f, 14f)
        horizontalLineTo(7.5f)
        lineTo(13f, 3f)
        horizontalLineTo(14f)
        lineTo(13f, 10f)
        horizontalLineTo(16.5f)
        lineTo(11f, 21f)
        close()
    }
}

val Icons.Filled.BugReport: ImageVector by lazy {
    makeIcon("Filled.BugReport") {
        moveTo(20f, 8f)
        horizontalLineTo(17.19f)
        lineTo(15f, 5.81f)
        lineTo(16.41f, 4.4f)
        lineTo(14.24f, 2.22f)
        lineTo(12f, 4.47f)
        lineTo(9.76f, 2.22f)
        lineTo(7.59f, 4.4f)
        lineTo(9f, 5.81f)
        lineTo(6.81f, 8f)
        horizontalLineTo(4f)
        verticalLineTo(10f)
        horizontalLineTo(6.09f)
        verticalLineTo(12f)
        horizontalLineTo(4f)
        verticalLineTo(14f)
        horizontalLineTo(6.09f)
        verticalLineTo(16f)
        horizontalLineTo(4f)
        verticalLineTo(18f)
        horizontalLineTo(6.81f)
        lineTo(9f, 20.19f)
        horizontalLineTo(15f)
        lineTo(17.19f, 18f)
        horizontalLineTo(20f)
        verticalLineTo(16f)
        horizontalLineTo(17.91f)
        verticalLineTo(14f)
        horizontalLineTo(20f)
        verticalLineTo(12f)
        horizontalLineTo(17.91f)
        verticalLineTo(10f)
        horizontalLineTo(20f)
        verticalLineTo(8f)
        close()
    }
}

val Icons.Filled.CameraAlt: ImageVector by lazy {
    makeIcon("Filled.CameraAlt") {
        moveTo(9f, 2f)
        lineTo(7.17f, 4f)
        horizontalLineTo(4f)
        verticalLineTo(20f)
        horizontalLineTo(20f)
        verticalLineTo(4f)
        horizontalLineTo(16.83f)
        lineTo(15f, 2f)
        horizontalLineTo(9f)
        close()
        moveTo(12f, 17f)
        lineTo(8.5f, 13.5f)
        lineTo(12f, 10f)
        lineTo(15.5f, 13.5f)
        lineTo(12f, 17f)
        close()
    }
}

val Icons.Filled.ContentCopy: ImageVector by lazy {
    makeIcon("Filled.ContentCopy") {
        moveTo(16f, 1f)
        horizontalLineTo(4f)
        verticalLineTo(15f)
        horizontalLineTo(6f)
        verticalLineTo(3f)
        horizontalLineTo(16f)
        verticalLineTo(1f)
        close()
        moveTo(19f, 5f)
        horizontalLineTo(8f)
        verticalLineTo(21f)
        horizontalLineTo(19f)
        verticalLineTo(5f)
        close()
    }
}

val Icons.Filled.EmojiEvents: ImageVector by lazy {
    makeIcon("Filled.EmojiEvents") {
        moveTo(19f, 5f)
        horizontalLineTo(17f)
        verticalLineTo(3f)
        horizontalLineTo(7f)
        verticalLineTo(5f)
        horizontalLineTo(5f)
        verticalLineTo(10f)
        lineTo(9f, 13f)
        lineTo(11f, 15f)
        verticalLineTo(19f)
        horizontalLineTo(7f)
        verticalLineTo(21f)
        horizontalLineTo(17f)
        verticalLineTo(19f)
        horizontalLineTo(13f)
        verticalLineTo(15f)
        lineTo(15f, 13f)
        lineTo(19f, 10f)
        verticalLineTo(5f)
        close()
    }
}

val Icons.Filled.Explore: ImageVector by lazy {
    makeIcon("Filled.Explore") {
        moveTo(12f, 2f)
        lineTo(4f, 6f)
        lineTo(2f, 12f)
        lineTo(4f, 18f)
        lineTo(12f, 22f)
        lineTo(20f, 18f)
        lineTo(22f, 12f)
        lineTo(20f, 6f)
        lineTo(12f, 2f)
        close()
        moveTo(14.19f, 14.19f)
        lineTo(6f, 18f)
        lineTo(9.81f, 9.81f)
        lineTo(18f, 6f)
        lineTo(14.19f, 14.19f)
        close()
    }
}

val Icons.Filled.Flag: ImageVector by lazy {
    makeIcon("Filled.Flag") {
        moveTo(14.4f, 6f)
        lineTo(14f, 4f)
        horizontalLineTo(5f)
        verticalLineTo(21f)
        horizontalLineTo(7f)
        verticalLineTo(14f)
        horizontalLineTo(12.6f)
        lineTo(13f, 16f)
        horizontalLineTo(20f)
        verticalLineTo(6f)
        horizontalLineTo(14.4f)
        close()
    }
}

val Icons.Filled.Groups: ImageVector by lazy {
    makeIcon("Filled.Groups") {
        moveTo(16f, 11f)
        lineTo(19f, 11f)
        verticalLineTo(8f)
        horizontalLineTo(16f)
        verticalLineTo(11f)
        close()
        moveTo(8f, 11f)
        horizontalLineTo(11f)
        verticalLineTo(8f)
        horizontalLineTo(8f)
        verticalLineTo(11f)
        close()
        moveTo(4f, 18f)
        verticalLineTo(15f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        horizontalLineTo(4f)
        close()
    }
}

val Icons.Filled.Mail: ImageVector by lazy {
    makeIcon("Filled.Mail") {
        moveTo(20f, 4f)
        horizontalLineTo(4f)
        verticalLineTo(20f)
        horizontalLineTo(20f)
        verticalLineTo(4f)
        close()
        moveTo(18f, 8f)
        lineTo(12f, 13f)
        lineTo(6f, 8f)
        verticalLineTo(6f)
        lineTo(12f, 11f)
        lineTo(18f, 6f)
        verticalLineTo(8f)
        close()
    }
}

val Icons.Filled.Map: ImageVector by lazy {
    makeIcon("Filled.Map") {
        moveTo(15f, 5.1f)
        lineTo(9f, 3f)
        lineTo(3f, 5.02f)
        verticalLineTo(21f)
        lineTo(9f, 18.9f)
        lineTo(15f, 21f)
        lineTo(21f, 18.98f)
        verticalLineTo(3f)
        lineTo(15f, 5.1f)
        close()
    }
}

val Icons.Filled.Mic: ImageVector by lazy {
    makeIcon("Filled.Mic") {
        moveTo(12f, 14f)
        lineTo(15f, 11f)
        verticalLineTo(5f)
        lineTo(12f, 2f)
        lineTo(9f, 5f)
        verticalLineTo(11f)
        lineTo(12f, 14f)
        close()
        moveTo(19f, 11f)
        horizontalLineTo(17f)
        verticalLineTo(14f)
        lineTo(12f, 17f)
        lineTo(7f, 14f)
        verticalLineTo(11f)
        horizontalLineTo(5f)
        verticalLineTo(15f)
        lineTo(11f, 18.9f)
        verticalLineTo(22f)
        horizontalLineTo(13f)
        verticalLineTo(18.9f)
        lineTo(19f, 15f)
        verticalLineTo(11f)
        close()
    }
}

val Icons.Filled.MicOff: ImageVector by lazy {
    makeIcon("Filled.MicOff") {
        moveTo(4.27f, 3f)
        lineTo(3f, 4.27f)
        lineTo(9f, 10.27f)
        verticalLineTo(11f)
        lineTo(12f, 14f)
        lineTo(13.73f, 15f)
        lineTo(11f, 17.73f)
        verticalLineTo(22f)
        horizontalLineTo(13f)
        verticalLineTo(17.73f)
        lineTo(19.73f, 21f)
        lineTo(21f, 19.73f)
        lineTo(4.27f, 3f)
        close()
    }
}

val Icons.Filled.NotificationsActive: ImageVector by lazy {
    makeIcon("Filled.NotificationsActive") {
        moveTo(12f, 22f)
        lineTo(14f, 20f)
        horizontalLineTo(10f)
        lineTo(12f, 22f)
        close()
        moveTo(18f, 16f)
        verticalLineTo(11f)
        lineTo(15f, 6f)
        verticalLineTo(4f)
        horizontalLineTo(9f)
        verticalLineTo(6f)
        lineTo(6f, 11f)
        verticalLineTo(16f)
        lineTo(4f, 18f)
        verticalLineTo(19f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        lineTo(18f, 16f)
        close()
    }
}

val Icons.Filled.Pause: ImageVector by lazy {
    makeIcon("Filled.Pause") {
        moveTo(6f, 19f)
        horizontalLineTo(10f)
        verticalLineTo(5f)
        horizontalLineTo(6f)
        verticalLineTo(19f)
        close()
        moveTo(14f, 5f)
        verticalLineTo(19f)
        horizontalLineTo(18f)
        verticalLineTo(5f)
        horizontalLineTo(14f)
        close()
    }
}

val Icons.Filled.PersonAdd: ImageVector by lazy {
    makeIcon("Filled.PersonAdd") {
        moveTo(15f, 12f)
        lineTo(19f, 8f)
        lineTo(15f, 4f)
        lineTo(11f, 8f)
        lineTo(15f, 12f)
        close()
        moveTo(6f, 10f)
        verticalLineTo(7f)
        horizontalLineTo(4f)
        verticalLineTo(10f)
        horizontalLineTo(1f)
        verticalLineTo(12f)
        horizontalLineTo(4f)
        verticalLineTo(15f)
        horizontalLineTo(6f)
        verticalLineTo(12f)
        horizontalLineTo(9f)
        verticalLineTo(10f)
        horizontalLineTo(6f)
        close()
        moveTo(15f, 14f)
        lineTo(7f, 18f)
        verticalLineTo(20f)
        horizontalLineTo(23f)
        verticalLineTo(18f)
        lineTo(15f, 14f)
        close()
    }
}

val Icons.Filled.Public: ImageVector by lazy {
    makeIcon("Filled.Public") {
        moveTo(12f, 2f)
        lineTo(5f, 5f)
        lineTo(2f, 12f)
        lineTo(5f, 19f)
        lineTo(12f, 22f)
        lineTo(19f, 19f)
        lineTo(22f, 12f)
        lineTo(19f, 5f)
        lineTo(12f, 2f)
        close()
        moveTo(11f, 19.93f)
        lineTo(6f, 15f)
        verticalLineTo(12f)
        horizontalLineTo(11f)
        verticalLineTo(19.93f)
        close()
    }
}

val Icons.Filled.Speed: ImageVector by lazy {
    makeIcon("Filled.Speed") {
        moveTo(20.38f, 8.57f)
        lineTo(19.15f, 10.42f)
        lineTo(18f, 16f)
        horizontalLineTo(6f)
        lineTo(4.85f, 10.42f)
        lineTo(12f, 5f)
        lineTo(16f, 7f)
        lineTo(10.59f, 12.41f)
        lineTo(12f, 13.83f)
        lineTo(20.38f, 8.57f)
        close()
    }
}

val Icons.Filled.Storefront: ImageVector by lazy {
    makeIcon("Filled.Storefront") {
        moveTo(20f, 4f)
        horizontalLineTo(4f)
        verticalLineTo(6f)
        horizontalLineTo(20f)
        verticalLineTo(4f)
        close()
        moveTo(21f, 14f)
        verticalLineTo(12f)
        lineTo(20f, 7f)
        horizontalLineTo(4f)
        lineTo(3f, 12f)
        verticalLineTo(14f)
        horizontalLineTo(4f)
        verticalLineTo(20f)
        horizontalLineTo(14f)
        verticalLineTo(14f)
        horizontalLineTo(18f)
        verticalLineTo(20f)
        horizontalLineTo(20f)
        verticalLineTo(14f)
        horizontalLineTo(21f)
        close()
    }
}

val Icons.Filled.Timer: ImageVector by lazy {
    makeIcon("Filled.Timer") {
        moveTo(15f, 1f)
        horizontalLineTo(9f)
        verticalLineTo(3f)
        horizontalLineTo(15f)
        verticalLineTo(1f)
        close()
        moveTo(11f, 14f)
        horizontalLineTo(13f)
        verticalLineTo(8f)
        horizontalLineTo(11f)
        verticalLineTo(14f)
        close()
        moveTo(12f, 5f)
        lineTo(5f, 12f)
        lineTo(12f, 21f)
        lineTo(19f, 12f)
        lineTo(12f, 5f)
        close()
    }
}

val Icons.Filled.ZoomIn: ImageVector by lazy {
    makeIcon("Filled.ZoomIn") {
        moveTo(15.5f, 14f)
        horizontalLineTo(14.71f)
        lineTo(14.43f, 13.73f)
        lineTo(16f, 9.5f)
        lineTo(9.5f, 3f)
        lineTo(3f, 9.5f)
        lineTo(9.5f, 16f)
        lineTo(13.73f, 14.43f)
        lineTo(14f, 14.71f)
        verticalLineTo(15.5f)
        lineTo(19f, 20.49f)
        lineTo(20.49f, 19f)
        lineTo(15.5f, 14f)
        close()
        moveTo(12f, 10f)
        horizontalLineTo(10f)
        verticalLineTo(12f)
        horizontalLineTo(9f)
        verticalLineTo(10f)
        horizontalLineTo(7f)
        verticalLineTo(9f)
        horizontalLineTo(9f)
        verticalLineTo(7f)
        horizontalLineTo(10f)
        verticalLineTo(9f)
        horizontalLineTo(12f)
        verticalLineTo(10f)
        close()
    }
}

val Icons.Filled.ZoomOut: ImageVector by lazy {
    makeIcon("Filled.ZoomOut") {
        moveTo(15.5f, 14f)
        horizontalLineTo(14.71f)
        lineTo(14.43f, 13.73f)
        lineTo(16f, 9.5f)
        lineTo(9.5f, 3f)
        lineTo(3f, 9.5f)
        lineTo(9.5f, 16f)
        lineTo(13.73f, 14.43f)
        lineTo(14f, 14.71f)
        verticalLineTo(15.5f)
        lineTo(19f, 20.49f)
        lineTo(20.49f, 19f)
        lineTo(15.5f, 14f)
        close()
        moveTo(7f, 9f)
        horizontalLineTo(12f)
        verticalLineTo(10f)
        horizontalLineTo(7f)
        verticalLineTo(9f)
        close()
    }
}
