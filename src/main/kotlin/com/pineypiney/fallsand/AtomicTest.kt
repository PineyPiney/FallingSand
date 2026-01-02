package com.pineypiney.fallsand

import com.pineypiney.game_engine.resources.shaders.AtomicCounter
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.*
import org.lwjgl.system.MemoryStack


fun main() {
	// create context

	GLFW.glfwInit()
	GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4)
	GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3)
	GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
	GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
	GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)

	val window = GLFW.glfwCreateWindow(800, 600, "", 0L, 0L)

	GLFW.glfwMakeContextCurrent(window)

	GL.createCapabilities()


	val counter = AtomicCounter(1)
	val atomicBuffer = counter.buffer

	counter.setValue(100)

	// create compute shader
	val program = GL20C.glCreateProgram()
	val cs = GL20C.glCreateShader(GL43C.GL_COMPUTE_SHADER)
	GL20C.glShaderSource(
		cs,
		"#version 430 core\n" +
				"layout(binding=1) uniform atomic_uint counter;\n" +
				"layout(local_size_x = 8, local_size_y = 8) in;\n" +
				"void main(void) {\n" +
				"  atomicCounterIncrement(counter);\n" +
				"}"
	)
	GL20C.glCompileShader(cs)
	GL20C.glAttachShader(program, cs)
	GL20C.glLinkProgram(program)


	// dispatch compute
	GL20C.glUseProgram(program)
	counter.bind(1)
	GL43C.glDispatchCompute(1, 1, 1)
	GL42C.glMemoryBarrier(GL42C.GL_ATOMIC_COUNTER_BARRIER_BIT)


	// read-back current counter value (should be 164 = 100 + 8*8)
	GL15C.glBindBuffer(GL42C.GL_ATOMIC_COUNTER_BUFFER, atomicBuffer)

	MemoryStack.stackPush().use { stack ->
		val buffer = stack.mallocInt(1)
		GL15C.glGetBufferSubData(GL42C.GL_ATOMIC_COUNTER_BUFFER, 0L, buffer)
		println("Current value: " + buffer.get(0))
	}
}