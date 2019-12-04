package net.aquadc.persistence.tokens

import org.junit.Assert.*
import org.junit.Test
import java.util.*
import kotlin.NoSuchElementException

class TokenStreamTest {

    @Test fun generate() {
        val delegate = tokens {
            yieldSequence {
                yieldString { "" }
                yieldDictionary {
                    yieldString { fail(); "" }; yieldBlob { byteArrayOf(1, 0, 0, 5, 0, 0) }
                }
            }
        }
        delegate.poll(Token.BeginSequence)
        delegate.poll(Token.Str)
        val stream = tokens {
            yieldSequence {
                yieldInt { 1 }
                yieldString { "" }
                yieldDictionary {
                    yieldString { "a" }; yieldString { "cool" }
                    yieldString { "b" }; yieldSequence {
                        yieldInt { 1 }
                    }
                }
                yieldDictionary {
                    yieldString { fail(); "unexpected" }
                    fail()
                }
                yieldSequence {
                    yieldString { fail(); "unexpected" }
                    fail()
                }
                yieldAll(tokens {})
                yieldSequence {
                    yieldString { "goodbye" }
                }
                yieldAll(tokens {})
                yieldAll(tokens {})
                yieldAll(tokens {})
                yieldAll(tokens { yieldString { "sub" } })
                yieldBracketSequence(delegate)
            }
        }

        assertEquals(Token.BeginSequence, stream.peek())
        assertEquals(emptyList<Any>(), stream.path)
        assertEquals(Token.BeginSequence, stream.poll())

        assertEquals(Token.I32, stream.peek())
        assertEquals(listOf(Index(0)), stream.path)
        assertEquals(1, stream.poll())

        assertEquals(Token.Str, stream.peek())
        assertEquals(listOf(Index(1)), stream.path)
        assertEquals("", stream.poll())

        assertEquals(Token.BeginDictionary, stream.peek())
        assertEquals(listOf(Index(2)), stream.path)
        assertEquals(Token.BeginDictionary, stream.poll())

        assertEquals(Token.Str, stream.peek())
        assertEquals(listOf(Index(2), null), stream.path)
        assertEquals("a", stream.poll())
        assertEquals(Token.Str, stream.peek())
        assertEquals(listOf(Index(2), "a"), stream.path)
        assertEquals("cool", stream.poll())

        assertEquals(Token.Str, stream.peek())
        assertEquals(listOf(Index(2), "a"), stream.path)
        assertEquals("b", stream.poll())
        assertEquals(Token.BeginSequence, stream.peek())
        assertEquals(listOf(Index(2), "b"), stream.path)
        assertEquals(Token.BeginSequence, stream.poll())
        assertEquals(Token.I32, stream.peek())
        assertEquals(listOf(Index(2), "b", Index(0)), stream.path)
        assertEquals(1, stream.poll())
        assertEquals(Token.EndSequence, stream.peek())
        assertEquals(listOf(Index(2), "b", Index(1)), stream.path)
        assertEquals(Token.EndSequence, stream.poll())
        assertEquals(Token.EndDictionary, stream.peek())
        assertEquals(listOf(Index(2), "b"), stream.path)
        stream.skipValue()

        assertEquals(Token.BeginDictionary, stream.peek())
        assertEquals(listOf(Index(3)), stream.path)
        stream.skipValue()

        assertEquals(Token.BeginSequence, stream.peek())
        assertEquals(listOf(Index(4)), stream.path)
        stream.skipValue()

        assertEquals(Token.BeginSequence, stream.peek())
        assertEquals(listOf(Index(5)), stream.path)
        assertEquals(Token.BeginSequence, stream.poll())

        assertEquals(Token.Str, stream.peek())
        assertEquals(listOf(Index(5), Index(0)), stream.path)
        assertEquals("goodbye", stream.poll())

        assertEquals(Token.EndSequence, stream.peek())
        assertEquals(listOf(Index(5), Index(1)), stream.path)
        stream.skipValue()

        assertEquals(Token.Str, stream.peek())
        assertEquals(listOf(Index(6)), stream.path)
        assertEquals("sub", stream.poll())

        assertEquals(listOf(Index(7)), stream.path)
        stream.poll(Token.BeginDictionary)
        stream.skipValue()
        assertEquals(Base64.getEncoder().encodeToString(byteArrayOf(1, 0, 0, 5, 0, 0)), stream.poll(Token.Str))
        assertEquals(Token.EndDictionary, stream.poll())

        assertEquals(Token.EndSequence, stream.peek())
        assertEquals(listOf(Index(8)), stream.path)
        assertEquals(Token.EndSequence, stream.poll())

        assertFalse(stream.hasNext())
        try {
            stream.peek()
            fail()
        } catch (expected: NoSuchElementException) {
        }
    }

    @Test fun coercions() {
        assertEquals(9000, tokens { yieldString { "9000" } }.poll(Token.I32))
    }

}
