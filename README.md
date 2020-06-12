# Demo CI/CD pipeline

* This demo pipeline is build using [AWS CodePipeline](https://aws.amazon.com/codepipeline/) and [AWS Cloud Development Kit](https://aws.amazon.com/cdk/). It uses "Infrastructure as code" and "Declarative style programming" approach.
* Supports continuous Integration using [AWS CodeBuild](https://aws.amazon.com/codebuild/) and [GitHub](https://github.com/). 
* Continuous Delivery of container image to [AWS ECR](https://aws.amazon.com/ecr/) repository.
* And continuous Deployment of containerized microservices to [AWS EKS](https://aws.amazon.com/eks/) using [AWS Cloud​Formation](https://aws.amazon.com/cloudformation/).

## Pipeline stages
### Stage 1 - Source : 
* Checkout code from GitHub repo 1 (java springboot code defining microservice), GitHub repo 2 (java aws cdk code defining Kubernetes resource of microservices). 
* It uses [AWS S3](https://aws.amazon.com/s3/) to store the checked out code.
### Stage 2 - spring-code-build : 
* Compile, test (java springboot code defining microservice). Create Docker image and push the image and tag to [AWS ECR](https://aws.amazon.com/ecr/) repository.
* It uses [AWS CodeBuild](https://aws.amazon.com/codebuild/) environment to compile the code and prepare the container image.
### Stage 3 - cdk-code-build : 
* Compile, test (java aws cdk code defining kubernetes resource of microservices) and using the image tag build in the previous stage, produce the AWS CloudFormation template representing kubernetes resource of microservices.
* It uses [AWS CodeBuild](https://aws.amazon.com/codebuild/) environment and AWS CDK to synthesize the couldFormation template.
### Stage 4 - cdkDeploy : 
* Invoke AWS CloudFormation and pass the AWS CloudFormation template produced in the previous stage in order to deploy/update the micro-service running inside the kubernetes cluster.