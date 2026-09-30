package app.ergo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentTest {
    @Test fun pluralRussian() {
        assertEquals("урок", plural(1, "урок", "урока", "уроков"))
        assertEquals("урока", plural(3, "урок", "урока", "уроков"))
        assertEquals("уроков", plural(5, "урок", "урока", "уроков"))
        assertEquals("уроков", plural(11, "урок", "урока", "уроков"))
        assertEquals("уроков", plural(12, "урок", "урока", "уроков"))
        assertEquals("урок", plural(21, "урок", "урока", "уроков"))
        assertEquals("урока", plural(23, "урок", "урока", "уроков"))
    }

    @Test fun demoFlagMatchesCyrillicCaseInsensitively() {
        assertEquals("Соломенное чучело", demoFlag("То есть вы хотите запретить всё?")?.name)
        assertEquals("Переход на личности", demoFlag("Вы просто не понимаете сути.")?.name)
        assertEquals("Ложная дилемма", demoFlag("Либо мы запрещаем, либо дети страдают.")?.name)
        assertEquals("Поспешное обобщение", demoFlag("Все подростки сидят в телефонах.")?.name)
        assertEquals("Скользкая дорожка", demoFlag("Если мы уступим сейчас, то скоро потеряем всё.")?.name)
        assertNull(demoFlag("Исследования показывают умеренный эффект."))
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
}
