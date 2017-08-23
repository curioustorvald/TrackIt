#version 100
#ifdef GL_ES
    precision mediump float;
#endif


varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

uniform vec2 scroll; // texcoord

void main() {
    vec4 pixel = texture2D(u_texture, v_texCoords - scroll);
    gl_FragColor = vec4(pixel);
}