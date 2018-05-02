package net.aquadc.properties.fx

import javafx.application.Application
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Scene
import javafx.stage.Stage
import net.aquadc.properties.sample.logic.InMemoryUser
import net.aquadc.properties.sample.logic.defaultUser

class FxNativeSample : Application() {

    private val userProp = SimpleObjectProperty<InMemoryUser>(defaultUser)

    override fun start(stage: Stage) {
        val vm = FxViewModel(userProp)
        stage.scene = Scene(nativeView(vm), 400.0, 300.0)
        stage.show()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            launch(FxNativeSample::class.java)
        }
    }

}
