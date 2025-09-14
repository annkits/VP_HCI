import kotlin.random.Random

class Human{
    var name: String = ""
    var surname: String = ""
    var second_name: String = ""
    var age: Int = -1

    var speed: Int = 1

    var x: Int = 0
    var y: Int = 0

    constructor(_name: String, _surname: String, _second: String, _age: Int){
        name = _name
        surname = _surname
        second_name = _second
        age = _age
        speed = Random.nextInt(6)
        println("We created the Human object with name: $name")
    }

    fun move() {
        val direction = Random.nextInt(4)
        when (direction) {
            0 -> y -= speed
            1 -> x -= speed
            2 -> y += speed
            3 -> x += speed
        }
    }
}

fun main(){

    val humans = arrayOf(
        Human("Павел", "Нейдорф", "Яковлевич", 21),
        Human("Ольга", "Моренкова", "Ильинична", 55),
        Human("Руслан ", "Ахпашев", "Владимирович", 30),
        Human("Татьяна", "Храмова", "Викторовна", 35),
        Human("Антон", "Сибирцев", "Владимирович", 31),
        Human("Павел", "Солодов", "Сергеевич", 32),
        Human("Лариса", "Рогулина", "Геннадьевна", 46),
        Human("Андрей", "Андреев", "Валерьевич", 33),
        Human("Елена", "Янченко", "Викторовна", 34),
        Human("Егор", "Сибиряков", "Борисович", 48),
        Human("Александр", "Лошкарев", "Васильевич", 36),
    )

    val simulationTime = 5

    for (time in 1..simulationTime) {
        println("Время: $time сек")
        humans.forEach {
            human -> human.move()
            println("${human.name}: позиция (${human.x}, ${human.y}), возраст ${human.age}, скорость ${human.speed}")
        }
        println("------------------")
    }
}