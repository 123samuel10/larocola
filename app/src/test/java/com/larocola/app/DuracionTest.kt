package com.larocola.app

import com.larocola.app.core.util.formatearDuracion
import org.junit.Assert.assertEquals
import org.junit.Test

class DuracionTest {
    @Test fun `menos de una hora`() = assertEquals("4:12", 252.formatearDuracion())
    @Test fun `mas de una hora`() = assertEquals("1:02:03", 3723.formatearDuracion())
    @Test fun `cero segundos`() = assertEquals("0:00", 0.formatearDuracion())
}
