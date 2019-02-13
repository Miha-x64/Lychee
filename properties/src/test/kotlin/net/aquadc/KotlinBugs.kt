package net.aquadc

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.diff.DiffChangeListener
import net.aquadc.properties.diff.addUnconfinedChangeListener
import net.aquadc.properties.diff.concurrentDiffPropertyOf
import net.aquadc.properties.getValue
import net.aquadc.properties.propertyOf
import net.aquadc.properties.setValue
import org.junit.Assert
import org.junit.Test

class KotlinBugs {

    @Test fun `subscribe multi unconfined single-thread`() = multiArityListener(0)
    @Test fun `subscribe multi single-thread`() = multiArityListener(1)
    @Test fun `subscribe multi unconfined multi-thread`() = multiArityListener(2)

    private fun multiArityListener(mode: Int) {
        val prop = propertyOf(0, mode == 2)

        var called2 = false

        val obj = object : () -> Unit, (Any?) -> Unit, ChangeListener<Int> {

            override fun invoke() {
                error("unexpected")
            }

            override fun invoke(p1: Any?) {
                error("unexpected")
            }

            override fun invoke(old: Int, new: Int) {
                called2 = true
            }
        }

        val listener: ChangeListener<Int> = obj

        if (mode == 1) prop.addChangeListener(listener) else prop.addUnconfinedChangeListener(listener)
        prop.value = 1
        Assert.assertTrue(called2)

        if (mode == 1) prop.addChangeListener(listener) else prop.addUnconfinedChangeListener(listener)
    }

    @Test fun `subscribe both on diff`() = BothCh().let { multiArityDiffListener(it, it.called2, it, it.called3) }
    @Test fun `subscribe both+unary on diff`() = BothChUnary().let { multiArityDiffListener(it, it.called2, it, it.called3) }
    private fun multiArityDiffListener(
            listener: ChangeListener<Int>, called: MutableProperty<Boolean>,
            diffListener: DiffChangeListener<Int, Int>, calledDiff: MutableProperty<Boolean>) {
        val prop = concurrentDiffPropertyOf<Int, Int>(0)

        var called2 by called
        var called3 by calledDiff

        // should call only binary function
        prop.addUnconfinedChangeListener(listener)
        prop.casValue(0, 1, 1)
        Assert.assertTrue(called2)
        Assert.assertFalse(called3)

        called2 = false
        prop.removeChangeListener(listener)

        // should call only ternary one
        prop.addUnconfinedChangeListener(diffListener)
        prop.casValue(1, 2, 1)
        Assert.assertFalse(called2)
        Assert.assertTrue(called3)

        called3 = false
        prop.removeChangeListener(diffListener)

        prop.addUnconfinedChangeListener(listener)
        prop.addUnconfinedChangeListener(diffListener)
        prop.casValue(2, 3, 1)
        Assert.assertTrue(called2)
        Assert.assertTrue(called3)

        called2 = false
        called3 = false
        prop.removeChangeListener(listener)
        prop.removeChangeListener(diffListener)

        prop.addUnconfinedChangeListener(diffListener)
        prop.addUnconfinedChangeListener(listener)
        prop.casValue(3, 4, 1)
        Assert.assertTrue(called2)
        Assert.assertTrue(called3)
    }

    private open class BothCh : ChangeListener<Int>, DiffChangeListener<Int, Int> {

        val called3 = propertyOf(false)

        override fun invoke(old: Int, new: Int, diff: Int) {
            called3.value = true
        }

        val called2 = propertyOf(false)

        override fun invoke(old: Int, new: Int) {
            called2.value = true
        }

    }
    private class BothChUnary : BothCh(), (Any?) -> Unit {

        override fun invoke(p1: Any?) {
            error("unexpected")
        }

    }

}
