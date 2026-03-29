package io.github.mobdev

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView

    private var currentInput = ""
    private var firstNumber: Double? = null
    private var operator: String? = null
    private var shouldClearInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)

        savedInstanceState?.let {
            currentInput = it.getString("currentInput", "")
            firstNumber = if (it.containsKey("firstNumber")) it.getDouble("firstNumber") else null
            operator = it.getString("operator")
            shouldClearInput = it.getBoolean("shouldClearInput", false)
            tvExpression.text = it.getString("expressionText", "")
            tvResult.text = it.getString("resultText", "0")
        }

        val digitButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        for (id in digitButtons) {
            findViewById<Button>(id).setOnClickListener {
                appendDigit((it as Button).text.toString())
            }
        }

        findViewById<Button>(R.id.btnDot).setOnClickListener {
            appendDot()
        }

        findViewById<Button>(R.id.btnPlus).setOnClickListener {
            setOperator("+")
        }

        findViewById<Button>(R.id.btnMinus).setOnClickListener {
            setOperator("-")
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            calculate()
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            clearAll()
        }
    }

    private fun appendDigit(digit: String) {
        if (shouldClearInput) {
            currentInput = ""
            shouldClearInput = false
        }
        currentInput += digit
        tvResult.text = currentInput
    }

    private fun appendDot() {
        if (shouldClearInput) {
            currentInput = ""
            shouldClearInput = false
        }
        if (!currentInput.contains(".")) {
            currentInput = if (currentInput.isEmpty()) "0." else "$currentInput."
            tvResult.text = currentInput
        }
    }

    private fun setOperator(op: String) {
        if (currentInput.isEmpty()) return
        firstNumber = currentInput.toDoubleOrNull()
        operator = op
        tvExpression.text = "$currentInput $op"
        shouldClearInput = true
    }

    private fun calculate() {
        val first = firstNumber
        val op = operator
        val second = currentInput.toDoubleOrNull()

        if (first == null || op == null || second == null) return

        val result = when (op) {
            "+" -> first + second
            "-" -> first - second
            else -> return
        }

        tvExpression.text = "$first $op $second ="
        tvResult.text = result.toString()

        currentInput = result.toString()
        firstNumber = null
        operator = null
        shouldClearInput = true
    }

    private fun clearAll() {
        currentInput = ""
        firstNumber = null
        operator = null
        shouldClearInput = false
        tvExpression.text = ""
        tvResult.text = "0"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentInput", currentInput)
        firstNumber?.let { outState.putDouble("firstNumber", it) }
        outState.putString("operator", operator)
        outState.putBoolean("shouldClearInput", shouldClearInput)
        outState.putString("expressionText", tvExpression.text.toString())
        outState.putString("resultText", tvResult.text.toString())
    }
}