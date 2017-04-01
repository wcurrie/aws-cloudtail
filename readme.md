## Visualize AWS CloudTrail logs

As a sequence diagram. Using aws-cloudtrail-processing-library to read CloudTrail logs.

### Instructions for tail -f style

Follow the AWS [instructions](http://docs.aws.amazon.com/awscloudtrail/latest/userguide/send-cloudtrail-events-to-cloudwatch-logs.html) to enable CloudTrail to forward logs CloudWatch Logs.
You can use the watch-cloudtrail.template cloudformation template to create the log group and role. The use to update the trail (created manually).

    aws cloudtrail update-trail --name $trail_name --cloud-watch-logs-log-group-arn $log_group_arn --cloud-watch-logs-role-arn $role_arn

If you prefer to do enable cloudtrail to cloudwatch using terraform, threatstack have a [post](https://blog.threatstack.com/incorporating-aws-security-best-practices-into-terraform-design) showing how.    
    
Run main() in CloudWatchRead.kt. From intellij or whatever.

### config.properties

Should look like:

    bucket_name=<name of bucket cloudtrail is writing logs to>
    key_prefix=Eg: AWSLogs/123456789012/CloudTrail/us-west-2/2017/02/01/
    exclusion_regex=Eg: SomeService|10\\.0\\.0\\.42

## Deploying as a Lambda

Following [serverless deploy](http://docs.aws.amazon.com/lambda/latest/dg/serverless-deploy-wt.html#serverless-deploy):

    aws s3 mb s3://bucket-name --region region
    
    aws cloudformation package \
       --template-file sam.yaml \
       --output-template-file serverless-output.yaml \
       --s3-bucket s3-bucket-name
       
    aws cloudformation deploy \
      --template-file serverless-output.yaml \
      --stack-name new-stack-name \
      --capabilities CAPABILITY_IAM

### TODO

* Navigation arrow on each participant to scroll to next/previous events?
* Tail -f (follow mode)
* Add auth for deployment with api gateway. Eg [IAM auth with api gateway](https://aws.amazon.com/premiumsupport/knowledge-center/iam-authentication-api-gateway/)
* Handle api gateway max response size 10mb [limits](http://docs.aws.amazon.com/apigateway/latest/developerguide/limits.html). Need 302 redirection to s3 object? 