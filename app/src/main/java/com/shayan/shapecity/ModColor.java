package com.shayan.shapecity;

import com.nurverek.firestorm.FSBufferAddress;
import com.nurverek.firestorm.FSBufferManager;
import com.nurverek.firestorm.FSConfig;
import com.nurverek.firestorm.FSConfigLocated;
import com.nurverek.firestorm.FSG;
import com.nurverek.firestorm.FSLinkBufferedType;
import com.nurverek.firestorm.FSMesh;
import com.nurverek.firestorm.FSP;
import com.nurverek.firestorm.FSPMod;
import com.nurverek.firestorm.FSShader;
import com.nurverek.firestorm.FSVertexBuffer;
import com.nurverek.vanguard.VLArrayFloat;
import com.nurverek.vanguard.VLDebug;

public final class ModColor{

    private static final class SetupAttribute implements FSPMod{

        public SetupAttribute(){

        }

        @Override
        public void modify(FSP program){
            FSShader vertex = program.vertexShader();

            FSConfig colorattrib = new FSP.AttribPointer(FSConfig.POLICY_ALWAYS, FSG.ELEMENT_COLOR, 0);

            program.registerAttributeLocation(vertex, colorattrib);

            vertex.addAttribute(colorattrib.location(), "vec4", "colorin");
            vertex.addPipedOutputField("vec4", "colorvertex");
            vertex.addMainCode("colorvertex = colorin;");

            program.fragmentShader().addPipedInputField("vec4", "colorvertex");

            program.addSetupConfig(new FSP.AttribEnable(colorattrib.location()));
            program.addMeshConfig(colorattrib);
            program.addPostDrawConfig(new FSP.AttribDisable(colorattrib.location()));
        }
    }

    private static final class SetupUniform implements FSPMod{

        public SetupUniform(){

        }

        @Override
        public void modify(FSP program){
            FSShader fragment = program.fragmentShader();
            FSConfig color = new FSP.Uniform4fve(FSConfig.POLICY_ALWAYS, 0, FSG.ELEMENT_COLOR, 0, 1);

            program.registerUniformLocation(fragment, color);

            program.addMeshConfig(color);
            fragment.addUniform(color.location(), "vec4", "coloruni");
        }
    }

    private static final class SetupUBO implements FSPMod{

        private int segments;
        private int instancecount;

        public SetupUBO(int segments, int instancecount){
            this.segments = segments;
            this.instancecount = instancecount / segments;
        }

        @Override
        public void modify(FSP program){
            FSShader vertex = program.vertexShader();
            FSShader fragment = program.fragmentShader();

            if(segments == 1){
                program.addMeshConfig(new FSP.UniformBlockElement(FSConfig.POLICY_ALWAYS, FSG.ELEMENT_COLOR, "COLORS", 0));

                vertex.addUniformBlock("std140", "COLORS", "vec4 colors[" + instancecount + "]");
                vertex.addMainCode("colorubo = colors[gl_InstanceID];");

            }else if(segments == 2){
                program.addMeshConfig(new FSP.UniformBlockElement(FSConfig.POLICY_ALWAYS, FSG.ELEMENT_COLOR, "COLORS1", 0));
                program.addMeshConfig(new FSP.UniformBlockElement(FSConfig.POLICY_ALWAYS, FSG.ELEMENT_COLOR, "COLORS2", 1));

                vertex.addUniformBlock("std140", "COLORS1", "vec2 colors1[" + instancecount + "]");
                vertex.addUniformBlock("std140", "COLORS2", "vec2 colors2[" + instancecount + "]");

                vertex.addMainCode("colorubo = vec4(colors1[gl_InstanceID], colors2[gl_InstanceID]);");

            }else if(segments == 4){
                program.addMeshConfig(new FSP.UniformBlockElement(FSConfig.POLICY_ALWAYS, FSG.ELEMENT_COLOR, "COLORS1", 0));
                program.addMeshConfig(new FSP.UniformBlockElement(FSConfig.POLICY_ALWAYS, FSG.ELEMENT_COLOR, "COLORS2", 1));
                program.addMeshConfig(new FSP.UniformBlockElement(FSConfig.POLICY_ALWAYS, FSG.ELEMENT_COLOR, "COLORS3", 2));
                program.addMeshConfig(new FSP.UniformBlockElement(FSConfig.POLICY_ALWAYS, FSG.ELEMENT_COLOR, "COLORS4", 3));

                vertex.addUniformBlock("std140", "COLORS1", "vec1 colors1[" + instancecount + "]");
                vertex.addUniformBlock("std140", "COLORS2", "vec1 colors2[" + instancecount + "]");
                vertex.addUniformBlock("std140", "COLORS3", "vec1 colors3[" + instancecount + "]");
                vertex.addUniformBlock("std140", "COLORS4", "vec1 colors4[" + instancecount + "]");

                vertex.addMainCode("colorubo = vec4(colors1[gl_InstanceID], colors2[gl_InstanceID], colors3[gl_InstanceID], colors4[gl_InstanceID]);");

            }else{
                throw new RuntimeException("Invalid hints : segment[" + segments + "] instance-per-segment[" + instancecount + "].");
            }

            vertex.addPipedOutputField("vec4", "colorubo");
            fragment.addPipedInputField("vec4", "colorubo");
        }
    }

    private static final class SetupTexture implements FSPMod{

        private boolean instanced;
        private boolean instancedcoords;
        private boolean texturecontrol;
        private int instancecount;

        public SetupTexture(boolean instanced, boolean instancedcoords, int instancecount, boolean texturecontrol){
            this.instanced = instanced;
            this.instancedcoords = instancedcoords;
            this.instancecount = instancecount;
            this.texturecontrol = texturecontrol;
        }

        @Override
        public void modify(FSP program){
            FSShader vertex = program.vertexShader();
            FSShader fragment = program.fragmentShader();

            FSConfig unit = new FSP.TextureColorUnit(FSConfig.POLICY_ALWAYS);
            FSConfig bind = new FSP.TextureColorBind(FSConfig.POLICY_ALWAYS);
            FSConfig coords;

            if(instancedcoords && instanced){
                coords = new FSP.UniformBlockElement(FSG.ELEMENT_TEXCOORD, "TEXCOORDS", 0);

            }else{
                coords = new FSP.AttribPointer(FSConfig.POLICY_ALWAYS, FSG.ELEMENT_TEXCOORD, 0);

                program.registerAttributeLocation(vertex, coords);
                program.addSetupConfig(new FSP.AttribEnable(FSConfig.POLICY_ALWAYS, coords.location()));
                program.addPostDrawConfig(new FSP.AttribDisable(FSConfig.POLICY_ALWAYS, coords.location()));
            }

            if(texturecontrol){
                program.addMeshConfig(new TexControlConfig("TEXCONTROL"));
            }

            program.registerUniformLocation(fragment, unit);

            program.addMeshConfig(bind);
            program.addMeshConfig(unit);
            program.addMeshConfig(coords);

            if(instanced){
                vertex.addPipedOutputField("vec3", "vcolortexcoord");

                if(instancedcoords){
                    vertex.addUniformBlock("std140", "TEXCOORDS", new String[]{ "vec2 colortexcoords[" + instancecount + "]" });
                    vertex.addMainCode("vcolortexcoord = vec3(colortexcoords[gl_InstanceID], gl_InstanceID);");

                }else{
                    vertex.addAttribute(coords.location(), "vec2", "colortexcoords");
                    vertex.addMainCode("vcolortexcoord = vec3(colortexcoords, gl_InstanceID);");
                }

                fragment.addUniform(unit.location(), "mediump sampler2DArray", "colortexture");
                fragment.addPipedInputField("vec3", "vcolortexcoord");

            }else{
                vertex.addAttribute(coords.location(), "vec2", "colortexcoords");
                vertex.addPipedOutputField("vec2", "vcolortexcoord");
                vertex.addMainCode("vcolortexcoord = colortexcoords;");

                fragment.addUniform(unit.location(), "mediump sampler2D", "colortexture");
                fragment.addPipedInputField("vec2", "vcolortexcoord");
            }

            if(texturecontrol){
                vertex.addUniformBlock("std140", "TEXCONTROL", new String[]{ "float texcontrol[" + instancecount + "]" });
                vertex.addPipedOutputField("float", "vtexcontrol");
                vertex.addMainCode("vtexcontrol = texcontrol[gl_InstanceID];");

                fragment.addPipedInputField("float", "vtexcontrol");
                fragment.addMainCode("vec4 colortex = texture(colortexture, vcolortexcoord) * vtexcontrol;");

            }else{
                fragment.addMainCode("vec4 colortex = texture(colortexture, vcolortexcoord);");
            }
        }
    }

    public static final class Attribute implements FSPMod{

        private SetupAttribute setupattribute;

        public Attribute(){
            setupattribute = new SetupAttribute();
        }

        @Override
        public void modify(FSP program){
            program.modify(setupattribute, FSConfig.POLICY_ALWAYS);
            program.fragmentShader().addMainCode("vec4 vcolor = colorvertex;");
        }
    }

    public static final class UBO implements FSPMod{

        private SetupUBO setupubo;

        public UBO(int segments, int instancecount){
            setupubo = new SetupUBO(segments, instancecount);
        }

        @Override
        public void modify(FSP program){
            program.modify(setupubo, FSConfig.POLICY_ALWAYS);
            program.fragmentShader().addMainCode("vec4 vcolor = colorubo;");
        }
    }

    public static final class Uniform implements FSPMod{

        private SetupUniform setupuniform;

        public Uniform(){
            setupuniform = new SetupUniform();
        }

        @Override
        public void modify(FSP program){
            program.modify(setupuniform, FSConfig.POLICY_ALWAYS);
            program.fragmentShader().addMainCode("vec4 vcolor = coloruni;");
        }
    }

    public static final class Texture implements FSPMod{

        private SetupTexture setuptexture;

        public Texture(boolean instanced, boolean instancedcoords, int instancecount, boolean texturecontrol){
            setuptexture = new SetupTexture(instanced, instancedcoords, instancecount, texturecontrol);
        }

        @Override
        public void modify(FSP program){
            program.modify(setuptexture, FSConfig.POLICY_ALWAYS);
            program.fragmentShader().addMainCode("vec4 vcolor = colortex;");
        }
    }

    public static final class TextureAndUBO implements FSPMod{

        private SetupTexture setuptexture;
        private SetupUBO setupubo;

        public TextureAndUBO(int segments, int instancecount, boolean instancedtex, boolean instancedtexcoords, boolean texturecontrol){
            setuptexture = new SetupTexture(instancedtex, instancedtexcoords, instancecount, texturecontrol);
            setupubo = new SetupUBO(segments, instancecount);
        }

        @Override
        public void modify(FSP program){
            program.modify(setuptexture, FSConfig.POLICY_ALWAYS);
            program.modify(setupubo, FSConfig.POLICY_ALWAYS);

            program.fragmentShader().addMainCode("vec4 vcolor = colortex + colorubo;");
        }
    }

    public static final class TextureAndUniform implements FSPMod{

        private SetupTexture setuptexture;
        private SetupUniform setupuniform;

        public TextureAndUniform(boolean instancedtex, boolean instancedtexcoords, int instancecount, boolean texturecontrol){
            setuptexture = new SetupTexture(instancedtex, instancedtexcoords, instancecount, texturecontrol);
            setupuniform = new SetupUniform();
        }

        @Override
        public void modify(FSP program){
            program.modify(setuptexture, FSConfig.POLICY_ALWAYS);
            program.modify(setupuniform, FSConfig.POLICY_ALWAYS);

            program.fragmentShader().addMainCode("vec4 vcolor = colortex * coloruni;");
        }
    }

    public static final class UniformAndUBO implements FSPMod{

        private SetupUniform setupuniform;
        private SetupUBO setupubo;

        public UniformAndUBO(int segments, int instancecount){
            setupuniform = new SetupUniform();
            setupubo = new SetupUBO(segments, instancecount);
        }

        @Override
        public void modify(FSP program){
            program.modify(setupuniform, FSConfig.POLICY_ALWAYS);
            program.modify(setupubo, FSConfig.POLICY_ALWAYS);

            program.fragmentShader().addMainCode("vec4 vcolor = coloruni * colorubo;");
        }
    }

    public static final class Combined implements FSPMod{

        private SetupUniform setupuniform;
        private SetupUBO setupubo;
        private SetupTexture setuptexture;

        public Combined(int segments, int instancecount, boolean instancedtex, boolean instancedtexcoords, boolean texturecontrol){
            setupuniform = new SetupUniform();
            setupubo = new SetupUBO(segments, instancecount);
            setuptexture = new SetupTexture(instancedtex, instancedtexcoords, instancecount, texturecontrol);
        }

        @Override
        public void modify(FSP program){
            program.modify(setupuniform, FSConfig.POLICY_ALWAYS);
            program.modify(setupubo, FSConfig.POLICY_ALWAYS);
            program.modify(setuptexture, FSConfig.POLICY_ALWAYS);

            program.fragmentShader().addMainCode("vec4 vcolor = coloruni * colorubo * colortex;");
        }
    }

    public static final class TexControlConfig extends FSConfigLocated{

        private String name;

        public TexControlConfig(String name){
            this.name = name;
        }

        @Override
        public void programBuilt(FSP program){
            location(program.getUniformBlockIndex(name));
        }

        @Override
        public void configure(FSP program, FSMesh mesh, int meshindex, int passindex){
            FSVertexBuffer target = mesh.link(0).address().target().vertexBuffer();

            program.uniformBlockBinding(location, target.bindPoint());
            target.bindBufferBase();
        }

        @Override
        public int getGLSLSize(){
            return 0;
        }

        @Override
        public void debugInfo(FSP program, FSMesh mesh, int debug){
            super.debugInfo(program, mesh, debug);

            VLDebug.append(" name[");
            VLDebug.append(name);
            VLDebug.append("]");
        }
    }

    public static final class TextureControlLink extends FSLinkBufferedType<VLArrayFloat, FSBufferManager, FSBufferAddress>{

        public TextureControlLink(VLArrayFloat array){
            super(array);
        }

        @Override
        public void buffer(FSBufferManager buffer, int bufferindex){
            buffer.buffer(address, bufferindex, data);
        }

        @Override
        public void buffer(FSBufferManager buffer, int bufferindex, int arrayoffset, int arraycount, int unitoffset, int unitsize, int unitsubcount, int stride){
            buffer.bufferSync(address, bufferindex, data, arrayoffset, arraycount, unitoffset, unitsize, unitsubcount, stride);
        }

        @Override
        public int size(){
            return data.size();
        }
    }
}
