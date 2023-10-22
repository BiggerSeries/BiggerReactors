package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.vk;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.ocl.CLUtil;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK13.*;

@NonnullDefault
public class VkUtil {
    
    public static final long MAX_RAY_STEPS = Config.CONFIG.Reactor.IrradiationDistance * 2;
    public static final long SIZEOF_RAY = 16 * MAX_RAY_STEPS;
    
    private static final Logger LOGGER = LogManager.getLogger("BiggerReactors/Reactor/Vulkan");
    public static final Cleaner VK_CLEANER = Cleaner.create();
    
    public static final boolean available;
    
    public static final VkInstance instance;
    public static final VkDevice device;
    public static final ByteBuffer raySimSPV;
    public static final ByteBuffer raySimVec2SPV;
    public static final ByteBuffer reductionSPV;
    public static final int queueFamilyIndex;
    public static final List<VkQueue> queues = new ReferenceArrayList<>();
    public static final IntArrayList availableQueueIndices = new IntArrayList();
    private static final ThreadLocal<Integer> threadQueueIndex = ThreadLocal.withInitial(() -> -1);
    
    private static boolean roundRobinQueues = false;
    private static int nextQueue = 0;
    
    static {
        final int physicalDeviceIndex = 0;
        final boolean enableValidationLayers = false;
        
        boolean initComplete = false;
        @Nullable
        VkInstance createdInstance = null;
        VkDevice createdDevice = null;
        ByteBuffer loadedRaysimSPV = null;
        ByteBuffer loadedRaysimvec2SPV = null;
        ByteBuffer loadedReductionSPV = null;
        int familyIndex = -1;
        
        failedBreak:
        try {
            try {
                LOGGER.info("Checking for LWJGL Vulkan classes");
                var instanceVersionSupported = VK.getInstanceVersionSupported();
                LOGGER.info("Classes found, attempting Vk init");
                if (instanceVersionSupported < VK_API_VERSION_1_3) {
                    LOGGER.info("Vulkan version too low");
                    break failedBreak;
                }
            } catch (Throwable ignored) { // yes this will catch anything, thats the point
                LOGGER.info("Failed to load LWJGL OpenCL Classes");
                break failedBreak;
            }
            
            try (var stack = MemoryStack.stackPush()) {
                var applicationInfo = VkApplicationInfo.calloc(stack);
                applicationInfo.sType$Default();
                applicationInfo.apiVersion(VK_API_VERSION_1_3);
                
                var instanceCreateInfo = VkInstanceCreateInfo.calloc(stack);
                instanceCreateInfo.sType$Default();
                instanceCreateInfo.pApplicationInfo(applicationInfo);
                
                if (enableValidationLayers) {
                    var namePtrs = stack.callocPointer(1);
                    stack.nASCII("VK_LAYER_KHRONOS_validation\0", true);
                    namePtrs.put(0, stack.getPointerAddress());
//                    stack.nASCII("VK_LAYER_RENDERDOC_Capture\0", true);
//                    namePtrs.put(1, stack.getPointerAddress());
                    instanceCreateInfo.ppEnabledLayerNames(namePtrs);
                }
                
                var instancePtr = stack.callocPointer(1);
                checkVkResult(vkCreateInstance(instanceCreateInfo, null, instancePtr));
                createdInstance = new VkInstance(instancePtr.get(0), instanceCreateInfo);
            }
            
            VkPhysicalDevice physicalDevice;
            try (var stack = MemoryStack.stackPush()) {
                var intPtr = stack.callocInt(1);
                vkEnumeratePhysicalDevices(createdInstance, intPtr, null);
                if (intPtr.get(0) == 0) {
                    LOGGER.info("No physical devices available");
                    break failedBreak;
                }
                var devicesPtr = stack.callocPointer(intPtr.get(0));
                vkEnumeratePhysicalDevices(createdInstance, intPtr, devicesPtr);
                
                
                var deviceToUse = physicalDeviceIndex;
                if (physicalDeviceIndex != 0 && intPtr.get(0) <= physicalDeviceIndex) {
                    LOGGER.warn("No physical device for index " + physicalDeviceIndex);
                    LOGGER.warn("Falling back to first device");
                    deviceToUse = 0;
                }
                physicalDevice = new VkPhysicalDevice(devicesPtr.get(deviceToUse), createdInstance);
            }
            
            try (var stack = MemoryStack.stackPush()) {
                var intPtr = stack.callocInt(1);
                vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, intPtr, null);
                var families = VkQueueFamilyProperties.calloc(intPtr.get(0), stack);
                vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, intPtr, families);
                
                var physicalDeviceProperties = VkPhysicalDeviceProperties.calloc(stack);
                vkGetPhysicalDeviceProperties(physicalDevice, physicalDeviceProperties);
                var vendorID = physicalDeviceProperties.vendorID();
                LOGGER.info("Device selected: " + physicalDeviceProperties.deviceNameString());
                if (physicalDeviceProperties.apiVersion() < VK_API_VERSION_1_3) {
                    LOGGER.info("Device API version too low");
                    break failedBreak;
                }
                
                VkQueueFamilyProperties familyToUse = null;
                final int requiredFlags = VK_QUEUE_COMPUTE_BIT | VK_QUEUE_TRANSFER_BIT;
                // while more queues could be used in some cases, its easier to just use one
                // even when using multiple queues, so long as its withing a single queue family queue transfers aren't needed
                switch (vendorID) {
                    case 0x8086 -> {
                        // Intel, single queue and family that does everything
                        familyToUse = families.get(0);
                        familyIndex = 0;
                    }
                    case 0x1002 -> {
                        // AMD, at least two families, second one has 4 queues that can do compute
                        familyToUse = families.get(1);
                        familyIndex = 1;
                        if (familyToUse.queueCount() != 4 || (familyToUse.queueFlags() & requiredFlags) != requiredFlags) {
                            LOGGER.info("Unexpected AMD queue family properties");
                            break failedBreak;
                        }
                    }
                    case 0x10DE -> {
                        // Nvidia, first family will have 16 queues
                        familyToUse = families.get(0);
                        familyIndex = 0;
                        if (familyToUse.queueCount() != 16 || (familyToUse.queueFlags() & requiredFlags) != requiredFlags) {
                            LOGGER.info("Unexpected Nvidia queue family properties");
                            break failedBreak;
                        }
                    }
                    default -> {
                        // unknown vendor, just pick the first family that has compute
                        for (int i = 0; i < families.limit(); i++) {
                            final var family = families.get(i);
                            if ((family.queueFlags() & requiredFlags) == requiredFlags) {
                                familyToUse = family;
                                familyIndex = i;
                                break;
                            }
                        }
                        if (familyToUse == null) {
                            LOGGER.info("No compute queue family found");
                            break failedBreak;
                        }
                    }
                }
                
                LOGGER.info("Queue family " + familyIndex);
                LOGGER.info("Queue count " + familyToUse.queueCount());
                
                var queuePriorities = stack.callocFloat(familyToUse.queueCount());
                for (int i = 0; i < familyToUse.queueCount(); i++) {
                    queuePriorities.put(i, 1);
                }
                
                var queueCreateInfos = VkDeviceQueueCreateInfo.calloc(1, stack);
                queueCreateInfos.sType$Default();
                queueCreateInfos.queueFamilyIndex(familyIndex);
                queueCreateInfos.pQueuePriorities(queuePriorities);
                
                // TODO: check for these features when selecting the device
                {
                    var baseFeatures = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();
                    var vk10Features = baseFeatures.features();
                    var vk11Features = VkPhysicalDeviceVulkan11Features.calloc(stack).sType$Default();
                    var vk12Features = VkPhysicalDeviceVulkan12Features.calloc(stack).sType$Default();
                    var vk13Features = VkPhysicalDeviceVulkan13Features.calloc(stack).sType$Default();
                    baseFeatures.pNext(vk11Features.address());
                    vk11Features.pNext(vk12Features.address());
                    vk12Features.pNext(vk13Features.address());
                    vkGetPhysicalDeviceFeatures2(physicalDevice, baseFeatures);
                    
                    List<String> missingFeatures = new ReferenceArrayList<>();
                    if (!vk10Features.shaderInt16()) {
                        missingFeatures.add("shaderInt16");
                    }
                    if (!vk11Features.storageBuffer16BitAccess()) {
                        missingFeatures.add("storageBuffer16BitAccess");
                    }
                    if (!vk11Features.uniformAndStorageBuffer16BitAccess()) {
                        missingFeatures.add("uniformAndStorageBuffer16BitAccess");
                    }
                    if (!vk12Features.timelineSemaphore()) {
                        missingFeatures.add("timelineSemaphore");
                    }
                    if (!vk12Features.uniformAndStorageBuffer8BitAccess()) {
                        missingFeatures.add("uniformAndStorageBuffer8BitAccess");
                    }
                    if (!vk12Features.shaderFloat16()) {
                        missingFeatures.add("shaderFloat16");
                    }
                    if (!vk12Features.shaderInt8()) {
                        missingFeatures.add("shaderInt8");
                    }
                    if (!vk13Features.synchronization2()) {
                        missingFeatures.add("synchronization2");
                    }
                    if (!missingFeatures.isEmpty()) {
                        StringBuilder errorString = new StringBuilder("Device missing feature ");
                        for (String missingFeature : missingFeatures) {
                            errorString.append(missingFeature).append(", ");
                        }
                        LOGGER.info(errorString.toString());
                        break failedBreak;
                    }
                }
                var baseFeatures = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();
                var vk10Features = baseFeatures.features();
                vk10Features.shaderInt16(true);
                
                var vk11Features = VkPhysicalDeviceVulkan11Features.calloc(stack).sType$Default();
                vk11Features.storageBuffer16BitAccess(true);
                vk11Features.uniformAndStorageBuffer16BitAccess(true);
                
                var vk12Features = VkPhysicalDeviceVulkan12Features.calloc(stack).sType$Default();
                vk12Features.timelineSemaphore(true);
                vk12Features.uniformAndStorageBuffer8BitAccess(true);
                vk12Features.shaderFloat16(true);
                vk12Features.shaderInt8(true);
                
                var vk13Features = VkPhysicalDeviceVulkan13Features.calloc(stack).sType$Default();
                vk13Features.synchronization2(true);
                
                var deviceCreateInfo = VkDeviceCreateInfo.calloc(stack).sType$Default();
                
                deviceCreateInfo.pQueueCreateInfos(queueCreateInfos);
                deviceCreateInfo.pNext(baseFeatures.address());
                baseFeatures.pNext(vk11Features.address());
                vk11Features.pNext(vk12Features.address());
                vk12Features.pNext(vk13Features.address());
                
                var ptrReturn = stack.callocPointer(1);
                checkVkResult(vkCreateDevice(physicalDevice, deviceCreateInfo, null, ptrReturn));
                createdDevice = new VkDevice(ptrReturn.get(0), physicalDevice, deviceCreateInfo);
                
                for (int i = 0; i < familyToUse.queueCount(); i++) {
                    vkGetDeviceQueue(createdDevice, familyIndex, i, ptrReturn);
                    queues.add(new VkQueue(ptrReturn.get(0), createdDevice));
                    availableQueueIndices.add(i);
                }
            }
            
            loadedRaysimSPV = loadSPV("/biggerreactors/simulation/vk/spv/raysim.comp.spv");
            loadedRaysimvec2SPV = loadSPV("/biggerreactors/simulation/vk/spv/raysimvec2.comp.spv");
            loadedReductionSPV = loadSPV("/biggerreactors/simulation/vk/spv/reduction.comp.spv");
            
            if (loadedRaysimSPV == null || loadedRaysimvec2SPV == null || loadedReductionSPV == null) {
                break failedBreak;
            }
            
            initComplete = true;
        } catch (VulkanException e) {
            e.printStackTrace();
        }
        
        
        if (!initComplete) {
            if (createdDevice != null) {
                vkDeviceWaitIdle(createdDevice);
                vkDestroyDevice(createdDevice, null);
                createdDevice = null;
            }
            if (createdInstance != null) {
                vkDestroyInstance(createdInstance, null);
                createdInstance = null;
            }
        }
        
        //noinspection DataFlowIssue
        instance = createdInstance;
        //noinspection DataFlowIssue
        device = createdDevice;
        queueFamilyIndex = familyIndex;
        //noinspection DataFlowIssue
        raySimSPV = loadedRaysimSPV;
        //noinspection DataFlowIssue
        raySimVec2SPV = loadedRaysimvec2SPV;
        //noinspection DataFlowIssue
        reductionSPV = loadedReductionSPV;
        
        try {
            if (initComplete) {
                VkMemUtil.init();
            }
        } catch (VulkanException e) {
            initComplete = false;
            e.printStackTrace();
        }
        
        available = initComplete;

//        if (enableValidationLayers) {
//            System.out.println("RenderDocAttachWait");
//            try {
//                System.in.read();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
    }
    
    @Nullable
    private static ByteBuffer loadSPV(String file) {
        try (InputStream in = CLUtil.class.getResourceAsStream(file)) {
            if (in == null) {
                return null;
            }
            var bytes = in.readAllBytes();
            var byteBuf = BufferUtils.createByteBuffer(bytes.length);
            byteBuf.put(0, bytes);
            return byteBuf;
        } catch (IOException e) {
            return null;
        }
    }
    
    //    @OnModLoad
    public static void onModLoad() {
        if (!available) {
            return;
        }
        roundRobinQueues = true;
        VkMemUtil.init();
    }
    
    // explicit init call
    public static void init() {
        if (!available) {
            throw new IllegalStateException();
        }
        roundRobinQueues = false;
        VkMemUtil.init();
        VkPipelines.init();
        VkPools.init();
    }
    
    // TODO: on JVM shutdown this needs to be called
    public static void terminate() {
        if (!available) {
            return;
        }
        // run GC passes so cleaners run
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        vkDeviceWaitIdle(device);
        VkPools.terminate();
        VkPipelines.terminate();
        VkMemUtil.terminate();
        vkDestroyDevice(device, null);
        vkDestroyInstance(instance, null);
    }
    
    public static synchronized void acquireQueueForThread() {
        if (!available) {
            return;
        }
        var index = availableQueueIndices.removeInt(0);
        threadQueueIndex.set(index);
    }
    
    public static synchronized void releaseQueueForThread() {
        if (!available) {
            return;
        }
        availableQueueIndices.add((int) threadQueueIndex.get());
        threadQueueIndex.set(-1);
    }
    
    public static class VulkanException extends RuntimeException {
        public VulkanException(int returnCode) {
            super("VK return code " + returnCode);
        }
        
        public VulkanException(String message) {
            super(message);
        }
    }
    
    @Contract
    public static int checkVkResult(int returnCode) {
        if (returnCode < 0) {
            switch (returnCode) {
                case VK_ERROR_OUT_OF_HOST_MEMORY -> throw new OutOfMemoryError("VK_ERROR_OUT_OF_HOST_MEMORY");
                case VK_ERROR_OUT_OF_DEVICE_MEMORY -> throw new VulkanException("VK_ERROR_OUT_OF_DEVICE_MEMORY");
                case VK_ERROR_INITIALIZATION_FAILED -> throw new VulkanException("VK_ERROR_INITIALIZATION_FAILED");
                case VK_ERROR_DEVICE_LOST -> throw new VulkanException("VK_ERROR_DEVICE_LOST");
                case VK_ERROR_MEMORY_MAP_FAILED -> throw new VulkanException("VK_ERROR_MEMORY_MAP_FAILED");
                case VK_ERROR_LAYER_NOT_PRESENT -> throw new VulkanException("VK_ERROR_LAYER_NOT_PRESENT");
                case VK_ERROR_EXTENSION_NOT_PRESENT -> throw new VulkanException("VK_ERROR_EXTENSION_NOT_PRESENT");
                case VK_ERROR_FEATURE_NOT_PRESENT -> throw new VulkanException("VK_ERROR_FEATURE_NOT_PRESENT");
                case VK_ERROR_INCOMPATIBLE_DRIVER -> throw new VulkanException("VK_ERROR_INCOMPATIBLE_DRIVER");
                case VK_ERROR_TOO_MANY_OBJECTS -> throw new VulkanException("VK_ERROR_TOO_MANY_OBJECTS");
                case VK_ERROR_FORMAT_NOT_SUPPORTED -> throw new VulkanException("VK_ERROR_FORMAT_NOT_SUPPORTED");
                case VK_ERROR_FRAGMENTED_POOL -> throw new VulkanException("VK_ERROR_FRAGMENTED_POOL");
                case -13 /* VK_ERROR_UNKNOWN */ -> throw new VulkanException("VK_ERROR_UNKNOWN");
                case VK_ERROR_OUT_OF_POOL_MEMORY -> throw new VulkanException("VK_ERROR_OUT_OF_POOL_MEMORY");
                case VK_ERROR_INVALID_EXTERNAL_HANDLE -> throw new VulkanException("VK_ERROR_INVALID_EXTERNAL_HANDLE");
                case -1000161000 /* VK_ERROR_FRAGMENTATION */ -> throw new VulkanException("VK_ERROR_FRAGMENTATION");
                case -1000257000 /* VK_ERROR_INVALID_OPAQUE_CAPTURE_ADDRESS */ -> throw new VulkanException("VK_ERROR_INVALID_OPAQUE_CAPTURE_ADDRESS");
                default -> throw new VulkanException(returnCode);
            }
            
        }
        return returnCode;
    }
    
    public static boolean canThreadSubmit(){
        return roundRobinQueues || threadQueueIndex.get() != -1;
    }
    
    public static void queueSubmit(VkSubmitInfo2.Buffer submitInfos) {
        final int queueIndex;
        if (roundRobinQueues) {
            queueIndex = nextQueue++;
            nextQueue %= queues.size();
        } else {
            queueIndex = threadQueueIndex.get();
            if (queueIndex == -1) {
                throw new VulkanException("No queue acquired for thread");
            }
        }
        vkQueueSubmit2(queues.get(queueIndex), submitInfos, VK_NULL_HANDLE);
    }
}
