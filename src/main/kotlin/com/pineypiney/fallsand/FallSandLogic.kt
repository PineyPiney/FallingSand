package com.pineypiney.fallsand

import com.pineypiney.game_engine.rendering.FrameBuffer
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

	val pixelCounter = AtomicCounter(2)

	override fun init() {
		super.init()

		GL43C.glBindImageTexture(0, stateTexture.texturePointer, 0, false, 0, GL43C.GL_READ_WRITE, GL43C.GL_R32UI)
		GL43C.glBindImageTexture(1, editTexture.texturePointer, 0, false, 0, GL43C.GL_READ_WRITE, GL43C.GL_R32UI)

		paint(Vec2i(width / 2, height / 2), 19u)
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
//				GLFW.GLFW_KEY_SPACE -> compute()
				GLFW.GLFW_KEY_ESCAPE -> window.shouldClose = true
			}
		}
		return action
	}

	private fun processMouse(){
		if(GLFW.glfwGetMouseButton(window.windowHandle, GLFW.GLFW_MOUSE_BUTTON_1) == 1){
			paint(Vec2i((Vec2(width, height) * window.input.mouse.lastPos.pixels) / window.framebufferSize), 7u)
		}
	}

	private fun compute(){
		updateShader.use()
		updateShader.setVec2i("gridSize", width, height)
		direction = -direction
		updateShader.setInt("direction", direction)

		updateShader.setUInt("yoffset", 0u)
		updateShader.dispatch(width / 32, height / 64, 1)
		updateShader.setUInt("yoffset", 1u)
		updateShader.dispatch(width / 32, height / 64, 1)

 		editTexture.clear()

		countPixels()
		GL43C.glFinish()
	}

	private fun paint(pos: Vec2i, radius: UInt){
		paintShader.use()
		paintShader.setUInt("type", 1u)
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

		println("Pixel Counts: ${pixelCounter.getValues().joinToString(", ")}")
	}
}