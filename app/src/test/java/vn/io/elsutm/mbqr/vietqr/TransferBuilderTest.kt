package vn.io.elsutm.mbqr.vietqr

import org.junit.Assert.assertEquals
import org.junit.Test

class TransferBuilderTest {

    @Test
    fun `builds transfer string without amount and name`() {
        val data = data(amount = null, name = null)

        assertEquals("STK 0941196908 - MB Bank", TransferBuilder.build(data, "MB Bank"))
    }

    @Test
    fun `builds transfer string with amount`() {
        val data = data(amount = 10_500)

        assertEquals("STK 0941196908 10.5 - MB Bank", TransferBuilder.build(data, "MB Bank"))
    }

    @Test
    fun `appends recipient name`() {
        val data = data(amount = 1_000, name = "NGUYEN VAN A")

        assertEquals(
            "STK 0941196908 1.0 - MB Bank NGUYEN VAN A",
            TransferBuilder.build(data, "MB Bank"),
        )
    }

    @Test
    fun `collapses whitespace in recipient name`() {
        val data = data(amount = null, name = "  NGUYEN \n VAN   A ")

        assertEquals(
            "STK 0941196908 - MB Bank NGUYEN VAN A",
            TransferBuilder.build(data, "MB Bank"),
        )
    }

    @Test
    fun `keeps account as parsed`() {
        val data = VietQrData(
            bankBin = "970422",
            account = "0941196908",
            amountVnd = null,
            recipientName = null,
            content = null,
        )

        assertEquals("STK 0941196908 - MB Bank", TransferBuilder.build(data, "MB Bank"))
    }

    private fun data(amount: Long? = null, name: String? = null) = VietQrData(
        bankBin = "970422",
        account = "0941196908",
        amountVnd = amount,
        recipientName = name,
        content = null,
    )
}
