/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smartfarm.common.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.smartfarm.common.rendering.geometry.LineString;

import java.io.IOException;

/** Renders a point cloud. */
public class LineRenderer {
  private static final String TAG = "LINE_RENDERER";

  // Shader names.
  private static final String VERTEX_SHADER_NAME = "shaders/line.vert";
  private static final String FRAGMENT_SHADER_NAME = "shaders/line.frag";

  private static final int BYTES_PER_FLOAT = Float.SIZE / 8;                      // float = 4byte
  private static final int FLOATS_PER_POINT = 4; // X,Y,Z,confidence.             // 점 하나당 float 갯수
  private static final int BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT;  //점하나당 32byte
  private static final int INITIAL_BUFFER_POINTS = 1000;

  private int vbo;
  private int vboSize;

  private int programName;
  private int positionAttribute;
  private int modelViewProjectionUniform;         // MVP값을 저장할 주소값
  private int colorUniform;                       // 색상값을 저장할 주소값

  private int numPoints = 0;


  private float r;
  private float g;
  private float b;
  public LineRenderer(float r, float g, float b) {
    this.r = r;
    this.g = g;
    this.b = b;
  }

  public void createOnGlThread(Context context) throws IOException {
    ShaderUtil.checkGLError(TAG, "before create");

    int[] buffers = new int[1];
    GLES20.glGenBuffers(1, buffers, 0);             //버퍼 1개 생성
    vbo = buffers[0];
    
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);     //VBO 버퍼 사용시작.
    vboSize = INITIAL_BUFFER_POINTS * BYTES_PER_POINT;    //32byte * 1000
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);     //VBO 버퍼 NULL로 채움
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);   //VBO 버퍼 사용종료.

    ShaderUtil.checkGLError(TAG, "buffer alloc");

    int vertexShader = ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
    int passthroughShader = ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

    programName = GLES20.glCreateProgram();

    GLES20.glAttachShader(programName, vertexShader);
    GLES20.glAttachShader(programName, passthroughShader);
    GLES20.glLinkProgram(programName);
    GLES20.glUseProgram(programName);

    ShaderUtil.checkGLError(TAG, "program");

    positionAttribute = GLES20.glGetAttribLocation(programName, "a_Position");

    colorUniform = GLES20.glGetUniformLocation(programName, "u_Color");
    modelViewProjectionUniform = GLES20.glGetUniformLocation(programName, "u_ModelViewProjection");
    ShaderUtil.checkGLError(TAG, "program  params");
  }

  /**
   * Updates the OpenGL buffer contents to the provided point. Repeated calls with the same point
   * cloud will be ignored.
   */
  public void update(LineString linestring) {

    ShaderUtil.checkGLError(TAG, "before update");

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);     //VBO 버퍼 사용시작.

    numPoints = linestring.getPoints().remaining() / FLOATS_PER_POINT;
    if (numPoints * BYTES_PER_POINT > vboSize) {
      while (numPoints * BYTES_PER_POINT > vboSize) {
        vboSize *= 2;
      }
      GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);         //VBO 버퍼 NULL로 채움
    }
    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, numPoints * BYTES_PER_POINT, linestring.getPoints());

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);   //VBO 버퍼 사용종료.

    ShaderUtil.checkGLError(TAG, "after update");
  }
  
  public void draw(float[] cameraView, float[] cameraPerspective) {
    float[] modelViewProjection = new float[16];
    Matrix.multiplyMM(modelViewProjection, 0, cameraPerspective, 0, cameraView, 0);

    ShaderUtil.checkGLError(TAG, "Before draw");

    GLES20.glUseProgram(programName);                                                                         //라인렌더러 사용시작
    GLES20.glEnableVertexAttribArray(positionAttribute);                                                      //POSITION 속성 활성 시작

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);                                                         //VBO 버퍼 사용시작.
    GLES20.glVertexAttribPointer(positionAttribute, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);

    GLES20.glUniform4f(colorUniform, r, g, b, 1.0f);   //컬러 변수 넘김
    GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjection, 0);        //MVP 변수 넘김
    
    GLES20.glLineWidth(5f);
    GLES20.glDrawArrays(GLES20.GL_LINES, 0, numPoints);
    GLES20.glDisableVertexAttribArray(positionAttribute);                                                    //POSITION 속성 활성 종료
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);                                                       //VBO 버퍼 사용종료.

    ShaderUtil.checkGLError(TAG, "Draw");
  }
}
