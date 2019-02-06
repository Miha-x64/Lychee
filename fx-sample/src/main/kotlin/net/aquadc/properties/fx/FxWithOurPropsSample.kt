package net.aquadc.properties.fx

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import net.aquadc.persistence.struct.build
import net.aquadc.properties.persistence.ObservableStruct
import net.aquadc.propertiesSampleLogic.MainVm
import net.aquadc.propertiesSampleLogic.User


class FxWithOurPropsSample : Application() {

    private val user = ObservableStruct(User.build {  }, false)

    override fun start(stage: Stage) {
        val vm = MainVm(user.transactional())
        stage.scene = Scene(viewWithOurProps(vm), 400.0, 300.0)
        stage.show()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            launch(FxWithOurPropsSample::class.java)
        }
    }

}