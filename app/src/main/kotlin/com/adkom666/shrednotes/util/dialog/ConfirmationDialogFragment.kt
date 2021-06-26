package com.adkom666.shrednotes.util.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.parcelize.Parcelize

typealias OnConfirmListener = () -> Unit
typealias OnNotConfirmListener = () -> Unit
typealias OnCancelConfirmListener = (DialogInterface) -> Unit

/**
 * Dialog fragment for a suggestion that the user can agree to or not.
 */
class ConfirmationDialogFragment : KeyboardlessDialogFragment() {

    companion object {

        /**
         * Preferred way to create a fragment.
         *
         * @param titleResId dialog title resource identifier.
         * @param messageResId dialog message resource identifier.
         * @param messageFormatArgs the format arguments that will be used for substitution if
         * [messageResId] refers to the format message; should all be of the same type: [Int] or
         * [String].
         * @return new instance as [ConfirmationDialogFragment].
         */
        fun newInstance(
            @StringRes titleResId: Int,
            @StringRes messageResId: Int,
            vararg messageFormatArgs: Any
        ): ConfirmationDialogFragment {
            val arguments = Bundle()
            arguments.putInt(ARG_TITLE_RES_ID, titleResId)
            arguments.putInt(ARG_MESSAGE_RES_ID, messageResId)
            val formatArgList = messageFormatArgs.toList()
            val envelope = FormatArgsEnvelope.with(formatArgList)
            arguments.putParcelable(ARG_FORMAT_ARGS_ENVELOPE, envelope)
            val fragment = ConfirmationDialogFragment()
            fragment.arguments = arguments
            return fragment
        }

        private const val ARG_TITLE_RES_ID =
            "${BuildConfig.APPLICATION_ID}.args.title_res_id"

        private const val ARG_MESSAGE_RES_ID =
            "${BuildConfig.APPLICATION_ID}.args.message_res_id"

        private const val ARG_FORMAT_ARGS_ENVELOPE =
            "${BuildConfig.APPLICATION_ID}.args.format_args_envelope"
    }

    private var onConfirmListener: OnConfirmListener? = null
    private var onNotConfirmListener: OnNotConfirmListener? = null
    private var onCancelConfirmListener: OnCancelConfirmListener? = null

    private val titleResId: Int
        get() = requireNotNull(_titleResId)

    private val messageResId: Int
        get() = requireNotNull(_messageResId)

    private val formatArgsEnvelope: FormatArgsEnvelope
        get() = requireNotNull(_formatArgsEnvelope)

    private var _titleResId: Int? = null
    private var _messageResId: Int? = null
    private var _formatArgsEnvelope: FormatArgsEnvelope? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = requireArguments()
        _titleResId = arguments.getInt(ARG_TITLE_RES_ID)
        _messageResId = arguments.getInt(ARG_MESSAGE_RES_ID)
        _formatArgsEnvelope = arguments.getParcelable(ARG_FORMAT_ARGS_ENVELOPE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.AppTheme_MaterialAlertDialog_Confirmation
        )
        formatArgsEnvelope.formatArgs?.let { args ->
            @Suppress("SpreadOperator")
            val messageString = getString(messageResId, *args)
            builder.setMessage(messageString)
        } ?: builder.setMessage(messageResId)
        return builder
            .setTitle(titleResId)
            .setPositiveButton(R.string.button_title_yes) { _, _ ->
                onConfirmListener?.invoke()
            }
            .setNegativeButton(R.string.button_title_no) { _, _ ->
                onNotConfirmListener?.invoke()
            }
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelConfirmListener?.let { it(dialog) }
    }

    /**
     * Set [listener] to use on positive button click.
     *
     * @param listener the [OnConfirmListener] to use on positive button click.
     */
    fun setOnConfirmListener(listener: OnConfirmListener) {
        onConfirmListener = listener
    }

    /**
     * Set [listener] to use on negative button click.
     *
     * @param listener the [OnNotConfirmListener] to use on negative button click.
     */
    fun setOnNotConfirmListener(listener: OnNotConfirmListener) {
        onNotConfirmListener = listener
    }

    /**
     * Set [listener] to use when the dialog is cancelled.
     *
     * @param listener the [OnCancelConfirmListener] to use when the dialog is cancelled.
     */
    fun setOnCancelConfirmListener(listener: OnCancelConfirmListener) {
        onCancelConfirmListener = listener
    }

    @Parcelize
    private data class FormatArgsEnvelope(
        val listOfInt: List<Int>? = null,
        val listOfString: List<String>? = null
    ) : Parcelable {

        companion object {

            fun with(
                formatArgList: List<Any>
            ): FormatArgsEnvelope = when (formatArgList.firstOrNull()) {
                is Int -> FormatArgsEnvelope(listOfInt = selectFrom(formatArgList))
                is String -> FormatArgsEnvelope(listOfString = selectFrom(formatArgList))
                else -> FormatArgsEnvelope()
            }

            private inline fun <reified T> selectFrom(formatArgList: List<Any>): List<T> {
                val listOfT = mutableListOf<T>()
                formatArgList.forEach { element ->
                    if (element is T) {
                        listOfT.add(element)
                    }
                }
                return listOfT
            }
        }

        val formatArgs: Array<Any>?
            get() = listOfInt?.toTypedArray() ?: listOfString?.toTypedArray()
    }
}
