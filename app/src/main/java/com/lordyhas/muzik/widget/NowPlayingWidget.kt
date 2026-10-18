package com.lordyhas.muzik.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaButtonReceiver
import com.lordyhas.muzik.MainActivity
import com.lordyhas.muzik.R

/**
 * Home-screen widget: cover, title/artist and previous / play-pause / next buttons.
 * Buttons send media-button events through Media3's [MediaButtonReceiver], which starts the
 * playback service if needed (and resumes the last session on play).
 */
@UnstableApi
class NowPlayingWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        // Without a running service we only know the last text state; artwork comes with the next update
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        render(
            context,
            title = prefs.getString(KEY_TITLE, null),
            artist = prefs.getString(KEY_ARTIST, null),
            isPlaying = false,
            artwork = null
        )
    }

    companion object {
        private const val PREFS = "muzik_widget"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"

        fun hasWidgets(context: Context): Boolean =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, NowPlayingWidget::class.java))
                .isNotEmpty()

        /** Called by the playback service whenever the track or play state changes. */
        fun update(context: Context, title: String?, artist: String?, isPlaying: Boolean, artwork: Bitmap?) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_TITLE, title)
                .putString(KEY_ARTIST, artist)
                .apply()
            render(context, title, artist, isPlaying, artwork)
        }

        private fun render(context: Context, title: String?, artist: String?, isPlaying: Boolean, artwork: Bitmap?) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NowPlayingWidget::class.java)
            if (manager.getAppWidgetIds(component).isEmpty()) return

            val views = RemoteViews(context.packageName, R.layout.widget_now_playing).apply {
                setTextViewText(R.id.widget_title, title ?: "Muzik")
                setTextViewText(R.id.widget_artist, artist ?: "Tap play to resume your music")
                if (artwork != null) {
                    setImageViewBitmap(R.id.widget_artwork, artwork)
                } else {
                    setImageViewResource(R.id.widget_artwork, R.drawable.widget_artwork_placeholder)
                }
                setImageViewResource(
                    R.id.widget_play_pause,
                    if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                )
                setContentDescription(R.id.widget_play_pause, if (isPlaying) "Pause" else "Play")

                setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
                setOnClickPendingIntent(R.id.widget_previous, mediaButtonIntent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                setOnClickPendingIntent(R.id.widget_play_pause, mediaButtonIntent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                setOnClickPendingIntent(R.id.widget_next, mediaButtonIntent(context, KeyEvent.KEYCODE_MEDIA_NEXT))
            }
            manager.updateAppWidget(component, views)
        }

        private fun openAppIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        private fun mediaButtonIntent(context: Context, keyCode: Int): PendingIntent {
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(ComponentName(context, MediaButtonReceiver::class.java))
                .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            return PendingIntent.getBroadcast(
                context,
                keyCode, // distinct request code per button
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
