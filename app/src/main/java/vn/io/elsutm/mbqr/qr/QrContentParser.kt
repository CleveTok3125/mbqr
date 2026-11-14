package vn.io.elsutm.mbqr.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.client.result.AddressBookParsedResult
import com.google.zxing.client.result.EmailAddressParsedResult
import com.google.zxing.client.result.GeoParsedResult
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.ResultParser
import com.google.zxing.client.result.SMSParsedResult
import com.google.zxing.client.result.TelParsedResult
import com.google.zxing.client.result.URIParsedResult
import com.google.zxing.client.result.WifiParsedResult
import vn.io.elsutm.mbqr.handoff.UrlDetector
import vn.io.elsutm.mbqr.vietqr.VietQrParseResult
import vn.io.elsutm.mbqr.vietqr.VietQrParser

/**
 * Detects QR content from specific, structured formats to generic text.
 *
 * VietQR is checked first because it has the strictest structure and carries
 * transfer data. Prefix rules are ordered so specific schemes such as `mailto:`
 * and `sms:` are never mistaken for broader URL or text content.
 */
object QrContentParser {

    private enum class Format {
        Wifi,
        Contact,
        Email,
        Phone,
        Sms,
        Geo,
        Isbn,
        Url,
    }

    private data class PrefixRule(
        val format: Format,
        val prefixes: List<String>,
    )

    // A single ordered table keeps related schemes grouped and makes the
    // specificity order explicit.
    private val RULES = listOf(
        PrefixRule(Format.Wifi, listOf("WIFI:")),
        PrefixRule(Format.Contact, listOf("BEGIN:VCARD", "MECARD:", "BIZCARD:")),
        PrefixRule(Format.Email, listOf("mailto:", "MATMSG:", "SMTP:")),
        PrefixRule(Format.Phone, listOf("tel:")),
        PrefixRule(Format.Sms, listOf("sms:", "smsto:", "mms:", "mmsto:")),
        PrefixRule(Format.Geo, listOf("geo:")),
        PrefixRule(Format.Isbn, listOf("ISBN:")),
        PrefixRule(Format.Url, listOf("http://", "https://", "URLTO:")),
    )

    /**
     * Parses [raw] into transfer data, displayable details, or plain text.
     *
     * Surrounding whitespace and invisible markers are removed while line breaks
     * remain available to formats such as vCard. When [sanitize] is true, the
     * output keeps its structure but invisible characters are stripped from
     * every displayed value; parsing still runs on the original payload so TLV
     * lengths and the CRC stay valid.
     */
    fun parse(raw: String, sanitize: Boolean = true): QrContent {
        val content = parseUnsanitized(raw)
        return if (sanitize) content.sanitized() else content
    }

    private fun parseUnsanitized(raw: String): QrContent {
        val normalized = normalized(raw)
        if (normalized.isEmpty()) return QrContent.Text(normalized)

        when (val result = VietQrParser.parse(normalized)) {
            is VietQrParseResult.VietQr -> {
                return QrContent.VietQr(
                    raw = normalized,
                    data = result.data,
                    crcValid = result.crcValid,
                )
            }

            VietQrParseResult.NotVietQr -> Unit
        }

        val rule = detectRule(normalized) ?: return QrContent.Text(normalized)
        return parseRule(rule, normalized)
    }

    private fun QrContent.sanitized(): QrContent = when (this) {
        is QrContent.VietQr -> {
            val clean = data.copy(
                bankBin = TextSanitizer.sanitize(data.bankBin),
                account = TextSanitizer.sanitize(data.account),
                merchantName = data.merchantName.sanitizedOrNull(),
                content = data.content.sanitizedOrNull(),
            )
            if (clean.bankBin.length != 6 || clean.account.isBlank()) {
                QrContent.Text(TextSanitizer.sanitize(raw))
            } else {
                copy(raw = TextSanitizer.sanitize(raw), data = clean)
            }
        }

        is QrContent.Details -> {
            val cleanFields = fields.mapNotNull { field ->
                val value = TextSanitizer.sanitize(field.value)
                if (value.isBlank()) {
                    null
                } else {
                    field.copy(label = TextSanitizer.sanitize(field.label), value = value)
                }
            }
            if (cleanFields.isEmpty()) {
                QrContent.Text(TextSanitizer.sanitize(raw))
            } else {
                copy(
                    raw = TextSanitizer.sanitize(raw),
                    title = TextSanitizer.sanitize(title),
                    fields = cleanFields,
                )
            }
        }

        is QrContent.Text -> copy(raw = TextSanitizer.sanitize(raw))
    }

    private fun String?.sanitizedOrNull(): String? =
        this?.let(TextSanitizer::sanitize)?.takeIf { it.isNotBlank() }

    /** Returns the payload with surrounding whitespace and invisible markers removed. */
    fun normalized(raw: String): String =
        raw.filter { it != '\uFEFF' && it != '\u200B' }.trim()

    private fun detectRule(raw: String): PrefixRule? =
        RULES.firstOrNull { rule ->
            rule.prefixes.any { prefix -> raw.startsWith(prefix, ignoreCase = true) }
        }

    private fun parseRule(rule: PrefixRule, raw: String): QrContent {
        val canonical = canonicalize(raw, rule.prefixes)
        return when (rule.format) {
            Format.Wifi -> parseWifi(canonical)
            Format.Contact -> parseContact(canonical)
            Format.Email -> parseEmail(canonical)
            Format.Phone -> parsePhone(canonical)
            Format.Sms -> parseSms(canonical)
            Format.Geo -> parseGeo(canonical)
            Format.Isbn -> parseIsbn(canonical)
            Format.Url -> parseUrl(raw, canonical)
        }
    }

    /**
     * Prefix matching is case-insensitive; canonicalizing the matched prefix
     * lets ZXing parsers keep their expected spelling.
     */
    private fun canonicalize(raw: String, prefixes: List<String>): String {
        val prefix = prefixes.firstOrNull { raw.startsWith(it, ignoreCase = true) } ?: return raw
        return prefix + raw.substring(prefix.length)
    }

    private fun parseWifi(raw: String): QrContent {
        val parsed = parseWithZxing(raw) as? WifiParsedResult ?: return QrContent.Text(raw)
        val ssid = parsed.ssid?.takeIf { it.isNotBlank() } ?: return QrContent.Text(raw)

        val fields = buildList {
            add(QrField("Tên mạng", ssid, QrFieldSensitivity.Personal))
            parsed.networkEncryption
                ?.takeIf { it.isNotBlank() }
                ?.let { add(QrField("Bảo mật", wifiSecurityLabel(it))) }
            parsed.password
                ?.takeIf { it.isNotBlank() }
                ?.let { add(QrField("Mật khẩu", it, QrFieldSensitivity.Direct)) }
            if (parsed.isHidden) add(QrField("Mạng ẩn", "Có"))
            parsed.identity
                ?.takeIf { it.isNotBlank() }
                ?.let { add(QrField("Danh tính", it, QrFieldSensitivity.Direct)) }
            parsed.anonymousIdentity
                ?.takeIf { it.isNotBlank() }
                ?.let { add(QrField("Danh tính ẩn", it, QrFieldSensitivity.Direct)) }
            parsed.eapMethod?.takeIf { it.isNotBlank() }?.let { add(QrField("EAP", it)) }
            parsed.phase2Method?.takeIf { it.isNotBlank() }?.let { add(QrField("Phase 2", it)) }
        }

        return QrContent.Details(raw = raw, title = "Wi-Fi", fields = fields)
    }

    private fun parseContact(raw: String): QrContent {
        val parsed = parseWithZxing(raw) as? AddressBookParsedResult ?: return QrContent.Text(raw)
        val fields = buildList {
            addAll(parsed.names, "Tên", QrFieldSensitivity.Personal)
            addAll(parsed.nicknames, "Biệt danh", QrFieldSensitivity.Personal)
            addAll(parsed.phoneNumbers, "Điện thoại", QrFieldSensitivity.Direct)
            addAll(parsed.emails, "Email", QrFieldSensitivity.Direct)
            addAll(parsed.addresses, "Địa chỉ", QrFieldSensitivity.Direct)
            add("Tổ chức", parsed.org, QrFieldSensitivity.Personal)
            add("Chức danh", parsed.title, QrFieldSensitivity.Personal)
            addAll(parsed.getURLs(), "URL", QrFieldSensitivity.Personal)
            add("Ghi chú", parsed.note, QrFieldSensitivity.Direct)
            add("Sinh nhật", parsed.birthday, QrFieldSensitivity.Direct)
            add("Nhắn tin", parsed.instantMessenger, QrFieldSensitivity.Direct)
            addAll(parsed.geo, "Vị trí", QrFieldSensitivity.Direct)
        }

        return detailsOrText(raw, "Danh bạ", fields)
    }

    private fun parseEmail(raw: String): QrContent {
        val parsed = parseWithZxing(raw) as? EmailAddressParsedResult ?: return QrContent.Text(raw)
        val fields = buildList {
            addAll(parsed.tos, "Đến", QrFieldSensitivity.Direct)
            addAll(parsed.getCCs(), "CC", QrFieldSensitivity.Direct)
            addAll(parsed.getBCCs(), "BCC", QrFieldSensitivity.Direct)
            add("Tiêu đề", parsed.subject, QrFieldSensitivity.Personal)
            add("Nội dung", parsed.body, QrFieldSensitivity.Direct)
        }

        return detailsOrText(raw, "Email", fields)
    }

    private fun parsePhone(raw: String): QrContent {
        val parsed = parseWithZxing(raw) as? TelParsedResult ?: return QrContent.Text(raw)
        val number = parsed.number?.takeIf { it.isNotBlank() } ?: return QrContent.Text(raw)
        return QrContent.Details(
            raw = raw,
            title = "Điện thoại",
            fields = buildList {
                add(QrField("Số điện thoại", number, QrFieldSensitivity.Direct))
                add("Tên", parsed.title, QrFieldSensitivity.Personal)
            },
        )
    }

    private fun parseSms(raw: String): QrContent {
        val parsed = parseWithZxing(raw) as? SMSParsedResult ?: return QrContent.Text(raw)
        val fields = buildList {
            addAll(parsed.numbers, "Đến", QrFieldSensitivity.Direct)
            add("Tiêu đề", parsed.subject, QrFieldSensitivity.Personal)
            add("Nội dung", parsed.body, QrFieldSensitivity.Direct)
        }

        return detailsOrText(raw, "Tin nhắn", fields)
    }

    private fun parseGeo(raw: String): QrContent {
        val parsed = parseWithZxing(raw) as? GeoParsedResult ?: return QrContent.Text(raw)
        val fields = buildList {
            add(QrField("Vĩ độ", parsed.latitude.toString(), QrFieldSensitivity.Direct))
            add(QrField("Kinh độ", parsed.longitude.toString(), QrFieldSensitivity.Direct))
            if (parsed.altitude > 0.0) {
                add(QrField("Độ cao", "${parsed.altitude} m", QrFieldSensitivity.Personal))
            }
            add("Truy vấn", parsed.query, QrFieldSensitivity.Personal)
        }

        return QrContent.Details(raw = raw, title = "Vị trí", fields = fields)
    }

    private fun parseIsbn(raw: String): QrContent {
        val value = raw.substringAfter(':', missingDelimiterValue = "")
            .trim()
            .replace("-", "")
            .replace(" ", "")
        if (!value.matches(ISBN_PATTERN)) return QrContent.Text(raw)
        return QrContent.Details(
            raw = raw,
            title = "ISBN",
            fields = listOf(QrField("ISBN", value)),
        )
    }

    private fun parseUrl(displayRaw: String, canonical: String): QrContent {
        // URLTO bookmarks are `URLTO:[title]:[url]`; ZXing handles that form.
        val zxing = parseWithZxing(canonical) as? URIParsedResult
        val zxingUrl = zxing?.getURI()?.takeIf { UrlDetector.isHttpUrl(it) }
        val title = zxing?.getTitle()
            ?.takeIf { zxingUrl != null && it.isNotBlank() }
        val stripped = if (canonical.startsWith("URLTO:", ignoreCase = true)) {
            canonical.substring("URLTO:".length).trim()
        } else {
            canonical.trim()
        }
        val candidate = zxingUrl
            ?: stripped.takeIf { UrlDetector.isHttpUrl(it) }
            ?: return QrContent.Text(displayRaw)

        return QrContent.Details(
            raw = candidate,
            title = "Liên kết",
            fields = buildList {
                title?.let { add(QrField("Tiêu đề", it, QrFieldSensitivity.Personal)) }
                add(QrField("Liên kết", candidate, QrFieldSensitivity.Personal))
            },
        )
    }

    private fun parseWithZxing(raw: String): ParsedResult? = runCatching {
        ResultParser.parseResult(Result(raw, null, null, BarcodeFormat.QR_CODE))
    }.getOrNull()

    private fun detailsOrText(raw: String, title: String, fields: List<QrField>): QrContent =
        if (fields.isEmpty()) QrContent.Text(raw) else QrContent.Details(raw, title, fields)

    private val ISBN_PATTERN = Regex("""(?:\d{9}[\dXx]|\d{13})""")

    private fun wifiSecurityLabel(value: String): String = when {
        value.equals("nopass", ignoreCase = true) -> "Không mật khẩu"
        value.equals("WEP", ignoreCase = true) -> "WEP"
        value.equals("WPA", ignoreCase = true) -> "WPA/WPA2"
        value.equals("WPA2", ignoreCase = true) -> "WPA2"
        value.equals("WPA3", ignoreCase = true) || value.equals("SAE", ignoreCase = true) -> "WPA3"
        else -> value
    }

    private fun MutableList<QrField>.add(
        label: String,
        value: String?,
        sensitivity: QrFieldSensitivity = QrFieldSensitivity.None,
    ) {
        value?.takeIf { it.isNotBlank() }?.let { add(QrField(label, it, sensitivity)) }
    }

    private fun MutableList<QrField>.addAll(
        values: Array<String>?,
        label: String,
        sensitivity: QrFieldSensitivity = QrFieldSensitivity.None,
    ) {
        values?.forEach { value -> add(label, value, sensitivity) }
    }
}
