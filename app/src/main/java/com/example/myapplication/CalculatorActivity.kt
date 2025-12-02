package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class CalculatorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calculator)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val bBackToMain = findViewById<Button>(R.id.back_to_main)

        bBackToMain.setOnClickListener{
            val backToMain = Intent(this, MainActivity::class.java)
            startActivity(backToMain)
        }

        val resultTextView = findViewById<TextView>(R.id.resultTextView)
        var currentInput = ""

        val numberButtonsId = arrayOf(
            R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5, R.id.button6, R.id.button7, R.id.button8, R.id.button9, R.id.button0
        )

        val operationButtons = mapOf(
            R.id.buttonPlus to "+",
            R.id.buttonMinus to "-",
            R.id.buttonMultiply to "*",
            R.id.buttonDivide to "/"
        )

        for (i in 0 until numberButtonsId.size) {
            val buttonId = numberButtonsId[i]
            val button = findViewById<Button>(buttonId)

            button.setOnClickListener {
                val textFromButton = button.text.toString()
                currentInput += textFromButton
                resultTextView.text = currentInput
            }
        }

        for (input in operationButtons) {
            val id = input.key
            val operation = input.value
            val button = findViewById<Button>(id)

            button.setOnClickListener {
                if (currentInput != "") {
                    currentInput += operation
                    resultTextView.text = currentInput
                }
            }
        }

        val equalsButton = findViewById<Button>(R.id.buttonEquals)

        equalsButton.setOnClickListener {
            if (currentInput.isNotEmpty()) {
                val calculationResult = calculateString(currentInput)
                resultTextView.text = calculationResult
                currentInput = calculationResult
            }
            resultTextView.setTextColor(Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)))
        }

        val deleteButton = findViewById<Button>(R.id.buttonDelete)

        deleteButton.setOnClickListener {
            if (currentInput.isNotEmpty()) {
                currentInput = currentInput.substring(0, currentInput.length - 1)

                if (currentInput.isEmpty()) {
                    resultTextView.text = "0"
                } else {
                    resultTextView.text = currentInput
                }
            }
        }
    }

    fun calculateString(expression: String): String {
        var firstNumberString = ""
        var operatorString = ""
        var secondNumberString = ""

        for (character in expression) {
            if (character.isDigit()) {
                if (operatorString.isEmpty()) {
                    firstNumberString = firstNumberString + character
                } else {
                    secondNumberString = secondNumberString + character
                }
            }
            else if (character == '+' || character == '-' || character == '*' || character == '/') {
                operatorString = character.toString()
            }
        }

        if (firstNumberString.isEmpty()) return "Ошибка: нет первого числа"
        if (operatorString.isEmpty()) return "Ошибка: нет оператора"
        if (secondNumberString.isEmpty()) return "Ошибка: нет второго числа"

        val firstNumber = firstNumberString.toInt()
        val secondNumber = secondNumberString.toInt()

        val result = when (operatorString) {
            "+" -> firstNumber + secondNumber
            "-" -> firstNumber - secondNumber
            "*" -> firstNumber * secondNumber
            "/" -> if (secondNumber == 0) {
                return "Ошибка: деление на ноль"
            } else {
                firstNumber / secondNumber
            } else -> return "Ошибка"
        }

        return result.toString()
    }
}

