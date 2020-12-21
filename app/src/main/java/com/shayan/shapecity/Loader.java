package com.nurverek.firestorm;

import android.opengl.GLES32;
import android.util.Log;

import com.nurverek.vanguard.VLArrayFloat;
import com.nurverek.vanguard.VLBufferFloat;
import com.nurverek.vanguard.VLBufferShort;
import com.nurverek.vanguard.VLFloat;
import com.nurverek.vanguard.VLInt;
import com.nurverek.vanguard.VLListType;
import com.shayan.shapecity.Animations;
import com.shayan.shapecity.Game;
import com.shayan.shapecity.ModColor;
import com.shayan.shapecity.ModDepthMap;
import com.shayan.shapecity.ModLight;
import com.shayan.shapecity.ModModel;
import com.shayan.shapecity.ModNoLight;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

public final class Loader extends FSG{

    //        7 	1.0 	0.7 	1.8
    //        13 	1.0 	0.35 	0.44
    //        20 	1.0 	0.22 	0.20
    //        32 	1.0 	0.14 	0.07
    //        50 	1.0 	0.09 	0.032
    //        65 	1.0 	0.07 	0.017
    //        100 	1.0 	0.045 	0.0075
    //        160 	1.0 	0.027 	0.0028
    //        200 	1.0 	0.022 	0.0019
    //        325 	1.0 	0.014 	0.0007
    //        600 	1.0 	0.007 	0.0002
    //        3250 	1.0 	0.0014 	0.000007

    private static final int DEBUG_MODE_AUTOMATOR = FSControl.DEBUG_DISABLED;
    private static final int DEBUG_MODE_PROGRAMS = FSControl.DEBUG_DISABLED;

    private static final FSLightMaterial MATERIAL_GOLD = new FSLightMaterial(new VLArrayFloat(new float[]{ 0.24725f, 0.1995f, 0.0745f }), new VLArrayFloat(new float[]{ 0.75164f, 0.60648f, 0.22648f }), new VLArrayFloat(new float[]{ 0.628281f, 0.555802f, 0.366065f }), new VLFloat(16));
    private static final FSLightMaterial MATERIAL_OBSIDIAN = new FSLightMaterial(new VLArrayFloat(new float[]{ 0.05375f, 0.05f, 0.06625f }), new VLArrayFloat(new float[]{ 0.18275f, 0.17f, 0.22525f }), new VLArrayFloat(new float[]{ 0.332741f, 0.328634f, 0.346435f }), new VLFloat(16));
    private static final FSLightMaterial MATERIAL_WHITE_RUBBER = new FSLightMaterial(new VLArrayFloat(new float[]{ 0.05f, 0.05f, 0.05f }), new VLArrayFloat(new float[]{ 0.5f, 0.5f, 0.5f }), new VLArrayFloat(new float[]{ 0.7f, 0.7f, 0.7f }), new VLFloat(16));

    private static final int SHADOW_PROGRAMSET = 0;
    private static final int MAIN_PROGRAMSET = 1;
    private static final int SHADOWMAP_ORTHO_DIAMETER = 4;
    private static final int SHADOWMAP_ORTHO_NEAR = 1;
    private static final int SHADOWMAP_ORTHO_FAR = 1500;

    private static final VLInt SHADOW_POINT_PCF_SAMPLES = new VLInt(20);
    private static final FSBrightness BRIGHTNESS = new FSBrightness(new VLFloat(2f));
    private static final FSGamma GAMMA_DIRECT = new FSGamma(new VLFloat(2f));
    private static final FSGamma GAMMA_POINT = new FSGamma(new VLFloat(1.5f));

    private static int UBOBINDPOINT = 0;
    public static int TEXUNIT = 1;

    public static FSMesh layer1;
    public static FSMesh layer2;
    public static FSMesh layer3;

    public static FSMesh[] layers;

    private static FSP programCenterDepth;
    private static FSP programCenter;
    private static FSP programLayersDepth;
    private static FSP programLayers;
    private static FSP programBaseDepth;
    private static FSP programBase;
    private static FSP programPillarsDepth;
    private static FSP programPillars;

    private static int BUFFER_ELEMENT_SHORT_DEFAULT;
    private static int BUFFER_ARRAY_FLOAT_DEFAULT;

    public static final int LAYER_INSTANCE_COUNT = 24;
    public static final int PILLAR_INSTANCE_COUNT = 252;

    public static FSLightDirect lightDirect;
    public static FSLightPoint lightPoint;

    public static FSShadowDirect shadowDirect;
    public static FSShadowPoint shadowPoint;

    public Loader(){
        super(2, 50, 10);
    }

    private void addBasics(){
        programCenterDepth = new FSP(DEBUG_MODE_PROGRAMS);
        programLayersDepth = new FSP(DEBUG_MODE_PROGRAMS);
        programBaseDepth = new FSP(DEBUG_MODE_PROGRAMS);
        programPillarsDepth = new FSP(DEBUG_MODE_PROGRAMS);

        programCenter = new FSP(DEBUG_MODE_PROGRAMS);
        programLayers = new FSP(DEBUG_MODE_PROGRAMS);
        programBase = new FSP(DEBUG_MODE_PROGRAMS);
        programPillars = new FSP(DEBUG_MODE_PROGRAMS);

        lightPoint = new FSLightPoint(new FSAttenuation(new VLFloat(1.0F), new VLFloat(0.007F), new VLFloat(0.0002F)), new VLArrayFloat(new float[]{ 0F, 15F, -15F, 1.0F }));
        lightDirect = new FSLightDirect(new VLArrayFloat(new float[]{ 0F, 400F, 600F, 1.0F }), new VLArrayFloat(new float[]{ 0F, 0F, 0F, 1.0F }));

        shadowPoint = new FSShadowPoint(lightPoint, new VLInt(1024), new VLInt(1024), new VLFloat(0.005F), new VLFloat(0.005F), new VLFloat(1.1F), new VLFloat(1F), new VLFloat(50));
        shadowDirect = new FSShadowDirect(lightDirect, new VLInt(2048), new VLInt(2048), new VLFloat(0.0001F), new VLFloat(0.01F), new VLFloat(1.2F));

        shadowPoint.initialize(new VLInt(TEXUNIT++));
        shadowDirect.initialize(new VLInt(TEXUNIT++));

        BUFFER_ELEMENT_SHORT_DEFAULT = BUFFERMANAGER.add(new FSBufferManager.EntryShort(new FSVertexBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, GLES32.GL_STATIC_DRAW), new VLBufferShort()));
        BUFFER_ARRAY_FLOAT_DEFAULT = BUFFERMANAGER.add(new FSBufferManager.EntryFloat(new FSVertexBuffer(GLES32.GL_ARRAY_BUFFER, GLES32.GL_STATIC_DRAW), new VLBufferFloat()));
    }

    @Override
    public void assemble(FSActivity act){
        try{
            constructAutomator(act.getAssets().open("meshes.fsm"), ByteOrder.LITTLE_ENDIAN, true, 300);

        }catch(Exception ex){
            throw new RuntimeException(ex.getMessage());
        }

        addBasics();
        Game.initialize();

        registerCenter();
        registerLayers();
        registerBase();
        registerPillars();

        AUTOMATOR.build(DEBUG_MODE_AUTOMATOR);

        createLinks();
        fillLayouts();

        AUTOMATOR.buffer(DEBUG_MODE_AUTOMATOR);

        setupPrograms();

        AUTOMATOR.program(DEBUG_MODE_AUTOMATOR);

        postFullSetup();
        Game.startGame(this);
    }

    @Override
    public void update(int passindex, int programsetindex){
        BUFFERMANAGER.updateIfNeeded();
    }

    private void createLinks(){
        VLListType<FSLinkType> links1 = new VLListType<>(1, 0);
        VLListType<FSLinkType> links2 = new VLListType<>(1, 0);
        VLListType<FSLinkType> links3 = new VLListType<>(1, 0);

        float[] array1 = new float[LAYER_INSTANCE_COUNT];
        float[] array2 = new float[LAYER_INSTANCE_COUNT];
        float[] array3 = new float[LAYER_INSTANCE_COUNT];

        Arrays.fill(array1, Animations.TEXCONTROL_IDLE);
        Arrays.fill(array2, Animations.TEXCONTROL_IDLE);
        Arrays.fill(array3, Animations.TEXCONTROL_IDLE);

        links1.add(new ModColor.TextureControlLink(new VLArrayFloat(array1)));
        links2.add(new ModColor.TextureControlLink(new VLArrayFloat(array2)));
        links3.add(new ModColor.TextureControlLink(new VLArrayFloat(array3)));

        layer1.initLinks(links1);
        layer2.initLinks(links2);
        layer3.initLinks(links3);

        center.initLinks(new VLListType<>(0, 0));
    }

    private void fillLayouts(){
        FSBufferLayout layout;

        for(int i = 0; i < layerlayouts.length; i++){
            layout = layerlayouts[i];

            int modelbuffer = BUFFERMANAGER.add(new FSBufferManager.EntryFloat(new FSVertexBuffer(GLES32.GL_UNIFORM_BUFFER, GLES32.GL_DYNAMIC_DRAW, UBOBINDPOINT++), new VLBufferFloat()));
            int texcontrolbuffer = BUFFERMANAGER.add(new FSBufferManager.EntryFloat(new FSVertexBuffer(GLES32.GL_UNIFORM_BUFFER, GLES32.GL_DYNAMIC_DRAW, UBOBINDPOINT++), new VLBufferFloat()));
            int colorbuffer = BUFFERMANAGER.add(new FSBufferManager.EntryFloat(new FSVertexBuffer(GLES32.GL_UNIFORM_BUFFER, GLES32.GL_DYNAMIC_DRAW, UBOBINDPOINT++), new VLBufferFloat()));

            layout.add(BUFFERMANAGER, modelbuffer, 1)
                    .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_SEQUENTIAL_INSTANCED, ELEMENT_MODEL));

            layout.add(BUFFERMANAGER, colorbuffer, 1)
                    .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_SEQUENTIAL_INSTANCED, ELEMENT_COLOR));

            layout.add(BUFFERMANAGER, BUFFER_ARRAY_FLOAT_DEFAULT, 3)
                    .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, ELEMENT_POSITION))
                    .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, ELEMENT_TEXCOORD))
                    .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, ELEMENT_NORMAL));

            layout.add(BUFFERMANAGER, BUFFER_ELEMENT_SHORT_DEFAULT, 1)
                    .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_SEQUENTIAL_INDICES, ELEMENT_INDEX));

            layout.add(BUFFERMANAGER, texcontrolbuffer, 1)
                    .addLink(new FSBufferLayout.EntryLink(FSBufferLayout.LINK_SEQUENTIAL_SINGULAR, 0, 0, 1, 1, 4));
        }

        centerlayout.add(BUFFERMANAGER, BUFFER_ARRAY_FLOAT_DEFAULT, 3)
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, ELEMENT_POSITION))
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, ELEMENT_TEXCOORD))
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, ELEMENT_NORMAL));

        centerlayout.add(BUFFERMANAGER, BUFFER_ELEMENT_SHORT_DEFAULT, 1)
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_SEQUENTIAL_INDICES, ELEMENT_INDEX));

        int modelbuffer = BUFFERMANAGER.add(new FSBufferManager.EntryFloat(new FSVertexBuffer(GLES32.GL_UNIFORM_BUFFER, GLES32.GL_DYNAMIC_DRAW, UBOBINDPOINT++), new VLBufferFloat()));
        int colorbuffer = BUFFERMANAGER.add(new FSBufferManager.EntryFloat(new FSVertexBuffer(GLES32.GL_UNIFORM_BUFFER, GLES32.GL_DYNAMIC_DRAW, UBOBINDPOINT++), new VLBufferFloat()));

        pillarslayout.add(BUFFERMANAGER, modelbuffer, 1)
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_SEQUENTIAL_INSTANCED, ELEMENT_MODEL));

        pillarslayout.add(BUFFERMANAGER, colorbuffer, 1)
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_SEQUENTIAL_INSTANCED, ELEMENT_COLOR));

        pillarslayout.add(BUFFERMANAGER, BUFFER_ARRAY_FLOAT_DEFAULT, 2)
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, ELEMENT_POSITION))
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, ELEMENT_NORMAL));

        pillarslayout.add(BUFFERMANAGER, BUFFER_ELEMENT_SHORT_DEFAULT, 1)
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_SEQUENTIAL_INDICES, ELEMENT_INDEX));

        baselayout.add(BUFFERMANAGER, BUFFER_ARRAY_FLOAT_DEFAULT, 2)
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, ELEMENT_POSITION))
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, ELEMENT_NORMAL));

        baselayout.add(BUFFERMANAGER, BUFFER_ELEMENT_SHORT_DEFAULT, 1)
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_SEQUENTIAL_INDICES, ELEMENT_INDEX));
    }

    private void setupPrograms(){
        FSP.Modifier moddepthinitpoint = new ModDepthMap.Prepare(shadowPoint.frameBuffer(), shadowPoint.width(), shadowPoint.height(), true);
        FSP.Modifier moddepthsetuppoint = new ModDepthMap.SetupPoint(shadowPoint, FSShadowPoint.SELECT_LIGHT_TRANSFORMS, lightPoint.position(), shadowPoint.zFar());
        FSP.Modifier moddepthfinishpoint = new ModDepthMap.Finish(shadowPoint.frameBuffer());

        FSP.Modifier moddepthinitdirect = new ModDepthMap.Prepare(shadowDirect.frameBuffer(), shadowDirect.width(), shadowDirect.height(), true);
        FSP.Modifier moddepthsetupdirect = new ModDepthMap.SetupDirect(shadowDirect.lightViewProjection());
        FSP.Modifier moddepthfinishdirect = new ModDepthMap.Finish(shadowDirect.frameBuffer());

        FSP.Modifier modlightpoint = new ModLight.Point(GAMMA_POINT, null, BRIGHTNESS, lightPoint, shadowPoint, MATERIAL_WHITE_RUBBER.getGLSLSize());
        FSP.Modifier modlightdirect = new ModLight.Direct(GAMMA_DIRECT, BRIGHTNESS, lightDirect, shadowDirect, MATERIAL_WHITE_RUBBER.getGLSLSize());

        FSP.Modifier modmodeluniform = new ModModel.Uniform();
        FSP.Modifier modmodelubo = new ModModel.UBO(1, LAYER_INSTANCE_COUNT);
        FSP.Modifier modmodelubo2 = new ModModel.UBO(1, PILLAR_INSTANCE_COUNT);
        FSP.Modifier modcolorlayers = new ModColor.TextureAndUBO(1, LAYER_INSTANCE_COUNT, true, false, true);
        FSP.Modifier modcolortex = new ModColor.Texture(false,false,1, false);
        FSP.Modifier modcoloruniform = new ModColor.Uniform();
        FSP.Modifier modcolorpillars = new ModColor.UBO(1, PILLAR_INSTANCE_COUNT);

        FSConfig draw = new FSP.DrawElements(FSConfig.POLICY_ALWAYS, 0);
        FSConfig drawinstanced = new FSP.DrawElementsInstanced(FSConfig.POLICY_ALWAYS, 0);

        programCenterDepth.modify(moddepthinitpoint, FSConfig.POLICY_ALWAYS);
        programCenterDepth.modify(modmodeluniform, FSConfig.POLICY_ALWAYS);
        programCenterDepth.modify(moddepthsetuppoint, FSConfig.POLICY_ALWAYS);
        programCenterDepth.addMeshConfig(draw);
        programCenterDepth.build();

        programSet(SHADOW_PROGRAMSET).add(programCenterDepth);

        programLayersDepth.modify(modmodelubo, FSConfig.POLICY_ALWAYS);
        programLayersDepth.modify(moddepthsetuppoint, FSConfig.POLICY_ALWAYS);
        programLayersDepth.modify(moddepthfinishpoint, FSConfig.POLICY_ALWAYS);
        programLayersDepth.addMeshConfig(drawinstanced);
        programLayersDepth.build();

        programSet(SHADOW_PROGRAMSET).add(programLayersDepth);

        programBaseDepth.modify(moddepthinitdirect, FSConfig.POLICY_ALWAYS);
        programBaseDepth.modify(modmodeluniform, FSConfig.POLICY_ALWAYS);
        programBaseDepth.modify(moddepthsetupdirect, FSConfig.POLICY_ALWAYS);
        programBaseDepth.addMeshConfig(draw);
        programBaseDepth.build();

        programSet(SHADOW_PROGRAMSET).add(programBaseDepth);

        programPillarsDepth.modify(modmodelubo2, FSConfig.POLICY_ALWAYS);
        programPillarsDepth.modify(moddepthsetupdirect, FSConfig.POLICY_ALWAYS);
        programPillarsDepth.modify(moddepthfinishdirect, FSConfig.POLICY_ALWAYS);
        programPillarsDepth.addMeshConfig(drawinstanced);
        programPillarsDepth.build();

        programSet(SHADOW_PROGRAMSET).add(programPillarsDepth);

        programCenter.modify(modmodeluniform, FSConfig.POLICY_ALWAYS);
        programCenter.modify(modcolortex, FSConfig.POLICY_ALWAYS);
        programCenter.modify(modlightpoint, FSConfig.POLICY_ALWAYS);
        programCenter.addMeshConfig(draw);
        programCenter.build();

        programSet(MAIN_PROGRAMSET).add(programCenter);

        programLayers.modify(modmodelubo, FSConfig.POLICY_ALWAYS);
        programLayers.modify(modcolorlayers, FSConfig.POLICY_ALWAYS);
        programLayers.modify(modlightpoint, FSConfig.POLICY_ALWAYS);
        programLayers.addMeshConfig(drawinstanced);
        programLayers.build();

        programSet(MAIN_PROGRAMSET).add(programLayers);

        programBase.modify(modmodeluniform, FSConfig.POLICY_ALWAYS);
        programBase.modify(modcoloruniform, FSConfig.POLICY_ALWAYS);
        programBase.modify(modlightdirect, FSConfig.POLICY_ALWAYS);
        programBase.addMeshConfig(draw);
        programBase.build();

        programSet(MAIN_PROGRAMSET).add(programBase);

        programPillars.modify(modmodelubo2, FSConfig.POLICY_ALWAYS);
        programPillars.modify(modcolorpillars, FSConfig.POLICY_ALWAYS);
        programPillars.modify(modlightdirect, FSConfig.POLICY_ALWAYS);
        programPillars.addMeshConfig(drawinstanced);
        programPillars.build();

        programSet(MAIN_PROGRAMSET).add(programPillars);
    }

    private void postFullSetup(){
        layers = new FSMesh[]{ layer1, layer2, layer3 };
    }

    @Override
    protected void destroyAssets(){
        shadowPoint.destroy();

        Game.destroy();
        Animations.destroy();
    }
}