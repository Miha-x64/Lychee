package net.aquadc.persistence.extended.tokens

import net.aquadc.persistence.tokens.Index
import net.aquadc.persistence.tokens.Token
import net.aquadc.persistence.tokens.tokens
import net.aquadc.persistence.tokens.yieldDictionary
import net.aquadc.persistence.tokens.yieldInt
import net.aquadc.persistence.tokens.yieldSequence
import net.aquadc.persistence.tokens.yieldString
import org.junit.Assert.*
import org.junit.Test

class TokenStreamTest {

    @Test fun generate() {
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
        stream.skip()

        assertEquals(Token.BeginDictionary, stream.peek())
        assertEquals(listOf(Index(3)), stream.path)
        stream.skip()

        assertEquals(Token.BeginSequence, stream.peek())
        assertEquals(listOf(Index(4)), stream.path)
        stream.skip()

        assertEquals(Token.BeginSequence, stream.peek())
        assertEquals(listOf(Index(5)), stream.path)
        assertEquals(Token.BeginSequence, stream.poll())

        assertEquals(Token.Str, stream.peek())
        assertEquals(listOf(Index(5), Index(0)), stream.path)
        assertEquals("goodbye", stream.poll())

        assertEquals(Token.EndSequence, stream.peek())
        assertEquals(listOf(Index(5), Index(1)), stream.path)
        stream.skip()

        assertEquals(Token.Str, stream.peek())
        assertEquals(listOf(Index(6)), stream.path)
        assertEquals("sub", stream.poll())

        assertEquals(Token.EndSequence, stream.peek())
        assertEquals(listOf(Index(7)), stream.path)
        assertEquals(Token.EndSequence, stream.poll())

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
