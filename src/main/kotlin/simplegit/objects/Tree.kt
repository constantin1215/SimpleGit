package simplegit.objects

import simplegit.hash.HashGenerator

class Tree(hash : String) : CompositeElement(hash) {
    private val elements = mutableListOf<CompositeElement>()

    fun addElement(blob: CompositeElement) = elements.add(blob)

    fun removeElement(blob: CompositeElement) = elements.remove(blob)

    fun recalculateHash() {
        hash = HashGenerator.generateSHA1(elements.map { it.hash }.joinToString(""))
    }

    override fun toString(): String {
        return "Tree($hash, elements=$elements)"
    }
}