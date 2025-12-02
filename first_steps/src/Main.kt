fun main(){

    val humans = arrayOf(
        Human("Павел", "Нейдорф", "Яковлевич", 21),
        Human("Ольга", "Моренкова", "Ильинична", 55),
        Human("Александр", "Лошкарев", "Васильевич", 36),
    )

    val driver = Driver("Демьян", "Мастинин", "Валерьевич", 22)

    val simulationTime = 5

    for (time in 1..simulationTime) {
        println("Время: $time сек")
        val threads = mutableListOf<Thread>()

        humans.forEach { human ->
            val thread = Thread {
                human.move()
                println("${human.name}: позиция (${human.x}, ${human.y}), возраст ${human.age}, скорость ${human.speed}")
            }
            threads.add(thread)
            thread.start()
        }

        val driverThread = Thread {
            driver.move()
            println("${driver.name} (Driver): позиция (${driver.x}, ${driver.y}), возраст ${driver.age}, скорость ${driver.speed}")
        }
        threads.add(driverThread)
        driverThread.start()

        threads.forEach { it.join() }
        println("-------------------")
        Thread.sleep(1000)
    }
}