package app.ergo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentTest {
    private val bank = TestBank.bank

    @Test fun pluralRussian() {
        assertEquals("урок", plural(1, "урок", "урока", "уроков"))
        assertEquals("урока", plural(3, "урок", "урока", "уроков"))
        assertEquals("уроков", plural(5, "урок", "урока", "уроков"))
        assertEquals("уроков", plural(11, "урок", "урока", "уроков"))
        assertEquals("уроков", plural(12, "урок", "урока", "уроков"))
        assertEquals("урок", plural(21, "урок", "урока", "уроков"))
        assertEquals("урока", plural(23, "урок", "урока", "уроков"))
        assertEquals("26 уроков", count(26, "урок", "урока", "уроков"))
    }

    @Test fun demoFlagMatchesCyrillicCaseInsensitively() {
        assertEquals("straw_man", demoFlag("То есть вы хотите запретить всё?")?.topic)
        assertEquals("ad_hominem", demoFlag("Вы просто не понимаете сути.")?.topic)
        assertEquals("false_dilemma", demoFlag("Либо мы запрещаем, либо дети страдают.")?.topic)
        assertEquals("hasty", demoFlag("Все подростки сидят в телефонах.")?.topic)
        assertEquals("slippery", demoFlag("Если мы уступим сейчас, то скоро потеряем всё.")?.topic)
        assertEquals("bandwagon", demoFlag("Все так делают, и ничего.")?.topic)
        assertEquals("tu_quoque", demoFlag("А сами-то вы не опаздываете?")?.topic)
        assertEquals("authority", demoFlag("Учёные доказали, что это работает.")?.topic)
        assertNull(demoFlag("Исследования показывают умеренный эффект."))
    }

    @Test fun demoFlagUsesBankNames() {
        assertEquals("Соломенное чучело", demoFlag("То есть вы хотите запретить всё?", bank)?.name)
    }

    @Test fun demoFlagQuoteIsTrimmedAndCapped() {
        val f = demoFlag("  Все " + "очень ".repeat(30) + "плохо.")!!
        assertEquals(90, f.quote.length)
        assertEquals('В', f.quote.first())
    }

    @Test fun costFormatting() {
        assertEquals("", fmtCost(null))
        assertEquals("$0", fmtCost(0.0))
        assertEquals("$0.00012", fmtCost(0.00012))
        assertEquals("$0.125", fmtCost(0.125))
        assertEquals("бесплатно", per1M(0.0))
        assertEquals("$3.00", per1M(3e-6))
        assertEquals("$0.050", per1M(5e-8))
        assertEquals("—", money(null))
        assertEquals("$1.50", money(1.5))
    }

    @Test fun parsesModelJsonWrappedInFencesAndProse() {
        val o = parseModelJson("Вот ответ:\n```json\n{\"reply\": \"Нет.\", \"flag\": null}\n```")
        assertEquals("Нет.", o.getString("reply"))
        assertEquals(listOf("a", "b"), parseModelJson("{\"s\": [\"a\", null, \"b\"]}").stringList("s"))
    }

    @Test fun jsonNullFieldsReadAsEmpty() {
        val o = org.json.JSONObject("{\"source\": null, \"answer\": \"Ложная дилемма\"}")
        assertEquals("", o.str("source"))
        assertEquals("", o.str("missing"))
        assertEquals("Ложная дилемма", o.str("answer"))
    }

    @Test(expected = org.json.JSONException::class) fun truncatedReplyIsJsonError() {
        parseModelJson("{\"source\": \"Рабочий чат\", \"sentences\": [\"Думаю,")
    }

    @Test(expected = org.json.JSONException::class) fun nullReplyIsJsonError() {
        parseModelJson("null")
    }
}
