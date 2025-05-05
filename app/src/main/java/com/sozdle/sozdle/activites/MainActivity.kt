package com.sozdle.sozdle.activites
import android.app.AlertDialog
import android.content.Intent
import okhttp3.*
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import com.sozdle.sozdle.R
import com.sozdle.sozdle.fragments.LoginFragment
import com.sozdle.sozdle.services.MusicService
import java.util.Timer
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity() {
    private fun game(wordsOfLength: List<String>, wordLength: Int, maxGuesses: Int) {
        supportActionBar?.show()
        setContentView(R.layout.game)
        val input = findViewById<TableLayout>(R.id.input)

        // Apply additional top margin to the TableLayout
        val params = input.layoutParams as ConstraintLayout.LayoutParams
        val padding = dpToPx(16) // Base padding (matches XML margin of 16dp)
        val extraTopMargin = dpToPx(20) // Additional 20dp top margin
        params.setMargins(padding, padding + extraTopMargin, padding, padding)
        input.layoutParams = params

        var chosenWord = wordsOfLength.random()
        Log.w("chosenWord", "$chosenWord")
        val inputLabelsList = Array(maxGuesses) { Array(wordLength) { TextView(this) } }

        for (rowNum in 0 until maxGuesses) {
            val inputRow = TableRow(this)
            val inputLabels = inputLabelsList[rowNum]

            val labelSize = 1000 / maxOf(wordLength, maxGuesses)
            val padding = labelSize.floorDiv(50)
            for (i in 0 until wordLength) {
                val label = inputLabels[i]
                label.width = (labelSize - padding)
                label.height = (labelSize - padding)
                label.textSize = (labelSize / 3).toFloat()
                label.gravity = Gravity.CENTER
                label.setTextColor(ContextCompat.getColorStateList(this, R.color.textColor))
                label.setBackgroundResource(R.color.grey)

                val params = TableRow.LayoutParams(labelSize, labelSize)
                params.setMargins(padding, padding + 20, padding, padding)
                label.layoutParams = params

                inputRow.addView(label)
            }
            inputRow.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            inputRow.gravity = Gravity.CENTER_HORIZONTAL
            input.addView(inputRow)
        }


        var rowNum = 0
        val inputStrings = Array(maxGuesses) { CharArray(wordLength) }
        var inputLen = 0
        var inputString = inputStrings[rowNum]
        var inputLabels = inputLabelsList[rowNum]

        val keyboard = findViewById<LinearLayout>(R.id.keyboard)
        val keyToButton = mutableMapOf<Char, Button>()
        fun makeKeyRow(keys: String) {
            val row = LinearLayout(this)
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            row.gravity = Gravity.CENTER
            row.setPadding(0, 0, 0, 0)
            for (character in keys) {
                val key = Button(this)
                key.backgroundTintList = ContextCompat.getColorStateList(this, R.color.grey)
                key.text = character.toString()
                key.setTextColor(ContextCompat.getColorStateList(this, R.color.textColor))
                key.textSize = 34f
                key.textAlignment = Button.TEXT_ALIGNMENT_CENTER
                key.setPadding(0, 0, 0, 0)
                key.layoutParams = LinearLayout.LayoutParams(105, 160)
                row.addView(key)
                keyToButton[character] = key
            }
            keyboard.addView(row)
        }
        makeKeyRow("ркұғүқөңп")
        makeKeyRow("әіангшжзх")
        makeKeyRow("очсмитбыд")
        makeKeyRow("елһу>?")

        fun newGame() {
            // Restart music for new game
            val musicIntent = Intent(this, MusicService::class.java)
            stopService(musicIntent)
            startService(musicIntent)

            for (labels in inputLabelsList) {
                for (label in labels) {
                    label.text = ""
                    label.backgroundTintList = ContextCompat.getColorStateList(this, R.color.grey)
                }
            }
            for (list in inputStrings) {
                list.fill('\u0000')
            }
            for (button in keyToButton.values) {
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.grey)
            }
            rowNum = 0
            inputLen = 0
            inputString = inputStrings[rowNum]
            inputLabels = inputLabelsList[rowNum]
            chosenWord = wordsOfLength.random()
            Log.w("chosenWord", "$chosenWord")
        }

        var stopInputs = false
        for ((key, button) in keyToButton) {
            button.setOnClickListener {
                if (stopInputs) {
                    return@setOnClickListener
                }
                if (key == '?') {
                    if (inputLen != wordLength) {
                        return@setOnClickListener
                    }
                    if (!wordsOfLength.contains(inputString.joinToString(separator = ""))) {
                        Toast.makeText(this, "дұрыс сөз емес", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    for (i in 0 until rowNum) {
                        if (inputStrings[i].contentEquals(inputString)) {
                            Toast.makeText(this, "ол сөзді жаздың", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                    }
                    var allCorrect = true
                    for (i in 0 until wordLength) {
                        val guess = inputString[i]
                        val correctCharacter = chosenWord[i]
                        if (guess == correctCharacter) {
                            inputLabels[i].backgroundTintList =
                                ContextCompat.getColorStateList(this, R.color.green)
                            keyToButton[guess]!!.backgroundTintList =
                                ContextCompat.getColorStateList(this, R.color.green)
                        } else {
                            allCorrect = false
                            if (chosenWord.contains(guess)) {
                                inputLabels[i].backgroundTintList =
                                    ContextCompat.getColorStateList(this, R.color.orange)
                                keyToButton[guess]!!.let {
                                    if (it.backgroundTintList != ContextCompat.getColorStateList(this, R.color.green)) {
                                        it.backgroundTintList = ContextCompat.getColorStateList(this, R.color.orange)
                                    }
                                }
                            } else {
                                keyToButton[guess]!!.let {
                                    if (it.backgroundTintList == ContextCompat.getColorStateList(this, R.color.grey)) {
                                        it.backgroundTintList = ContextCompat.getColorStateList(this, R.color.black)
                                    }
                                }
                            }
                        }
                    }
                    if (allCorrect) {
                        stopInputs = true
                        Toast.makeText(this, "мықты", Toast.LENGTH_LONG).show()
                        Timer().schedule(5000) {
                            newGame()
                            stopInputs = false
                        }
                        return@setOnClickListener
                    }
                    if (rowNum + 1 == maxGuesses) {
                        stopInputs = true
                        runOnUiThread {
                            AlertDialog.Builder(this)
                                .setTitle("ойын бітті")
                                .setMessage("таппадың, сөз осындай $chosenWord")
                                .setPositiveButton("жаңа ойын") { _, _ ->
                                    newGame()
                                    stopInputs = false
                                }
                                .setNegativeButton("шығу") { _, _ ->
                                    finish()
                                }
                                .setCancelable(false)
                                .show()
                        }
                        return@setOnClickListener
                    }
                    rowNum += 1
                    inputString = inputStrings[rowNum]
                    inputLen = 0
                    inputLabels = inputLabelsList[rowNum]
                } else if (key == '>') {
                    if (inputLen != 0) {
                        inputLen -= 1
                        inputLabels[inputLen].text = ""
                        inputString[inputLen] = '\u0000'
                    }
                } else {
                    if (inputLen == wordLength) {
                        return@setOnClickListener
                    }
                    inputLabels[inputLen].text = key.uppercase()
                    inputString[inputLen] = key
                    inputLen += 1
                }
            }
        }
    }
    fun menu() {
        supportActionBar?.hide()
        setContentView(R.layout.menu)

        val wordLengthBar = findViewById<Slider>(R.id.wordLengthBar)
        val textLengthLabel = findViewById<TextView>(R.id.wordLength)

        textLengthLabel.text = wordLengthBar.value.toInt().toString()
        wordLengthBar.addOnChangeListener { _, value, _ ->
            textLengthLabel.text = value.toInt().toString()
        }

        val maxGuessesBar = findViewById<Slider>(R.id.maxGuessesBar)
        val maxGuessesLabel = findViewById<TextView>(R.id.maxGuesses)
        maxGuessesLabel.text = maxGuessesBar.value.toInt().toString()
        maxGuessesBar.addOnChangeListener { _, value, _ ->
            maxGuessesLabel.text = value.toInt().toString()
        }

        val playButton = findViewById<Button>(R.id.play)
        playButton.setOnClickListener {
            val musicIntent = Intent(this, MusicService::class.java)
            stopService(musicIntent)
            startService(musicIntent)
            val wordLength = wordLengthBar.value.toInt()
            val maxGuesses = maxGuessesBar.value.toInt()

            val words = listOf(
                "а", "ә", "б", "г", "ғ", "д", "е", "ж", "з", "и", "й", "к", "қ", "л", "м",
                "н", "ң", "о", "ө", "п", "р", "с", "т", "у", "ұ", "ү", "ф", "х", "һ", "ч",
                "ш", "ы", "і", "э",
                "ай", "су", "от", "ақ", "көк", "ал", "ан", "ас", "ат", "ау", "ба", "бі", "бұ",
                "да", "де", "до", "ек", "ел", "ем", "ен", "ер", "ес", "ет", "жа", "же", "жо",
                "жу", "за", "зе", "зо", "йа", "ке", "кі", "қа", "құ", "ла", "ле", "ма", "ме",
                "мо", "на", "не", "но", "па", "ра", "ре", "са", "се", "со", "та", "те", "то",
                "ту", "ты", "уа", "ұн", "үз",
                "ана", "әке", "бала", "үй", "көз", "қол", "аяқ", "бас", "ауа", "жол", "тау",
                "күн", "жер", "гүл", "құс", "ит", "ат", "қой", "нан", "ет", "сүт", "май", "бал",
                "тұз", "қызыл", "сары", "қара", "алма", "екі", "үш", "төрт", "бес", "алты",
                "жеті", "сегіз", "тоғыз", "нөл", "бір", "дау", "жан", "жар", "жас", "кел",
                "кет", "қаз", "қыс", "сау", "сен", "сіз", "тек", "тіл", "шай", "шаш",
                "орман", "ай", "жұлдыз", "аспан", "ағаш", "балық", "мысық", "сиыр", "ешкі",
                "түйе", "алмұрт", "өрік", "жидек", "қарбыз", "қауын", "кітап", "дәптер",
                "қалам", "сызғыш", "өшіргіш", "мектеп", "сынып", "үйде", "жазу", "оқу", "сана",
                "тамақ", "асу", "ішу", "ұйықтау", "жүру", "отыр", "тұру", "күлу", "жылу", "суық",
                "ыстық", "қар", "жаңбыр", "жел", "бұлт", "көлеңке", "жарық", "қараңғы", "таза",
                "лас", "жұмсақ", "қатты",
                "шабдалы", "дос", "жақсы", "жаман", "үлкен", "кіші", "ұзын", "қысқа", "ащы",
                "тәтті", "ағайын", "бауыр", "немере", "жиен", "ата", "әже", "аға", "іні",
                "апа", "сіңлі", "көйлек", "шалбар", "аяқ киім", "бас киім", "қолғап", "жібек",
                "мақта", "жүн", "тері", "тас", "құм", "топырақ", "отын", "темір", "алтын",
                "күміс", "жез", "мырыш", "көмір", "майда", "ұсақ", "ірі", "орта", "теңіз",
                "өзен", "көл", "бұлақ", "сарқырама",
                "уақыт", "сағат", "минут", "күнделік", "жыл", "айлар", "апта", "демалыс",
                "жұмыс", "ойын", "жүзу", "би", "ән", "сурет", "бояу", "қылқалам", "қағаз",
                "тақта", "бор", "жоспар", "мақсат", "арман", "күш", "дене", "саулық", "дәрі",
                "ауру", "ем", "тарих", "мұғалім", "оқушы", "баға", "жолдас", "көрші", "ауыл",
                "қала", "асхана", "жуынатын", "қонақ бөлме", "жатын бөлме", "балкон", "есік",
                "терезе", "кілем", "үстел", "орындық", "диван", "шкаф", "сөре", "айна", "сүлгі",
                "көлік", "жаяу", "жүгіру", "секіру", "отырғыш", "саяхат", "демалу", "оқиға",
                "хикаят", "өлең", "жыр", "ертегі", "аңыз", "достық", "махаббат", "бақыт",
                "сағыныш", "қуаныш", "мейірім", "сыйластық", "адалдық", "шындық", "өтірік",
                "көңіл", "сезім", "ой", "ес", "ақыл", "түсінік", "бірлік", "тәуелсіздік",
                "еркіндік", "бостандық", "табиғат", "орманшы", "бағбан", "фермер", "малшы",
                "балықшы", "аңшы", "суретші", "жазушы", "ақын", "биші", "ғалым", "дәрігер",
                "құрылысшы"
            ).filter { it.length == wordLength }

            if (words.isEmpty()) {
                Log.w("Menu", "No words found for length $wordLength")
                Toast.makeText(this, "Сөздер табылмады", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            game(words, wordLength, maxGuesses)
        }

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            finish()
        }
    }

    private lateinit var words: List<String>

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu -> {
            menu()
            true
        }
        R.id.music -> {
            // Show popup menu for music controls
            val popup = PopupMenu(this, findViewById(R.id.music))
            menuInflater.inflate(R.menu.popup_menu, popup.menu)
            popup.setOnMenuItemClickListener { popupItem ->
                when (popupItem.itemId) {
                    R.id.start_music -> {
                        val musicIntent = Intent(this, MusicService::class.java)
                        startService(musicIntent)
                        Toast.makeText(this, "Музыка басталды", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.stop_music -> {
                        val musicIntent = Intent(this, MusicService::class.java)
                        stopService(musicIntent)
                        Toast.makeText(this, "Музыка тоқтатылды", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game)

        // Load LoginFragment on startup
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }


}