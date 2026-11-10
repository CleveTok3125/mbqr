package vn.io.elsutm.mbqr.vietqr

import org.junit.Assert.assertEquals
import org.junit.Test

class TransferBuilderTest {

    @Test
    fun `builds transfer string with account only`() {
        val data = data(amount = null, merchantName = null)

        assertEquals("MB 0941196908", TransferBuilder.build(data, "MB"))
    }

    @Test
    fun `builds transfer string with amount`() {
        val data = data(amount = 10_500)

        assertEquals("MB 0941196908 10.5", TransferBuilder.build(data, "MB"))
    }

    @Test
    fun `appends merchant name`() {
        val data = data(amount = 1_000, merchantName = "NGUYEN VAN A")

        assertEquals(
            "MB 0941196908 1.0 NGUYEN VAN A",
            TransferBuilder.build(data, "MB"),
        )
    }

    @Test
    fun `appends merchant name without amount`() {
        val data = data(amount = null, merchantName = "NGUYEN VAN A")

        assertEquals("MB 0941196908 NGUYEN VAN A", TransferBuilder.build(data, "MB"))
    }

    @Test
    fun `collapses whitespace in merchant name`() {
        val data = data(amount = null, merchantName = "  NGUYEN \n VAN   A ")

        assertEquals("MB 0941196908 NGUYEN VAN A", TransferBuilder.build(data, "MB"))
    }

    @Test
    fun `builds an interbank transfer with the directory prefix`() {
        val data = data(
            bankBin = "970407",
            account = "00000000000001",
            amount = 1_000_000,
            merchantName = "TCB",
        )

        assertEquals(
            "TCB 00000000000001 1000.0 TCB",
            TransferBuilder.build(data, "TCB"),
        )
    }

    @Test
    fun `does not append content or transfer metadata`() {
        val data = data(
            bankBin = "970407",
            account = "00000000000001",
            amount = 1_000_000,
            merchantName = "TCB",
            content = "test",
        )

        assertEquals(
            "TCB 00000000000001 1000.0 TCB",
            TransferBuilder.build(data, "TCB"),
        )
    }

    private fun data(
        bankBin: String = "970422",
        account: String = "0941196908",
        amount: Long? = null,
        merchantName: String? = null,
        content: String? = null,
    ) = VietQrData(
        bankBin = bankBin,
        account = account,
        amountVnd = amount,
        merchantName = merchantName,
        content = content,
    )
}
