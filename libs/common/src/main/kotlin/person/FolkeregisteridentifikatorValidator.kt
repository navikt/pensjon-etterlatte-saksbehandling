package no.nav.etterlatte.libs.common.person

class FolkeregisteridentifikatorValidator {
    companion object {
        private val controlDigits1 = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
        private val controlDigits2 = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

        /**
         * Validates foedselsnummer.
         *
         * It consists of 11 digits.
         * It does not have a BigInteger value of 0.
         * Control digits are valid.
         */
        fun isValid(value: String): Boolean {
            return !Regex("[0]{11}").matches(value) &&
                value.length == 11 &&
                value.toBigIntegerOrNull() != null &&
                validateControlDigits(value)
        }

        /**
         * Validate control digits.
         */
        private fun validateControlDigits(value: String): Boolean {
            val ks1 = Character.getNumericValue(value[9])

            val c1 = mod(controlDigits1, value)
            if (c1 == 10 || c1 != ks1) {
                return false
            }

            val c2 = mod(controlDigits2, value)
            if (c2 == 10 || c2 != Character.getNumericValue(value[10])) {
                return false
            }

            return true
        }

        /**
         * Control Digits 1:
         *  k1 = 11 - ((3 × d1 + 7 × d2 + 6 × m1 + 1 × m2 + 8 × å1 + 9 × å2 + 4 × i1 + 5 × i2 + 2 × i3) mod 11)
         *
         * Control Digits 2
         *  k2 = 11 - ((5 × d1 + 4 × d2 + 3 × m1 + 2 × m2 + 7 × å1 + 6 × å2 + 5 × i1 + 4 × i2 + 3 × i3 + 2 × k1) mod 11)
         */
        private fun mod(arr: IntArray, value: String): Int {
            val sum = arr.withIndex()
                .sumOf { (i, m) -> m * Character.getNumericValue(value[i]) }

            val result = 11 - (sum % 11)
            return if (result == 11) 0 else result
        }
    }
}