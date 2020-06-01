@file:JvmName("FxWithOurPropsSample")
package net.aquadc.properties.fx

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import net.aquadc.persistence.struct.invoke
import net.aquadc.properties.persistence.ObservableStruct
import net.aquadc.propertiesSampleLogic.MainVm
import net.aquadc.propertiesSampleLogic.User

val start = System.currentTimeMillis()

fun main(args: Array<String>) {
    Application.launch(PropsSampleApp::class.java)
}

class PropsSampleApp : Application() {

    private val user = ObservableStruct(User {  }, false)

    override fun start(stage: Stage) {
        val vm = MainVm(user.transactional())
        stage.scene = Scene(viewWithOurProps(vm), 400.0, 300.0)
        stage.title = "Lychee properties"
        stage.show()
        println("All this JavaFX madness ate " + (System.currentTimeMillis() - start) + " ms of user's time")
    }

}
