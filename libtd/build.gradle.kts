//
// Copyright Aliaksei Levin (levlam@telegram.org), Arseny Smirnov (arseny30@gmail.com) 2014-2021
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
plugins {
    id("com.android.library")
}

android {
    namespace = "com.example.td"
    compileSdk = 34

    sourceSets.getByName("main") {
        jniLibs.srcDir("src/main/libs")
    }

    defaultConfig {
        minSdk = 10
        version = 1
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt")
        }
    }

    lint {
        // use this line to check all rules except those listed
        disable += "InvalidPackage"
    }
}

dependencies {
    implementation("com.android.support:support-annotations:28.0.0")
}
