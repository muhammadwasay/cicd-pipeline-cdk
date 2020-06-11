package io.mwk;

import software.amazon.awscdk.core.App;

public class CicdPipelineCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        //TwitchMicroserviceCdkStack twitchMicroserviceCdkStack = new TwitchMicroserviceCdkStack(app, "TwitchMicroserviceCdkStack");
        new CicdPipelineCdkStack(app, "CicdPipelineCdkStack", null);

        app.synth();
    }
}
