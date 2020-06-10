package com.myorg;

import software.amazon.awscdk.core.App;

import java.util.Arrays;

public class CicdPipelineCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        new CicdPipelineCdkStack(app, "CicdPipelineCdkStack");

        app.synth();
    }
}
