#version 430 core

layout (local_size_x = 4, local_size_y = 4, local_size_z = 1) in;

layout(r32ui, binding = 0) uniform uimage2D stateImg;
layout(r32ui, binding = 1) uniform uimage2D editImg;

layout (location = 0) uniform uint type;
layout (location = 1) uniform ivec2 pos;
layout (location = 2) uniform uint rad;

void main() {
	ivec2 minPos = pos - ivec2(rad);
	minPos.x = max(minPos.x, 0);
	minPos.y = max(minPos.y, 0);

	ivec2 instancePos = minPos + ivec2(gl_GlobalInvocationID.xy);
	uvec2 gridSize = uvec2(gl_NumWorkGroups * gl_WorkGroupSize);
	if(instancePos.x >= gridSize.x || instancePos.y >= gridSize.y) return;
	if(distance(instancePos, pos) <= (.5 + rad)){
		imageStore(stateImg, instancePos, uvec4(type, 0, 0, 1));
		imageStore(editImg, instancePos, uvec4(type, 0, 0, 1));
	}
}
