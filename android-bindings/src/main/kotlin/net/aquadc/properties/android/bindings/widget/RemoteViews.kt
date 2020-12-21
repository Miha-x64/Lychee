@file:[Suppress("NOTHING_TO_INLINE") JvmName("RemoteViewBindings")]
package net.aquadc.properties.android.bindings.widget

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import net.aquadc.properties.Property
import net.aquadc.properties.map
import net.aquadc.properties.mapValueList
import net.aquadc.properties.mapWith
import net.aquadc.properties.propertyOf


fun RemoteViews.bind(
    vararg bindings: RemoteBinding,
    debouncer: Handler = Handler(Looper.getMainLooper())
): Property<RemoteViews> {
    val original = propertyOf(this)
    val recompose = Runnable { original.value = original.value }
    val recomposeProp = bindings.map(RemoteBinding::property).mapValueList {
        debouncer.removeCallbacks(recompose) // Don't clone and bind remote views on every change.
        debouncer.post(recompose) // Wait for next event loop cycle instead.
    }
    return original.mapWith(recomposeProp) { it, _ ->
        @Suppress("DEPRECATION")
        (if (Build.VERSION.SDK_INT >= 28) RemoteViews(it) else it.clone()).also { views ->
            bindings.forEach { binding -> binding.bindTo(views) }
        }
    }
}

// RemoteViews API replica

inline infix fun @receiver:IdRes Int.displayedChildTo(childIndexProperty: Property<Int>): RemoteBinding =
    RemoteBinding.Int(this, "setDisplayedChild", childIndexProperty)
inline infix fun @receiver:IdRes Int.visibilityTo(visibilityProperty: Property<Int>): RemoteBinding =
    RemoteBinding.Int(this, "setVisibility", visibilityProperty)
inline infix fun @receiver:IdRes Int.visibilitySoftlyTo(visibilityProperty: Property<Boolean>): RemoteBinding =
    visibilityTo(visibilityProperty, View.INVISIBLE)
inline infix fun @receiver:IdRes Int.visibilityHardlyTo(visibilityProperty: Property<Boolean>): RemoteBinding =
    visibilityTo(visibilityProperty, View.GONE)
@PublishedApi @JvmSynthetic
internal fun Int.visibilityTo(visibilityProperty: Property<Boolean>, invisible: Int): RemoteBinding =
    RemoteBinding.Int(this, "setVisibility", visibilityProperty.map { if (it) View.VISIBLE else invisible })
inline infix fun @receiver:IdRes Int.textTo(textProperty: Property<CharSequence>): RemoteBinding =
    RemoteBinding.CharSeq(this, "setText", textProperty)
// textSize, compoundDrawables skipped
inline infix fun @receiver:IdRes Int.imageResTo(imageResProperty: Property<Int>): RemoteBinding =
    RemoteBinding.Int(this, "setImageResource", imageResProperty)
inline infix fun @receiver:IdRes Int.imageUriTo(imageUriProperty: Property<Uri?>): RemoteBinding =
    RemoteBinding.Uri(this, "setImageURI", imageUriProperty)
inline infix fun @receiver:IdRes Int.imageBitmapTo(imageBitmapProperty: Property<Bitmap?>): RemoteBinding =
    RemoteBinding.Bitmap(this, "setImageBitmap", imageBitmapProperty)
@RequiresApi(23) inline infix fun @receiver:IdRes Int.imageIconTo(imageBitmapProperty: Property<Icon?>): RemoteBinding =
    RemoteBinding.Icon(this, "setImageIcon", imageBitmapProperty)
// setEmptyView, setChronometer**, setProgressBar, setDrawableTint skipped
inline infix fun @receiver:IdRes Int.textColorTo(textColorProperty: Property<Int>): RemoteBinding =
    RemoteBinding.Int(this, "setTextColor", textColorProperty)
// setRemoteAdapter, set(Relative)ScrollPosition, setViewPadding skipped
inline infix fun @receiver:IdRes Int.contentDescriptionTo(contentDescriptionProperty: Property<CharSequence>): RemoteBinding =
    RemoteBinding.CharSeq(this, "setContentDescription", contentDescriptionProperty)
// setAccessibilityTraversal**, setLabelFor, setLightBackgroundLayoutId skipped


class RemoteBinding @PublishedApi internal constructor(
    @IdRes private val viewId: Int,
    private val methodName: String,
    @JvmField @JvmSynthetic internal val property: Property<*>,
    private val type: Int
) { /*
    from RemoteViews.ReflectionAction
    static final int BOOLEAN = 1;
    static final int BYTE = 2;
    static final int SHORT = 3;
    static final int INT = 4;
    static final int LONG = 5;
    static final int FLOAT = 6;
    static final int DOUBLE = 7;
    static final int CHAR = 8;
    static final int STRING = 9;
    static final int CHAR_SEQUENCE = 10;
    static final int URI = 11;
    static final int BITMAP = 12;
    static final int BUNDLE = 13;
    static final int INTENT = 14;
    static final int COLOR_STATE_LIST = 15; (@hide)
    static final int ICON = 16;
     */
    @SuppressLint("NewApi") // setIcon()
    fun bindTo(remoteViews: RemoteViews) {
        val viewId = viewId
        val methodName = methodName
        val value = property.value
        when (type) {
            1 -> remoteViews.setBoolean(viewId, methodName, value as Boolean)
            2 -> remoteViews.setByte(viewId, methodName, value as Byte)
            3 -> remoteViews.setShort(viewId, methodName, value as Short)
            4 -> remoteViews.setInt(viewId, methodName, value as Int)
            5 -> remoteViews.setLong(viewId, methodName, value as Long)
            6 -> remoteViews.setFloat(viewId, methodName, value as Float)
            7 -> remoteViews.setDouble(viewId, methodName, value as Double)
            8 -> remoteViews.setChar(viewId, methodName, value as Char)
            9 -> remoteViews.setString(viewId, methodName, value as String?)
            10 -> remoteViews.setCharSequence(viewId, methodName, value as CharSequence?)
            11 -> remoteViews.setUri(viewId, methodName, value as Uri?)
            12 -> remoteViews.setBitmap(viewId, methodName, value as Bitmap?)
            13 -> remoteViews.setBundle(viewId, methodName, value as Bundle?)
            14 -> remoteViews.setIntent(viewId, methodName, value as Intent?)
            16 -> remoteViews.setIcon(viewId, methodName, value as Icon?) // SDK23
            else -> throw AssertionError()
        }
    }
    companion object {
        inline fun Bool(@IdRes viewId: Int, methodName: String, property: Property<Boolean>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 1)
        inline fun Byte(@IdRes viewId: Int, methodName: String, property: Property<Byte>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 2)
        inline fun Short(@IdRes viewId: Int, methodName: String, property: Property<Short>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 3)
        inline fun Int(@IdRes viewId: Int, methodName: String, property: Property<Int>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 4)
        inline fun Long(@IdRes viewId: Int, methodName: String, property: Property<Long>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 5)
        inline fun Float(@IdRes viewId: Int, methodName: String, property: Property<Float>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 6)
        inline fun Double(@IdRes viewId: Int, methodName: String, property: Property<Double>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 7)
        inline fun Char(@IdRes viewId: Int, methodName: String, property: Property<Char>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 8)
        inline fun String(@IdRes viewId: Int, methodName: String, property: Property<String?>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 9)
        inline fun CharSeq(@IdRes viewId: Int, methodName: String, property: Property<CharSequence?>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 10)
        inline fun Uri(@IdRes viewId: Int, methodName: String, property: Property<Uri?>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 11)
        inline fun Bitmap(@IdRes viewId: Int, methodName: String, property: Property<Bitmap?>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 12)
        inline fun Bundle(@IdRes viewId: Int, methodName: String, property: Property<Bundle?>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 13)
        inline fun Intent(@IdRes viewId: Int, methodName: String, property: Property<Intent?>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 14)
        @RequiresApi(23)
        inline fun Icon(@IdRes viewId: Int, methodName: String, property: Property<Icon?>): RemoteBinding =
            RemoteBinding(viewId, methodName, property, 16)
    }
}
