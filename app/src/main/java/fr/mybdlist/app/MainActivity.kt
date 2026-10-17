package fr.mybdlist.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
import android.text.Editable
import android.view.View
import android.widget.*
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var db: BooksDb
    private lateinit var items: LinearLayout
    private lateinit var counter: TextView
    private lateinit var search: EditText
    private val importRequest = 501
    private val exportRequest = 502

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = BooksDb(this)
        render()
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun column(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(10), dp(14), dp(10)) }
    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; setOnClickListener { action() } }
    private fun edit(hint: String, value: String = "") = EditText(this).apply { this.hint = hint; setText(value); setSingleLine(true) }
    private fun render() {
        val outer = column()
        val title = TextView(this).apply { text = "📚 MyBDList"; textSize = 26f; setPadding(0, 0, 0, dp(8)) }
        outer.addView(title)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(button("+ Album") { showEditor() }, LinearLayout.LayoutParams(0, dp(55), 1f))
        actions.addView(button("Scanner") { scan() }, LinearLayout.LayoutParams(0, dp(55), 1f))
        outer.addView(actions)
        val files = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        files.addView(button("Importer") { pickImport() }, LinearLayout.LayoutParams(0, dp(55), 1f))
        files.addView(button("Exporter") { pickExport() }, LinearLayout.LayoutParams(0, dp(55), 1f))
        outer.addView(files)
        search = edit("Rechercher titre, série, auteur ou ISBN")
        outer.addView(search)
        counter = TextView(this).apply { textSize = 14f; setPadding(0, dp(4), 0, dp(10)) }
        outer.addView(counter)
        val scroller = ScrollView(this)
        items = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroller.addView(items)
        outer.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(outer)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refresh() }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        refresh()
    }
    private fun refresh() {
        val all = db.all()
        val shown = db.all(search.text.toString().trim())
        counter.text = "${all.count { it.owned }} possédés • ${all.count { !it.owned }} recherchés • ${shown.size} affichés"
        items.removeAllViews()
        if (shown.isEmpty()) items.addView(TextView(this).apply { text = "Aucun album. Ajoute ta première BD !"; textSize = 17f })
        shown.forEach { a ->
            val info = listOf(a.series, if (a.volume.isBlank()) "" else "Tome ${a.volume}", a.author).filter { it.isNotBlank() }.joinToString(" · ")
            val row = button("${if (a.owned) "✓" else "☆"} ${a.title}\n$info") { showEditor(a) }
            row.isAllCaps = false
            row.gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            items.addView(row, LinearLayout.LayoutParams(-1, dp(76)))
        }
    }
    private fun showEditor(existing: Album? = null, scannedIsbn: String = "") {
        val content = column()
        val title = edit("Titre *", existing?.title ?: "")
        val series = edit("Série", existing?.series ?: "")
        val volume = edit("Numéro du tome", existing?.volume ?: "")
        val author = edit("Auteur", existing?.author ?: "")
        val isbn = edit("ISBN / EAN", existing?.isbn ?: scannedIsbn)
        val notes = edit("Notes", existing?.notes ?: "")
        val owned = CheckBox(this).apply { text = "Je possède cet album"; isChecked = existing?.owned ?: true }
        listOf(title, series, volume, author, isbn, notes, owned).forEach { content.addView(it) }
        val dialog = AlertDialog.Builder(this).setTitle(if (existing == null) "Ajouter une BD" else "Modifier la BD")
            .setView(ScrollView(this).apply { addView(content) })
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Enregistrer", null)
            .apply { if (existing != null) setNeutralButton("Supprimer") { _, _ -> confirmDelete(existing) } }
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (title.text.toString().isBlank()) { title.error = "Titre obligatoire"; return@setOnClickListener }
                val a = Album(existing?.id ?: 0, title.text.toString(), series.text.toString(), volume.text.toString(), author.text.toString(), isbn.text.toString(), owned.isChecked, notes.text.toString())
                if (existing == null && db.duplicate(a)) {
                    AlertDialog.Builder(this).setTitle("Doublon possible").setMessage("Un album avec cet ISBN ou ce tome est déjà enregistré. Ajouter quand même ?")
                        .setNegativeButton("Non", null).setPositiveButton("Oui") { _, _ -> db.insert(a); dialog.dismiss(); refresh() }.show()
                } else {
                    if (existing == null) db.insert(a) else db.update(a)
                    dialog.dismiss(); refresh()
                }
            }
        }
        dialog.show()
        if (existing == null && scannedIsbn.isNotBlank()) fetchOpenLibrary(scannedIsbn) { candidateTitle, candidateAuthor ->
            if (!isFinishing && dialog.isShowing && title.text.isBlank()) title.setText(candidateTitle)
            if (!isFinishing && dialog.isShowing && author.text.isBlank()) author.setText(candidateAuthor)
        }
    }
    private fun confirmDelete(a: Album) {
        AlertDialog.Builder(this).setTitle("Supprimer l'album ?").setMessage(a.title)
            .setNegativeButton("Annuler", null).setPositiveButton("Supprimer") { _, _ -> db.delete(a.id); refresh() }.show()
    }
    private fun scan() {
        IntentIntegrator(this).apply {
            setDesiredBarcodeFormats(listOf("EAN_13", "EAN_8", "UPC_A", "UPC_E", "CODE_128"))
            setPrompt("Scanne le code-barres de ta BD")
            setBeepEnabled(false)
            setOrientationLocked(false)
            initiateScan()
        }
    }
    @Deprecated("Uses onActivityResult for simple document picker and ZXing integration")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == importRequest && resultCode == RESULT_OK && data?.data != null) {
            try {
                val raw = contentResolver.openInputStream(data.data!!)?.bufferedReader()?.use { it.readText() } ?: error("Fichier vide")
                val parsed = if (raw.trimStart().startsWith("[")) decodeJson(raw) else decodeCsv(raw)
                var added = 0; var duplicates = 0
                parsed.forEach { a ->
                    if (a.title.isNotBlank()) { if (db.duplicate(a)) duplicates++ else { db.insert(a); added++ } }
                }
                refresh(); message("Import terminé", "$added albums ajoutés, $duplicates doublons ignorés.")
            } catch (e: Exception) { message("Erreur d'import", e.message ?: "Format illisible") }
            return
        }
        if (requestCode == exportRequest && resultCode == RESULT_OK && data?.data != null) {
            try {
                contentResolver.openOutputStream(data.data!!)?.bufferedWriter()?.use { writer -> writer.write(encodeJson(db.all())) }
                message("Sauvegarde créée", "Export JSON enregistré.")
            } catch (e: Exception) { message("Erreur d'export", e.message ?: "Échec") }
            return
        }
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) showEditor(scannedIsbn = result.contents)
    }
    private fun pickImport() = startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/csv", "text/plain", "application/octet-stream"))
    }, importRequest)
    private fun pickExport() = startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        type = "application/json"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_TITLE, "mybdlist-sauvegarde.json")
    }, exportRequest)
    private fun message(title: String, body: String) { AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("OK", null).show() }
    private fun encodeJson(albums: List<Album>): String = JSONArray().apply {
        albums.forEach { a -> put(JSONObject().apply {
            put("title", a.title); put("series", a.series); put("volume", a.volume); put("author", a.author)
            put("isbn", a.isbn); put("owned", a.owned); put("notes", a.notes)
        }) }
    }.toString(2)
    private fun decodeJson(raw: String): List<Album> {
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Album(title = o.optString("title"), series = o.optString("series"), volume = o.optString("volume"),
                author = o.optString("author"), isbn = o.optString("isbn"), owned = o.optBoolean("owned", true), notes = o.optString("notes"))
        }
    }
    private fun decodeCsv(raw: String): List<Album> {
        val lines = raw.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return emptyList()
        val delimiter = if (lines.first().count { it == ';' } >= lines.first().count { it == ',' }) ';' else ','
        val header = parseCsvLine(lines.first().removePrefix("\uFEFF"), delimiter).map { it.trim().lowercase().replace("é", "e").replace("è", "e") }
        fun field(row: List<String>, vararg names: String): String {
            val i = header.indexOfFirst { h -> names.any { it == h } }
            return if (i >= 0) row.getOrElse(i) { "" }.trim() else ""
        }
        return lines.drop(1).map { line ->
            val row = parseCsvLine(line, delimiter)
            Album(title = field(row, "titre", "title", "album", "nom"), series = field(row, "serie", "series", "collection"),
                volume = field(row, "tome", "numero", "numero tome", "volume", "num"),
                author = field(row, "auteur", "author", "scenariste"), isbn = field(row, "isbn", "ean", "ean13", "code barre"),
                owned = field(row, "possession", "owned", "possede").let { it.isBlank() || it.lowercase() !in listOf("non", "false", "0") }, notes = field(row, "notes", "commentaire"))
        }
    }
    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>(); val current = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (quoted && i + 1 < line.length && line[i + 1] == '"') { current.append('"'); i++ } else quoted = !quoted
            } else if (c == delimiter && !quoted) { result.add(current.toString()); current.clear() } else current.append(c)
            i++
        }
        result.add(current.toString()); return result
    }
    private fun fetchOpenLibrary(isbn: String, done: (String, String) -> Unit) {
        thread {
            try {
                val conn = URL("https://openlibrary.org/isbn/${java.net.URLEncoder.encode(isbn, "UTF-8")}.json").openConnection() as HttpURLConnection
                conn.connectTimeout = 5000; conn.readTimeout = 5000
                val obj = conn.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                conn.disconnect()
                runOnUiThread { done(obj.optString("title"), "") }
            } catch (_: Exception) { /* Le scan reste utilisable hors ligne. */ }
        }
    }
}
