class Driver : Human {
    constructor(_name: String, _surname: String, _second: String, _age: Int) : super(_name, _surname, _second, _age) {
        println("We created the Driver object with name: $name")
    }

    override fun move() {
        x += speed
    }
}