package com.adkom666.shrednotes.ui.notes

import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.core.view.children
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.databinding.ItemNoteGroupHeaderBinding
import com.adkom666.shrednotes.util.time.Days
import com.adkom666.shrednotes.util.titleResId
import com.adkom666.shrednotes.util.weekday
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/**
 * [RecyclerView.ItemDecoration] to set a date header for notes with the same date.
 *
 * @property headerMarginBottom margin from header's bottom to item's top in pixels.
 * @property noteProvider should provide a note on its position in the recycler view.
 */
class NoteGroupHeader(
    @Px
    private val headerMarginBottom: Int,
    private val noteProvider: (position: Int) -> Note?
) : RecyclerView.ItemDecoration() {

    private val dateFormat: DateFormat = DateFormat.getDateInstance(
        DateFormat.SHORT,
        Locale.getDefault()
    )

    private val calendar = Calendar.getInstance()

    private val headerMap: MutableMap<Days, WeakReference<View>> = mutableMapOf()

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (parent.isHeaderNeed(position)) {
            parent.daysForPosition(position)?.let { days ->
                val headerView = parent.headerForDays(days)
                outRect.top = headerView.height + view.marginTop + headerMarginBottom
            }
        }
    }

    override fun onDrawOver(
        canvas: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.onDrawOver(canvas, parent, state)
        val firstHeaderDays = parent.firstHeaderDays()
        val secondHeaderTop = parent.drawHeadersFromSecond(canvas, firstHeaderDays)
        parent.drawFirstHeader(canvas, firstHeaderDays, secondHeaderTop)
    }

    private fun RecyclerView.firstHeaderDays(): Days? {
        return childInContact(paddingTop)?.let { firstView ->
            val firstViewPosition = getChildAdapterPosition(firstView)
            daysForPosition(firstViewPosition)
        }
    }

    private fun RecyclerView.drawHeadersFromSecond(
        canvas: Canvas,
        firstHeaderDays: Days?
    ): Int? {
        var secondHeaderTop: Int? = null
        val viewHolders = firstHeaderDays?.let { viewHolders(it) }
        viewHolders?.forEach { viewHolder ->
            val view = viewHolder.itemView
            val position = getChildAdapterPosition(view)
            daysForPosition(position)?.let { days ->
                if (days != firstHeaderDays && isHeaderNeed(position)) {
                    val headerView = headerForDays(days)
                    val headerViewTop = view.top - headerView.height - headerMarginBottom
                    canvas.drawView(headerView, headerViewTop)

                    val consideredTop = secondHeaderTop
                    if (consideredTop == null || headerViewTop < consideredTop) {
                        secondHeaderTop = headerViewTop
                    }
                }
            }
        }
        return secondHeaderTop
    }

    private fun RecyclerView.viewHolders(firstHeaderDays: Days) = children
        .map { findContainingViewHolder(it) }
        .filterNotNull()
        .filter { viewHolder ->
            val view = viewHolder.itemView
            val position = getChildAdapterPosition(view)
            daysForPosition(position)?.let { days ->
                days.epochMillis < firstHeaderDays.epochMillis
            } ?: false
        }

    private fun RecyclerView.drawFirstHeader(
        canvas: Canvas,
        firstHeaderDays: Days?,
        secondHeaderTop: Int?
    ) {
        firstHeaderDays?.let { headerForDays(it) }?.let { firstHeaderView ->
            val headerViewTop = secondHeaderTop?.let {
                min(it - firstHeaderView.height, paddingTop)
            } ?: paddingTop
            canvas.drawView(firstHeaderView, headerViewTop)
        }
    }

    private fun RecyclerView.isHeaderNeed(
        position: Int
    ): Boolean = when (position) {
        0 -> true
        RecyclerView.NO_POSITION -> false
        else -> (adapter as? NotePagingDataAdapter)?.isHeaderNeed(position) ?: false
    }

    private fun <VH : RecyclerView.ViewHolder?> RecyclerView.Adapter<VH>.isHeaderNeed(
        position: Int
    ): Boolean = when (position) {
        0 -> true
        in 1 until itemCount ->
            noteProvider(position)?.let { note ->
                noteProvider(position - 1)?.let { previousNote ->
                    Days(note.dateTime) != Days(previousNote.dateTime)
                }
            } ?: false
        else -> false
    }

    private fun RecyclerView.daysForPosition(
        position: Int
    ): Days? = if (position != RecyclerView.NO_POSITION) {
        (adapter as? NotePagingDataAdapter)?.daysForPosition(position)
    } else {
        null
    }

    private fun <VH : RecyclerView.ViewHolder?> RecyclerView.Adapter<VH>.daysForPosition(
        position: Int
    ): Days? = if (position in 0 until itemCount) {
        noteProvider(position)?.let { note ->
            Days(note.dateTime)
        }
    } else {
        null
    }

    private fun RecyclerView.headerForDays(days: Days ): View {
        return headerMap[days]?.get()
            ?: createHeader(days)
                .also { headerMap[days] = WeakReference(it) }
    }

    private fun RecyclerView.createHeader(days: Days): View {
        val inflater = LayoutInflater.from(context)
        val binding = ItemNoteGroupHeaderBinding.inflate(inflater, this, false)

        val weekday = days.date.weekday(calendar)
        val weekdayTitleResId = weekday.titleResId()
        val weekdayTitle = context.getString(weekdayTitleResId)
        val headerText = "$weekdayTitle, ${dateFormat.format(days.date)}"

        binding.noteGroupHeaderTextView.text = headerText
        val headerView = binding.root
        fixChildLayoutSize(headerView)
        return headerView
    }

    /**
     * This child overlaps the [contactPoint].
     *
     * @param contactPoint the Y coordinate of the target [View] point.
     * @return child [View] overlapped by [contactPoint] or null.
      */
    private fun RecyclerView.childInContact(contactPoint: Int): View? {
        children.forEach { child ->
            val bounds = Rect()
            getDecoratedBoundsWithMargins(child, bounds)
            if (contactPoint in bounds.top until bounds.bottom) {
                return child
            }
        }
        return null
    }

    private fun ViewGroup.fixChildLayoutSize(child: View) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            width,
            View.MeasureSpec.EXACTLY
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(
            height,
            View.MeasureSpec.UNSPECIFIED
        )
        val childWidth = ViewGroup.getChildMeasureSpec(
            widthSpec,
            paddingLeft + paddingRight,
            child.layoutParams.width
        )
        val childHeight = ViewGroup.getChildMeasureSpec(
            heightSpec,
            paddingTop + paddingBottom,
            child.layoutParams.height
        )
        child.measure(childWidth, childHeight)
        child.layout(0, 0, child.measuredWidth, child.measuredHeight)
    }

    private fun Canvas.drawView(
        view: View,
        paddingTop: Int
    ) {
        save()
        translate(0f, paddingTop.toFloat())
        view.draw(this)
        restore()
    }
}
