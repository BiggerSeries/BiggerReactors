package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.vk;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.vk.VkUtil.checkVkResult;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
import static org.lwjgl.vulkan.VK13.vkCreateCommandPool;

public class VkPools {
    
    private static final long persistentCommandsPool;
    private static final ReferenceArrayList<VkCommandBuffer> freePersistentCommandBuffers = new ReferenceArrayList<>();
    private static final long singleIssueCommandsPool;
    private static final ReferenceArrayList<VkCommandBuffer> freeSingleIssueCommandBuffers = new ReferenceArrayList<>();
    private static final LongArrayList descriptorPools = new LongArrayList();
    
    public record DescriptorSet(long set, long pool) {
        public void free() {
            freeDescriptorSet(this);
        }
    }
    
    private static final ReferenceArrayList<DescriptorSet> freeDescriptorSets = new ReferenceArrayList<DescriptorSet>();
    private static final ReferenceArrayList<DescriptorSet> descriptorSetsToFree = new ReferenceArrayList<DescriptorSet>();
    
    static {
        persistentCommandsPool = createCommandPool(false);
        singleIssueCommandsPool = createCommandPool(true);
    }
    
    private static long createCommandPool(boolean singleIssue) {
        try (var stack = MemoryStack.stackPush()) {
            
            var createInfo = VkCommandPoolCreateInfo.calloc(stack).sType$Default();
            createInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
            if (singleIssue) {
                createInfo.flags(createInfo.flags() | VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
            }
            createInfo.queueFamilyIndex(VkUtil.queueFamilyIndex);
            
            var handlePtr = stack.mallocLong(1);
            checkVkResult(vkCreateCommandPool(VkUtil.device, createInfo, null, handlePtr));
            return handlePtr.get(0);
        }
    }
    
    private static long createDescriptorPool(int descriptorsPerSet) {
        final int setsToAllocate = 8192;
        try (var stack = MemoryStack.stackPush()) {
            final var poolSizes = VkDescriptorPoolSize.calloc(1, stack);
            poolSizes.type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
            poolSizes.descriptorCount(setsToAllocate * descriptorsPerSet);
            
            
            var createInfo = VkDescriptorPoolCreateInfo.calloc(stack).sType$Default();
            createInfo.flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT);
            createInfo.pPoolSizes(poolSizes);
            createInfo.maxSets(setsToAllocate);
            
            var handlePtr = stack.mallocLong(1);
            checkVkResult(vkCreateDescriptorPool(VkUtil.device, createInfo, null, handlePtr));
            return handlePtr.get(0);
        }
    }
    
    private static void allocPool() {
        descriptorPools.add(createDescriptorPool(7));
    }
    
    static void init() {
        allocPool();
        allocateMoreBuffers(true);
        allocateMoreBuffers(false);
    }
    
    static void terminate() {
        vkDestroyCommandPool(VkUtil.device, persistentCommandsPool, null);
        vkDestroyCommandPool(VkUtil.device, singleIssueCommandsPool, null);
        for (int i = 0; i < descriptorPools.size(); i++) {
            vkDestroyDescriptorPool(VkUtil.device, descriptorPools.getLong(i), null);
        }
    }
    
    private static void allocateMoreBuffers(boolean singleIssue) {
        final var buffersToCreate = 32;
        
        var commandPool = singleIssue ? singleIssueCommandsPool : persistentCommandsPool;
        var freeBuffers = singleIssue ? freeSingleIssueCommandBuffers : freePersistentCommandBuffers;
        try (final var stack = MemoryStack.stackPush()) {
            
            final var allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool);
            allocInfo.commandBufferCount(buffersToCreate);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            
            final var pointerPtr = stack.mallocPointer(buffersToCreate);
            checkVkResult(vkAllocateCommandBuffers(VkUtil.device, allocInfo, pointerPtr));
            for (int i = 0; i < buffersToCreate; i++) {
                freeBuffers.add(new VkCommandBuffer(pointerPtr.get(i), VkUtil.device));
            }
        }
    }
    
    public static synchronized VkCommandBuffer allocCommandBuffer(boolean singleIssue) {
        var freeBuffers = singleIssue ? freeSingleIssueCommandBuffers : freePersistentCommandBuffers;
        if (freeBuffers.isEmpty()) {
            allocateMoreBuffers(singleIssue);
        }
        return freeBuffers.pop();
    }
    
    public static synchronized void freeCommandBufferNow(VkCommandBuffer commandBuffer, boolean singleIssue) {
        var freeBuffers = singleIssue ? freeSingleIssueCommandBuffers : freePersistentCommandBuffers;
//        freeBuffers.add(commandBuffer);
        var pool = singleIssue ? singleIssueCommandsPool : persistentCommandsPool;
        vkFreeCommandBuffers(VkUtil.device, pool, commandBuffer);
    }
    
    public static synchronized DescriptorSet allocDescriptorSet() {
        try (final var stack = MemoryStack.stackPush()) {
            final var setLayout = stack.mallocLong(1);
            setLayout.put(0, VkPipelines.descriptorLayout);
            
            final var allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.pSetLayouts(setLayout);
            
            final var longPtr = stack.mallocLong(1);
            
            boolean allocated = false;
            long currentPool = VK_NULL_HANDLE;
            for (int j = 0; j < 2; j++) {
                for (int i = 0; i < descriptorPools.size(); i++) {
                    currentPool = descriptorPools.getLong(i);
                    allocInfo.descriptorPool(currentPool);
                    int code = vkAllocateDescriptorSets(VkUtil.device, allocInfo, longPtr);
                    if (code == VK_SUCCESS) {
                        allocated = true;
                        break;
                    }
                }
                if (allocated || descriptorSetsToFree.isEmpty()) {
                    break;
                }
                for (int i = 0; i < descriptorSetsToFree.size(); i++) {
                    // TODO: big calls, they can be passed more than one at a time
                    var set = descriptorSetsToFree.get(i);
                    longPtr.put(0, set.set);
                    vkFreeDescriptorSets(VkUtil.device, set.pool, longPtr);
                }
            }
            if (!allocated) {
                allocPool();
                currentPool = descriptorPools.getLong(descriptorPools.size() - 1);
                allocInfo.descriptorPool(currentPool);
                int code = vkAllocateDescriptorSets(VkUtil.device, allocInfo, longPtr);
                if (code != VK_SUCCESS) {
                    throw new IllegalStateException("Unable to allocate descriptor in new pool");
                }
            }
            return new DescriptorSet(longPtr.get(0), currentPool);
        }
    }
    
    public static synchronized void freeDescriptorSet(DescriptorSet set) {
        descriptorSetsToFree.add(set);
    }
    
}
