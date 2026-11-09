package vn.io.elsutm.mbqr.vietqr

import org.junit.Assert.assertEquals
import org.junit.Test

class TransferBuilderTest {

    @Test
    fun `builds transfer string with account only`() {
        val data = data(amount = null, name = null)

        assertEquals("MB 0941196908", TransferBuilder.build(data, "MB"))
    }

    @Test
    fun `builds transfer string with amount`() {
        val data = data(amount = 10_500)

        assertEquals("MB 0941196908 10.5", TransferBuilder.build(data, "MB"))
    }

    @Test
    fun `appends recipient name`() {
        val data = data(amount = 1_000, name = "NGUYEN VAN A")

        assertEquals(
            "MB 0941196908 1.0 NGUYEN VAN A",
            TransferBuilder.build(data, "MB"),
        )
    }

    @Test
    fun `appends recipient name without amount`() {
        val data = data(amount = null, name = "NGUYEN VAN A")

        assertEquals("MB 0941196908 NGUYEN VAN A", TransferBuilder.build(data, "MB"))
    }

    @Test
    fun `collapses whitespace in recipient name`() {
        val data = data(amount = null, name = "  NGUYEN \n VAN   A ")

        assertEquals("MB 0941196908 NGUYEN VAN A", TransferBuilder.build(data, "MB"))
    }

    private fun data(amount: Long? = null, name: String? = null) = VietQrData(
        bankBin = "970422",
        account = "0941196908",
        amountVnd = amount,
        recipientName = name,
        content = null,
    )
}
