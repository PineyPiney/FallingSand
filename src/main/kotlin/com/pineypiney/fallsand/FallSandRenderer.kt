package com.pineypiney.fallsand

import com.pineypiney.game_engine.objects.ObjectCollection
import com.pineypiney.game_engine.rendering.FrameBuffer
import com.pineypiney.game_engine.rendering.WindowRendererI
import com.pineypiney.game_engine.rendering.cameras.CameraI
import com.pineypiney.game_engine.rendering.cameras.OrthographicCamera
import com.pineypiney.game_engine.rendering.meshes.Mesh
import com.pineypiney.game_engine.resources.shaders.ShaderLoader
import com.pineypiney.game_engine.util.ResourceKey
import com.pineypiney.game_engine.util.maths.I
import com.pineypiney.game_engine.window.WindowI
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2i
import glm_.vec3.Vec3

class FallSandRenderer(override val window: WindowI): WindowRendererI<FallSandLogic> {

	override val camera: CameraI = OrthographicCamera(window)

	override val viewPos: Vec3 get() = camera.cameraPos
	override lateinit var viewportSize: Vec2i
	override var aspectRatio: Float = 1f

	override val view: Mat4 = I
	override val projection: Mat4 = I
	override val guiProjection: Mat4 = I

	val sandShader = ShaderLoader[ResourceKey("vertex/frame_buffer"), ResourceKey("fragment/frame_buffer")]

	override fun init() {
		camera.init()
	}

	override fun updateAspectRatio(window: WindowI, objects: ObjectCollection) {
		camera.updateAspectRatio(window.aspectRatio)
		viewportSize = window.size
		aspectRatio = window.aspectRatio
		val w = window.aspectRatio
		glm.ortho(-w, w, -1f, 1f, guiProjection)
	}

	override fun render(game: FallSandLogic, tickDelta: Double) {

		camera.getView(view)
		camera.getProjection(projection)

		sandShader.use()
		sandShader.setInt("screenTexture", 1)
		game.stateTexture.bind()
		Mesh.screenQuadShape.bindAndDraw()
	}

	override fun delete() {}
}