plugins {
    kotlin("jvm")
    id("test-inputs-check")
}

dependencies {
    api(project(":compiler:cli-base"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:ir.inline"))
    api(project(":compiler:ir.serialization.common"))

    compileOnly(intellijCore())

    testImplementation(kotlinTest("junit"))
    testImplementation(project(":compiler:tests-common"))
    testImplementation(project(":compiler:tests-common-new"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
