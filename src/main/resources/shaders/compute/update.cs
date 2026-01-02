#version 430 core

#define AIR 0
#define SAND 1

layout (local_size_x = 32, local_size_y = 32, local_size_z = 1) in;

layout(r32ui, binding = 0) uniform uimage2D stateImg;
layout(r32ui, binding = 1) uniform uimage2D editImg;

layout(location = 1) uniform uint yoffset;
layout(location = 2) uniform int direction;

ivec2 down(ivec2 pixel){
	return ivec2(pixel.x, pixel.y - 1);
}
ivec2 downLeft(ivec2 pixel){
	return ivec2(pixel.x - 1, pixel.y - 1);
}
ivec2 downRight(ivec2 pixel){
	return ivec2(pixel.x + 1, pixel.y - 1);
}
ivec2 left(ivec2 pixel){
	return ivec2(pixel.x - 1, pixel.y);
}
ivec2 right(ivec2 pixel){
	return ivec2(pixel.x + 1, pixel.y);
}

uint checkState(ivec2 pixel){
	return imageLoad(stateImg, pixel).r;
}

void updateAir(ivec2 pixel);

void updateSand(ivec2 pixel);

bool tryPlace(int state, ivec2 newPos, ivec2 oldPos);

ivec2 gridSize = ivec2(gl_WorkGroupSize.xy) * ivec2(gl_NumWorkGroups.xy);

void main() {
	ivec2 updatingPixel = ivec2(gl_GlobalInvocationID.x, gl_GlobalInvocationID.y * 2 + yoffset);

	// This pixel has already been edited this frame so skip it
	if(imageLoad(editImg, updatingPixel).r == 255) return;

	uint pixelType = checkState(updatingPixel);

	switch(pixelType){
		case AIR: updateAir(updatingPixel); break;
		case SAND: updateSand(updatingPixel); break;
	}
}

void updateAir(ivec2 pixel){

}

void updateSand(ivec2 pixel){
	if(pixel.y <= 0) return;
	ivec2 down = down(pixel);
	if(tryPlace(SAND, down, pixel)){}
	else if(tryPlace(SAND, ivec2(down.x + direction, down.y), pixel)){}
	else if(tryPlace(SAND, ivec2(down.x - direction, down.y), pixel)){}

//	else if(imageAtomicCompSwap(stateImg, ivec2(down.x + direction, down.y), 0, SAND) == 0){
//		imageStore(stateImg, pixel, uvec4(AIR));
//		imageStore(editImg, ivec2(down.x + direction, down.y), uvec4(SAND));
//		imageStore(editImg, pixel, uvec4(AIR));
//	}
//	else if(imageAtomicCompSwap(editImg, ivec2(down.x - direction, down.y), AIR, SAND) == AIR){
//		imageStore(editImg, pixel, uvec4(AIR));
//		imageStore(stateImg, ivec2(down.x - direction, down.y), uvec4(SAND));
//		imageStore(stateImg, pixel, uvec4(AIR));
//	}
}

bool tryPlace(int state, ivec2 newPos, ivec2 oldPos){
	// Don't let pixels fall outside of bounds
	if(newPos.x < 0 || newPos.x >= gridSize.x) return false;

	// Attempt to atomically place the sand pixel in the new spot,
	// if successfull then replace the old position with AIR
	// and record the change in the editImg texture so it doesn't get double moved
	if(imageAtomicCompSwap(stateImg, newPos, AIR, SAND) == AIR){
		imageStore(stateImg, oldPos, uvec4(AIR));
		imageStore(editImg, newPos, uvec4(255));
		return true;
	}
	else return false;
}