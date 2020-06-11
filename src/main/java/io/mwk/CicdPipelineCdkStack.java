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
import software.amazon.awscdk.services.eks.KubernetesResource;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;

import java.util.Arrays;
import java.util.HashMap;
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
        var cdkBuildOutput = new Artifact("CdkBuildOutput");

        var springCodeSourceAction = GitHubSourceAction.Builder.create()
                .actionName("twitch-spring-microservice-source")
                .owner("muhammadwasay")
                .repo("twitch-spring-microservice")
                .oauthToken(SecretValue.secretsManager("arn:aws:secretsmanager:us-east-1:481137230390:secret:mwk-github-repo-access-token-JifB2P",
                        SecretsManagerSecretOptions.builder().jsonField("token").build()))
                .output(springCodeSourceOutput)
                .branch("master")
                .trigger(GitHubTrigger.WEBHOOK)
                .variablesNamespace("MyNamespace")
                .build();

        var cdkCodeSourceAction = GitHubSourceAction.Builder.create()
                .actionName("kubernetes-resource-source")
                .owner("muhammadwasay")
                .repo("cicd-pipeline-cdk")
                .oauthToken(SecretValue.secretsManager("arn:aws:secretsmanager:us-east-1:481137230390:secret:mwk-github-repo-access-token-JifB2P",
                        SecretsManagerSecretOptions.builder().jsonField("token").build()))
                .output(cdkCodeSourceOutput)
                .branch("master")
                .trigger(GitHubTrigger.NONE)
                .variablesNamespace("MyNamespace")
                .build();

        var sourceStage = StageOptions
                .builder()
                .stageName("Source")
                .actions(asList(springCodeSourceAction, cdkCodeSourceAction))
                .build();

        /* BUILD */

        var buildEnvironment = new BuildEnvironment.Builder()
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

        PipelineProject codeBuildProject = PipelineProject.Builder.create(this,"twitchms-codebuild")
                .projectName("twitch-spring-microservice-codebuild")
                .environment(buildEnvironment)
                .build();

        codeBuildProject.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(asList("ecr:*", "cloudtrail:LookupEvents"))
                .resources(asList("*"))
                .build());

        PipelineProject cdkBuildProject = PipelineProject.Builder.create(this, "cdkBuildProject")
                .projectName("cdkBuildProject")
                .environment(buildEnvironment)
                .build();

        var buildAction = CodeBuildAction.Builder.create()
                .actionName("CodeBuild")
                .project(codeBuildProject)
                .input(springCodeSourceOutput)
                .outputs(asList(new Artifact()))
                .build();

        var cdkBuildAction = CodeBuildAction.Builder.create()
                .actionName("cdkBuildAction")
                .project(cdkBuildProject)
                .input(cdkCodeSourceOutput)
                .outputs(Arrays.asList(cdkBuildOutput))
                .build();

        var buildStage = StageOptions
                .builder()
                .stageName("build")
                .actions(asList(buildAction, cdkBuildAction))
                .build();

        /* DEPLOY */

        var deployAction = CloudFormationCreateUpdateStackAction.Builder.create()
                .actionName("Kubernetes_Resource_Deploy")
                .templatePath(cdkBuildOutput.atPath("KubernetesResourceCdkStack.template.json"))
                .adminPermissions(true)
                .stackName("KubernetesResourceDeploymentStack")
                .build();

        var deployStage = StageOptions
                .builder()
                .stageName("cdkDeploy")
                .actions(asList(deployAction))
                .build();

        pipeline.addStage(sourceStage);
        pipeline.addStage(buildStage);
        pipeline.addStage(deployStage);
    }
}
