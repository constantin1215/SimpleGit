package simplegit.objects

class Blob(hash : String, private val content : String) : CompositeElement(hash) {
    override fun toString(): String {
        return "Blob($hash, $content)"
    }
}
