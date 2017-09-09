# reactive-properties

Lightweight properties implementation.
Sample:

```kt
val prop = concurrentMutablePropertyOf(1)
val mapped = prop.map { 10 * it }
assertEquals(10, mapped.value)

prop.value = 5
assertEquals(50, mapped.value)


val tru = concurrentMutablePropertyOf(true)
assertEquals(false, (!tru).value)
```
