package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.ocl;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL11.*;

public class CLUtil {
    
    public static final long SIZEOF_REACTOR_INFO = 4 * 9;
    public static final long SIZEOF_MODERATOR = 4 * 3;
    public static final long SIZEOF_RAY = 4 * 2;
    public static final long SIZEOF_RAY_STEP = 4 * 4;
    public static final long SIZEOF_ROD_RAY_INFO = 4;
    public static final long SIZEOF_RAY_BURN_INFO = 4 * 3;
    
    private static final Cleaner CL_CLEANER = Cleaner.create();
    
    public long createCommandQueue(long device, IntBuffer returnCode) {
        // TODO: CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE
        long queue = clCreateCommandQueue(context, device, 0, returnCode);
        checkReturnCode(returnCode.get(0));
        CL_CLEANER.register(this, () -> clReleaseCommandQueue(queue));
        return queue;
    }
    
    public PointerBuffer allocPointer(int capacity) {
        var buffer = MemoryUtil.memAllocPointer(capacity);
        CL_CLEANER.register(this, () -> MemoryUtil.memFree(buffer));
        return buffer;
    }
    
    public LongBuffer allocLong(int capacity) {
        var buffer = MemoryUtil.memAllocLong(capacity);
        CL_CLEANER.register(this, () -> MemoryUtil.memFree(buffer));
        return buffer;
    }
    
    public IntBuffer allocInt(int capacity) {
        var buffer = MemoryUtil.memAllocInt(capacity);
        CL_CLEANER.register(this, () -> MemoryUtil.memFree(buffer));
        return buffer;
    }
    
    public FloatBuffer allocFloat(int capacity) {
        var buffer = MemoryUtil.memAllocFloat(capacity);
        CL_CLEANER.register(this, () -> MemoryUtil.memFree(buffer));
        return buffer;
    }
    
    public long createCLKernel(String name, IntBuffer errorBuffer) {
        long kernel = clCreateKernel(program, name, errorBuffer);
        checkReturnCode(errorBuffer.get(0));
        CL_CLEANER.register(this, () -> clReleaseKernel(kernel));
        return kernel;
    }
    
    public long createCLBuffer(long flags, long size, IntBuffer errorBuffer) {
        long buffer = clCreateBuffer(context, flags, size, errorBuffer);
        checkReturnCode(errorBuffer.get(0));
        CL_CLEANER.register(this, () -> clReleaseMemObject(buffer));
        return buffer;
    }
    
    private static final Logger LOGGER = LogManager.getLogger("BiggerReactors/Reactor/OpenCL");
    public static final boolean available;
    private static final long platform;
    private static final long context;
    private static final long program;
    private static final LongArrayList devices = new LongArrayList();
    private static final long largestSizeMultiple;
    private static final LongArrayList sizeMultiples = new LongArrayList();
    private static final LongArrayList maxWorkGroupSizes = new LongArrayList();
    private static final AtomicInteger nextDevice = new AtomicInteger();
    
    private static int getNextDeviceIndex() {
        if (devices.size() == 1) {
            return 0;
        }
        int deviceIndex = nextDevice.incrementAndGet();
        if (deviceIndex > devices.size()) {
            nextDevice.addAndGet(-devices.size());
        }
        deviceIndex %= devices.size();
        return deviceIndex;
    }
    
    static long nextDevice() {
        return devices.getLong(getNextDeviceIndex());
    }
    
    @OnModLoad
    private static void onModLoad() {
    }
    
    static {
        final int forcePlatform = -1;
        final boolean debug = false;
        LOGGER.warn("Initializing OpenCL, may cause native level crash, check debug log for details");
        boolean available1 = false;
        long platform1 = -1;
        long context1 = -1;
        long largestSizeMultiple1 = -1;
        long program1 = -1;
        createReturn:
        {
            LOGGER.info("Creating LWJGL memory stack");
            try (var stack = MemoryStack.create(64 * 1024 * 1024).push()) {
                var platformCount = stack.mallocInt(1);
                try {
                    LOGGER.info("Checking for LWJGL OpenCL classes");
                    clGetPlatformIDs(null, platformCount);
                } catch (NoClassDefFoundError e) {
                    LOGGER.info("Failed to load LWJGL OpenCL Classes");
                    break createReturn;
                }
                LOGGER.info("Success");
                // OpenCL classes are available, attempt to load platform info
                var platforms = platformCount.get(0);
                if (platforms == 0) {
                    LOGGER.info("No OpenCL platforms found, disabling");
                    break createReturn;
                }
                var platformIDs = stack.mallocPointer(platforms);
                LOGGER.info("Querying Platforms");
                checkReturnCode(clGetPlatformIDs(platformIDs, platformCount));
                LOGGER.info(String.format("%d Platforms found", platformCount.get(0)));
                
                // TODO: print platform names
                
                LOGGER.info("Querying Platform Devices");
                long selectedPlatform = 0;
                int selectedPlatformIndex = -1;
                int selectedGPUCount = 0;
                var deviceCount = stack.mallocInt(1);
                var platformNameSize = stack.mallocPointer(1);
                ByteBuffer platformName = null;
                for (int i = 0; i < platformCount.get(0); i++) {
                    LOGGER.info(String.format("Querying GPUs for platform %d", i));
                    long currentPlatform = platformIDs.get(i);
                    clGetPlatformInfo(currentPlatform, CL_PLATFORM_NAME, (ByteBuffer) null, platformNameSize);
                    if (platformName == null || platformName.capacity() < platformNameSize.get(0)) {
                        platformName = stack.malloc((int) platformNameSize.get(0));
                    }
                    clGetPlatformInfo(currentPlatform, CL_PLATFORM_NAME, platformName, null);
//                    platformName.put((int) (platformNameSize.get(0) - 1), (byte) 0);
                    LOGGER.info(String.format("Platform Name: %s", Charset.defaultCharset().decode(platformName)));
                    int returnCode = clGetDeviceIDs(currentPlatform, CL_DEVICE_TYPE_GPU, null, deviceCount);
                    if (returnCode == CL_DEVICE_NOT_FOUND) {
                        LOGGER.info(String.format("No GPUs found for platform %d", i));
                        if (selectedPlatform == 0) {
                            selectedPlatform = currentPlatform;
                            selectedPlatformIndex = i;
                            LOGGER.info(String.format("Platform %d currently selected", selectedPlatformIndex));
                        }
                        continue;
                    }
                    checkReturnCode(returnCode);
                    LOGGER.info(String.format("%d GPUs found for platform %d", deviceCount.get(0), i));
                    if (deviceCount.get(0) > selectedGPUCount) {
                        selectedPlatform = currentPlatform;
                        selectedGPUCount = deviceCount.get(0);
                        selectedPlatformIndex = i;
                        LOGGER.info(String.format("Platform %d currently selected", selectedPlatformIndex));
                    }
                }
                if (forcePlatform != -1) {
                    selectedPlatformIndex = forcePlatform;
                    selectedPlatform = platformIDs.get(forcePlatform);
                }
                if (selectedPlatform == 0) {
                    LOGGER.info("No platform selected");
                    break createReturn;
                }
                LOGGER.info(String.format("Selected platform %d", selectedPlatformIndex));
                platform1 = selectedPlatform;
                var contextProperties = stack.mallocPointer(4);
                contextProperties.put(0, CL_CONTEXT_PLATFORM);
                contextProperties.put(1, platform1);
                contextProperties.put(2, 0);
                contextProperties.put(3, 0);
                LOGGER.info("Creating context");
                context1 = clCreateContextFromType(contextProperties, CL_DEVICE_TYPE_ALL, null, 0, (IntBuffer) null);
                if (context1 == 0) {
                    break createReturn;
                }
                LOGGER.info("Getting context device info");
                clGetContextInfo(context1, CL_CONTEXT_NUM_DEVICES, deviceCount, null);
                var devicesBB = stack.malloc(deviceCount.get(0) * 8);
                clGetContextInfo(context1, CL_CONTEXT_DEVICES, devicesBB, null);
                var devicesLB = devicesBB.asLongBuffer();
                var intBuffer = stack.mallocInt(1);
                var longBuffer = stack.mallocLong(1);
                LOGGER.info("Getting devices");
                for (int i = 0; i < deviceCount.get(0); i++) {
                    long device = devicesLB.get(i);
                    devices.add(device);
                    clGetDeviceInfo(device, CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE, longBuffer, null);
                    final long sizeMultiple = longBuffer.get(0);
                    sizeMultiples.add(sizeMultiple);
                    if (sizeMultiple > largestSizeMultiple1) {
                        largestSizeMultiple1 = sizeMultiple;
                    }
                    clGetDeviceInfo(device, CL_DEVICE_MAX_WORK_GROUP_SIZE, longBuffer, null);
                    maxWorkGroupSizes.add(longBuffer.get(0));
                }
                LOGGER.info("Creating program");
                final String programSource;
                LOGGER.info("Reading file");
                try (InputStream in = CLUtil.class.getResourceAsStream("/opencl/reactorsim.cl"); BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(in)))) {
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                        builder.append('\n');
                    }
                    programSource = builder.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                    break createReturn;
                }
                LOGGER.info("Creating CL program");
                program1 = clCreateProgramWithSource(context1, programSource, intBuffer);
                checkReturnCode(intBuffer.get(0));
                LOGGER.info("Building CL program");
                int returnCode = clBuildProgram(program1, null, String.format("-DMAX_RAY_STEPS=%d " + (debug ? "-cl-opt-disable" : "-cl-fast-relaxed-math"), Config.CONFIG.Reactor.IrradiationDistance * 2), null, 0);
                if (returnCode == CL_BUILD_PROGRAM_FAILURE) {
                    var pointerBuf = stack.mallocPointer(1);
                    for (Long device : devices) {
                        clGetProgramBuildInfo(program1, device, CL_PROGRAM_BUILD_LOG, (ByteBuffer) null, pointerBuf);
                        ByteBuffer stringBuf = stack.malloc((int) pointerBuf.get(0));
                        clGetProgramBuildInfo(program1, device, CL_PROGRAM_BUILD_LOG, stringBuf, pointerBuf);
                        LOGGER.info(Charset.defaultCharset().decode(stringBuf));
                    }
                }
                checkReturnCode(returnCode);
            }
            LOGGER.info("CL loading complete");
            available1 = true;
        }
        available = available1;
        platform = platform1;
        context = context1;
        largestSizeMultiple = largestSizeMultiple1;
        program = program1;
        if (!available) {
            LOGGER.info("OpenCL acceleration not available");
        }
    }
    
    public static void shutdown() {
        if (!available) {
            return;
        }
        clReleaseContext(context);
    }
    
    static void checkReturnCode(int code) {
        switch (code) {
            case CL_SUCCESS -> {
            }
            case CL_DEVICE_NOT_FOUND -> throw new IllegalStateException("CL_DEVICE_NOT_FOUND");
            case CL_DEVICE_NOT_AVAILABLE -> throw new IllegalStateException("CL_DEVICE_NOT_AVAILABLE");
            case CL_COMPILER_NOT_AVAILABLE -> throw new IllegalStateException("CL_COMPILER_NOT_AVAILABLE");
            case CL_MEM_OBJECT_ALLOCATION_FAILURE -> throw new IllegalStateException("CL_MEM_OBJECT_ALLOCATION_FAILURE");
            case CL_OUT_OF_RESOURCES -> throw new IllegalStateException("CL_OUT_OF_RESOURCES");
            case CL_OUT_OF_HOST_MEMORY -> throw new IllegalStateException("CL_OUT_OF_HOST_MEMORY");
            case CL_PROFILING_INFO_NOT_AVAILABLE -> throw new IllegalStateException("CL_PROFILING_INFO_NOT_AVAILABLE");
            case CL_MEM_COPY_OVERLAP -> throw new IllegalStateException("CL_MEM_COPY_OVERLAP");
            case CL_IMAGE_FORMAT_MISMATCH -> throw new IllegalStateException("CL_IMAGE_FORMAT_MISMATCH");
            case CL_IMAGE_FORMAT_NOT_SUPPORTED -> throw new IllegalStateException("CL_IMAGE_FORMAT_NOT_SUPPORTED");
            case CL_BUILD_PROGRAM_FAILURE -> throw new IllegalStateException("CL_BUILD_PROGRAM_FAILURE");
            case CL_MAP_FAILURE -> throw new IllegalStateException("CL_MAP_FAILURE");
            case CL_INVALID_VALUE -> throw new IllegalStateException("CL_INVALID_VALUE");
            case CL_INVALID_DEVICE_TYPE -> throw new IllegalStateException("CL_INVALID_DEVICE_TYPE");
            case CL_INVALID_PLATFORM -> throw new IllegalStateException("CL_INVALID_PLATFORM");
            case CL_INVALID_DEVICE -> throw new IllegalStateException("CL_INVALID_DEVICE");
            case CL_INVALID_CONTEXT -> throw new IllegalStateException("CL_INVALID_CONTEXT");
            case CL_INVALID_QUEUE_PROPERTIES -> throw new IllegalStateException("CL_INVALID_QUEUE_PROPERTIES");
            case CL_INVALID_COMMAND_QUEUE -> throw new IllegalStateException("CL_INVALID_COMMAND_QUEUE");
            case CL_INVALID_HOST_PTR -> throw new IllegalStateException("CL_INVALID_HOST_PTR");
            case CL_INVALID_MEM_OBJECT -> throw new IllegalStateException("CL_INVALID_MEM_OBJECT");
            case CL_INVALID_IMAGE_FORMAT_DESCRIPTOR -> throw new IllegalStateException("CL_INVALID_IMAGE_FORMAT_DESCRIPTOR");
            case CL_INVALID_IMAGE_SIZE -> throw new IllegalStateException("CL_INVALID_IMAGE_SIZE");
            case CL_INVALID_SAMPLER -> throw new IllegalStateException("CL_INVALID_SAMPLER");
            case CL_INVALID_BINARY -> throw new IllegalStateException("CL_INVALID_BINARY");
            case CL_INVALID_BUILD_OPTIONS -> throw new IllegalStateException("CL_INVALID_BUILD_OPTIONS");
            case CL_INVALID_PROGRAM -> throw new IllegalStateException("CL_INVALID_PROGRAM");
            case CL_INVALID_PROGRAM_EXECUTABLE -> throw new IllegalStateException("CL_INVALID_PROGRAM_EXECUTABLE");
            case CL_INVALID_KERNEL_NAME -> throw new IllegalStateException("CL_INVALID_KERNEL_NAME");
            case CL_INVALID_KERNEL_DEFINITION -> throw new IllegalStateException("CL_INVALID_KERNEL_DEFINITION");
            case CL_INVALID_KERNEL -> throw new IllegalStateException("CL_INVALID_KERNEL");
            case CL_INVALID_ARG_INDEX -> throw new IllegalStateException("CL_INVALID_ARG_INDEX");
            case CL_INVALID_ARG_VALUE -> throw new IllegalStateException("CL_INVALID_ARG_VALUE");
            case CL_INVALID_ARG_SIZE -> throw new IllegalStateException("CL_INVALID_ARG_SIZE");
            case CL_INVALID_KERNEL_ARGS -> throw new IllegalStateException("CL_INVALID_KERNEL_ARGS");
            case CL_INVALID_WORK_DIMENSION -> throw new IllegalStateException("CL_INVALID_WORK_DIMENSION");
            case CL_INVALID_WORK_GROUP_SIZE -> throw new IllegalStateException("CL_INVALID_WORK_GROUP_SIZE");
            case CL_INVALID_WORK_ITEM_SIZE -> throw new IllegalStateException("CL_INVALID_WORK_ITEM_SIZE");
            case CL_INVALID_GLOBAL_OFFSET -> throw new IllegalStateException("CL_INVALID_GLOBAL_OFFSET");
            case CL_INVALID_EVENT_WAIT_LIST -> throw new IllegalStateException("CL_INVALID_EVENT_WAIT_LIST");
            case CL_INVALID_EVENT -> throw new IllegalStateException("CL_INVALID_EVENT");
            case CL_INVALID_OPERATION -> throw new IllegalStateException("CL_INVALID_OPERATION");
            case CL_INVALID_BUFFER_SIZE -> throw new IllegalStateException("CL_INVALID_BUFFER_SIZE");
            case CL_INVALID_GLOBAL_WORK_SIZE -> throw new IllegalStateException("CL_INVALID_GLOBAL_WORK_SIZE");
            default -> throw new IllegalStateException("Unknown CL Error: " + code);
        }
    }
}
