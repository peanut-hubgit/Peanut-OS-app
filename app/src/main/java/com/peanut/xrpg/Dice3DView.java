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

/** Stable native OpenGL ES 2.0 dice renderer. */
public final class Dice3DView extends GLSurfaceView {
    private final Renderer renderer;
    public Dice3DView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        renderer = new Renderer();
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setBackgroundColor(0xFF1E212A);
        setFocusable(true);
    }
    public void setDice(int sides, int count) { renderer.setDice(sides, count); requestRender(); }
    public void roll() { renderer.roll(); requestRender(); }

    private static final class Renderer implements GLSurfaceView.Renderer {
        private final Random random = new Random();
        private final float[] projection=new float[16], view=new float[16], model=new float[16], mv=new float[16], mvp=new float[16];
        private DiceMesh mesh=new DiceMesh(20);
        private int sides=20,count=1;
        private long animationStart, animationEnd;
        private float spin;
        void setDice(int s,int c){sides=normalize(s);count=Math.max(1,Math.min(8,c));mesh=new DiceMesh(sides);}
        void roll(){animationStart=SystemClock.uptimeMillis();animationEnd=animationStart+950L;spin+=360f+random.nextInt(360);}
        @Override public void onSurfaceCreated(GL10 gl,EGLConfig config){GLES20.glClearColor(.075f,.082f,.105f,1f);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glDepthFunc(GLES20.GL_LEQUAL);GLES20.glDisable(GLES20.GL_CULL_FACE);mesh.ensureProgram();}
        @Override public void onSurfaceChanged(GL10 gl,int w,int h){GLES20.glViewport(0,0,Math.max(1,w),Math.max(1,h));float a=Math.max(.1f,w/(float)Math.max(1,h));float hy=1.05f;float hx=hy*a;Matrix.frustumM(projection,0,-hx,hx,-hy,hy,2f,20f);}
        @Override public void onDrawFrame(GL10 gl){
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            Matrix.setLookAtM(view,0,0,.15f,6.5f,0,0,0,0,1,0);
            long now=SystemClock.uptimeMillis();boolean rolling=now<animationEnd;float t=rolling?Math.min(1f,(now-animationStart)/950f):1f;float eased=1f-(float)Math.pow(1f-t,3);
            int cols=count<=2?count:2, rows=(count+cols-1)/cols;float gap=count==1?0f:1.55f, scale=count==1?1.35f:(count<=4?.82f:.64f);
            for(int i=0;i<count;i++){
                int col=i%cols,row=i/cols;float x=(col-(cols-1)/2f)*gap,y=((rows-1)/2f-row)*1.45f;
                Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,0);float extra=rolling?(1f-eased)*1250f:0f;
                Matrix.rotateM(model,0,spin+i*47f+extra,.55f,1f,.35f);Matrix.rotateM(model,0,18f+i*7f,1f,.25f,0);Matrix.scaleM(model,0,scale,scale,scale);
                Matrix.multiplyMM(mv,0,view,0,model,0);Matrix.multiplyMM(mvp,0,projection,0,mv,0);mesh.draw(mvp);
            }
        }
        private static int normalize(int s){switch(s){case 4:case 6:case 8:case 10:case 12:case 20:case 100:return s;default:return 20;}}
    }

    private static final class DiceMesh {
        private final int sides; private FloatBuffer data; private int vertexCount,program,positionHandle,normalHandle,mvpHandle;
        DiceMesh(int sides){this.sides=sides;build();}
        void ensureProgram(){if(program!=0)return;String vs="attribute vec3 aPosition;attribute vec3 aNormal;uniform mat4 uMvp;varying vec3 vNormal;void main(){vNormal=aNormal;gl_Position=uMvp*vec4(aPosition,1.0);}";String fs="precision mediump float;varying vec3 vNormal;void main(){vec3 n=normalize(vNormal);vec3 l=normalize(vec3(-.45,.75,1.0));float d=max(dot(n,l),0.0);float s=.42+d*.58;gl_FragColor=vec4(vec3(.90,.93,.98)*s,1.0);}";int v=compile(GLES20.GL_VERTEX_SHADER,vs),f=compile(GLES20.GL_FRAGMENT_SHADER,fs);if(v==0||f==0)return;program=GLES20.glCreateProgram();GLES20.glAttachShader(program,v);GLES20.glAttachShader(program,f);GLES20.glLinkProgram(program);int[]ok={0};GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0){GLES20.glDeleteProgram(program);program=0;return;}GLES20.glDeleteShader(v);GLES20.glDeleteShader(f);positionHandle=GLES20.glGetAttribLocation(program,"aPosition");normalHandle=GLES20.glGetAttribLocation(program,"aNormal");mvpHandle=GLES20.glGetUniformLocation(program,"uMvp");}
        private static int compile(int type,String src){int s=GLES20.glCreateShader(type);if(s==0)return 0;GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);int[]ok={0};GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0){GLES20.glDeleteShader(s);return 0;}return s;}
        void draw(float[] mvp){ensureProgram();if(program==0||data==null)return;GLES20.glUseProgram(program);int stride=24;data.position(0);GLES20.glEnableVertexAttribArray(positionHandle);GLES20.glVertexAttribPointer(positionHandle,3,GLES20.GL_FLOAT,false,stride,data);data.position(3);GLES20.glEnableVertexAttribArray(normalHandle);GLES20.glVertexAttribPointer(normalHandle,3,GLES20.GL_FLOAT,false,stride,data);GLES20.glUniformMatrix4fv(mvpHandle,1,false,mvp,0);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertexCount);GLES20.glDisableVertexAttribArray(positionHandle);GLES20.glDisableVertexAttribArray(normalHandle);}
        private void build(){float[]p;switch(sides){case 4:p=tetra();break;case 6:p=cube();break;case 8:p=octa();break;case 10:p=bipyramid(5);break;case 12:p=icosa();break;case 100:p=sphere();break;default:p=icosa();}ArrayList<Float>o=new ArrayList<>();for(int i=0;i<p.length;i+=9){float ax=p[i],ay=p[i+1],az=p[i+2],bx=p[i+3],by=p[i+4],bz=p[i+5],cx=p[i+6],cy=p[i+7],cz=p[i+8];float ux=bx-ax,uy=by-ay,uz=bz-az,vx=cx-ax,vy=cy-ay,vz=cz-az,nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(len<.0001f){nx=0;ny=0;nz=1;len=1;}nx/=len;ny/=len;nz/=len;add(o,ax,ay,az,nx,ny,nz);add(o,bx,by,bz,nx,ny,nz);add(o,cx,cy,cz,nx,ny,nz);}float[]a=new float[o.size()];for(int i=0;i<a.length;i++)a[i]=o.get(i);vertexCount=a.length/6;data=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();data.put(a).position(0);}
        private static void add(ArrayList<Float>o,float x,float y,float z,float nx,float ny,float nz){o.add(x*.82f);o.add(y*.82f);o.add(z*.82f);o.add(nx);o.add(ny);o.add(nz);}
        private static void tri(ArrayList<Float>o,float[]a,float[]b,float[]c){o.add(a[0]);o.add(a[1]);o.add(a[2]);o.add(b[0]);o.add(b[1]);o.add(b[2]);o.add(c[0]);o.add(c[1]);o.add(c[2]);}
        private static float[] from(ArrayList<Float>o){float[]a=new float[o.size()];for(int i=0;i<a.length;i++)a[i]=o.get(i);return a;}
        private static float[] faces(float[][]v,int[][]f){ArrayList<Float>o=new ArrayList<>();for(int[]q:f)tri(o,v[q[0]],v[q[1]],v[q[2]]);return from(o);}
        private static float[] tetra(){float[][]v={{1,1,1},{1,-1,-1},{-1,1,-1},{-1,-1,1}};int[][]f={{0,1,2},{0,3,1},{0,2,3},{1,3,2}};return faces(v,f);}
        private static float[] cube(){float a=1;float[][]v={{-a,-a,-a},{a,-a,-a},{a,a,-a},{-a,a,-a},{-a,-a,a},{a,-a,a},{a,a,a},{-a,a,a}};int[][]f={{0,1,2},{0,2,3},{4,6,5},{4,7,6},{0,4,5},{0,5,1},{3,2,6},{3,6,7},{1,5,6},{1,6,2},{0,3,7},{0,7,4}};return faces(v,f);}
        private static float[] octa(){float[][]v={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};int[][]f={{0,2,4},{2,1,4},{1,3,4},{3,0,4},{2,0,5},{1,2,5},{3,1,5},{0,3,5}};return faces(v,f);}
        private static float[] bipyramid(int n){ArrayList<Float>o=new ArrayList<>();float r=.95f;for(int i=0;i<n;i++){double a=i*2*Math.PI/n,b=(i+1)*2*Math.PI/n;float[]p={0,1.25f,0},q={(float)(r*Math.cos(a)),0,(float)(r*Math.sin(a))},s={(float)(r*Math.cos(b)),0,(float)(r*Math.sin(b))},d={0,-1.25f,0};tri(o,p,q,s);tri(o,d,s,q);}return from(o);}
        private static float[] icosa(){float t=(float)((1+Math.sqrt(5))/2);float[][]v={{-1,t,0},{1,t,0},{-1,-t,0},{1,-t,0},{0,-1,t},{0,1,t},{0,-1,-t},{0,1,-t},{t,0,-1},{t,0,1},{-t,0,-1},{-t,0,1}};int[][]f={{0,11,5},{0,5,1},{0,1,7},{0,7,10},{0,10,11},{1,5,9},{5,11,4},{11,10,2},{10,7,6},{7,1,8},{3,9,4},{3,4,2},{3,2,6},{3,6,8},{3,8,9},{4,9,5},{2,4,11},{6,2,10},{8,6,7},{9,8,1}};return faces(v,f);}
        private static float[] sphere(){ArrayList<Float>o=new ArrayList<>();int rings=9,segments=18;for(int r=0;r<rings-1;r++){double la=-Math.PI/2+Math.PI*r/(rings-1),lb=-Math.PI/2+Math.PI*(r+1)/(rings-1);for(int s=0;s<segments;s++){double a=2*Math.PI*s/segments,b=2*Math.PI*(s+1)/segments;float[]p={(float)(Math.cos(la)*Math.cos(a)),(float)Math.sin(la),(float)(Math.cos(la)*Math.sin(a))},q={(float)(Math.cos(lb)*Math.cos(a)),(float)Math.sin(lb),(float)(Math.cos(lb)*Math.sin(a))},r1={(float)(Math.cos(lb)*Math.cos(b)),(float)Math.sin(lb),(float)(Math.cos(lb)*Math.sin(b))},s1={(float)(Math.cos(la)*Math.cos(b)),(float)Math.sin(la),(float)(Math.cos(la)*Math.sin(b))};tri(o,p,q,r1);tri(o,p,r1,s1);}}return from(o);}
    }
}
