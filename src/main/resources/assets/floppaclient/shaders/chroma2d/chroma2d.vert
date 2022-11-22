#version 120

attribute vec3 position;

varying vec4 outColor;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * vec4(position, 1.0);
    outColor = gl_Color;
}