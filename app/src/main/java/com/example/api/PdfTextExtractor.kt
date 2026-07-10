package com.example.api

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Inflater

object PdfTextExtractor {
    private const val TAG = "PdfTextExtractor"

    fun extractText(context: Context, uri: Uri): String {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Input stream is null")
                return ""
            }
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            val textBuilder = StringBuilder()
            var index = 0
            val size = bytes.size

            while (index < size) {
                // Look for "stream" and "endstream"
                if (index + 6 < size && bytes[index] == 's'.toByte() &&
                    bytes[index + 1] == 't'.toByte() &&
                    bytes[index + 2] == 'r'.toByte() &&
                    bytes[index + 3] == 'e'.toByte() &&
                    bytes[index + 4] == 'a'.toByte() &&
                    bytes[index + 5] == 'm'.toByte()
                ) {
                    index += 6
                    if (index < size && bytes[index] == '\r'.toByte()) index++
                    if (index < size && bytes[index] == '\n'.toByte()) index++

                    var endStreamIndex = -1
                    var searchIndex = index
                    while (searchIndex + 9 < size) {
                        if (bytes[searchIndex] == 'e'.toByte() &&
                            bytes[searchIndex + 1] == 'n'.toByte() &&
                            bytes[searchIndex + 2] == 'd'.toByte() &&
                            bytes[searchIndex + 3] == 's'.toByte() &&
                            bytes[searchIndex + 4] == 't'.toByte() &&
                            bytes[searchIndex + 5] == 'r'.toByte() &&
                            bytes[searchIndex + 6] == 'e'.toByte() &&
                            bytes[searchIndex + 7] == 'a'.toByte() &&
                            bytes[searchIndex + 8] == 'm'.toByte()
                        ) {
                            endStreamIndex = searchIndex
                            break
                        }
                        searchIndex++
                    }

                    if (endStreamIndex != -1) {
                        val streamBytes = bytes.copyOfRange(index, endStreamIndex)
                        
                        // Check if the stream is FlateDecode (usually declared before "stream")
                        var isFlateDecode = false
                        val lookbackStart = (index - 150).coerceAtLeast(0)
                        val lookbackBytes = bytes.copyOfRange(lookbackStart, index - 6)
                        val lookbackString = String(lookbackBytes, Charsets.US_ASCII)
                        if (lookbackString.contains("/FlateDecode") || lookbackString.contains("/Flate")) {
                            isFlateDecode = true
                        }

                        try {
                            val uncompressedBytes = if (isFlateDecode) {
                                decompressFlate(streamBytes)
                            } else {
                                streamBytes
                            }
                            if (uncompressedBytes != null) {
                                extractStringsFromStream(uncompressedBytes, textBuilder)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to decompress or parse stream", e)
                        }
                        
                        index = endStreamIndex + 9
                    } else {
                        index++
                    }
                } else {
                    index++
                }
            }
            
            val result = textBuilder.toString().trim()
            if (result.length > 50) {
                result
            } else {
                extractPlainParentheses(bytes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting PDF text", e)
            ""
        }
    }

    private fun decompressFlate(compressed: ByteArray): ByteArray? {
        return try {
            val inflater = Inflater()
            inflater.setInput(compressed)
            val bos = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            while (!inflater.finished()) {
                val count = inflater.inflate(buf)
                if (count > 0) {
                    bos.write(buf, 0, count)
                } else {
                    break
                }
            }
            inflater.end()
            bos.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun extractStringsFromStream(stream: ByteArray, builder: StringBuilder) {
        var index = 0
        val size = stream.size
        while (index < size) {
            if (stream[index] == '('.toByte()) {
                index++
                val start = index
                var escaped = false
                while (index < size) {
                    val b = stream[index]
                    if (escaped) {
                        escaped = false
                    } else if (b == '\\'.toByte()) {
                        escaped = true
                    } else if (b == ')'.toByte()) {
                        break
                    }
                    index++
                }
                if (index < size && stream[index] == ')'.toByte()) {
                    val strBytes = stream.copyOfRange(start, index)
                    val cleanStr = String(strBytes, Charsets.UTF_8)
                    if (cleanStr.isNotBlank() && cleanStr.all { it.code in 32..126 || it.isWhitespace() }) {
                        builder.append(cleanStr).append(" ")
                    }
                }
            }
            index++
        }
    }

    private fun extractPlainParentheses(bytes: ByteArray): String {
        val builder = StringBuilder()
        var index = 0
        val size = bytes.size
        while (index < size) {
            if (bytes[index] == '('.toByte()) {
                index++
                val start = index
                while (index < size && bytes[index] != ')'.toByte()) {
                    index++
                }
                if (index < size && bytes[index] == ')'.toByte()) {
                    val chunk = bytes.copyOfRange(start, index)
                    val s = String(chunk, Charsets.UTF_8)
                    if (s.length > 3 && s.all { it.code in 32..126 || it.isWhitespace() }) {
                        builder.append(s).append(" ")
                    }
                }
            }
            index++
        }
        return builder.toString().trim()
    }
}
