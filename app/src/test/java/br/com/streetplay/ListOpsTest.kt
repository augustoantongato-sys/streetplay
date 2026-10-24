package br.com.streetplay

import br.com.streetplay.core.common.moved
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ListOpsTest {
    private val list = listOf("a", "b", "c")

    @Test fun movesForward() = assertEquals(listOf("b", "a", "c"), list.moved(0, 1))
    @Test fun movesBackward() = assertEquals(listOf("a", "c", "b"), list.moved(2, -1))
    @Test fun outOfRangeReturnsSameInstance() {
        assertSame(list, list.moved(0, -1))
        assertSame(list, list.moved(2, 1))
        assertSame(list, list.moved(5, 1))
    }
    @Test fun zeroDeltaIsNoOp() = assertSame(list, list.moved(1, 0))
}
