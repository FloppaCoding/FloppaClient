#version 400

uniform float chromaSize;
uniform float chromaTime;
uniform float chromaAngle;

varying vec4 vertColor;

//
// Taken from
// https://www.shadertoy.com/view/MsS3Wc
// by Inigo Quilez under MIT License

// Smooth HSV to RGB conversion
vec3 hsv2rgb_smooth( in vec3 c )
{
    vec3 rgb = clamp( abs(mod(c.x*6.0+vec3(0.0,4.0,2.0),6.0)-3.0)-1.0, 0.0, 1.0 );
    rgb = rgb*rgb*(3.0-2.0*rgb); // cubic smoothing
    return c.z * mix( vec3(1.0), rgb, c.y);
}

void main() {
    float hue = mod( (sin(chromaAngle) * gl_FragCoord.x + cos(chromaAngle) * gl_FragCoord.y) * chromaSize + chromaTime, 1.0);
    gl_FragColor = vec4(hsv2rgb_smooth(vec3(hue, 1.0, 1.0)), vertColor.a);
}