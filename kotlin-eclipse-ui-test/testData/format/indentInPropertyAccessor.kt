class C {
    var someProp: Int
        get() = throw UnsupportedOperationException()
        set(value) {
        println("10")
        }
}