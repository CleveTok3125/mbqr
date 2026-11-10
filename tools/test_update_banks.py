from __future__ import annotations

import pathlib
import sys
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from update_banks import kotlin_string, render, validate_bank


class KotlinStringTest(unittest.TestCase):
    def test_escapes_kotlin_syntax(self) -> None:
        self.assertEqual(
            'a\\"b\\\\c\\$d\\n\\t\\u0001',
            kotlin_string('a"b\\c$d\n\t\x01'),
        )

    def test_escapes_unicode_line_separator(self) -> None:
        self.assertEqual("a\\u2028b", kotlin_string("a\u2028b"))

    def test_escapes_quotes_in_generated_data(self) -> None:
        bank = validate_bank(
            {
                "bin": "970407",
                "code": "TCB",
                "shortName": 'Tech"combank',
                "transferSupported": 1,
            }
        )

        rendered = render([bank])

        self.assertIn('"Tech\\"combank"', rendered)
        self.assertNotIn('Tech"combank', rendered)


class ValidateBankTest(unittest.TestCase):
    def test_keeps_the_api_prefix_exactly(self) -> None:
        bank = validate_bank(
            {
                "bin": "971025",
                "code": "momo",
                "shortName": "MoMo",
                "transferSupported": 1,
            }
        )

        self.assertEqual("momo", bank["prefix"])
        self.assertTrue(bank["transferSupported"])

    def test_rejects_invalid_bin(self) -> None:
        with self.assertRaises(ValueError):
            validate_bank(
                {
                    "bin": "97040",
                    "code": "TCB",
                    "shortName": "Techcombank",
                    "transferSupported": 1,
                }
            )

    def test_rejects_control_characters(self) -> None:
        with self.assertRaises(ValueError):
            validate_bank(
                {
                    "bin": "970407",
                    "code": "TCB\ninjected",
                    "shortName": "Techcombank",
                    "transferSupported": 1,
                }
            )

    def test_rejects_bidi_control(self) -> None:
        with self.assertRaises(ValueError):
            validate_bank(
                {
                    "bin": "970407",
                    "code": "TCB\u202e",
                    "shortName": "Techcombank",
                    "transferSupported": 1,
                }
            )

    def test_rejects_invalid_transfer_support(self) -> None:
        with self.assertRaises(ValueError):
            validate_bank(
                {
                    "bin": "970407",
                    "code": "TCB",
                    "shortName": "Techcombank",
                    "transferSupported": 2,
                }
            )


if __name__ == "__main__":
    unittest.main()
