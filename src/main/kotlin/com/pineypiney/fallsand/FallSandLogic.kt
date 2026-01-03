package com.pineypiney.fallsand

import com.pineypiney.game_engine.resources.shaders.AtomicCounter
import com.pineypiney.game_engine.resources.shaders.ShaderLoader
import com.pineypiney.game_engine.resources.textures.Texture
import com.pineypiney.game_engine.resources.textures.TextureLoader
import com.pineypiney.game_engine.resources.textures.TextureParameters
import com.pineypiney.game_engine.util.ResourceKey
import com.pineypiney.game_engine.util.extension_functions.times
import com.pineypiney.game_engine.util.input.InputState
import com.pineypiney.game_engine.window.DefaultWindowedEngine
import com.pineypiney.game_engine.window.WindowGameLogic
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL43C
import kotlin.math.ceil
import kotlin.random.Random

class FallSandLogic(override val gameEngine: DefaultWindowedEngine<FallSandLogic>) : WindowGameLogic() {
	override val renderer = FallSandRenderer(window)

	val width = 128
	val height = 64

	val stateTexture = Texture("State Texture", TextureLoader.createTexture(null, width, height, GL43C.GL_RED_INTEGER, GL43C.GL_R32UI, TextureParameters().withFilter(GL43C.GL_NEAREST)), binding = 1)
	val editTexture = Texture("Edit Texture", TextureLoader.createTexture(null, width, height, GL43C.GL_RED_INTEGER, GL43C.GL_R32UI), binding = 1)

	val paintShader = ShaderLoader[ResourceKey("compute/paint")]
	val updateShader = ShaderLoader[ResourceKey("compute/update")]
	val counterShader = ShaderLoader[ResourceKey("compute/pixel_counter")]

	var direction = 1

	val pixelCounter = AtomicCounter(7)
	val random = Random(1)

	override fun init() {
		super.init()

		GL43C.glBindImageTexture(0, stateTexture.texturePointer, 0, false, 0, GL43C.GL_READ_WRITE, GL43C.GL_R32UI)
		GL43C.glBindImageTexture(1, editTexture.texturePointer, 0, false, 0, GL43C.GL_READ_WRITE, GL43C.GL_R32UI)
	}

	override fun addObjects() {

	}

	override fun render(tickDelta: Double) {
		processMouse()
		compute()
		renderer.render(this, tickDelta)
	}

	override fun onInput(state: InputState, action: Int): Int {
		if(action > 0){
			when(state.i){
				GLFW.GLFW_KEY_SPACE -> compute()
				GLFW.GLFW_KEY_ESCAPE -> window.shouldClose = true
			}
		}
		return action
	}

	private fun processMouse(){
		val shift = GLFW.glfwGetKey(window.windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == 1
		val ctrl = GLFW.glfwGetKey(window.windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL) == 1
		val pixel = Vec2i((Vec2(width, height) * window.input.mouse.lastPos.pixels) / window.framebufferSize)
		if(GLFW.glfwGetMouseButton(window.windowHandle, GLFW.GLFW_MOUSE_BUTTON_1) == 1){
			if(shift) paint(pixel, 7u, 0u)
			else paint(pixel, 7u, 1u)
		}
		else if(GLFW.glfwGetMouseButton(window.windowHandle, GLFW.GLFW_MOUSE_BUTTON_2) == 1){
			if(shift) paint(pixel, 2u, 3u) // Water
			else if(ctrl) paint(pixel, 2u, 5u) // Acid
			else paint(pixel, 2u, 2u) // Stone
		}
	}

	private fun compute(){
		val q = GL43C.glGenQueries()
		GL43C.glBeginQuery(GL43C.GL_TIME_ELAPSED, q)
		updateShader.use()
		direction = -direction
		updateShader.setInt("direction", direction)
		updateShader.setInt("randomSeed", random.nextInt())

		updateShader.setVec2i("offset", 0, 0)
		updateShader.dispatch(width / 64, height / 64)
		updateShader.setVec2i("offset", 0, 1)
		updateShader.dispatch(width / 64, height / 64)
		updateShader.setVec2i("offset", 1, 0)
		updateShader.dispatch(width / 64, height / 64)
		updateShader.setVec2i("offset", 1, 1)
		updateShader.dispatch(width / 64, height / 64)

		GL43C.glEndQuery(GL43C.GL_TIME_ELAPSED)
		val t = GL43C.glGetQueryObjecti64(q, GL43C.GL_QUERY_RESULT)
		GL43C.glDeleteQueries(q)
		logger.info("Frame Time = ${t / 1000}μs")

 		editTexture.clear()


		countPixels()
		GL43C.glFinish()
	}

	private fun paint(pos: Vec2i, radius: UInt, type: UInt){
		paintShader.use()
		paintShader.setUInt("type", type)
		paintShader.setVec2i("pos", pos)
		paintShader.setUInt("rad", radius)
		paintShader.setVec2i("gridSize", width, height)
		val i = ceil((2u * radius + 1u) * 25f).toInt()
		paintShader.dispatch(i, i, 1)

		countPixels()
	}

	private fun countPixels(){
		pixelCounter.reset()
		counterShader.use()
		pixelCounter.bind(1)
		counterShader.dispatch(width / 32, height / 32)
		GL43C.glMemoryBarrier(GL43C.GL_ATOMIC_COUNTER_BARRIER_BIT)

		val values = pixelCounter.getValues()
		val str = StringBuilder()
		fun add(name: String, value: Int) = if(value > 0) str.append("$name: $value, ") else str

		add("Air", values[0])
		add("Sand", values[1])
		add("Stone", values[2])
		add("Water", values[3] + values[3])
		add("Acid", values[5] + values[6])

		println("Pixel Counts: $str")
	}
}