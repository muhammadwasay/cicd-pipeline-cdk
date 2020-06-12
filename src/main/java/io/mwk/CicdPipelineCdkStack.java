package io.mwk;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageOptions;
import software.amazon.awscdk.services.codepipeline.actions.CloudFormationCreateUpdateStackAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubTrigger;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;

import java.util.Arrays;
import java.util.Map;

import static java.util.Arrays.asList;

public class CicdPipelineCdkStack extends Stack {
    public CicdPipelineCdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CicdPipelineCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var pipeline = Pipeline.Builder.create(this, "TwitchMs")
                .pipelineName("TwichMsPipeline")
                .build();

        /* SOURCE */

        var springCodeSourceOutput = new Artifact();
        var cdkCodeSourceOutput = new Artifact();
        var springCodeBuildOutput = new Artifact("spring-codeoutput");
        var cdkCodeBuildOutput = new Artifact("cdk-codeoutput");

        var springCodeSourceAction = GitHubSourceAction.Builder.create()
                .actionName("twitch-spring-microservice-source")
                .owner("muhammadwasay")
                .repo("twitch-spring-microservice")
                .oauthToken(SecretValue.secretsManager("arn:aws:secretsmanager:us-east-1:481137230390:secret:mwk-github-repo-access-token-JifB2P",
                        SecretsManagerSecretOptions.builder().jsonField("token").build()))
                .output(springCodeSourceOutput)
                .branch("master")
                .trigger(GitHubTrigger.WEBHOOK)
                .variablesNamespace("springSource")
                .build();

        var cdkCodeSourceAction = GitHubSourceAction.Builder.create()
                .actionName("kubernetes-resource-source")
                .owner("muhammadwasay")
                .repo("eks-cdk-demo")
                .oauthToken(SecretValue.secretsManager("arn:aws:secretsmanager:us-east-1:481137230390:secret:mwk-github-repo-access-token-JifB2P",
                        SecretsManagerSecretOptions.builder().jsonField("token").build()))
                .output(cdkCodeSourceOutput)
                .branch("master")
                .trigger(GitHubTrigger.NONE)
                .variablesNamespace("kubernetesSource")
                .build();

        var sourceStage = StageOptions
                .builder()
                .stageName("Source")
                .actions(asList(springCodeSourceAction, cdkCodeSourceAction))
                .build();

        /* Spring BUILD */

        var springCodeBuildEnvironment = new BuildEnvironment.Builder()
                .buildImage(LinuxBuildImage.AMAZON_LINUX_2_3)
                .computeType(ComputeType.SMALL)
                .privileged(true)
                .environmentVariables(Map.of(
                        "AWS_DEFAULT_REGION",BuildEnvironmentVariable.builder()
                                .type(BuildEnvironmentVariableType.PLAINTEXT)
                                .value("us-east-1")
                                .build(),
                        "AWS_ACCOUNT_ID",BuildEnvironmentVariable.builder()
                                .type(BuildEnvironmentVariableType.PLAINTEXT)
                                .value("481137230390")
                                .build(),
                        "IMAGE_TAG",BuildEnvironmentVariable.builder()
                                .type(BuildEnvironmentVariableType.PLAINTEXT)
                                .value("latest")
                                .build(),
                        "IMAGE_REPO_NAME",BuildEnvironmentVariable.builder()
                                .type(BuildEnvironmentVariableType.PLAINTEXT)
                                .value("java-twitch-integration")
                                .build()))
                .build();

        PipelineProject springCodeBuildProject = PipelineProject.Builder.create(this,"spring-codebuild")
                .projectName("twitch-spring-microservice-codebuild")
                .environment(springCodeBuildEnvironment)
                .build();

        springCodeBuildProject.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(asList("ecr:*", "cloudtrail:LookupEvents"))
                .resources(asList("*"))
                .build());

        var springBuildAction = CodeBuildAction.Builder.create()
                .actionName("CodeBuild")
                .project(springCodeBuildProject)
                .input(springCodeSourceOutput)
                .outputs(asList(springCodeBuildOutput))
                .build();



        var springCodeBuildStage = StageOptions
                .builder()
                .stageName("spring-code-build")
                .actions(asList(springBuildAction))
                .build();

        /* CDK BUILD */

        var springCodeArtifactPath = springCodeBuildOutput.atPath("pom.properties");
        System.out.println("springCodeArtifactPath"+springCodeArtifactPath.getFileName()+"<>"+springCodeArtifactPath.getLocation()+"<>"+springCodeArtifactPath.toString());

        var cdkCodeBuildEnvironment = new BuildEnvironment.Builder()
                .buildImage(LinuxBuildImage.AMAZON_LINUX_2_3)
                .computeType(ComputeType.SMALL)
                .privileged(true)
                .environmentVariables(Map.of(
                        "TWITCH_MS_IMAGE_TAG",BuildEnvironmentVariable.builder()
                                .type(BuildEnvironmentVariableType.PLAINTEXT)
                                .value("1.0-RELEASE") //todo
                                .build(),
                        "SPRING_CODE_ARTIFACT_PATH",BuildEnvironmentVariable.builder()
                                .type(BuildEnvironmentVariableType.PLAINTEXT)
                                .value(springCodeArtifactPath.getLocation())
                                .build()))
                .build();

        PipelineProject cdkBuildProject = PipelineProject.Builder.create(this, "cdk-codebuild")
                .projectName("cdkBuildProject")
                .environment(cdkCodeBuildEnvironment)
                .build();

        var cdkBuildAction = CodeBuildAction.Builder.create()
                .actionName("cdkBuildAction")
                .project(cdkBuildProject)
                .input(cdkCodeSourceOutput)
                .outputs(Arrays.asList(cdkCodeBuildOutput))
                .build();

        var cdkCodeBuildStage = StageOptions
                .builder()
                .stageName("cdk-code-build")
                .actions(asList(cdkBuildAction))
                .build();

        /* DEPLOY */

        var deployAction = CloudFormationCreateUpdateStackAction.Builder.create()
                .actionName("Kubernetes_Resource_Deploy")
                .templatePath(cdkCodeBuildOutput.atPath("EksCdkStack.template.json"))
                .adminPermissions(true)
                .stackName("KubernetesResourceDeploymentStack")
                .build();

        var deployStage = StageOptions
                .builder()
                .stageName("cdkDeploy")
                .actions(asList(deployAction))
                .build();

        pipeline.addStage(sourceStage);
        pipeline.addStage(springCodeBuildStage);
        pipeline.addStage(cdkCodeBuildStage);
        pipeline.addStage(deployStage);
    }
}
