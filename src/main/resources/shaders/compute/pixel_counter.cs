#version 430 core

layout (local_size_x = 32, local_size_y = 32, local_size_z = 1) in;

layout(r32ui, binding = 0) uniform uimage2D stateImg;

layout(binding = 1, offset = 0) uniform atomic_uint partCounter[7];

void main() {
	ivec2 updatingPixel = ivec2(gl_GlobalInvocationID.xy);
	uint pixelType = imageLoad(stateImg, updatingPixel).r;

	if(pixelType < partCounter.length()) {
		atomicCounterIncrement(partCounter[pixelType]);
	}
}
