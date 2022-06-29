#ifndef MAX_RAY_STEPS
#define MAX_RAY_STEPS 4
#define global
#define kernel
#define local
#define constant
#endif

#define TOKENPASTE(x, y) x ## y
#define TOKENPASTE2(x, y) TOKENPASTE(x, y)
#define STRUCT typedef struct __attribute__ ((packed)) TOKENPASTE2(_STRUCT, __COUNTER__)

#ifndef TTL
#define TTL (4)
#endif
#define ROD_AREA_WIDTH (TTL * 2 + 1)

STRUCT {
    int xSize, ySize, zSize;

    float FuelAbsorptionTemperatureCoefficient;
    float InitialHardness;

    float FEPerRadiationUnit;
    float FuelAbsorptionCoefficient;
    float FuelModerationFactor;
    float FuelHardnessMultiplier;
} ReactorInfo;

STRUCT {
    float absorption;
    float heatEfficiency;
    float moderation;
} Moderator;

STRUCT {
    int startingStep;
    int stepCount;
} Ray;

STRUCT {
    float length;
    int x, y, z;
} RayStep;

STRUCT {
    float initialIntensity;
} RodRayInfo;

STRUCT {
    float fuelRFAdded;
    float fuelRadAdded;
    float caseRFAdded;
} RayBurnInfo;

RayBurnInfo raySimFloat1(
        global char* moderatorIndices, local Moderator* moderators,
        local float* insertions,
        local RayStep* raySteps, const int rayStepCount,
        local RodRayInfo* rodRayInfo, local ReactorInfo* reactorInfo,
        const int3 reactorPos
) {
    RayBurnInfo burnInfo;
    burnInfo.caseRFAdded = 0;
    burnInfo.fuelRFAdded = 0;
    burnInfo.fuelRadAdded = 0;
    float hardness = reactorInfo->InitialHardness;
    float intensity = rodRayInfo->initialIntensity;

    for (int i = 0; i < rayStepCount && i < MAX_RAY_STEPS; i++) {
        RayStep step = raySteps[i];
        const int currentX = reactorPos.x + step.x;
        const int currentZ = reactorPos.z + step.z;
        if (currentX < 0 || currentX >= reactorInfo->xSize || currentZ < 0 || currentZ >= reactorInfo->zSize) {
            // all work items in this workgroup will hit this at the same time
            break;
        }
        const float stepLength = step.length;
        // Y level isn't guaranteed for all work items, so, i need to safely no-op this
        const int currentY = reactorPos.y + step.y;
        const bool inRangeY = currentY >= 0 && currentY < reactorInfo->ySize;
        const float NoOpMultiplier = (float) inRangeY;
        const int moderatorIndexIndex = ((((currentX * reactorInfo->zSize) + currentZ)) * reactorInfo->ySize) + (currentY * NoOpMultiplier);
        // yes double indirection, but the actual Moderator array is much smaller and should be cached, also makes writing it on the CPU less intensive
        const char moderatorIndex = moderatorIndices[moderatorIndexIndex];
        if (moderatorIndex != 0) {
            const Moderator moderator = moderators[moderatorIndex];
            // not a fuel rod
            const float radAbsorbed = (intensity * moderator.absorption * (1.0f - hardness) * stepLength);
            // end values of these done matter, so, writing anyway is fine
            intensity = max(0.0f, intensity - radAbsorbed);
            hardness /= (((moderator.moderation) * stepLength) + 1.0f);
            burnInfo.caseRFAdded += moderator.heatEfficiency * radAbsorbed * reactorInfo->FEPerRadiationUnit * NoOpMultiplier;
        } else {
            // not not fuel rod
            const int rodX = step.x + TTL;
            const int rodZ = step.z + TTL;
            const int rodIndex = rodX * ROD_AREA_WIDTH + rodZ;
            const float controlRodInsertion = insertions[rodIndex]; // TODO: bench this against local memory

            const float baseAbsorption = reactorInfo->FuelAbsorptionTemperatureCoefficient * (1.0f - (hardness * reactorInfo->FuelHardnessMultiplier));

            const float scaledAbsorption = baseAbsorption * reactorInfo->FuelAbsorptionCoefficient * stepLength;

            const float controlRodBonus = (1.0f - scaledAbsorption) * controlRodInsertion * 0.5f;
            const float controlRodPenalty = scaledAbsorption * controlRodInsertion * 0.5f;

            const float radiationAbsorbed = (scaledAbsorption + controlRodBonus) * intensity;
            const float fertilityAbsorbed = (scaledAbsorption - controlRodPenalty) * intensity;

            const float fuelModerationFactor = reactorInfo->FuelModerationFactor + (reactorInfo->FuelModerationFactor * controlRodInsertion + controlRodInsertion);

            intensity = max(0.0f, intensity - (radiationAbsorbed));
            hardness /= ((fuelModerationFactor - 1.0f) * stepLength) + 1.0f;

            burnInfo.fuelRFAdded += radiationAbsorbed * reactorInfo->FEPerRadiationUnit * NoOpMultiplier;
            burnInfo.fuelRadAdded += fertilityAbsorbed * NoOpMultiplier;
        }
    }

    return burnInfo;
}

kernel void raySim(
    const int globalBaseRay, const int raysPerBatch,
    global char* moderatorIndices, global Moderator* moderatorsGlobal, local Moderator* moderatorsLocal, const int moderatorCount,
    global short2* controlRodPositions, global float* controlRodInsertions,  local float* localInsertions,
    global RodRayInfo* rodRayInfosGlobal, global ReactorInfo* reactorInfoGlobal,
    global Ray* rays, global RayStep* rayStepsGlobal,

    global RayBurnInfo* results
) {
    const int3 globalPos = (int3)(get_global_id(0), get_global_id(1), get_global_id(2));
    const int3 globalSize = (int3)(get_global_size(0), get_global_size(1), get_global_size(2));
    const int batchBaseRay = globalBaseRay + (raysPerBatch * globalPos.z);


    const int localY = get_local_id(1);
    local ReactorInfo reactorInfo;
    local RodRayInfo rodRayInfo;
    local short2 controlRodPos;

    {
        event_t rodPosCopy = async_work_group_copy(&controlRodPos, controlRodPositions + globalPos.x, 1, 0);
        event_t reactorInfoCopy = async_work_group_copy((local char*) &reactorInfo, (global char*) (reactorInfoGlobal), sizeof(ReactorInfo), 0);

        event_t events[ROD_AREA_WIDTH + 2];
        events[0] = async_work_group_copy((local char*) moderatorsLocal, (global char*) (moderatorsGlobal), sizeof(Moderator) * moderatorCount, 0);
        events[1] = async_work_group_copy((local char*) &rodRayInfo, (global char*) (rodRayInfosGlobal + globalPos.x), sizeof(RodRayInfo), 0);

        wait_group_events(1, &rodPosCopy);
        wait_group_events(1, &reactorInfoCopy);

//        wait_group_events(1, &events[0]);
//        wait_group_events(1, &events[1]);

        int usedEvents = 2;
        for (int i = 0; i < ROD_AREA_WIDTH; ++i) {
            int readOffset = controlRodPos.x * reactorInfo.zSize + controlRodPos.y - TTL;

            int writeOffset = 0;
            if (readOffset < 0) {
                writeOffset -= readOffset;
                readOffset = 0;
            }

            int size = ROD_AREA_WIDTH;
            size = clamp(size, 0, (reactorInfo.xSize * reactorInfo.zSize) - readOffset - 1);
            size = clamp(size, 0, ROD_AREA_WIDTH - writeOffset);

            writeOffset += i * ROD_AREA_WIDTH;

            if (size != 0) {
                events[usedEvents++] = async_work_group_copy(localInsertions + writeOffset, controlRodInsertions + readOffset, size, 0);
//                wait_group_events(1, &events[usedEvents - 1]);
            }
        }


        wait_group_events(usedEvents, events);
    }

    const int3 reactorPos = (int3)(controlRodPos.x, globalPos.y, controlRodPos.y);

    RayBurnInfo accumBurnInfo;
    accumBurnInfo.caseRFAdded = 0;
    accumBurnInfo.fuelRFAdded = 0;
    accumBurnInfo.fuelRadAdded = 0;

    local int rayStepCount;
    // maybe make this a kernel parameter?
    local RayStep raySteps[MAX_RAY_STEPS];

    for (int i = 0; i < raysPerBatch; ++i) {
        int currentRay = batchBaseRay + i;
        if (i + 1 < raysPerBatch) {
            prefetch((global char*) (rays + currentRay + 1), sizeof(Ray));
        }
        if (localY == 0) {
            Ray ray = rays[currentRay];
            rayStepCount = ray.stepCount;
            for (int j = 0; j < rayStepCount && j < MAX_RAY_STEPS; ++j) {
                raySteps[j] = rayStepsGlobal[ray.startingStep + j];
            }
        }
        barrier(CLK_LOCAL_MEM_FENCE);

        RayBurnInfo rayBurnInfo = raySimFloat1(moderatorIndices, moderatorsLocal, localInsertions, raySteps, rayStepCount, &rodRayInfo, &reactorInfo, reactorPos);

        accumBurnInfo.caseRFAdded += rayBurnInfo.caseRFAdded;
        accumBurnInfo.fuelRFAdded += rayBurnInfo.fuelRFAdded;
        accumBurnInfo.fuelRadAdded += rayBurnInfo.fuelRadAdded;
    }

    const int fuelRodLinearIndex = ((globalPos.x) * globalSize.z + globalPos.z) * globalSize.y + globalPos.y;
    results[fuelRodLinearIndex] = accumBurnInfo;
}

kernel void rayReduction(global RayBurnInfo* inputs, global RayBurnInfo* outputs, int reductionCount) {
    const int globalID = get_global_id(0);
    const int globalSize = get_global_size(0);
    RayBurnInfo output;
    output.caseRFAdded = 0;
    output.fuelRFAdded = 0;
    output.fuelRadAdded = 0;
    global RayBurnInfo* inputsOffset = inputs + globalID;
    for (int i = 0; i < reductionCount; i++) {
        const RayBurnInfo input = *inputsOffset;
        inputsOffset += globalSize;
        output.caseRFAdded += input.caseRFAdded;
        output.fuelRFAdded += input.fuelRFAdded;
        output.fuelRadAdded += input.fuelRadAdded;
    }
    outputs[globalID] = output;
}