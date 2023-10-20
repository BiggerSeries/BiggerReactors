package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.vk;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLongImmutablePair;
import it.unimi.dsi.fastutil.longs.LongLongPair;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;

import static net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.vk.VkUtil.checkVkResult;
import static org.lwjgl.vulkan.VK13.*;

@NonnullDefault
public class VkPipelines {
    
    private static final long reductionShaderModule;
    private static final long simShaderModule;
    private static final long simvec2ShaderModule;
    static final long descriptorLayout;
    static final LongLongPair pipelineLayouts;
    static final long reductionPipeline;
    private static final long baseSimPipeline;
    private static final LongArrayList simPipelines = new LongArrayList();
    
    // best sizes benchmarked on a 5800u iGPU
    private static final int[] zSizes = new int[]{
            0, // height 0 is invalid
            16, 16, 16, 16,
            16,  8,  8, 16,
            16,  8,  8,  8,
             8,  8,  8,  8,
             4,  4,  4,  4,
             4,  4,  4,  4,
             4,  4,  4,  4,
             4,  4,  4,  4,
             4,  4,  4,  4,
             4,  4,  4,  4,
             4,  4,  2,  2,
             2,  2,  2,  2,
             2,  2,  2,  2,
             2,  2,  2,  2,
             2,  2,  2,  2,
             2,  2,  2,  2,
    };
    
    static {
        reductionShaderModule = createShaderModule(VkUtil.reductionSPV);
        simShaderModule = createShaderModule(VkUtil.raySimSPV);
        simvec2ShaderModule = createShaderModule(VkUtil.raySimVec2SPV);
        descriptorLayout = createDescriptorLayout();
        pipelineLayouts = createPipelineLayouts();
        reductionPipeline = createReductionPipeline();
        baseSimPipeline = getSimPiplineForHeight(1);
    }
    
    public static void init() {
    
    }
    
    public static void terminate() {
        for (int i = 0; i < simPipelines.size(); i++) {
            vkDestroyPipeline(VkUtil.device, simPipelines.getLong(i), null);
        }
        if (testSize != 0) {
            vkDestroyPipeline(VkUtil.device, baseSimPipeline, null);
        }
        vkDestroyPipeline(VkUtil.device, reductionPipeline, null);
        vkDestroyPipelineLayout(VkUtil.device, pipelineLayouts.firstLong(), null);
        vkDestroyPipelineLayout(VkUtil.device, pipelineLayouts.secondLong(), null);
        vkDestroyDescriptorSetLayout(VkUtil.device, descriptorLayout, null);
        vkDestroyShaderModule(VkUtil.device, reductionShaderModule, null);
        vkDestroyShaderModule(VkUtil.device, simShaderModule, null);
        vkDestroyShaderModule(VkUtil.device, simvec2ShaderModule, null);
    }
    
    private static long createShaderModule(ByteBuffer code) {
        try (var stack = MemoryStack.stackPush()) {
            
            final var shaderModuleCreateInfo = VkShaderModuleCreateInfo.calloc(stack).sType$Default();
            shaderModuleCreateInfo.pCode(code);
            final var longPtr = stack.mallocLong(1);
            checkVkResult(vkCreateShaderModule(VkUtil.device, shaderModuleCreateInfo, null, longPtr));
            return longPtr.get(0);
        }
    }
    
    private static long createDescriptorLayout() {
        try (var stack = MemoryStack.stackPush()) {
            
            final var bindings = VkDescriptorSetLayoutBinding.calloc(7, stack);
            
            bindings.position(0);
            bindings.binding();
            bindings.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            bindings.descriptorCount(1);
            bindings.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
            
            for (int i = 1; i < 7; i++) {
                bindings.position(i);
                bindings.binding(i);
                bindings.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
                bindings.descriptorCount(1);
                bindings.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
            }
            bindings.position(0);
            
            
            final var createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default();
            final var longPtr = stack.callocLong(1);
            
            createInfo.pBindings(bindings);
            checkVkResult(vkCreateDescriptorSetLayout(VkUtil.device, createInfo, null, longPtr));
            return longPtr.get(0);
        }
    }
    
    private static LongLongPair createPipelineLayouts() {
        try (var stack = MemoryStack.stackPush()) {
            
            final var descriptorLayouts = stack.mallocLong(1);
            descriptorLayouts.put(0, descriptorLayout);
            
            final var raySimPushConstants = VkPushConstantRange.calloc(1, stack);
            raySimPushConstants.offset(0);
            raySimPushConstants.size(24);
            raySimPushConstants.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
            
            var raySimLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default();
            raySimLayoutCreateInfo.pPushConstantRanges(raySimPushConstants);
            raySimLayoutCreateInfo.pSetLayouts(descriptorLayouts);
            
            final var reductionPushConstants = VkPushConstantRange.calloc(1, stack);
            reductionPushConstants.offset(0);
            reductionPushConstants.size(4);
            reductionPushConstants.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
            
            var rayReductionLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default();
            rayReductionLayoutCreateInfo.pPushConstantRanges(reductionPushConstants);
            rayReductionLayoutCreateInfo.pSetLayouts(descriptorLayouts);
            
            var longPtr = stack.mallocLong(1);
            
            checkVkResult(vkCreatePipelineLayout(VkUtil.device, raySimLayoutCreateInfo, null, longPtr));
            long simLayout = longPtr.get(0);
            
            checkVkResult(vkCreatePipelineLayout(VkUtil.device, rayReductionLayoutCreateInfo, null, longPtr));
            long reductionLayout = longPtr.get(0);
            
            return new LongLongImmutablePair(simLayout, reductionLayout);
        }
    }
    
    private static long createReductionPipeline() {
        try (var stack = MemoryStack.stackPush()) {
            var createInfo = VkComputePipelineCreateInfo.calloc(1, stack).sType$Default();
            
            var stage = createInfo.stage().sType$Default();
            stage.stage(VK_SHADER_STAGE_COMPUTE_BIT);
            stage.module(reductionShaderModule);
            // this is dumb, LWJGL doesnt have any way to pass the pointer directly
            var name = MemoryUtil.memUTF8("main", true);
            stage.pName(name);
            
            createInfo.layout(pipelineLayouts.secondLong());
            
            var pipelineHandlePtr = stack.mallocLong(1);
            int returnCode = vkCreateComputePipelines(VkUtil.device, VK_NULL_HANDLE, createInfo, null, pipelineHandlePtr);
            MemoryUtil.memFree(name);
            checkVkResult(returnCode);
            return pipelineHandlePtr.get(0);
        }
    }
    
    public static int testSize = 0;
    
    public static int zSizeForHeight(int height) {
        if(height > 1024){
            throw new IllegalArgumentException("Heights above 1024 not supported");
        }
        int requestedSize = requestedZSizeForHeight(height);
        while (requestedSize * height > 1024){
            requestedSize >>= 1;
        }
        return requestedSize;
    }
    
    private static int requestedZSizeForHeight(int height){
        if (testSize != 0) {
            return testSize;
        }
        if (height < zSizes.length) {
            return zSizes[height];
        }
        return 1;
    }
    
    public static long getSimPiplineForHeight(int height) {
        if (testSize == 0) {
            if (height < simPipelines.size()) {
                var pipeline = VkPipelines.simPipelines.getLong(height);
                if (pipeline != 0) {
                    return pipeline;
                }
            }
        }
        simPipelines.size(height + 1);
        
        try (var stack = MemoryStack.stackPush()) {
            var createInfo = VkComputePipelineCreateInfo.calloc(1, stack);
            createInfo.sType$Default();
            if (height == 1) {
                createInfo.flags(VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT);
            } else {
                createInfo.flags(VK_PIPELINE_CREATE_DERIVATIVE_BIT);
                createInfo.basePipelineHandle(baseSimPipeline);
                createInfo.basePipelineIndex(-1);
            }
            
            var stage = createInfo.stage().sType$Default();
            stage.stage(VK_SHADER_STAGE_COMPUTE_BIT);
//            stage.module((height & 1) == 0 ? simvec2ShaderModule : simShaderModule);
            stage.module(simShaderModule);
            // this is dumb, LWJGL doesnt have any way to pass the pointer directly
            var name = MemoryUtil.memUTF8("main", true);
            stage.pName(name);


//            final var localYSize = (height & 1) == 0 ? height >> 1 : height;
            final var localYSize = height;
            final var localZSize = zSizeForHeight(height);
            final var RayTTL = Config.CONFIG.Reactor.IrradiationDistance;
            final var RodAreaSize = (2 * RayTTL) + 1;
            final var RayPerBatch = Config.CONFIG.Reactor.SimulationRays / localZSize;
            final var MaxRaySteps = VkUtil.MAX_RAY_STEPS;
            
            var specializationData = stack.calloc(6 * 4); // its all uints
            var mapEntries = VkSpecializationMapEntry.calloc(5, stack);
            
            var specializationIntData = specializationData.asIntBuffer();
            specializationIntData.put(0, localYSize);
            specializationIntData.put(1, localZSize);
            specializationIntData.put(2, (int) RayTTL);
            specializationIntData.put(3, (int) RodAreaSize);
            specializationIntData.put(4, RayPerBatch);
            specializationIntData.put(5, (int) MaxRaySteps);
            
            for (int i = 0; i < mapEntries.limit(); i++) {
                mapEntries.position(i);
                mapEntries.constantID(i);
                mapEntries.offset(i * 4);
                mapEntries.size(4);
            }
            mapEntries.position(0);
            
            var specializationInfo = VkSpecializationInfo.calloc(stack);
            specializationInfo.pMapEntries(mapEntries);
            specializationInfo.pData(specializationData);
            stage.pSpecializationInfo(specializationInfo);
            
            createInfo.layout(pipelineLayouts.firstLong());
            
            var pipelineHandlePtr = stack.mallocLong(1);
            int returnCode = vkCreateComputePipelines(VkUtil.device, VK_NULL_HANDLE, createInfo, null, pipelineHandlePtr);
            MemoryUtil.memFree(name);
            checkVkResult(returnCode);
            long pipeline = pipelineHandlePtr.get(0);
            if (testSize == 0) {
                simPipelines.set(height, pipelineHandlePtr.get(0));
            }
            return pipeline;
        }
    }
}
