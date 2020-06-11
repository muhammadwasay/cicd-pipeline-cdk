package io.mwk;

import software.amazon.awscdk.core.App;

public class CicdPipelineCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        //new KubernetesResourceCdkStack(app, "twitch-ms-k8s-resource-stack");
        new CicdPipelineCdkStack(app, "cicd-pipeline-stack");

        app.synth();
    }
}
