package net.torvald.trackit

object ScrollShaderSingleton {

    const val FRAG = """
#version 120
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
"""


    const val VERT = """
attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}
"""

}