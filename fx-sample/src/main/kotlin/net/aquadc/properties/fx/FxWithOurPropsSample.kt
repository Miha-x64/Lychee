package net.aquadc.properties.fx

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import net.aquadc.properties.concurrentMutablePropertyOf
import net.aquadc.properties.sample.logic.MainPresenter
import net.aquadc.properties.sample.logic.defaultUser

class FxWithOurPropsSample : Application() {

    private val userProp = concurrentMutablePropertyOf(defaultUser)

    override fun start(stage: Stage) {
        val presenter = MainPresenter(userProp)
        stage.scene = Scene(viewWithOurProps(presenter), 400.0, 300.0)
        stage.show()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            launch(FxWithOurPropsSample::class.java)
        }
    }

}