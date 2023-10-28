package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.vk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import net.roguelogix.phosphophyllite.util.Pair;
import net.roguelogix.quartz.internal.util.PointerWrapper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.libc.LibCString;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.util.Collections;

import static java.lang.Math.max;
import static net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.vk.VkUtil.checkVkResult;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
import static org.lwjgl.vulkan.VK13.vkGetPhysicalDeviceMemoryProperties;

@NonnullDefault
public class VkMemUtil {
    
    // 1GiB
    private static final long KiB = 1024;
    private static final long MiB = 1024 * KiB;
    private static final long GiB = 1024 * MiB;
    private static final long VK_ALLOCATION_SIZE = GiB;
    
    private static class AllocationBlock {
        
        private record Info(long offset, long size) implements Comparable<Info> {
            public Info(Info a, Info b) {
                this(a.offset, a.size + b.size);
                if (a.offset + a.size != b.offset) {
                    throw new IllegalStateException("Cannot combine non-consecutive alloc infos");
                }
            }
            
            @Override
            public int compareTo(Info info) {
                return Long.compare(offset, info.offset);
            }
            
            private Pair<Info, Info> split(long size) {
                if (size > this.size) {
                    throw new IllegalArgumentException("Cannot split allocation to larger size");
                }
                if (size == this.size) {
                    return new Pair<>(new Info(this.offset, size), null);
                }
                return new Pair<>(new Info(this.offset, size), new Info(this.offset + size, this.size - size));
            }
        }
        
        
        private final ObjectArrayList<Info> freeAllocations = new ObjectArrayList<>() {
            @Override
            public boolean add(@Nullable Info allocation) {
                if (allocation == null) {
                    return false;
                }
                int index = Collections.binarySearch(this, allocation);
                if (index < 0) {
                    index = ~index;
                    super.add(index, allocation);
                } else {
                    super.set(index, allocation);
                }
                return true;
            }
        };
        
        private final long vkMemoryHandle;
        private final long hostPtr;
        private final long size;
        
        private AllocationBlock(long vkMemoryHandle, long hostPtr, long size) {
            this.vkMemoryHandle = vkMemoryHandle;
            this.hostPtr = hostPtr;
            this.size = size;
            freeAllocations.add(new Info(0, size));
        }
        
        public boolean canFit(long size, long alignment) {
            for (Info freeAlloc : freeAllocations) {
                final long nextValidAlignment = (freeAlloc.offset + (alignment - 1)) & (-alignment);
                final long alignmentWaste = nextValidAlignment - freeAlloc.offset;
                if (freeAlloc.size - alignmentWaste >= size) {
                    // fits
                    return true;
                }
            }
            // dont fit
            return false;
        }
        
        public boolean owns(VkAllocation allocation) {
            return allocation.vkMemoryHandle == this.vkMemoryHandle;
        }
        
        public VkAllocation alloc(long size, long align) {
            Info allocatedSpace = null;
            for (Info freeAllocation : freeAllocations) {
                var alloc = attemptAllocInSpace(freeAllocation, size, align);
                if (alloc != null) {
                    allocatedSpace = alloc;
                    freeAllocations.remove(freeAllocation);
                    break;
                }
            }
            if (allocatedSpace == null) {
                throw new IllegalStateException("Failed to make allocation in block");
            }
            PointerWrapper hostPtr = PointerWrapper.NULLPTR;
            if (this.hostPtr != 0) {
                hostPtr = new PointerWrapper(this.hostPtr + allocatedSpace.offset, size);
            }
            return new VkAllocation(this, vkMemoryHandle, allocatedSpace.offset, size, hostPtr);
        }
        
        @Nullable
        private Info attemptAllocInSpace(Info freeAlloc, long size, long alignment) {
            // next value guaranteed to be at *most* one less than the next alignment, then bit magic because powers of two to round down without a divide
            final long nextValidAlignment = (freeAlloc.offset + (alignment - 1)) & (-alignment);
            final long alignmentWaste = nextValidAlignment - freeAlloc.offset;
            if (freeAlloc.size - alignmentWaste < size) {
                // wont fit, *neeeeeeeeeeeext*
                return null;
            }
            boolean collapse = false;
            if (alignmentWaste > 0) {
                final var newAllocs = freeAlloc.split(alignmentWaste);
                // not concurrent modification because this will always return
                freeAllocations.add(newAllocs.first);
                freeAlloc = newAllocs.second;
                
                int index = freeAllocations.indexOf(newAllocs.first);
                collapseFreeAllocationWithNext(index - 1);
                collapseFreeAllocationWithNext(index);
            }
            if (freeAlloc.size > size) {
                final var newAllocs = freeAlloc.split(size);
                // not concurrent modification because this will always return
                freeAlloc = newAllocs.first;
                freeAllocations.add(newAllocs.second);
                int index = freeAllocations.indexOf(newAllocs.second);
                collapseFreeAllocationWithNext(index - 1);
                collapseFreeAllocationWithNext(index);
            }
            
            return freeAlloc;
        }
        
        public void free(VkAllocation allocation) {
            if (!owns(allocation)) {
                return;
            }
            var info = new Info(allocation.offset, allocation.size);
            freeAllocations.add(info);
            var index = freeAllocations.indexOf(info);
            collapseFreeAllocationWithNext(index - 1);
            collapseFreeAllocationWithNext(index);
        }
        
        private boolean collapseFreeAllocationWithNext(int freeAllocationIndex) {
            if (freeAllocationIndex < 0 || freeAllocationIndex >= freeAllocations.size() - 1) {
                return false;
            }
            var allocA = freeAllocations.get(freeAllocationIndex);
            var allocB = freeAllocations.get(freeAllocationIndex + 1);
            if (allocA.offset + allocA.size == allocB.offset) {
                // neighboring allocations, collapse them
                freeAllocations.remove(freeAllocationIndex + 1);
                freeAllocations.remove(freeAllocationIndex);
                freeAllocations.add(new Info(allocA.offset, allocA.size + allocB.size));
                return true;
            }
            return false;
        }
    }
    
    private static final int hostMemoryType;
    private static final int gpuMemoryType;
    private static final int gpuMemoryProperties;
    private static final ReferenceArrayList<AllocationBlock> hostBlocks = new ReferenceArrayList<>();
    private static final ReferenceArrayList<AllocationBlock> gpuBlocks = new ReferenceArrayList<>();
    
    
    static {
        int hostType = -1;
        int gpuType = -1;
        boolean hostVisibleGPUMemory = false;
        try (var stack = MemoryStack.stackPush()) {
            var memoryProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
            vkGetPhysicalDeviceMemoryProperties(VkUtil.device.getPhysicalDevice(), memoryProperties);
            var memoryTypes = memoryProperties.memoryTypes();
            var typeCount = memoryProperties.memoryTypeCount();
            // earlier types preferred by implementation
            for (int i = typeCount - 1; i >= 0; i--) {
                var type = memoryTypes.get(i);
                var propertyFlags = type.propertyFlags();
                if ((propertyFlags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0) {
                    if(!hostVisibleGPUMemory || (propertyFlags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0){
                        gpuType = i;
                    }
                }
                if ((propertyFlags & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) != 0) {
                    hostType = i;
                }
            }
            if (gpuType == -1) {
                throw new IllegalStateException("Unable to find GPU memory");
            }
            if (hostType == -1) {
                throw new IllegalStateException("Unable to find GPU memory");
            }
            hostMemoryType = hostType;
            gpuMemoryType = gpuType;
            gpuMemoryProperties = memoryTypes.get(gpuMemoryType).propertyFlags();
        }
    }
    
    static void init() {
    }
    
    static void terminate() {
        for (AllocationBlock hostBlock : hostBlocks) {
            vkFreeMemory(VkUtil.device, hostBlock.vkMemoryHandle, null);
        }
        for (AllocationBlock hostBlock : gpuBlocks) {
            vkFreeMemory(VkUtil.device, hostBlock.vkMemoryHandle, null);
        }
    }
    
    public record VkAllocation(AllocationBlock block, long vkMemoryHandle, long offset, long size, PointerWrapper hostPtr) {
    
    }
    
    public static synchronized VkAllocation allocHost(VkMemoryRequirements memoryRequirements) {
        if ((memoryRequirements.memoryTypeBits() & (1 << hostMemoryType)) == 0) {
            throw new IllegalStateException("Cannot allocate on host");
        }
        return allocHost(memoryRequirements.size(), memoryRequirements.alignment());
    }
    
    public static VkAllocation allocHost(long size, long alignment) {
        var block = getBlockWithSpace(size, alignment, hostBlocks, hostMemoryType);
        return block.alloc(size, alignment);
    }
    
    public static synchronized void freeHost(@Nullable VkAllocation allocation) {
        if (allocation == null) {
            return;
        }
        for (AllocationBlock hostBlock : hostBlocks) {
            if (hostBlock.owns(allocation)) {
                hostBlock.free(allocation);
                return;
            }
        }
    }
    
    public static synchronized VkAllocation allocGPU(VkMemoryRequirements memoryRequirements) {
        if ((memoryRequirements.memoryTypeBits() & (1 << gpuMemoryType)) == 0) {
            throw new IllegalStateException("Cannot allocate on gpu");
        }
        return allocGPU(memoryRequirements.size(), memoryRequirements.alignment());
    }
    
    public static VkAllocation allocGPU(long size, long alignment) {
        var block = getBlockWithSpace(size, alignment, gpuBlocks, gpuMemoryType);
        return block.alloc(size, alignment);
    }
    
    public static synchronized void freeGPU(@Nullable VkAllocation allocation) {
        if (allocation == null) {
            return;
        }
        for (AllocationBlock hostBlock : gpuBlocks) {
            if (hostBlock.owns(allocation)) {
                hostBlock.free(allocation);
                return;
            }
        }
    }
    
    
    private static AllocationBlock getBlockWithSpace(long size, long alignment, ReferenceArrayList<AllocationBlock> list, int memoryType) {
        for (AllocationBlock allocationBlock : list) {
            if (allocationBlock.canFit(size, alignment)) {
                return allocationBlock;
            }
        }
        var newBlock = allocBlock(max(VK_ALLOCATION_SIZE, size), memoryType);
        list.add(newBlock);
        return newBlock;
    }
    
    private static AllocationBlock allocBlock(long size, int memoryType) {
        try (var stack = MemoryStack.stackPush()) {
            var longReturn = stack.mallocLong(1);
            var ptrReturn = stack.mallocPointer(1);
            
            var allocInfo = VkMemoryAllocateInfo.calloc(stack).sType$Default();
            allocInfo.allocationSize(size);
            allocInfo.memoryTypeIndex(memoryType);
            checkVkResult(vkAllocateMemory(VkUtil.device, allocInfo, null, longReturn));
            long memHandle = longReturn.get(0);
            long memPtr = 0;
//            if (memoryType == hostMemoryType || (gpuMemoryProperties & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) {
            if (memoryType == hostMemoryType) {
                vkMapMemory(VkUtil.device, memHandle, 0, size, 0, ptrReturn);
                memPtr = ptrReturn.get(0);
                LibCString.nmemset(memPtr, 0, size);
            }
            return new AllocationBlock(memHandle, memPtr, size);
        }
    }
}
