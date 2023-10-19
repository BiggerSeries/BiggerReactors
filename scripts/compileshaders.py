import multiprocessing
import os
import sys

def processFile(command):
    print(command)
    os.system(command)
    pass

def walkPath(inputDir, outputDir):
    if not os.path.exists("glslc"):
        return []
    inputs = os.listdir(inputDir)
    if not os.path.exists(outputDir):
        os.mkdir(outputDir)
    inputs.sort()
    folders = []
    commands = []
    for file in inputs:
        fullFile = f"{inputDir}/{file}"
        if os.path.isdir(fullFile):
            folders.append(file)
            continue
        if not file.endswith(".comp"):
            continue
        outputFile = file + ".spv"
        commands.append(f"./glslc -fshader-stage=comp ./{inputDir}/{file} -o ./{outputDir}/{outputFile} -I ./include")
        pass
    for folder in folders:
        for command in walkPath(inputDir + "/" + folder, outputDir + "/" + folder):
            commands.append(command)
        pass
    return commands


if __name__ == '__main__':
    if os.path.abspath(".").endswith("scripts"):
        os.chdir("..")
    try:
        allCommands = walkPath("src/main/resources/biggerreactors/simulation/vk/glsl", "src/main/resources/biggerreactors/simulation/vk/spv")
        pool = multiprocessing.Pool(24)
        pool.map(processFile, allCommands)
    except Exception as e:
        print(e.__str__())
        sys.exit(-1)
        pass
    pass
