import kotlin.random.Random

open class Human : Movable {
    var name: String = ""
    var surname: String = ""
    var second_name: String = ""
    var age: Int = -1

    override var speed: Int = 1

    override var x: Int = 0
    override var y: Int = 0

    constructor(_name: String, _surname: String, _second: String, _age: Int){
        name = _name
        surname = _surname
        second_name = _second
        age = _age
        speed = Random.nextInt(6)
        println("We created the Human object with name: $name")
    }

    override fun move() {
        val direction = Random.nextInt(4)
        when (direction) {
            0 -> y -= speed
            1 -> x -= speed
            2 -> y += speed
            3 -> x += speed
        }
    }
}