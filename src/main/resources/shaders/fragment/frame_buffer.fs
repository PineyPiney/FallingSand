// FRAGMENT SHADER INFORMATION
#version 430 core

const vec3 colours[7] = vec3[](
	vec3(0.69019608, 0.96862745, 0.96470588),   // Air
	vec3(0.94117647, 0.80392157, 0.31764706),   // Sand
	vec3(0.47058824, 0.47058824, 0.47058824),   // Stone
	vec3(0.01568627, 0.16862745, 0.85098039),   // Water
	vec3(0.01568627, 0.16862745, 0.85098039),
	vec3(0, 1, 0),                              // Acid
	vec3(0, 1, 0)
);

in vec2 texCoords;

uniform usampler2D screenTexture;

out vec4 FragColour;

void main(){
	uint index = texture(screenTexture, texCoords).r & 0xff;
	if(index < colours.length()) {
		FragColour = vec4(colours[index], 1.0);
	}
	else FragColour = vec4(float(index) / pow(2, 8), 0.0, 0.0, 1.0);

}