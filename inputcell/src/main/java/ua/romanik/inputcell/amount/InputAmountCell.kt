package ua.romanik.inputcell.amount

import android.content.Context
import android.graphics.Rect
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.min

@ExperimentalCoroutinesApi
class InputAmountCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var currencySymbol: String = ua.romanik.inputcell.amount.Currency.RUR.value

    private var textWatcher: TextWatcher = getTextWatcher().also {
        addTextChangedListener(it)
    }

    private val amountState = MutableStateFlow(MIN_AMOUNT.toDouble())

    private val minFormattedAmount: String
        get() = "$MIN_AMOUNT $currencySymbol"

    val amountChannel: Flow<Double>
        get() = amountState

    init {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        takeIf { focused }
            ?.let {
                removeTextChangedListener(textWatcher)
                addTextChangedListener(textWatcher)
            } ?: removeTextChangedListener(textWatcher)
        takeIf { text.toString().isEmpty() || text.toString() == currencySymbol }?.let {
            setText(minFormattedAmount)
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        takeIf {
            !text.isNullOrEmpty()
                    && (selEnd == getCurrentText().length || selEnd == getCurrentText().length - 1)
        }?.let {
            setSelection(getCurrentText().length - 2)
        } ?: super.onSelectionChanged(selStart, selEnd)
    }

    fun setDefaultAmount(amount: Number) {
        val formattedAmount = "$amount $currencySymbol"
        setText(formattedAmount)
        setSelection(formattedAmount.length - 1)
    }

    fun setCurrencySymbol(currency: ua.romanik.inputcell.amount.Currency) {
        this.currencySymbol = currency.value
    }

    private fun getCurrentText(): String {
        return text?.toString() ?: minFormattedAmount
    }

    private fun getTextWatcher(): TextWatcher = object : TextWatcher {

        private var hasDecimalPoint = false

        private val wholeNumberDecimalFormat = DecimalFormat().apply {
            applyPattern(FORMAT_PATTERN)
            decimalFormatSymbols = decimalFormatSymbolsWatcher
        }

        private val fractionDecimalFormat = DecimalFormat().apply {
            decimalFormatSymbols = decimalFormatSymbolsWatcher
        }

        private val decimalFormatSymbolsWatcher: DecimalFormatSymbols
            get() = DecimalFormatSymbols().apply {
                groupingSeparator = ' '
            }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            fractionDecimalFormat.isDecimalSeparatorAlwaysShown = true
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            hasDecimalPoint =
                s.toString().contains(decimalFormatSymbolsWatcher.decimalSeparator.toString())
        }

        override fun afterTextChanged(s: Editable) {
            val newInputString = s.toString()

            takeUnless { isOnlyCurrencyInString(newInputString) } ?: return

            removeTextChangedListener(this)

            runCatching {
                val startLength = getCurrentText().length
                val numberWithoutGroupingSeparator =
                    parseMoneyValue(
                        newInputString,
                        decimalFormatSymbolsWatcher.groupingSeparator.toString(),
                        currencySymbol
                    )
                val selectionStartIndexBeforeFormatting = selectionStart

                amountState.value = numberWithoutGroupingSeparator.toDouble()

                setText(formatNumber(numberWithoutGroupingSeparator))
                setUpSelectionAfterFormatting(selectionStartIndexBeforeFormatting, startLength)

            }.onFailure {
                it.printStackTrace()
            }

            addTextChangedListener(this)
        }

        private fun isOnlyCurrencyInString(newInputString: String): Boolean {
            return takeIf { newInputString == currencySymbol || newInputString == " $currencySymbol" }
                ?.let {
                    setText(minFormattedAmount)
                    setSelection(SELECTION_FOR_MIN_AMOUNT)
                    true
                } ?: false
        }

        private fun formatNumber(numberWithoutGroupingSeparator: String): String {
            return takeIf {
                hasDecimalPoint
            }?.let {
                fractionDecimalFormat.applyPattern(
                    FRACTION_FORMAT_PATTERN_PREFIX +
                            getFormatSequenceAfterDecimalSeparator(
                                numberWithoutGroupingSeparator
                            )
                )
                "${fractionDecimalFormat.format(numberWithoutGroupingSeparator.toDouble())} $currencySymbol"
            } ?: "${wholeNumberDecimalFormat.format(numberWithoutGroupingSeparator.toDouble())} $currencySymbol"
        }

        private fun setUpSelectionAfterFormatting(
            selectionStartIndexBeforeFormatting: Int,
            startLength: Int
        ) {
            val endLength = getCurrentText().length
            val selection = selectionStartIndexBeforeFormatting + (endLength - startLength)
            takeIf { selection > 0 && selection <= getCurrentText().length }
                ?.let {
                    setSelection(selection)
                } ?: setSelection(getCurrentText().length - 2)
        }

        private fun getFormatSequenceAfterDecimalSeparator(number: String): String {
            val amountAfterDecimalPoint =
                number.length - number.indexOf(decimalFormatSymbolsWatcher.decimalSeparator) - 1
            return MIN_AMOUNT.toString().repeat(
                min(
                    amountAfterDecimalPoint,
                    MAX_NO_OF_DECIMAL_PLACES
                )
            )
        }

        private fun parseMoneyValue(
            value: String,
            groupingSeparator: String,
            currencySymbol: String
        ) = value.replace(groupingSeparator,
            EMPTY_STRING
        ).replace(currencySymbol,
            EMPTY_STRING
        )

    }

    companion object {
        private const val MAX_NO_OF_DECIMAL_PLACES = 2
        private const val FRACTION_FORMAT_PATTERN_PREFIX = "#,##0."
        private const val FORMAT_PATTERN = "#,##0"
        private const val MIN_AMOUNT = 0
        private const val SELECTION_FOR_MIN_AMOUNT = 1
        private const val EMPTY_STRING = ""
    }

}
