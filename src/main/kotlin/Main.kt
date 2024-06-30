interface Tree<out T, out N : Tree<T, N>> {
    val self: N
    val value: T?
    val children: Map<String, N>
}

operator fun <T, N : Tree<T, N>> N.get(path: List<String>): N? = when {
    path.isEmpty() -> this
    path.size == 1 -> children[path.first()]
    else -> children[path.first()]?.get(path.drop(1))
}

interface MutableTree<T, N : MutableTree<T, N>> : Tree<T, N> {
    override var value: T?
    fun createOrGet(token: String): N
    operator fun set(token: String, child: N)
}

fun <T, N : MutableTree<T, N>> N.createOrGet(path: List<String>): N = when {
    path.isEmpty() -> self
    path.size == 1 -> createOrGet(path.first())
    else -> createOrGet(path.first()).createOrGet(path.drop(1))
}

operator fun <T, N : MutableTree<T, N>> N.set(path: List<String>, child: N) {
    when {
        path.isEmpty() -> error("Invalid path")
        path.size == 1 -> set(path.first(), child)
        else -> createOrGet(path.first()).set(path.drop(1), child)
    }
}

fun <T, N : MutableTree<T, N>> N.setValue(path: List<String>, newValue: T) {
    when {
        path.isEmpty() -> this.value = newValue  // Установка значения для корня
        path.size == 1 -> createOrGet(path).value = newValue
        else -> createOrGet(path.first()).setValue(path.drop(1), newValue)
    }
}

class SimpleTree<T>(
    override var value: T?,
    override val children: MutableMap<String, SimpleTree<T>> = mutableMapOf(),
) : MutableTree<T, SimpleTree<T>> {

    override val self: SimpleTree<T> get() = this

    override fun createOrGet(token: String): SimpleTree<T> {
        return children.getOrPut(token) { SimpleTree(null) }
    }

    override fun set(token: String, child: SimpleTree<T>) {
        children[token] = child
    }
}

fun <T, N : Tree<T, N>> N.asSequence(): Sequence<Pair<List<String>, T>> = sequence {
    value?.let { yield(emptyList<String>() to it) }
    for ((token, subtree) in children) {
        for ((subPath, subValue) in subtree.asSequence()) {
            yield(listOf(token) + subPath to subValue)
        }
    }
}

fun <T, N : MutableTree<T, N>> N.fillFrom(other: Tree<T, *>) {
    other.asSequence().forEach { (path, value) ->
        setValue(path, value)
    }
}

fun <T, N : MutableTree<T, N>> N.addBranch(path: List<String>, branch: Tree<T, *>) {
    createOrGet(path).fillFrom(branch)
}

fun main() {
    val root = SimpleTree<String>("Root")

    root.setValue(listOf("child1"), "Child 1")
    root.setValue(listOf("child1", "grandchild1"), "Grandchild 1")
    root.setValue(listOf("child2"), "Child 2")

    println("Дерево после добавления значений:")
    root.asSequence().forEach { (path, value) ->
        println("Путь: ${path.joinToString("/")}, Значение: $value")
    }

    val newBranch = SimpleTree<String>("New Branch")
    newBranch.setValue(listOf("subchild1"), "Subchild 1")

    root.addBranch(listOf("child2"), newBranch)

    println("\nДерево после добавления новой ветки:")
    root.asSequence().forEach { (path, value) ->
        println("Путь: ${path.joinToString("/")}, Значение: $value")
    }
}