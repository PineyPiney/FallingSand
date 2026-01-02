plugins {
	kotlin("jvm") version "2.2.21"
}

group = "com.pineypiney.fallsand"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.3.6"

// Use https://www.lwjgl.org/customize to set natives
val lwjglNatives = run {
	val name = System.getProperty("os.name")
	val arch = System.getProperty("os.arch")
	val a64 = arch.startsWith("aarch64")
	when {
		name.startsWith("Windows") -> {
			if(arch.contains("64")) "natives-windows${if(a64) "-arm64" else ""}"
			else "natives-windows-x86"
		}
		arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } -> {
			if(a64) "natives-macos-arm64" else "natives-macos"
		}
		arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
			if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
				"natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
			else if (arch.startsWith("ppc"))
				"natives-linux-ppc64le"
			else if (arch.startsWith("riscv"))
				"natives-linux-riscv64"
			else
				"natives-linux"

		else -> throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
	}
}

repositories {
	mavenCentral()
	maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
	maven("https://jitpack.io")
}

dependencies {
	testImplementation(kotlin("test"))

	// Reflection
	implementation(kotlin("reflect"))

	// Logback
	implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
	implementation("ch.qos.logback:logback-classic:1.5.19")

	// Game Engine
	implementation("com.github.PineyPiney:GameEngine:5944279c91")

	// GLM
	implementation("kotlin.graphics:kool:0.9.75")
	implementation("kotlin.graphics:unsigned:3.3.32")
	implementation("kotlin.graphics:glm:0.9.9.1-11")

	// LWJGL
	implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

	implementation("org.lwjgl", "lwjgl")
	implementation("org.lwjgl", "lwjgl-assimp")
	implementation("org.lwjgl", "lwjgl-glfw")
	implementation("org.lwjgl", "lwjgl-jemalloc")
	implementation("org.lwjgl", "lwjgl-openal")
	implementation("org.lwjgl", "lwjgl-opengl")
	implementation("org.lwjgl", "lwjgl-openvr")
	implementation("org.lwjgl", "lwjgl-stb")
	runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-assimp", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-jemalloc", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-openvr", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)
}

kotlin {
	jvmToolchain(21)
}

tasks.test {
	useJUnitPlatform()
}