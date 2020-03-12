package net.aquadc.properties.fx

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import net.aquadc.persistence.struct.StructBuilder
import net.aquadc.persistence.struct.invoke
import net.aquadc.properties.persistence.ObservableStruct
import net.aquadc.propertiesSampleLogic.User


class FxNativeSample : Application() {

    private val user = ObservableStruct(User {  }, false)

    override fun start(stage: Stage) {
        val vm = FxViewModel(user)
        stage.scene = Scene(nativeView(vm), 400.0, 300.0)
        stage.show()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            launch(FxNativeSample::class.java)
        }
    }

}
