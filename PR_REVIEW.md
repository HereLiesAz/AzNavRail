The optimization is a great idea and brings a nice performance improvement! However, there is a bug in how `utf8ByteLength()` handles malformed surrogate pairs.

If a string ends with an unpaired high surrogate, the loop increments `i` without checking `i + 1 < length` before doing `i++ // Skip low surrogate`, which will cause an `IndexOutOfBoundsException` on the next iteration or when accessing the string later.

Additionally, when Java or Kotlin's `.toByteArray(Charsets.UTF_8)` encounters malformed surrogate sequences (unpaired high or low surrogates), it replaces them with the standard replacement character `?`, which is 1 byte in UTF-8. The current logic incorrectly adds 4 bytes for an unpaired high surrogate and 3 bytes for an unpaired low surrogate (hitting the `else` branch).

We need to update the `Character.isHighSurrogate(ch)` block to verify `i + 1 < length && Character.isLowSurrogate(this[i + 1])` before adding 4 bytes and skipping the next character. We also need to add fallback branches to add 1 byte for unpaired high or low surrogates. I will open a follow-up PR with these corrections.
