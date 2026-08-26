package com.peanut.xrpg;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Real lightweight OpenGL ES 2.0 dice renderer; no external 3D engine. */
public final class Dice3DView extends GLSurfaceView {
    private final Renderer renderer;

    public Dice3DView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new Renderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setBackgroundColor(0xFF0F1117);
    }

    public void setDice(int sides, int count) { renderer.setDice(sides, count); }
    public void roll() { renderer.roll(); }

    private static final class Renderer implements GLSurfaceView.Renderer {
        private final Random random = new Random();
        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];
        private final DiceMesh mesh = new DiceMesh();
        private int sides = 20, count = 1;
        private long rollUntil;
        private float spin = 0f, pitch = 22f;
        private float[] offsets = new float[1];

        void setDice(int sides, int count) {
            this.sides = sides;
            this.count = Math.max(1, Math.min(count, 8));
            mesh.setSides(sides);
            offsets = new float[this.count];
            for (int i = 0; i < offsets.length; i++) offsets[i] = i * 47f;
        }

        void roll() {
            rollUntil = SystemClock.uptimeMillis() + 950;
            spin += 360f + random.nextInt(360);
        }

        @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0.059f, 0.067f, 0.090f, 1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
        }

        @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float ratio = width / (float) Math.max(1, height);
            Matrix.frustumM(projection, 0, -ratio, ratio, -1f, 1f, 2f, 12f);
        }

        @Override public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            long now = SystemClock.uptimeMillis();
            float progress = rollUntil > now ? (rollUntil - now) / 950f : 0f;
            float extra = progress > 0 ? progress * 900f : 0f;
            Matrix.setLookAtM(view, 0, 0, 0.25f, 5.8f, 0, 0, 0, 0, 1, 0);

            int cols = count <= 2 ? count : 2;
            int rows = (count + cols - 1) / cols;
            for (int i = 0; i < count; i++) {
                int col = i % cols, row = i / cols;
                float x = (col - (cols - 1) / 2f) * 1.7f;
                float y = ((rows - 1) / 2f - row) * 1.65f;
                float scale = count == 1 ? 1.25f : 0.78f;
                Matrix.setIdentityM(model, 0);
                Matrix.translateM(model, 0, x, y, 0);
                Matrix.rotateM(model, 0, spin + offsets[i] + extra, 0.55f, 1f, 0.25f);
                Matrix.rotateM(model, 0, pitch, 1f, 0.2f, 0f);
                Matrix.scaleM(model, 0, scale, scale, scale);
                Matrix.multiplyMM(mvp, 0, view, 0, model, 0);
                Matrix.multiplyMM(mvp, 0, projection, 0, mvp, 0);
                mesh.draw(mvp);
            }
            if (rollUntil > now) spin += 5f;
        }
    }

    private static final class DiceMesh {
        private int program;
        private FloatBuffer vertices;
        private int vertexCount;
        private int sides = 20;
        private final float[] color = {0.92f, 0.94f, 0.97f, 1f};

        void setSides(int sides) { this.sides = sides; build(); }

        void draw(float[] mvp) {
            if (program == 0) buildProgram();
            GLES20.glUseProgram(program);
            int pos = GLES20.glGetAttribLocation(program, "aPosition");
            int col = GLES20.glGetUniformLocation(program, "uColor");
            int mat = GLES20.glGetUniformLocation(program, "uMvp");
            vertices.position(0);
            GLES20.glEnableVertexAttribArray(pos);
            GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, vertices);
            GLES20.glUniform4fv(col, 1, color, 0);
            GLES20.glUniformMatrix4fv(mat, 1, false, mvp, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
            GLES20.glDisableVertexAttribArray(pos);
        }

        private void buildProgram() {
            String vs = "attribute vec3 aPosition; uniform mat4 uMvp; void main(){gl_Position=uMvp*vec4(aPosition,1.0);}";
            String fs = "precision mediump float; uniform vec4 uColor; void main(){gl_FragColor=uColor;}";
            int v = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER); GLES20.glShaderSource(v, vs); GLES20.glCompileShader(v);
            int f = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER); GLES20.glShaderSource(f, fs); GLES20.glCompileShader(f);
            program = GLES20.glCreateProgram(); GLES20.glAttachShader(program, v); GLES20.glAttachShader(program, f); GLES20.glLinkProgram(program);
        }

        private void build() {
            float[] data;
            switch (sides) {
                case 4: data = tetra(); break;
                case 6: data = cube(); break;
                case 8: data = octa(); break;
                case 10: data = bipyramid(5); break;
                case 12: data = dodeca(); break;
                case 100: data = icoSphere(); break;
                default: data = icosa(); break;
            }
            vertexCount = data.length / 3;
            vertices = ByteBuffer.allocateDirect(data.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            vertices.put(data).position(0);
        }

        private float[] tetra() {
            float[][] v={{1,1,1},{1,-1,-1},{-1,1,-1},{-1,-1,1}};
            int[][] f={{0,1,2},{0,3,1},{0,2,3},{1,3,2}}; return faces(v,f);
        }
        private float[] cube() {
            float a=1f; float[][] v={{-a,-a,-a},{a,-a,-a},{a,a,-a},{-a,a,-a},{-a,-a,a},{a,-a,a},{a,a,a},{-a,a,a}};
            int[][] f={{0,1,2},{0,2,3},{4,6,5},{4,7,6},{0,4,5},{0,5,1},{3,2,6},{3,6,7},{1,5,6},{1,6,2},{0,3,7},{0,7,4}}; return faces(v,f);
        }
        private float[] octa() {
            float[][] v={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
            int[][] f={{0,2,4},{2,1,4},{1,3,4},{3,0,4},{2,0,5},{1,2,5},{3,1,5},{0,3,5}}; return faces(v,f);
        }
        private float[] icosa() {
            float t=(float)((1+Math.sqrt(5))/2); float[][] v={{-1,t,0},{1,t,0},{-1,-t,0},{1,-t,0},{0,-1,t},{0,1,t},{0,-1,-t},{0,1,-t},{t,0,-1},{t,0,1},{-t,0,-1},{-t,0,1}};
            int[][] f={{0,11,5},{0,5,1},{0,1,7},{0,7,10},{0,10,11},{1,5,9},{5,11,4},{11,10,2},{10,7,6},{7,1,8},{3,9,4},{3,4,2},{3,2,6},{3,6,8},{3,8,9},{4,9,5},{2,4,11},{6,2,10},{8,6,7},{9,8,1}}; return faces(v,f);
        }
        private float[] dodeca() {
            float[][] v={{-1,-1,-1},{-1,-1,1},{-1,1,-1},{-1,1,1},{1,-1,-1},{1,-1,1},{1,1,-1},{1,1,1},{0,-0.618034f,-1.618034f},{-0.618034f,-1.618034f,0},{-1.618034f,0,-0.618034f},{0,-0.618034f,1.618034f},{-0.618034f,1.618034f,0},{1.618034f,0,-0.618034f},{0,0.618034f,-1.618034f},{0.618034f,-1.618034f,0},{-1.618034f,0,0.618034f},{0,0.618034f,1.618034f},{0.618034f,1.618034f,0},{1.618034f,0,0.618034f}};
            int[][] f={{6,14,8},{6,4,8},{6,4,13},{2,0,10},{2,0,8},{2,14,8},{16,1,9},{16,0,10},{16,0,9},{16,3,12},{16,2,10},{16,2,12},{15,5,11},{15,1,9},{15,1,11},{15,0,8},{15,0,9},{15,4,8},{19,15,5},{19,4,13},{19,15,4},{17,1,11},{17,16,1},{17,16,3},{17,5,11},{17,19,7},{17,19,5},{18,2,14},{18,6,14},{18,2,12},{18,6,13},{18,19,13},{18,19,7},{18,3,12},{18,17,7},{18,17,3}};
            return faces(v,f);
        }
        private float[] icoSphere() {
            ArrayList<Float> o=new ArrayList<>(); int rings=7, segments=14;
            for(int r=0;r<rings-1;r++){ double lat=-Math.PI/2 + Math.PI*r/(rings-1), next=-Math.PI/2 + Math.PI*(r+1)/(rings-1);
                for(int s=0;s<segments;s++){ double a=2*Math.PI*s/segments,b=2*Math.PI*(s+1)/segments;
                    float x1=(float)(Math.cos(lat)*Math.cos(a)),z1=(float)(Math.cos(lat)*Math.sin(a)),y1=(float)Math.sin(lat); float x2=(float)(Math.cos(next)*Math.cos(a)),z2=(float)(Math.cos(next)*Math.sin(a)),y2=(float)Math.sin(next); float x3=(float)(Math.cos(next)*Math.cos(b)),z3=(float)(Math.cos(next)*Math.sin(b)),y3=(float)Math.sin(next); float x4=(float)(Math.cos(lat)*Math.cos(b)),z4=(float)(Math.cos(lat)*Math.sin(b)),y4=(float)Math.sin(lat);
                    tri(o,x1,y1,z1,x2,y2,z2,x3,y3,z3); tri(o,x1,y1,z1,x3,y3,z3,x4,y4,z4); }
            }
            float[] a=new float[o.size()]; for(int i=0;i<a.length;i++)a[i]=o.get(i)*1.05f; return a;
        }
        private float[] bipyramid(int n){ArrayList<Float> o=new ArrayList<>();float top=1.15f,bottom=-1.15f,r=.95f;for(int i=0;i<n;i++){double a=i*2*Math.PI/n,b=(i+1)*2*Math.PI/n;tri(o,0,top,0,(float)(r*Math.cos(a)),0,(float)(r*Math.sin(a)),(float)(r*Math.cos(b)),0,(float)(r*Math.sin(b)));tri(o,0,bottom,0,(float)(r*Math.cos(b)),0,(float)(r*Math.sin(b)),(float)(r*Math.cos(a)),0,(float)(r*Math.sin(a)));}float[] a=new float[o.size()];for(int i=0;i<a.length;i++)a[i]=o.get(i);return a;}
        private void tri(ArrayList<Float> o,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz){o.add(ax);o.add(ay);o.add(az);o.add(bx);o.add(by);o.add(bz);o.add(cx);o.add(cy);o.add(cz);}
        private float[] faces(float[][] v,int[][] f){ArrayList<Float> o=new ArrayList<>();for(int[] q:f)tri(o,v[q[0]][0],v[q[0]][1],v[q[0]][2],v[q[1]][0],v[q[1]][1],v[q[1]][2],v[q[2]][0],v[q[2]][1],v[q[2]][2]);float[] a=new float[o.size()];for(int i=0;i<a.length;i++)a[i]=o.get(i)*.78f;return a;}
    }
}
