package com.shayan.shapecity;

import android.opengl.Matrix;
import android.util.Log;

import com.nurverek.firestorm.FSAttenuation;
import com.nurverek.firestorm.FSRenderer;
import com.nurverek.vanguard.VLTask;
import com.nurverek.vanguard.VLTaskContinous;
import com.nurverek.vanguard.VLVControl;
import com.nurverek.vanguard.VLVCurved;
import com.nurverek.vanguard.VLVRunner;
import com.nurverek.vanguard.VLVRunnerEntry;
import com.nurverek.vanguard.VLVariable;

public final class Light{

    private static final int CYCLES_PHASE_CHANGE = 200;
    private static final int CYCLES_DESCEND = 120;
    private static final int CYCLES_RADIATE_FOR_PUZZLE = 60;
    private static final VLVCurved.Curve CURVE_DEFAULT = VLVCurved.CURVE_ACC_DEC_COS;

    private static VLVRunner controllerpos;
    private static VLVRunner controllerradius;
    private static VLVRunner controllerotate;

    public static void initialize(Gen gen){
        controllerpos = new VLVRunner(10, 10);
        controllerradius = new VLVRunner(10, 10);
        controllerotate = new VLVRunner(10, 10);

        FSRenderer.getControlManager().add(controllerpos);
        FSRenderer.getControlManager().add(controllerradius);
        FSRenderer.getControlManager().add(controllerotate);
    }

    public static void descend(Gen gen){
        setPosition(gen, 0F, 0F, 0F);
        setRadius(gen, 10000F);

        move(gen, 0F, 0F, 0F, 20F, 0, CYCLES_DESCEND, CURVE_DEFAULT, null);
    }

    public static void setForPlatformRise(Gen gen){
        setPosition(gen,0F, 0F, 0F);
        setRadius(gen,20F);
    }

    public static void radiateForPuzzle(final Gen gen){
        move(gen, 0.1F, 1.5F, 0.1F, 1.5F, 0, CYCLES_RADIATE_FOR_PUZZLE, CURVE_DEFAULT, new Runnable(){

            @Override
            public void run(){
                rotate(gen,0F, 360F, 0F, 1F, 0F,0,300, VLVCurved.CURVE_LINEAR, VLVariable.LOOP_FORWARD);
            }
        });
    }

    public static void setPosition(Gen gen, float x, float y, float z){
        float[] pos = gen.light.position().provider();
        pos[0] = x;
        pos[1] = y;
        pos[2] = z;
    }

    public static void setRadius(Gen gen, float radius){
        ((FSAttenuation.Radius)gen.light.attenuation()).radius().set(radius);
    }

    public static void movePosition(final Gen gen, float x, float y, float z, int delay, int cycles, VLVCurved.Curve curve, final Runnable post){
        final float[] orgpos = gen.light.position().provider().clone();

        VLVCurved controlx = new VLVCurved(orgpos[0], x, cycles, VLVariable.LOOP_NONE, curve);
        VLVCurved controly = new VLVCurved(orgpos[1], y, cycles, VLVariable.LOOP_NONE, curve);
        VLVCurved controlz = new VLVCurved(orgpos[2], z, cycles, VLVariable.LOOP_NONE, curve);

        VLVControl update = new VLVControl(cycles, VLVariable.LOOP_NONE, new VLTaskContinous(new VLTask.Task(){

            @Override
            public void run(VLTask task, VLVariable var){
                setPosition(gen, controlx.get(), controly.get(), controlz.get());

                if(!var.active()){
                    controllerpos.clear();

                    if(post != null){
                        post.run();
                    }
                }
            }
        }));

        controllerpos.add(new VLVRunnerEntry(controlx, delay));
        controllerpos.add(new VLVRunnerEntry(controly, delay));
        controllerpos.add(new VLVRunnerEntry(controlz, delay));
        controllerpos.add(new VLVRunnerEntry(update, delay));
        controllerpos.start();
    }

    public static void moveRadius(final Gen gen, float radius, int delay, int cycles, VLVCurved.Curve curve, final Runnable post){
        float orgradius = ((FSAttenuation.Radius)gen.light.attenuation()).radius().get();
        VLVCurved controlradius = new VLVCurved(orgradius, radius, cycles, VLVariable.LOOP_NONE, curve);

        VLVControl update = new VLVControl(cycles, VLVariable.LOOP_NONE, new VLTaskContinous(new VLTask.Task(){

            @Override
            public void run(VLTask task, VLVariable var){
                setRadius(gen, controlradius.get());

                if(!var.active()){
                    controllerradius.clear();

                    if(post != null){
                        post.run();
                    }
                }
            }
        }));

        controllerradius.add(new VLVRunnerEntry(controlradius, delay));
        controllerradius.add(new VLVRunnerEntry(update, delay));
        controllerradius.start();
    }

    public static void move(final Gen gen, float x, float y, float z, float radius, int delay, int cycles, VLVCurved.Curve curve, final Runnable post){
        movePosition(gen, x, y, z, delay, cycles, curve, post);
        moveRadius(gen, radius, delay, cycles, curve, null);
    }

    public static void rotate(final Gen gen, float fromangle, float toangle, final float x, final float y, final float z, int delay, int cycles, VLVCurved.Curve curve, VLVariable.Loop loop){
        final float[] orgpos = gen.light.position().provider().clone();

        VLVCurved angle = new VLVCurved(fromangle, toangle, cycles, loop, curve, new VLTaskContinous(new VLTask.Task(){

            private float[] cache = new float[16];

            @Override
            public void run(VLTask task, VLVariable var){
                final float[] pos = gen.light.position().provider();

                Matrix.setIdentityM(cache, 0);
                Matrix.rotateM(cache, 0, var.get(), x, y, z);
                Matrix.multiplyMV(pos, 0, cache, 0, orgpos, 0);

                if(!var.active()){
                    controllerotate.clear();
                }
            }
        }));

        controllerotate.add(new VLVRunnerEntry(angle, delay));
        controllerotate.start();
    }

    public static void stopPosition(){
        controllerpos.clear();
    }

    public static void stopRadius(){
        controllerradius.clear();
    }

    public static void stopRotation(){
        controllerotate.clear();
    }

    public static void stop(){
        stopPosition();
        stopRadius();
        stopRotation();
    }
}
