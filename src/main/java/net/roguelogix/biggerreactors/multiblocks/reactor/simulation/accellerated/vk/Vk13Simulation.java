package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.vk;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationConfiguration;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationDescription;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.base.SimUtil;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.cpu.FullPassReactorSimulation;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import net.roguelogix.quartz.internal.util.PointerWrapper;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.vk.VkPools.allocCommandBuffer;
import static net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.vk.VkPools.freeCommandBufferNow;
import static net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.vk.VkUtil.*;
import static org.lwjgl.system.MemoryUtil.memGetAddress;
import static org.lwjgl.system.MemoryUtil.memPutLong;
import static org.lwjgl.vulkan.VK13.*;

@NonnullDefault
public class Vk13Simulation extends FullPassReactorSimulation {
    
    private final VkSemaphoreWaitInfo semaphoreWaitInfo = VkSemaphoreWaitInfo.create();
    private final LongBuffer semaphorePointer = BufferUtils.createLongBuffer(1);
    private final LongBuffer semaphoreWaitValuePointer = BufferUtils.createLongBuffer(1);
    private long nextWaitValue = 1;
    
    private final VkPools.DescriptorSet descriptorSet;
    
    private final PointerWrapper moderatorsWritePtr;
    private final PointerWrapper controlRodWritePtr;
    
    private final VkCommandBuffer executeCommandBuffer;
    private final PointerWrapper resultsReadPtr;
    
    public Vk13Simulation(SimulationDescription simulationDescription, SimulationConfiguration configuration) {
        super(simulationDescription, configuration);
        
        final var SCOPE_OBJECT = new Object();
        final var SCOPE_CLEANINGS = new ReferenceArrayList<Runnable>();
        final var THIS_CLEANINGS = new ReferenceArrayList<Runnable>();
        VK_CLEANER.register(SCOPE_OBJECT, () -> SCOPE_CLEANINGS.forEach(Runnable::run));
        VK_CLEANER.register(this, () -> THIS_CLEANINGS.forEach(Runnable::run));
        
        try (var stack = MemoryStack.stackPush()) {
            var setupCommandBuffer = allocCommandBuffer(true);
            SCOPE_CLEANINGS.add(() -> freeCommandBufferNow(setupCommandBuffer, true));
            var executeCommandBuffer = allocCommandBuffer(false);
            THIS_CLEANINGS.add(() -> freeCommandBufferNow(executeCommandBuffer, false));
            
            var semaphoreTypeCreateInfo = VkSemaphoreTypeCreateInfo.calloc(stack).sType$Default().semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE).initialValue(0);
            var semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack).sType$Default().pNext(semaphoreTypeCreateInfo);
            checkVkResult(vkCreateSemaphore(VkUtil.device, semaphoreCreateInfo, null, semaphorePointer));
            semaphoreWaitInfo.sType$Default();
            semaphoreWaitInfo.semaphoreCount(1);
            semaphoreWaitInfo.pSemaphores(semaphorePointer);
            semaphoreWaitInfo.pValues(semaphoreWaitValuePointer);
            final long semaphore = semaphorePointer.get(0);
            THIS_CLEANINGS.add(() -> vkDestroySemaphore(VkUtil.device, semaphore, null));
            
            descriptorSet = VkPools.allocDescriptorSet();
            
            var descriptorUniformWrites = VkDescriptorBufferInfo.calloc(1, stack);
            var descriptorBufferWrites = VkDescriptorBufferInfo.calloc(6, stack);
            var descriptorWrite = VkWriteDescriptorSet.calloc(2, stack);
            descriptorWrite.position(0).sType$Default();
            descriptorWrite.dstSet(descriptorSet.set());
            descriptorWrite.descriptorCount(6);
            descriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
            descriptorWrite.dstBinding(1);
            descriptorWrite.dstArrayElement(0);
            descriptorWrite.pBufferInfo(descriptorBufferWrites);
            descriptorWrite.position(1).sType$Default();
            descriptorWrite.dstSet(descriptorSet.set());
            descriptorWrite.descriptorCount(1);
            descriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            descriptorWrite.dstBinding(0);
            descriptorWrite.dstArrayElement(0);
            descriptorWrite.pBufferInfo(descriptorUniformWrites);
            descriptorWrite.position(0);
            
            try (var __ = stack.push()) {
                var beginInfo = VkCommandBufferBeginInfo.calloc(stack).sType$Default();
                beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
                vkBeginCommandBuffer(setupCommandBuffer, beginInfo);
                beginInfo.flags(0);
                vkBeginCommandBuffer(executeCommandBuffer, beginInfo);
            }
            
            {
                var initialCopyStartBarrier = VkMemoryBarrier.calloc(1, stack).sType$Default();
                initialCopyStartBarrier.srcAccessMask(VK_ACCESS_HOST_WRITE_BIT);
                initialCopyStartBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                vkCmdPipelineBarrier(setupCommandBuffer, VK_PIPELINE_STAGE_HOST_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, initialCopyStartBarrier, null, null);
            }
            
            var bufferCreateInfo = VkBufferCreateInfo.calloc(stack).sType$Default();
            bufferCreateInfo.flags(0);
            bufferCreateInfo.size(0);
            bufferCreateInfo.usage(0);
            bufferCreateInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            var bufferHandlePtr = stack.callocLong(1);
            var bufferMemoryRequirements = VkMemoryRequirements.calloc(stack);
            
            // read/write each tick buffers
            {
                // the +8 is FuelAbsorptionTemperatureCoefficient and InitialHardness
                final var moderatorsSize = (256 * 16L) + 16;
                bufferCreateInfo.size(moderatorsSize);
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long hostModeratorBuffer = bufferHandlePtr.get(0);
                THIS_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, hostModeratorBuffer, null));
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long gpuModeratorBuffer = bufferHandlePtr.get(0);
                THIS_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, gpuModeratorBuffer, null));
                
                vkGetBufferMemoryRequirements(VkUtil.device, hostModeratorBuffer, bufferMemoryRequirements);
                final var hostModeratorMemory = VkMemUtil.allocHost(bufferMemoryRequirements);
                vkBindBufferMemory(VkUtil.device, hostModeratorBuffer, hostModeratorMemory.vkMemoryHandle(), hostModeratorMemory.offset());
                THIS_CLEANINGS.add(() -> VkMemUtil.freeHost(hostModeratorMemory));
                
                vkGetBufferMemoryRequirements(VkUtil.device, gpuModeratorBuffer, bufferMemoryRequirements);
                final var gpuModeratorMemory = VkMemUtil.allocGPU(bufferMemoryRequirements);
                vkBindBufferMemory(VkUtil.device, gpuModeratorBuffer, gpuModeratorMemory.vkMemoryHandle(), gpuModeratorMemory.offset());
                THIS_CLEANINGS.add(() -> VkMemUtil.freeGPU(gpuModeratorMemory));
                
                descriptorUniformWrites.position(0);
                descriptorUniformWrites.buffer(gpuModeratorBuffer);
                descriptorUniformWrites.offset(0);
                descriptorUniformWrites.range(bufferCreateInfo.size());
                
                moderatorsWritePtr = hostModeratorMemory.hostPtr();
                final var moderatorCopySize = bufferCreateInfo.size();
                
                
                final var controlRodBufferSize = x * z * 8L;
                bufferCreateInfo.size(controlRodBufferSize);
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long hostRodBuffer = bufferHandlePtr.get(0);
                THIS_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, hostRodBuffer, null));
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long gpuRodBuffer = bufferHandlePtr.get(0);
                THIS_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, gpuRodBuffer, null));
                
                vkGetBufferMemoryRequirements(VkUtil.device, hostRodBuffer, bufferMemoryRequirements);
                final var hostRodMemory = VkMemUtil.allocHost(bufferMemoryRequirements);
                vkBindBufferMemory(VkUtil.device, hostRodBuffer, hostRodMemory.vkMemoryHandle(), hostRodMemory.offset());
                THIS_CLEANINGS.add(() -> VkMemUtil.freeHost(hostRodMemory));
                
                vkGetBufferMemoryRequirements(VkUtil.device, gpuRodBuffer, bufferMemoryRequirements);
                final var gpuRodMemory = VkMemUtil.allocGPU(bufferMemoryRequirements);
                vkBindBufferMemory(VkUtil.device, gpuRodBuffer, gpuRodMemory.vkMemoryHandle(), gpuRodMemory.offset());
                THIS_CLEANINGS.add(() -> VkMemUtil.freeGPU(gpuRodMemory));
                
                descriptorBufferWrites.position(0);
                descriptorBufferWrites.buffer(gpuRodBuffer);
                descriptorBufferWrites.offset(0);
                descriptorBufferWrites.range(bufferCreateInfo.size());
                
                controlRodWritePtr = hostRodMemory.hostPtr();
                final var rodCopySize = bufferCreateInfo.size();
                
                var copyToGPUBarrier = VkBufferMemoryBarrier.calloc(2, stack);
                copyToGPUBarrier.sType$Default();
                copyToGPUBarrier.buffer(hostModeratorBuffer);
                copyToGPUBarrier.size(moderatorsSize);
                copyToGPUBarrier.srcAccessMask(VK_ACCESS_HOST_WRITE_BIT);
                copyToGPUBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                copyToGPUBarrier.position(1);
                copyToGPUBarrier.sType$Default();
                copyToGPUBarrier.buffer(hostRodBuffer);
                copyToGPUBarrier.size(controlRodBufferSize);
                copyToGPUBarrier.srcAccessMask(VK_ACCESS_HOST_WRITE_BIT);
                copyToGPUBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                copyToGPUBarrier.position(0);
                vkCmdPipelineBarrier(executeCommandBuffer, VK_PIPELINE_STAGE_HOST_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, copyToGPUBarrier, null);
                
                var bufferCopy = VkBufferCopy.calloc(1, stack);
                bufferCopy.srcOffset(0);
                bufferCopy.dstOffset(0);
                
                bufferCopy.size(moderatorCopySize);
                vkCmdCopyBuffer(executeCommandBuffer, hostModeratorBuffer, gpuModeratorBuffer, bufferCopy);
                
                bufferCopy.size(rodCopySize);
                vkCmdCopyBuffer(executeCommandBuffer, hostRodBuffer, gpuRodBuffer, bufferCopy);
                
                copyToGPUBarrier.position(0);
                copyToGPUBarrier.buffer(gpuModeratorBuffer);
                copyToGPUBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                copyToGPUBarrier.dstAccessMask(VK_ACCESS_UNIFORM_READ_BIT | VK_ACCESS_SHADER_READ_BIT);
                copyToGPUBarrier.position(1);
                copyToGPUBarrier.buffer(gpuRodBuffer);
                copyToGPUBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                copyToGPUBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                copyToGPUBarrier.position(0);
                vkCmdPipelineBarrier(executeCommandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, 0, null, copyToGPUBarrier, null);
                
                
            }
            final Runnable recordCopyBack;
            {
                final var burnResultsBufferSize = controlRods.length * 16L;
                bufferCreateInfo.size(burnResultsBufferSize);
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long hostBuffer = bufferHandlePtr.get(0);
                THIS_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, hostBuffer, null));
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long gpuBuffer = bufferHandlePtr.get(0);
                THIS_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, gpuBuffer, null));
                
                vkGetBufferMemoryRequirements(VkUtil.device, hostBuffer, bufferMemoryRequirements);
                final var hostMemory = VkMemUtil.allocHost(bufferMemoryRequirements);
                vkBindBufferMemory(VkUtil.device, hostBuffer, hostMemory.vkMemoryHandle(), hostMemory.offset());
                THIS_CLEANINGS.add(() -> VkMemUtil.freeHost(hostMemory));
                
                vkGetBufferMemoryRequirements(VkUtil.device, gpuBuffer, bufferMemoryRequirements);
                final var gpuMemory = VkMemUtil.allocGPU(bufferMemoryRequirements);
                vkBindBufferMemory(VkUtil.device, gpuBuffer, gpuMemory.vkMemoryHandle(), gpuMemory.offset());
                THIS_CLEANINGS.add(() -> VkMemUtil.freeGPU(gpuMemory));
                
                var bufferCopy = VkBufferCopy.calloc(1, stack);
                bufferCopy.srcOffset(0);
                bufferCopy.dstOffset(0);
                bufferCopy.size(bufferCreateInfo.size());
                // recording this needs to be deferred until after the main compute execution is recorded
                recordCopyBack = () -> {
                    var copyBackBarrier = VkBufferMemoryBarrier.calloc(1, stack).sType$Default();
                    
                    copyBackBarrier.buffer(gpuBuffer);
                    copyBackBarrier.size(bufferCopy.size());
                    copyBackBarrier.srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT);
                    copyBackBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                    vkCmdPipelineBarrier(executeCommandBuffer, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, copyBackBarrier, null);
                    
                    vkCmdCopyBuffer(executeCommandBuffer, gpuBuffer, hostBuffer, bufferCopy);
                    
                    copyBackBarrier.buffer(hostBuffer);
                    copyBackBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                    copyBackBarrier.dstAccessMask(VK_ACCESS_HOST_READ_BIT);
                    vkCmdPipelineBarrier(executeCommandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_HOST_BIT, 0, null, copyBackBarrier, null);
                };
                
                descriptorBufferWrites.position(5);
                descriptorBufferWrites.buffer(gpuBuffer);
                descriptorBufferWrites.offset(0);
                descriptorBufferWrites.range(bufferCreateInfo.size());
                
                resultsReadPtr = hostMemory.hostPtr();
            }
            
            // write once buffers
            {
                final var totalControlRodInfoSize = controlRods.length * 4;
                bufferCreateInfo.size(totalControlRodInfoSize);
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long hostBuffer = bufferHandlePtr.get(0);
                SCOPE_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, hostBuffer, null));
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long gpuBuffer = bufferHandlePtr.get(0);
                THIS_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, gpuBuffer, null));
                
                vkGetBufferMemoryRequirements(VkUtil.device, hostBuffer, bufferMemoryRequirements);
                final var hostMemory = VkMemUtil.allocHost(bufferMemoryRequirements);
                SCOPE_CLEANINGS.add(() -> VkMemUtil.freeHost(hostMemory));
                vkBindBufferMemory(VkUtil.device, hostBuffer, hostMemory.vkMemoryHandle(), hostMemory.offset());
                
                vkGetBufferMemoryRequirements(VkUtil.device, gpuBuffer, bufferMemoryRequirements);
                final var gpuMemory = VkMemUtil.allocGPU(bufferMemoryRequirements);
                THIS_CLEANINGS.add(() -> VkMemUtil.freeGPU(gpuMemory));
                vkBindBufferMemory(VkUtil.device, gpuBuffer, gpuMemory.vkMemoryHandle(), gpuMemory.offset());
                
                descriptorBufferWrites.position(1);
                descriptorBufferWrites.buffer(gpuBuffer);
                descriptorBufferWrites.offset(0);
                descriptorBufferWrites.range(bufferCreateInfo.size());
                
                var bufferCopy = VkBufferCopy.calloc(1, stack);
                bufferCopy.srcOffset(0);
                bufferCopy.dstOffset(0);
                bufferCopy.size(bufferCreateInfo.size());
                vkCmdCopyBuffer(setupCommandBuffer, hostBuffer, gpuBuffer, bufferCopy);
                
                var hostControlRodPtr = hostMemory.hostPtr();
                
                for (int i = 0; i < controlRods.length; i++) {
                    hostControlRodPtr.putShortIdx(i * 2L, (short) controlRods[i].x);
                    hostControlRodPtr.putShortIdx(i * 2L + 1, (short) controlRods[i].z);
                }
            }
            
            {
                bufferCreateInfo.size((long) x * y * z);
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long hostBuffer = bufferHandlePtr.get(0);
                SCOPE_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, hostBuffer, null));
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long gpuBuffer = bufferHandlePtr.get(0);
                THIS_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, gpuBuffer, null));
                
                vkGetBufferMemoryRequirements(VkUtil.device, hostBuffer, bufferMemoryRequirements);
                final var hostMemory = VkMemUtil.allocHost(bufferMemoryRequirements);
                SCOPE_CLEANINGS.add(() -> VkMemUtil.freeHost(hostMemory));
                vkBindBufferMemory(VkUtil.device, hostBuffer, hostMemory.vkMemoryHandle(), hostMemory.offset());
                
                vkGetBufferMemoryRequirements(VkUtil.device, gpuBuffer, bufferMemoryRequirements);
                final var gpuMemory = VkMemUtil.allocGPU(bufferMemoryRequirements);
                THIS_CLEANINGS.add(() -> VkMemUtil.freeGPU(gpuMemory));
                vkBindBufferMemory(VkUtil.device, gpuBuffer, gpuMemory.vkMemoryHandle(), gpuMemory.offset());
                
                descriptorBufferWrites.position(2);
                descriptorBufferWrites.buffer(gpuBuffer);
                descriptorBufferWrites.offset(0);
                descriptorBufferWrites.range(bufferCreateInfo.size());
                
                var bufferCopy = VkBufferCopy.calloc(1, stack);
                bufferCopy.srcOffset(0);
                bufferCopy.dstOffset(0);
                bufferCopy.size(bufferCreateInfo.size());
                vkCmdCopyBuffer(setupCommandBuffer, hostBuffer, gpuBuffer, bufferCopy);
                
                var moderatorIndicesPtr = hostMemory.hostPtr();
                
                int volume = x * y * z;
                for (int i = 0; i < volume; i++) {
                    moderatorIndicesPtr.putByte(i, getModeratorIndex(i));
                }
            }
            
            {
                final var totalRayCount = SimUtil.rays.size();
                final var totalRaySize = totalRayCount * VkUtil.SIZEOF_RAY;
                bufferCreateInfo.size(totalRaySize);
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long hostBuffer = bufferHandlePtr.get(0);
                SCOPE_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, hostBuffer, null));
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long gpuBuffer = bufferHandlePtr.get(0);
                THIS_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, gpuBuffer, null));
                
                vkGetBufferMemoryRequirements(VkUtil.device, hostBuffer, bufferMemoryRequirements);
                final var hostMemory = VkMemUtil.allocHost(bufferMemoryRequirements);
                SCOPE_CLEANINGS.add(() -> VkMemUtil.freeHost(hostMemory));
                vkBindBufferMemory(VkUtil.device, hostBuffer, hostMemory.vkMemoryHandle(), hostMemory.offset());
                
                vkGetBufferMemoryRequirements(VkUtil.device, gpuBuffer, bufferMemoryRequirements);
                final var gpuMemory = VkMemUtil.allocGPU(bufferMemoryRequirements);
                THIS_CLEANINGS.add(() -> VkMemUtil.freeGPU(gpuMemory));
                vkBindBufferMemory(VkUtil.device, gpuBuffer, gpuMemory.vkMemoryHandle(), gpuMemory.offset());
                
                descriptorBufferWrites.position(3);
                descriptorBufferWrites.buffer(gpuBuffer);
                descriptorBufferWrites.offset(0);
                descriptorBufferWrites.range(bufferCreateInfo.size());
                
                var bufferCopy = VkBufferCopy.calloc(1, stack);
                bufferCopy.srcOffset(0);
                bufferCopy.dstOffset(0);
                bufferCopy.size(bufferCreateInfo.size());
                vkCmdCopyBuffer(setupCommandBuffer, hostBuffer, gpuBuffer, bufferCopy);
                
                var hostRayPointer = hostMemory.hostPtr();
                
                for (int i = 0; i < totalRayCount; i++) {
                    final var ray = SimUtil.rays.get(i);
                    final var rayOffset = i * 4 * MAX_RAY_STEPS;
                    for (int j = 0; j < ray.size(); j++) {
                        final var totalOffset = rayOffset + (j * 4L);
                        hostRayPointer.putFloatIdx(totalOffset, (float) ray.get(j).length);
                        hostRayPointer.putIntIdx(totalOffset + 1, ray.get(j).offset.x);
                        hostRayPointer.putIntIdx(totalOffset + 2, ray.get(j).offset.y);
                        hostRayPointer.putIntIdx(totalOffset + 3, ray.get(j).offset.z);
                    }
                }
            }
            
            // GPU only buffers
            final Runnable recordReductionBarrier;
            {
                final var burnResultsBufferSize = controlRods.length * y * VkPipelines.zSizeForHeight(y) * 16L;
                bufferCreateInfo.size(burnResultsBufferSize);
                
                bufferCreateInfo.usage(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
                checkVkResult(vkCreateBuffer(VkUtil.device, bufferCreateInfo, null, bufferHandlePtr));
                final long gpuBuffer = bufferHandlePtr.get(0);
                THIS_CLEANINGS.add(() -> vkDestroyBuffer(VkUtil.device, gpuBuffer, null));
                
                vkGetBufferMemoryRequirements(VkUtil.device, gpuBuffer, bufferMemoryRequirements);
                final var gpuMemory = VkMemUtil.allocGPU(bufferMemoryRequirements);
                vkBindBufferMemory(VkUtil.device, gpuBuffer, gpuMemory.vkMemoryHandle(), gpuMemory.offset());
                THIS_CLEANINGS.add(() -> VkMemUtil.freeGPU(gpuMemory));
                
                descriptorBufferWrites.position(4);
                descriptorBufferWrites.buffer(gpuBuffer);
                descriptorBufferWrites.offset(0);
                descriptorBufferWrites.range(bufferCreateInfo.size());
                
                recordReductionBarrier = () -> {
                    var stageToStageBarrier = VkBufferMemoryBarrier.calloc(1, stack).sType$Default();
                    stageToStageBarrier.buffer(gpuBuffer);
                    stageToStageBarrier.size(burnResultsBufferSize);
                    stageToStageBarrier.srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT);
                    stageToStageBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                    vkCmdPipelineBarrier(executeCommandBuffer, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, 0, null, stageToStageBarrier, null);
                };
            }
            
            
            {
                var initialCopyDoneBarrier = VkMemoryBarrier.calloc(1, stack).sType$Default();
                initialCopyDoneBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                initialCopyDoneBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                vkCmdPipelineBarrier(setupCommandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, 0, initialCopyDoneBarrier, null, null);
            }
            
            
            vkUpdateDescriptorSets(device, descriptorWrite, null);
            var simPipeline = VkPipelines.getSimPiplineForHeight(y);
            vkCmdBindPipeline(executeCommandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, simPipeline);
            vkCmdPushConstants(executeCommandBuffer, VkPipelines.pipelineLayouts.firstLong(), VK_SHADER_STAGE_COMPUTE_BIT, 0, new int[]{x, z});
            vkCmdPushConstants(executeCommandBuffer, VkPipelines.pipelineLayouts.firstLong(), VK_SHADER_STAGE_COMPUTE_BIT, 8, new float[]{
                            (float) configuration.RFPerRadiationUnit(),
                            (float) configuration.fuelAbsorptionCoefficient(),
                            (float) configuration.fuelModerationFactor(),
                            (float) (1.0 / configuration.fuelHardnessDivisor()),
                    }
            );
            vkCmdBindDescriptorSets(executeCommandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, VkPipelines.pipelineLayouts.firstLong(), 0, new long[]{descriptorSet.set()}, null);
            vkCmdDispatch(executeCommandBuffer, controlRods.length, 1 /* group count is 1, global size is y*/, 1);
            recordReductionBarrier.run();
            vkCmdBindDescriptorSets(executeCommandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, VkPipelines.pipelineLayouts.secondLong(), 0, new long[]{descriptorSet.set()}, null);
            vkCmdBindPipeline(executeCommandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, VkPipelines.reductionPipeline);
            vkCmdPushConstants(executeCommandBuffer, VkPipelines.pipelineLayouts.secondLong(), VK_SHADER_STAGE_COMPUTE_BIT, 0, new int[]{y * VkPipelines.zSizeForHeight(y)});
            vkCmdDispatch(executeCommandBuffer, controlRods.length, 1, 1);
            recordCopyBack.run();
            
            vkEndCommandBuffer(setupCommandBuffer);
            vkEndCommandBuffer(executeCommandBuffer);
            this.executeCommandBuffer = executeCommandBuffer;
            
            var commandBufferInfo = VkCommandBufferSubmitInfo.calloc(1, stack).sType$Default();
            commandBufferInfo.commandBuffer(setupCommandBuffer);
            
            var semaphoreSignalInfo = VkSemaphoreSubmitInfo.calloc(1, stack).sType$Default();
            semaphoreSignalInfo.semaphore(semaphorePointer.get(0));
            semaphoreSignalInfo.value(nextWaitValue);
            semaphoreSignalInfo.stageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT);
            
            var submitInfo = VkSubmitInfo2.calloc(1, stack).sType$Default();
            submitInfo.pCommandBufferInfos(commandBufferInfo);
            submitInfo.pSignalSemaphoreInfos(semaphoreSignalInfo);
            
            VkUtil.queueSubmit(submitInfo);
            waitSemaphore();
            
            if (VkPipelines.testSize != 0) {
                THIS_CLEANINGS.add(() -> vkDestroyPipeline(device, simPipeline, null));
            }
        }
    }
    
    @Override
    public boolean isAsync() {
        return true;
    }
    
    @Override
    public boolean readyToTick() {
        semaphoreWaitValuePointer.put(0, nextWaitValue);
        return checkVkResult(vkWaitSemaphores(VkUtil.device, semaphoreWaitInfo, 0)) == VK_SUCCESS;
    }
    
    private void waitSemaphore() {
        semaphoreWaitValuePointer.put(0, nextWaitValue);
        vkWaitSemaphores(VkUtil.device, semaphoreWaitInfo, -1);
    }
    
    @Override
    protected void finalize() {
        waitSemaphore();
    }
    
    public void addSemaphoreToWaitInto(VkSemaphoreWaitInfo waitInfo, int index) {
        var structAddress = waitInfo.address();
        var semaphoresPointer = memGetAddress(structAddress + VkSemaphoreWaitInfo.PSEMAPHORES);
        var valuesPointer = memGetAddress(structAddress + VkSemaphoreWaitInfo.PVALUES);
        memPutLong(semaphoresPointer + ((long) index * 8), semaphorePointer.get(0));
        memPutLong(valuesPointer + ((long) index * 8), semaphoreWaitValuePointer.get(0));
    }
    
    @Override
    protected void startNextRadiate() {
        if (fuelTank.fuel() <= 0) {
            return;
        }
        setupIrradiationTick();
        fullPassIrradiationRequest.updateCache();
        waitSemaphore();
        
        moderatorsWritePtr.putFloatIdx(0L, (float) fuelAbsorptionTemperatureCoefficient);
        moderatorsWritePtr.putFloatIdx(1L, (float) initialHardness);
        
        var moderators = moderatorsWritePtr.slice(16, moderatorsWritePtr.size() - 8);
        for (int i = 0; i < moderatorCaches.size(); i++) {
            var cache = moderatorCaches.get(i);
            moderators.putFloatIdx(i * 4L, (float) cache.absorption);
            moderators.putFloatIdx(i * 4L + 1, (float) cache.heatEfficiency);
            moderators.putFloatIdx(i * 4L + 2, (float) cache.moderation);
        }
        
        for (int i = 0; i < controlRods.length; i++) {
            var controlRod = controlRods[i];
            int linearIndex = controlRod.x * z + controlRod.z;
            controlRodWritePtr.putFloatIdx(linearIndex * 2L, (float) (controlRod.insertion * 0.01));
            controlRodWritePtr.putFloatIdx(linearIndex * 2L + 1, (float) initialIntensties[i]);
        }
        
        nextWaitValue++;
        semaphoreWaitValuePointer.put(0, nextWaitValue);
        
        try (var stack = MemoryStack.stackPush()) {
            
            var commandBufferInfo = VkCommandBufferSubmitInfo.calloc(1, stack).sType$Default();
            commandBufferInfo.commandBuffer(executeCommandBuffer);
            
            var semaphoreSignalInfo = VkSemaphoreSubmitInfo.calloc(1, stack).sType$Default();
            semaphoreSignalInfo.semaphore(semaphorePointer.get(0));
            semaphoreSignalInfo.value(nextWaitValue);
            semaphoreSignalInfo.stageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT);
            
            var submitInfo = VkSubmitInfo2.calloc(1, stack).sType$Default();
            submitInfo.pCommandBufferInfos(commandBufferInfo);
            submitInfo.pSignalSemaphoreInfos(semaphoreSignalInfo);
            
            VkUtil.queueSubmit(submitInfo);
        }
    }
    
    public double radiate() {
        waitSemaphore();
        
        for (long i = 0; i < controlRods.length; i++) {
            fuelRFAdded += resultsReadPtr.getFloatIdx(i * 4) * rayMultiplier;
            fuelRadAdded += resultsReadPtr.getFloatIdx(i * 4 + 1) * rayMultiplier;
            caseRFAdded += resultsReadPtr.getFloatIdx(i * 4 + 2) * rayMultiplier;
        }
        
        fuelRFAdded *= configuration.RFPerRadiationUnit();
        caseRFAdded *= configuration.RFPerRadiationUnit();
        
        fuelRFAdded /= controlRods.length;
        fuelRadAdded /= controlRods.length;
        caseRFAdded /= controlRods.length;
        
        if (!Double.isNaN(fuelRadAdded)) {
            if (configuration.fuelRadScalingMultiplier() != 0) {
                fuelRadAdded *= configuration.fuelRadScalingMultiplier() * (configuration.fuelRodFuelCapacity() / Math.max(1.0, (double) fuelTank().totalStored()));
            }
            fuelFertility += fuelRadAdded;
        }
        if (!Double.isNaN(fuelRFAdded)) {
            fuelHeat.absorbRF(fuelRFAdded);
        }
        if (!Double.isNaN(caseRFAdded)) {
            stackHeat.absorbRF(caseRFAdded);
        }
        
        fuelRFAdded = 0;
        fuelRadAdded = 0;
        caseRFAdded = 0;
        
        return rawFuelUsage;
    }
}
