package io.mwk;

import software.amazon.awscdk.core.App;

public class CicdPipelineCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        //new KubernetesResourceCdkStack(app, "KubernetesResourceCdkStack");
        new CicdPipelineCdkStack(app, "CicdPipelineCdkStack");

        app.synth();
    }
}
