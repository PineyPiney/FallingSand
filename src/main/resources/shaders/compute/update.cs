#version 430 core
#line 1

#define AIR 0u
#define SAND 1u
#define SOLID 2u
#define WATER_L 3u
#define WATER_R 4u
#define ACID_L 5u
#define ACID_R 6u

#define HOLD 255u

layout (local_size_x = 32, local_size_y = 32, local_size_z = 1) in;

layout(r32ui, binding = 0) uniform uimage2D stateImg;
layout(r32ui, binding = 1) uniform uimage2D editImg;

layout(location = 1) uniform ivec2 offset;
layout(location = 2) uniform int direction;
layout(location = 3) uniform int randomSeed;

// Grid size must be multipled by 2 because the shader is dispatched 4 times with various offsets
ivec2 gridSize = ivec2(gl_WorkGroupSize.xy) * ivec2(gl_NumWorkGroups.xy) * 2;


int random(ivec2 pos){
	int x = pos.x + pos.y * gridSize.x + randomSeed * gridSize.x * gridSize.y;
	x ^= x << 13;
	x ^= x >> 17;
	x ^= x << 5;
	return x;
}

ivec2 down(ivec2 pixel){ return ivec2(pixel.x, pixel.y - 1); }
ivec2 downLeft(ivec2 pixel){ return ivec2(pixel.x - 1, pixel.y - 1); }
ivec2 downRight(ivec2 pixel){ return ivec2(pixel.x + 1, pixel.y - 1); }
ivec2 left(ivec2 pixel){ return ivec2(pixel.x - 1, pixel.y); }
ivec2 right(ivec2 pixel){ return ivec2(pixel.x + 1, pixel.y); }

uint checkState(ivec2 pixel){ return imageLoad(stateImg, pixel).r; }

void updateAir(ivec2 pixel);
void updateSand(ivec2 pixel);
void updateWater(ivec2 pixel, uint waterType);
void updateAcid(ivec2 pixel, uint acidType);

uint checkPixel(ivec2 pixel);
bool tryPlace(uint state, ivec2 newPos, ivec2 oldPos, uint posState, uint replace);

void main() {
	ivec2 updatingPixel = ivec2(gl_GlobalInvocationID.xy) * 2 + offset;

	// This pixel has already been edited this frame so skip it
	if(imageLoad(editImg, updatingPixel).r == 255) return;

	uint pixelType = checkState(updatingPixel);

	switch(pixelType){
		case AIR: updateAir(updatingPixel); break;
		case SAND: updateSand(updatingPixel); break;
		case WATER_L:
		case WATER_R: updateWater(updatingPixel, pixelType); break;
		case ACID_L:
		case ACID_R: updateAcid(updatingPixel, pixelType); break;
	}
}

void updateAir(ivec2 pixel){

}

bool tryMoveSand(ivec2 pixel, ivec2 newPos){
	// Get the pixel type at the new location,
	// placing a placeholder type in the mean time
	uint value = checkPixel(newPos);
	switch(value){
		case HOLD: return false; // If already held then don't modify the pixel
		// Sand displaces air
		case AIR: return tryPlace(SAND, newPos, pixel, HOLD, AIR);
		// Sand displaces water
		case WATER_L:
		case WATER_R:
		return tryPlace(SAND, newPos, pixel, HOLD, value);

		case ACID_L:
		case ACID_R:
			// Sand is dissolved slowly by acid
			if(random(pixel) % 10 == 0) {
				imageStore(stateImg, pixel, uvec4(AIR));
			}
			// It also displaces acid, this stops it sitting on top while dissolving
			else return tryPlace(SAND, newPos, pixel, HOLD, value);
	}
	// Ensure the held pixel has been replaced how it was if no change was made
	imageStore(stateImg, newPos, uvec4(value));
	return false;
}

void updateSand(ivec2 pixel){
	if(pixel.y == 0) return; // Sand can only move downward so this is an easy optimisation

	// Try to move sand down, then down to either side, alternating sides each frame got even distribution
	if(tryMoveSand(pixel, down(pixel))) return;
	else if(tryMoveSand(pixel, ivec2(pixel.x + direction, pixel.y - 1))) return;
	else if(tryMoveSand(pixel, ivec2(pixel.x - direction, pixel.y - 1))) return;
}

void updateWater(ivec2 pixel, uint waterType){

	int waterDirection = 2 * int(waterType - WATER_L) - 1;

	// First the water tries to fall down
	if(tryPlace(waterType, down(pixel), pixel, AIR, AIR)) return;
	// Or down and to the side it is travelling
	else if(tryPlace(waterType, ivec2(pixel.x + waterDirection, pixel.y - 1), pixel, AIR, AIR)) return;

	// Try to 'skate' the water across the surface it is on.
	else if(tryPlace(waterType, ivec2(pixel.x + waterDirection, pixel.y), pixel, AIR, AIR)) return;
	// If it cannot keep moving in the direction it is facing then bounce it back
	else imageStore(stateImg, pixel, uvec4(uint(int(waterType) - waterDirection)));
}

bool tryMoveAcid(uint acidType, ivec2 pixel, ivec2 newPos){
	// Get the pixel type at the new location,
	// placing a placeholder type in the mean time
	uint value = checkPixel(newPos);
	switch(value){
		case HOLD: return false; // If already held then don't modify the pixel
		// Acid displaces Air
		case AIR: return tryPlace(acidType, newPos, pixel, HOLD, AIR);
		// Acid dissolves Sand slowly
		case SAND:
			if(random(pixel) % 10 == 0) {
				return tryPlace(acidType, newPos, pixel, HOLD, AIR);
			}
	}
	// Ensure the held pixel has been replaced how it was if no change was made
	imageStore(stateImg, newPos, uvec4(value));
	return false;
}

void updateAcid(ivec2 pixel, uint acidType){

	int acidDirection = 2 * int(acidType - ACID_L) - 1;

	// Down
	if(tryMoveAcid(acidType, pixel, down(pixel))) return;
	// Down Diagonally
	else if(tryMoveAcid(acidType, pixel, ivec2(pixel.x + acidDirection, pixel.y - 1))) return;
	// Sideways
	if(tryMoveAcid(acidType, pixel, ivec2(pixel.x + acidDirection, pixel.y))) return;
	// Bounce
	else imageStore(stateImg, pixel, uvec4(uint(int(acidType) - acidDirection)));
}

uint checkPixel(ivec2 pixel){
	return imageAtomicExchange(stateImg, pixel, HOLD);
}

bool tryPlace(uint state, ivec2 newPos, ivec2 oldPos, uint posState, uint replace){
	// Don't let pixels fall outside of bounds
	if(newPos.y < 0 || newPos.x < 0 || newPos.x >= gridSize.x) return false;

	// Attempt to atomically place the sand pixel in the new spot,
	// if successfull then replace the old position with replace
	// and record the change in the editImg texture so it doesn't get double moved
	if(imageAtomicCompSwap(stateImg, newPos, posState, state) == posState){
		imageStore(stateImg, oldPos, uvec4(replace));
		imageStore(editImg, newPos, uvec4(255));
		return true;
	}
	else return false;
}

