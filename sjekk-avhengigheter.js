const fs = require("fs");
const path = require("path");

async function* walk(dir, maxDepth) {
  for await (const d of await fs.promises.opendir(dir)) {
    const entry = path.join(dir, d.name);
    if (d.isDirectory() && maxDepth > 0) yield* walk(entry, maxDepth - 1);
    else if (d.isFile()) yield entry;
  }
}

async function findBuildFiles(path, maxDepth) {
  const buildFiles = [];
  for await (const p of walk(path, maxDepth)) {
    if (p.endsWith("/build.gradle.kts")) {
      buildFiles.push(p);
    }
  }
  return buildFiles;
}

async function findGithubWorkflows() {
  const workflows = [];
  for await (const path of walk("./.github/workflows", 0)) {
    if (path.endsWith(".yaml")) {
      workflows.push(path);
    }
  }
  return workflows;
}

const libDependencyRegex =
  /^\s*implementation\(project\(":libs:((?:\w|-)+)"\)\)\s*$/;
const workflowLibDep = /^\s*- libs\/((?:\w|-)+)\/\*\*\s*$/;

async function findUsedDependencies(filePath) {
  const file = await fs.promises.open(filePath);
  const deps = [];
  for await (const line of file.readLines()) {
    if (libDependencyRegex.test(line)) {
      deps.push(libDependencyRegex.exec(line)[1]);
    }
  }
  await file.close();
  return new Set(deps);
}

async function findCheckedDependencies(filePath) {
  const file = await fs.promises.open(filePath);
  const dependencies = [];
  for await (const line of file.readLines()) {
    if (workflowLibDep.test(line)) {
      dependencies.push(workflowLibDep.exec(line)[1]);
    }
  }
  await file.close();
  return new Set(dependencies);
}

async function checkDependencies(buildFile, workflows) {
  const [type, app] = buildFile.split("/");
  const workflowFilename = `${type.slice(0, 3)}-${app}.yaml`;
  const workflowFullPath = workflows.find((flow) =>
    flow.endsWith(workflowFilename),
  );
  if (!workflowFullPath) {
    // console.error(`Could not find ${workflowFilename} in list of workflows!`);
    return [true, app, []];
  }
  const usedDependendcies = await findUsedDependencies(buildFile);
  const checkedDependencies = await findCheckedDependencies(workflowFullPath);

  const missing = usedDependendcies.difference(checkedDependencies);
  if (missing.size === 0) {
    return [true, app, []];
  } else {
    return [false, app, [...missing]];
  }
}

async function listUtManglendeAvhengigheter() {
  const appBuildFiles = await findBuildFiles("./apps", 1);
  const libBuildFiles = await findBuildFiles("./libs", 1);
  const workflows = await findGithubWorkflows();

  const appDependencies = await Promise.all(
    appBuildFiles.map((file) => checkDependencies(file, workflows)),
  );
  const missingAppDependencies = appDependencies.filter(([ok]) => !ok);
  if (missingAppDependencies.length > 0) {
    console.log(
      `${missingAppDependencies.length} apper mangler avhengigheter i github workflow:`,
    );
    missingAppDependencies.forEach(([, app, missing]) => {
      console.log(`${app} mangler følgende avhengigheter:`);
      missing.forEach((lib) => console.log(`libs/${lib}/**`));
      console.log();
    });
    console.log();
  } else {
    console.log(
      "Ingen apps mangler avhengigheter i github workflows! Bra jobba!",
    );
  }

  const libDependencies = await Promise.all(
    libBuildFiles.map((file) => checkDependencies(file, workflows)),
  );
  const missingLibDependencies = libDependencies.filter(([ok]) => !ok);
  if (missingLibDependencies.length > 0) {
    console.log(
      `${missingLibDependencies.length} libs mangler avhengigheter i github workflow:`,
    );
    missingLibDependencies.forEach(([, app, missing]) => {
      console.log(`${app} mangler følgende avhengigheter:`);
      missing.forEach((lib) => console.log(`libs/${lib}/**`));
      console.log();
    });
    console.log();
  } else {
    console.log(
      "Ingen libs mangler avhengigheter i github workflows! Bra jobba!",
    );
  }
}

listUtManglendeAvhengigheter().then();
