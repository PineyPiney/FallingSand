package com.pineypiney.fallsand

import com.pineypiney.game_engine.LibrarySetUp
import com.pineypiney.game_engine.window.DefaultWindow
import com.pineypiney.game_engine.window.DefaultWindowedEngine
import com.pineypiney.game_engine.window.Window
import org.lwjgl.glfw.GLFW

fun main() {
	LibrarySetUp.initLibraries()
	val window = DefaultWindow("Falling Sand", 1024, 512, Window.defaultHints + mapOf(GLFW.GLFW_CONTEXT_VERSION_MAJOR to 4, GLFW.GLFW_CONTEXT_VERSION_MINOR to 3))
	window.init()
	val engine = DefaultWindowedEngine(window, ::FallSandLogic, fps = 20)
	engine.run()
}