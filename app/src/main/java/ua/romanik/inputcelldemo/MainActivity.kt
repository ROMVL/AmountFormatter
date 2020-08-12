package ua.romanik.inputcelldemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ua.romanik.inputcell.amount.Currency

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        with(inputCell) {
            setCurrencySymbol(Currency.EUR)
            setDefaultAmount(50000)
            amountChannel
                .map { it.toString() }
                .onEach {
                    textViewAmountDouble.text = it
                }.launchIn(lifecycleScope)
            amountInStringChannel
                .onEach {
                    textViewAmountString.text = it
                }.launchIn(lifecycleScope)
        }

    }
}