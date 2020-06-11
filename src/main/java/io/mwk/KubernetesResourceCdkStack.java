package io.mwk;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.eks.Cluster;
import software.amazon.awscdk.services.eks.ClusterAttributes;
import software.amazon.awscdk.services.eks.KubernetesResource;

import java.util.Map;

import static java.util.Arrays.asList;

public class KubernetesResourceCdkStack extends Stack {

    private KubernetesResource kubernetesResource;

    public KubernetesResourceCdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public KubernetesResourceCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var imageTag = "";
        var imageTagEnvProperty = "TWITCH_MS_IMAGE_TAG";
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet()) {
            if(envName.equals(imageTagEnvProperty)){
                imageTag = env.get(envName);
            }
        }
        System.out.format("%s=%s%n", imageTagEnvProperty, env.get(imageTag));

        var demoEksClusterARN = "arn:aws:eks:us-east-1:481137230390:cluster/demoekscluster1190CA3C-001efee7020b42fd930dc360002a46d8";
        var latestTwitchMicroserviceImage = "481137230390.dkr.ecr.us-east-1.amazonaws.com/java-twitch-integration:latest";
        var twitchAppClientId = "djso1368ggr0fmbxgpijy0pxhsw712";
        var twitchAppClientSecret = "gp807tyjhao85g86z3269qvq06g6zw";
        /*String twitchAppClientSecret = SecretValue.secretsManager("arn:aws:secretsmanager:us-east-1:481137230390:secret:twitchAppClientSecret-BoUOUB",
                SecretsManagerSecretOptions.builder()
                        .jsonField("appsecret")
                        .build())
                .toString();*/

        Map<String, String> appLabel = Map.of("app", "twitch-ms");

        Object deployment = Map.of(
                "apiVersion", "apps/v1",
                "kind", "Deployment",
                "metadata", Map.of("name", "twitch-ms"),
                "spec", Map.of(
                        "replicas", 1,
                        "selector", Map.of("matchLabels", appLabel),
                        "template", Map.of(
                                "metadata", Map.of("labels", appLabel),
                                "spec", Map.of(
                                        "containers", asList(Map.of(
                                                "name", "twitch-ms",
                                                "image", latestTwitchMicroserviceImage,
                                                "env",asList(Map.of("name","TWITCH_CLIENT_ID",
                                                        "value", twitchAppClientId),
                                                        Map.of("name","TWITCH_CLIENT_SECRET",
                                                                "value", twitchAppClientSecret)),
                                                "ports", asList(Map.of("containerPort", 8080))))))));

        Object service = Map.of(
                "apiVersion", "v1",
                "kind", "Service",
                "metadata", Map.of("name", "twitch-ms"),
                "spec", Map.of(
                        "type", "LoadBalancer",
                        "ports", asList(Map.of("port", 80, "targetPort", 8080)),
                        "selector", appLabel));

        var cluster = (Cluster) Cluster.fromClusterAttributes(this,"demo-eks-cluster", ClusterAttributes.builder().clusterArn(demoEksClusterARN).build());

        cluster.addResource("twitch-microservice", deployment, service);

        /*kubernetesResource = KubernetesResource.Builder.create(this, "twitch-microservice")
                .cluster(cluster)
                .manifest(asList(deployment, service))
                .build();*/
    }

    public KubernetesResource getKubernetesResource() {
        return kubernetesResource;
    }
}
