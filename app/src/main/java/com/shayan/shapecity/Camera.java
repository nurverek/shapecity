package com.shayan.shapecity;

import android.opengl.Matrix;
import android.util.Log;

import com.nurverek.firestorm.FSControl;
import com.nurverek.firestorm.FSRenderer;
import com.nurverek.firestorm.FSViewConfig;
import com.nurverek.vanguard.VLTask;
import com.nurverek.vanguard.VLTaskContinous;
import com.nurverek.vanguard.VLVControl;
import com.nurverek.vanguard.VLVCurved;
import com.nurverek.vanguard.VLVLinear;
import com.nurverek.vanguard.VLVManager;
import com.nurverek.vanguard.VLVRunner;
import com.nurverek.vanguard.VLVRunnerEntry;
import com.nurverek.vanguard.VLVTypeManager;
import com.nurverek.vanguard.VLVariable;

public final class Camera{

    private static VLVLinear controlcamera;
    private static VLVRunner control;

    public static void initialize(Gen gen){
//        rotateCamera(gen);
    }

    public static void rotateCamera(Gen gen){
        control = new VLVRunner(2, 0);

        controlcamera = new VLVLinear(0, 360, 3000, VLVariable.LOOP_FORWARD, new VLTaskContinous(new VLTask.Task<VLVLinear>(){

            private float[] cache = new float[16];

            @Override
            public void run(VLTask<VLVLinear> task, VLVLinear var){
                FSViewConfig c = FSControl.getViewConfig();

//                c.eyePosition(0F, 5F, 5F);
//                c.eyePosition(0F, 6000, 5000);
//                c.eyePosition(0F, 1000, 1000);

                float[] eyepos = c.eyePosition().provider();

                Matrix.setIdentityM(cache, 0);
                Matrix.rotateM(cache, 0, var.get(), 0f, 1f, 0f);
                Matrix.multiplyMV(eyepos, 0, cache, 0, eyepos, 0);

                c.eyePositionDivideByW();
                c.lookAt(0f, 2.5f, 0f, 0f, 1f, 0f);
                c.updateViewProjection();
            }
        }));

        control.add(new VLVRunnerEntry(controlcamera, 0));
        control.start();

        FSRenderer.getControlManager().add(control);
    }
}
